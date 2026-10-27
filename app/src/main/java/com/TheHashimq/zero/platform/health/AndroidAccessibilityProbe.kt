/*
 * MODULE: app (platform adapter for foundation-health)
 * PURPOSE: Reports whether Zero's accessibility service is enabled in system settings.
 * TAKES: An Android Context.
 * GIVES: A PermissionState from the enabled-services setting.
 * OWNS: No state.
 * DOES NOT OWN: Whether the service is currently connected.
 * DEPENDENCIES: foundation-health, ZeroAccessibilityService, Android SDK.
 * CONTRACT: Returns GRANTED when the service component appears in enabled services, otherwise DENIED.
 * FAILURE BEHAVIOR: A missing setting returns DENIED.
 */
package com.TheHashimq.zero.platform.health

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.TheHashimq.zero.ZeroAccessibilityService
import zero.foundation.health.PermissionProbe
import zero.foundation.health.PermissionState

/** Reads the accessibility setting for Zero's service. */
class AndroidAccessibilityProbe(private val context: Context) : PermissionProbe {
    /** Checks the enabled accessibility service setting on each call. */
    override fun state(): PermissionState {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return PermissionState.DENIED
        val target = ComponentName(context, ZeroAccessibilityService::class.java).flattenToString()
        val granted = enabled.split(':').any { it.equals(target, ignoreCase = true) }
        return if (granted) PermissionState.GRANTED else PermissionState.DENIED
    }
}
