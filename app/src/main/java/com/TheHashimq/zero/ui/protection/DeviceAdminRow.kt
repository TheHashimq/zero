/*
 * MODULE: app (device administrator row)
 * PURPOSE: Shows whether Zero is a device administrator and offers to activate it.
 * TAKES: The current Context.
 * GIVES: A status line and a button that opens the system activation screen.
 * OWNS: The displayed active state.
 * DOES NOT OWN: The admin rules. See DeviceAdmin.
 * DEPENDENCIES: Compose Material3, shared components, platform/admin.
 * CONTRACT: The state is refreshed when the user returns from the system activation screen.
 * FAILURE BEHAVIOR: If the system screen cannot open, the row shows a message.
 */
package com.TheHashimq.zero.ui.protection

import android.content.ActivityNotFoundException
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.TheHashimq.zero.platform.admin.DeviceAdmin
import com.TheHashimq.zero.ui.components.SecondaryButton
import com.TheHashimq.zero.ui.components.StatusLine
import zero.foundation.design.DesignTokens

@Composable
fun DeviceAdminRow() {
    val context: Context = LocalContext.current
    var active by remember(context) { mutableStateOf(DeviceAdmin.isActive(context)) }
    var message by remember { mutableStateOf<String?>(null) }
    val activationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        active = DeviceAdmin.isActive(context)
    }

    StatusLine(
        if (active) "Device protection: on. Zero cannot be removed while protection is active."
        else "Device protection: off. Turn it on to block Zero's removal.",
        if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (!active) {
        SecondaryButton("Turn on device protection") {
            try {
                activationLauncher.launch(DeviceAdmin.requestIntent(context))
                message = null
            } catch (failure: ActivityNotFoundException) {
                message = "Could not open the activation screen: ${failure.message}"
            } catch (failure: SecurityException) {
                message = "Could not open the activation screen: ${failure.message}"
            }
        }
    }
    message?.let { StatusLine(it, Color(DesignTokens.CRITICAL)) }
}
