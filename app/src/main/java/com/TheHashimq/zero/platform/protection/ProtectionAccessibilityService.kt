/*
 * MODULE: app (accessibility adapter for protection)
 * PURPOSE: Reports foreground app changes to the protection coordinator and sends the user Home when told to.
 * TAKES: Window-state accessibility events, screen-off broadcasts, and a periodic tick.
 * GIVES: Global Home action when the coordinator returns RedirectHome.
 * OWNS: The screen-off receiver and the tick handler. No protection state.
 * DOES NOT OWN: Protection decisions, persistence, or events.
 * DEPENDENCIES: feature-protection-runtime, ZeroApplication, Android accessibility APIs.
 * CONTRACT: Reads package names only; window content is not requested. Sends Home only on RedirectHome.
 * FAILURE BEHAVIOR: Persistence failures on tick or screen-off are logged and retried on the next tick.
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
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.TheHashimq.zero.ZeroApplication
import zero.feature.protection.ProtectionAction
import zero.feature.protection.runtime.ProtectionCoordinator
import zero.foundation.persistence.PersistenceException

/** Thin Android adapter for the protection coordinator. */
class ProtectionAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var receiverRegistered = false

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
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        val c = coordinator() ?: return
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
    }
}
