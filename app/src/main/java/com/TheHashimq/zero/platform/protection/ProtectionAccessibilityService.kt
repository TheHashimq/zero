/*
 * MODULE: app (accessibility adapter for protection)
 * PURPOSE: Checks the app in front whenever it opens or changes, redirects blocked apps Home, and removes
 *          protected app cards from Recents when that screen is open.
 * TAKES: Window-state and content-change events, screen-off broadcasts, and a periodic tick.
 * GIVES: Global Home and Recents actions, and swipes on protected app cards in Recents.
 * OWNS: The foreground app, the throttle timestamp, the screen-off receiver, and the tick handler.
 * DOES NOT OWN: Protection decisions, persistence, or events.
 * DEPENDENCIES: feature-protection-runtime, feature-protection, ZeroApplication, Android accessibility APIs.
 * CONTRACT: A new window and any content change inside the app in front run the same check. Content checks are
 *           throttled to one per 300 ms. Text is read only on installer and Settings windows, checked in memory,
 *           and never stored or logged. Zero and the home screen are never removed from Recents.
 *           Protected cards are not removed during Grace.
 * FAILURE BEHAVIOR: If an automatic card isn't found, nothing is swiped and the user still goes Home.
 *                   Manual Recents scans leave the current screen unchanged when no card is found.
 *                   Persistence failures are logged and retried on the next event or tick.
 *                   A missing coordinator means no action.
 */
package com.TheHashimq.zero.platform.protection

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.ContextCompat
import com.TheHashimq.zero.ZeroApplication
import zero.feature.protection.ProtectionAction
import zero.feature.protection.ProtectionState
import zero.feature.protection.SelfScreen
import zero.feature.protection.SelfScreenClassifier
import zero.feature.protection.runtime.ProtectionCoordinator
import zero.foundation.persistence.PersistenceException

