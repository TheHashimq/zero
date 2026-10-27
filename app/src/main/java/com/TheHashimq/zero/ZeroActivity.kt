/*
 * MODULE: app (launcher activity)
 * PURPOSE: Hosts the Compose shell as the app's only screen.
 * TAKES: The launch intent and the services built by ZeroApplication.
 * GIVES: The shell UI.
 * OWNS: No state beyond the Compose tree.
 * DOES NOT OWN: Navigation registration, health checks, or protection behavior.
 * DEPENDENCIES: ZeroApplication, ui/shell, ui/theme.
 * CONTRACT: Reads services only after ZeroApplication has created them.
 * FAILURE BEHAVIOR: If services are missing, the lateinit access throws during launch.
 */
package com.TheHashimq.zero

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.TheHashimq.zero.ui.shell.ZeroShell
import com.TheHashimq.zero.ui.theme.ZeroTheme

class ZeroActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val services = (application as ZeroApplication).services
        setContent {
            ZeroTheme {
                ZeroShell(services.navigation, services.health)
            }
        }
    }
}
