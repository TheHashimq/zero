/*
 * MODULE: app (platform adapter for protection health)
 * PURPOSE: Reads whether Zero's accessibility service is enabled in system settings.
 * TAKES: A Context.
 * GIVES: true when the service's component appears in the enabled-services setting.
 * OWNS: No state.
 * DOES NOT OWN: Whether the service is connected. That comes from ProtectionModule.
 * DEPENDENCIES: Android SDK, ProtectionAccessibilityService.
 * CONTRACT: Reads the setting each time it is called; nothing is cached.
 * FAILURE BEHAVIOR: A missing setting returns false. Never throws for a missing setting.
 */
package com.TheHashimq.zero.platform.protection

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

/** Reads the accessibility setting for Zero's service. */
class AndroidAccessibilityEnabledProbe(private val context: Context) {
    fun isEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val target = ComponentName(context, ProtectionAccessibilityService::class.java).flattenToString()
        return enabled.split(':').any { it.equals(target, ignoreCase = true) }
    }
}
