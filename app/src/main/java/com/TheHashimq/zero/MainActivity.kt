package com.TheHashimq.zero

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var tvTimer: TextView? = null
    private var tvStatus: TextView? = null
    private var btnAction: Button? = null
    private var btnEnablePermission: Button? = null

    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTimer = findViewById(R.id.tvTimer)
        tvStatus = findViewById(R.id.tvStatus)
        btnAction = findViewById(R.id.btnStartDelay)
        btnEnablePermission = findViewById(R.id.btnEnablePermission)

        btnEnablePermission?.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        btnAction?.setOnClickListener {
            start90MinCooldown()
        }

        tvTimer?.setOnLongClickListener {
            showEscapeLog()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        evaluateState()
        checkProtectionStatus()
    }

    private fun checkProtectionStatus() {
        val prefs = getSharedPreferences("ZeroPrefs", Context.MODE_PRIVATE)
        val isAccessibilityEnabled = isAccessibilityServiceEnabled()

        if (!isAccessibilityEnabled) {
            tvStatus?.text = "⚠ PROTECTION DISABLED"
            tvTimer?.text = "—"
            btnAction?.isEnabled = false
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        return accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
            .any { it.id.contains("com.TheHashimq.zero/.ZeroAccessibilityService") }
    }

    private fun evaluateState() {
        val prefs = getSharedPreferences("ZeroPrefs", Context.MODE_PRIVATE)
        val state = prefs.getString("LOCK_STATE", "IDLE")
        val now = SystemClock.elapsedRealtime()

        when (state) {
            "IDLE" -> {
                tvStatus?.text = "ACCESS RESTRICTED"
                tvTimer?.text = "90:00"
                btnAction?.text = "START 90-MIN COOLDOWN"
                btnAction?.isEnabled = true
            }
            "COUNTDOWN" -> {
                val unlockTime = prefs.getLong("UNLOCK_TIMESTAMP", 0L)
                val remaining = unlockTime - now
                if (remaining > 0) {
                    startTimerUI(remaining, isGracePeriod = false)
                } else {
                    startGracePeriod()
                }
            }
            "UNLOCKED" -> {
                val graceExpiry = prefs.getLong("GRACE_EXPIRY", 0L)
                val remaining = graceExpiry - now
                if (remaining > 0) {
                    startTimerUI(remaining, isGracePeriod = true)
                } else {
                    prefs.edit().putString("LOCK_STATE", "IDLE").apply()
                    evaluateState()
                }
            }
        }
    }

    private fun start90MinCooldown() {
        val prefs = getSharedPreferences("ZeroPrefs", Context.MODE_PRIVATE)
        val unlockTime = SystemClock.elapsedRealtime() + (90 * 60 * 1000L)

        prefs.edit()
            .putString("LOCK_STATE", "COUNTDOWN")
            .putLong("UNLOCK_TIMESTAMP", unlockTime)
            .apply()

        evaluateState()
    }

    private fun startGracePeriod() {
        val prefs = getSharedPreferences("ZeroPrefs", Context.MODE_PRIVATE)
        val graceExpiry = SystemClock.elapsedRealtime() + (3 * 60 * 1000L)

        prefs.edit()
            .putString("LOCK_STATE", "UNLOCKED")
            .putLong("GRACE_EXPIRY", graceExpiry)
            .apply()

        evaluateState()
    }

    private fun startTimerUI(millisRemaining: Long, isGracePeriod: Boolean) {
        btnAction?.isEnabled = false
        countDownTimer?.cancel()

        if (isGracePeriod) {
            tvStatus?.text = "ACCESS GRANTED (3-MIN WINDOW)"
            btnAction?.text = "SETTINGS UNLOCKED"
        } else {
            tvStatus?.text = "COOLDOWN IN PROGRESS"
            btnAction?.text = "PLEASE WAIT..."
        }

        countDownTimer = object : CountDownTimer(millisRemaining, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                tvTimer?.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                evaluateState()
            }
        }.start()
    }

    private fun showEscapeLog() {
        val prefs = getSharedPreferences("ZeroPrefs", Context.MODE_PRIVATE)
        val escapeLog = prefs.getString("ESCAPE_LOG", "No escape attempts recorded") ?: "No escape attempts recorded"

        val builder = android.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
        builder.setTitle("ESCAPE ATTEMPTS")
        builder.setMessage(escapeLog)
        builder.setNeutralButton("CLEAR") { _, _ ->
            prefs.edit().putString("ESCAPE_LOG", "").apply()
        }
        builder.setPositiveButton("CLOSE") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}