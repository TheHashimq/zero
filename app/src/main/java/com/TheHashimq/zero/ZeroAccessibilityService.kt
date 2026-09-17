package com.TheHashimq.zero

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class ZeroAccessibilityService : AccessibilityService() {

    private lateinit var screenReceiver: BroadcastReceiver

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val prefs = getSharedPreferences("ZeroPrefs", Context.MODE_PRIVATE)
                val state = prefs.getString("LOCK_STATE", "IDLE")
                if (state == "UNLOCKED") {
                    prefs.edit().putString("LOCK_STATE", "IDLE").apply()
                    logEscapeAttempt("Screen off detected - grace period revoked")
                    Log.d("ZeroDebug", "Screen turned off. Grace period ended, re-arming shield.")
                }
            }
        }
        registerReceiver(screenReceiver, filter, Context.RECEIVER_EXPORTED)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            Log.d("ZeroDebug", "Foreground app detected: $packageName")

            if (packageName == "com.android.settings" || packageName == "com.google.android.packageinstaller") {
                val prefs = getSharedPreferences("ZeroPrefs", Context.MODE_PRIVATE)
                val state = prefs.getString("LOCK_STATE", "IDLE")
                val unlockTime = prefs.getLong("UNLOCK_TIMESTAMP", 0L)
                val now = SystemClock.elapsedRealtime()

                var shouldBlock = false

                when (state) {
                    "IDLE" -> {
                        shouldBlock = true
                        logEscapeAttempt("Attempted to access $packageName in IDLE state")
                    }
                    "COUNTDOWN" -> {
                        if (now < unlockTime) {
                            shouldBlock = true
                            logEscapeAttempt("Attempted to access $packageName during cooldown")
                        } else {
                            val gracePeriodExpiry = now + (3 * 60 * 1000L)
                            prefs.edit()
                                .putString("LOCK_STATE", "UNLOCKED")
                                .putLong("GRACE_EXPIRY", gracePeriodExpiry)
                                .apply()
                            Log.d("ZeroDebug", "90 minutes passed! Entering 3-minute grace period.")
                        }
                    }
                    "UNLOCKED" -> {
                        val graceExpiry = prefs.getLong("GRACE_EXPIRY", 0L)
                        if (now > graceExpiry) {
                            prefs.edit().putString("LOCK_STATE", "IDLE").apply()
                            shouldBlock = true
                            logEscapeAttempt("Grace period expired, re-arming shield")
                        }
                    }
                }

                if (shouldBlock) {
                    Log.d("ZeroDebug", "Blocking access to $packageName -> Redirecting to Zero UI.")
                    val intent = Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    }
                    startActivity(intent)
                }
            }
        }
    }

    override fun onInterrupt() {}

    private fun logEscapeAttempt(attempt: String) {
        val prefs = getSharedPreferences("ZeroPrefs", Context.MODE_PRIVATE)
        val timestamp = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val existingLog = prefs.getString("ESCAPE_LOG", "") ?: ""
        val newLog = if (existingLog.isEmpty()) {
            "$timestamp  $attempt"
        } else {
            "$existingLog\n$timestamp  $attempt"
        }
        prefs.edit().putString("ESCAPE_LOG", newLog).apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}