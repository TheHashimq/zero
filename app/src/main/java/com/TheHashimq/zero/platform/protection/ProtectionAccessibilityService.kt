/*
 * MODULE: app (accessibility adapter for protection)
 * PURPOSE: Checks the app in front whenever it opens or changes, and sends the user Home when told to.
 * TAKES: Window-state and content-change events, screen-off broadcasts, and a periodic tick.
 * GIVES: Global Home action when the coordinator returns RedirectHome.
 * OWNS: The foreground app, the throttle timestamp, the screen-off receiver, and the tick handler.
 * DOES NOT OWN: Protection decisions, persistence, or events.
 * DEPENDENCIES: feature-protection-runtime, feature-protection, ZeroApplication, Android accessibility APIs.
 * CONTRACT: A new window and any content change inside the app in front run the same check. Content checks are
 *           throttled to one per 300 ms. Text is read only on installer and Settings windows, checked in memory,
 *           and never stored or logged. Sends Home only on RedirectHome.
 * FAILURE BEHAVIOR: Persistence failures are logged and retried on the next event or tick.
 *                   A missing coordinator means no action.
 */
package com.TheHashimq.zero.platform.protection

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.ContextCompat
import com.TheHashimq.zero.ZeroApplication
import zero.feature.protection.ProtectionAction
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
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
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
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
            return
        }

        if (c.onForegroundChanged(packageName) == ProtectionAction.RedirectHome) {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

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
        const val SELF_LABEL = "Zero"
    }
}
