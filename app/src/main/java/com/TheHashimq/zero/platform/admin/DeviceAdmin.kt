/*
 * MODULE: app (device administrator)
 * PURPOSE: Checks whether Zero is an active device administrator and builds the request to become one.
 * TAKES: A Context.
 * GIVES: The active state and an intent that asks the user to activate Zero.
 * OWNS: No state.
 * DOES NOT OWN: Blocking removal during protection. That is handled by the protection module.
 * DEPENDENCIES: Android DevicePolicyManager.
 * CONTRACT: Activation is always confirmed by the user on the system screen. Zero cannot activate itself.
 * FAILURE BEHAVIOR: If the policy service is unavailable, the state reads as not active.
 */
package com.TheHashimq.zero.platform.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object DeviceAdmin {
    fun componentName(context: Context): ComponentName =
        ComponentName(context, ZeroDeviceAdminReceiver::class.java)

    fun isActive(context: Context): Boolean {
        val manager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return false
        return manager.isAdminActive(componentName(context))
    }

    fun requestIntent(context: Context): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName(context))
            .putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Zero blocks its own removal while protection is on. It sets no other device rules.",
            )
}