/** Thin Android adapter for the protection coordinator. */
class ProtectionAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var receiverRegistered = false
    private var foregroundPackage: String? = null
    private var foregroundClass: String? = null
    private var lastContentCheckMillis = 0L
    private var lastRecentsMillis = 0L
    private var lastRecentsScanMillis = 0L

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                coordinator()?.let { c -> runSafely("screen-off") { c.onScreenOff() } }
            }
        }
    }

    private val ticker = object : Runnable {
        override fun run() {
            coordinator()?.let { c -> runSafely("tick") { c.tick() } }
            handler.postDelayed(this, TICK_MILLIS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        coordinator()?.setServiceConnected(true)
        if (!receiverRegistered) {
            ContextCompat.registerReceiver(
                this,
                screenOffReceiver,
                IntentFilter(Intent.ACTION_SCREEN_OFF),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            receiverRegistered = true
        }
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val c = coordinator() ?: return
        val eventPackage = event.packageName?.toString()
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (eventPackage == null) return
                foregroundPackage = eventPackage
                foregroundClass = event.className?.toString()
                checkForeground(c, eventPackage, foregroundClass)
                if (isLauncherOrSystemUi(eventPackage)) scanRecents(c, force = true)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                val sourcePackage = eventPackage ?: foregroundPackage
                if (sourcePackage != null && isLauncherOrSystemUi(sourcePackage)) {
                    scanRecents(c)
                    return
                }
                // Content changes inside the app in front, such as a resumed browser, a search, or a
                // navigation within Settings, don't open a new window. Re-check the app in front for them.
                val current = foregroundPackage ?: return
                if (eventPackage != null && eventPackage != current) return
                val now = SystemClock.elapsedRealtime()
                if (now - lastContentCheckMillis < CONTENT_CHECK_INTERVAL_MILLIS) return
                lastContentCheckMillis = now
                checkForeground(c, current, foregroundClass)
            }
            else -> return
        }
    }

    /** The same check for the app in front, whether it just opened or changed inside. */
    private fun checkForeground(c: ProtectionCoordinator, packageName: String, className: String?) {
        val readable = SelfScreenClassifier.readsContentFor(packageName, className)
        val texts = if (readable) readTexts() else emptyList()
        val screen = SelfScreenClassifier.classify(packageName, texts, SELF_LABEL, className)
        if (screen != SelfScreen.NONE) {
            Log.i(TAG, "self screen pkg=$packageName class=$className screen=$screen")
            if (c.onSelfScreen(screen) == ProtectionAction.RedirectHome) {
                redirectHome(packageName)
            }
            return
        }

        if (c.onForegroundChanged(packageName) == ProtectionAction.RedirectHome) {
            redirectHome(packageName)
        }
    }

    /** Sends the user Home, then removes the blocked app's card from Recents. */
    private fun redirectHome(blockedPackage: String) {
        performGlobalAction(GLOBAL_ACTION_HOME)
        val homePackage = homePackage() ?: return
        if (blockedPackage == this.packageName || blockedPackage == homePackage) return
        handler.postDelayed({ removeFromRecents(blockedPackage) }, RECENTS_OPEN_DELAY_MILLIS)
    }

    /** Opens Recents, swipes the blocked app's card away if it is there, and goes Home again. */
    private fun removeFromRecents(blockedPackage: String) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastRecentsMillis < RECENTS_COOLDOWN_MILLIS) return
        lastRecentsMillis = now

        if (skipRecentsIfUserMovedOn(blockedPackage)) return

        val label = labelOf(blockedPackage)
        if (label == null) {
            returnHomeUnlessUserMovedOn(blockedPackage)
            return
        }
        if (skipRecentsIfUserMovedOn(blockedPackage)) return
        if (!performGlobalAction(GLOBAL_ACTION_RECENTS)) {
            Log.w(TAG, "recents: unable to open for pkg=$blockedPackage")
            returnHomeUnlessUserMovedOn(blockedPackage)
            return
        }
        handler.postDelayed({
            if (skipRecentsIfUserMovedOn(blockedPackage)) return@postDelayed
            val bounds = findCardBounds(blockedPackage, label)
            if (bounds == null) {
                Log.i(TAG, "recents: pkg=$blockedPackage label=$label swiped=false")
                returnHomeUnlessUserMovedOn(blockedPackage)
                return@postDelayed
            }

            val accepted = swipeAwayCard(bounds, blockedPackage, label)
            Log.i(TAG, "recents: pkg=$blockedPackage label=$label swiped=$accepted")
            if (!accepted) returnHomeUnlessUserMovedOn(blockedPackage)
        }, RECENTS_SETTLE_MILLIS)
    }

    private fun skipRecentsIfUserMovedOn(blockedPackage: String): Boolean {
        val current = foregroundPackage ?: return false
        if (
            current == blockedPackage ||
            current == homePackage() ||
            current == SYSTEM_UI_PACKAGE
        ) return false
        Log.i(TAG, "recents: skipped, user already in $current")
        return true
    }

    private fun returnHomeUnlessUserMovedOn(blockedPackage: String) {
        if (!skipRecentsIfUserMovedOn(blockedPackage)) returnHome()
    }

    private fun returnHome() {
        if (!performGlobalAction(GLOBAL_ACTION_HOME)) {
            Log.w(TAG, "recents: unable to return Home")
        }
    }

    /** True for the home screen launcher and the system UI, where Recents is drawn. */
    private fun isLauncherOrSystemUi(pkg: String): Boolean =
        pkg == SYSTEM_UI_PACKAGE || pkg == homePackage()

    /** The protected apps from the saved list. Empty when protection hasn't loaded. */
    private fun protectedPackages(): Set<String> =
        (application as ZeroApplication).protectionModule.repository?.packages().orEmpty()

    /**
     * Scans the launcher or system UI for a Recents screen and removes one protected app card.
     * During Grace, protected apps are allowed and their cards are left alone.
     */
    private fun scanRecents(c: ProtectionCoordinator, force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastRecentsScanMillis < RECENTS_SCAN_INTERVAL_MILLIS) return
        lastRecentsScanMillis = now
        if (c.state() is ProtectionState.Grace) return
        val root = rootInActiveWindow ?: return
        try {
            val recentsOpen = containsText(root) { text ->
                RECENTS_MARKERS.any { text.equals(it, ignoreCase = true) }
            }
            if (!recentsOpen) return
            val home = homePackage()
            for (pkg in protectedPackages()) {
                if (pkg == packageName || pkg == home || pkg == SYSTEM_UI_PACKAGE) continue
                val label = labelOf(pkg) ?: continue
                val bounds = findTextBounds(root, label) ?: continue
                if (swipeCardSilently(bounds)) {
                    Log.i(TAG, "recents: manual scan swiped pkg=$pkg label=$label")
                    return
                }
            }
        } finally {
            @Suppress("DEPRECATION")
            root.recycle()
        }
    }

    /** Swipes up on a card and stays on the current screen. Returns false when no gesture can be sent. */
    private fun swipeCardSilently(bounds: Rect): Boolean {
        if (bounds.isEmpty) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val x = bounds.centerX().toFloat()
        val startY = bounds.centerY().toFloat()
        val endY = (startY - resources.displayMetrics.heightPixels * 0.5f).coerceAtLeast(0f)
        val path = Path().apply {
            moveTo(x, startY)
            lineTo(x, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, SWIPE_DURATION_MILLIS))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    /** True when any text on the screen satisfies [match]. Child nodes are recycled as they're checked. */
    @Suppress("DEPRECATION")
    private fun containsText(node: AccessibilityNodeInfo, match: (String) -> Boolean): Boolean {
        val text = node.text?.toString()?.trim()
        if (text != null && match(text)) return true
        val description = node.contentDescription?.toString()?.trim()
        if (description != null && match(description)) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                if (containsText(child, match)) return true
            } finally {
                child.recycle()
            }
        }
        return false
    }

    private fun labelOf(blockedPackage: String): String? =
        try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(blockedPackage, 0)).toString()
        } catch (failure: PackageManager.NameNotFoundException) {
            Log.w(TAG, "recents: unable to resolve label for pkg=$blockedPackage", failure)
            null
        }

    /** Swipes up from the matching app label. Returns false when no gesture can be dispatched. */
    private fun swipeAwayCard(bounds: Rect, blockedPackage: String, label: String): Boolean {
        if (bounds.isEmpty) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val x = bounds.centerX().toFloat()
        val startY = bounds.centerY().toFloat()
        val endY = (startY - resources.displayMetrics.heightPixels * 0.5f).coerceAtLeast(0f)
        val path = Path().apply {
            moveTo(x, startY)
            lineTo(x, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, SWIPE_DURATION_MILLIS))
            .build()
        return dispatchGesture(
            gesture,
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    Log.i(TAG, "recents: swipe completed for pkg=$blockedPackage label=$label")
                    returnHomeUnlessUserMovedOn(blockedPackage)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    Log.w(TAG, "recents: swipe cancelled for pkg=$blockedPackage label=$label")
                    returnHomeUnlessUserMovedOn(blockedPackage)
                }
            },
            handler,
        )
    }

    /** Reads only the active window so text behind Recents cannot trigger a swipe. */
    private fun findCardBounds(blockedPackage: String, label: String): Rect? {
        val root = rootInActiveWindow ?: return null
        return try {
            val rootPackage = root.packageName?.toString()
            if (
                rootPackage != homePackage() &&
                rootPackage != SYSTEM_UI_PACKAGE
            ) return null
            if (rootPackage == blockedPackage || rootPackage == packageName) return null
            findTextBounds(root, label)
        } finally {
            @Suppress("DEPRECATION")
            root.recycle()
        }
    }

    @Suppress("DEPRECATION")
    private fun findTextBounds(node: AccessibilityNodeInfo, label: String): Rect? {
        val description = node.contentDescription?.toString()?.trim()
        if (node.text?.toString()?.trim() == label || description?.startsWith(label) == true) {
            return Rect().also { node.getBoundsInScreen(it) }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                findTextBounds(child, label)?.let { return it }
            } finally {
                child.recycle()
            }
        }
        return null
    }

    /** The package of the default home screen app, which Zero must never remove from Recents. */
    private fun homePackage(): String? =
        packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY,
        )?.activityInfo?.packageName

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        release()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    private fun release() {
        coordinator()?.setServiceConnected(false)
        handler.removeCallbacks(ticker)
        if (receiverRegistered) {
            unregisterReceiver(screenOffReceiver)
            receiverRegistered = false
        }
    }

    /** Collects visible text from the active window. The result is only used for classification. */
    private fun readTexts(): List<String> {
        val root = rootInActiveWindow ?: return emptyList()
        return try {
            val out = mutableListOf<String>()
            collectTexts(root, out)
            out
        } finally {
            @Suppress("DEPRECATION")
            root.recycle()
        }
    }

    @Suppress("DEPRECATION")
    private fun collectTexts(node: AccessibilityNodeInfo, out: MutableList<String>) {
        node.text?.let { out.add(it.toString()) }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                collectTexts(child, out)
            } finally {
                child.recycle()
            }
        }
    }

    private fun coordinator(): ProtectionCoordinator? =
        (application as ZeroApplication).protectionModule.coordinator

    private fun runSafely(what: String, block: () -> Unit) {
        try {
            block()
        } catch (failure: PersistenceException) {
            Log.w(TAG, "$what failed to persist; state unchanged", failure)
        }
    }

    private companion object {
        const val TAG = "ZeroProtection"
        const val TICK_MILLIS = 1_000L
        const val CONTENT_CHECK_INTERVAL_MILLIS = 300L
        const val RECENTS_OPEN_DELAY_MILLIS = 350L
        const val RECENTS_SETTLE_MILLIS = 700L
        const val SWIPE_DURATION_MILLIS = 250L
        const val SYSTEM_UI_PACKAGE = "com.android.systemui"
        const val RECENTS_COOLDOWN_MILLIS = 2_000L
        const val RECENTS_SCAN_INTERVAL_MILLIS = 800L
        val RECENTS_MARKERS = listOf("Clear all", "Close all")
        const val SELF_LABEL = "Zero"
    }
}
