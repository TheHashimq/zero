/*
 * MODULE: app (navigation shell)
 * PURPOSE: Renders the drawer menu, the built-in Diagnostics screen, and placeholders for module screens.
 * TAKES: A frozen NavigationRegistry and the HealthService.
 * GIVES: The main screen, Diagnostics with live health values, and module placeholders.
 * OWNS: Only the current screen selection and drawer state.
 * DOES NOT OWN: Module screen content, health checks, or protection behavior.
 * DEPENDENCIES: foundation-navigation, foundation-health, foundation-design, ui/theme.
 * CONTRACT: The shell never calls module code. Module entries without a screen show a placeholder.
 * FAILURE BEHAVIOR: Health reports never throw; a failed source appears as FAILED.
 */
package com.TheHashimq.zero.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.TheHashimq.zero.ui.protection.ProtectionScreen
import com.TheHashimq.zero.ui.protection.ProtectedAppsScreen
import com.TheHashimq.zero.ui.main.MainScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import zero.foundation.design.DesignTokens
import zero.foundation.design.StatusRole
import zero.feature.protection.runtime.ProtectionModule
import zero.foundation.health.HealthService
import zero.foundation.health.HealthState
import zero.foundation.navigation.NavCategory
import zero.foundation.navigation.NavigationRegistry

/** Screens the shell can show. */
private sealed interface Screen {
    data object Main : Screen
    data object Diagnostics : Screen
    data class Module(val id: String, val title: String) : Screen
}

/** Root composable: drawer menu plus the selected screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZeroShell(navigation: NavigationRegistry, health: HealthService, protection: ProtectionModule) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf<Screen>(Screen.Main) }

    fun go(next: Screen) {
        screen = next
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "ZERO",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp),
                )
                NavigationDrawerItem(
                    label = { Text("Main") },
                    selected = screen == Screen.Main,
                    onClick = { go(Screen.Main) },
                )
                navigation.grouped().forEach { (category, entries) ->
                    Text(
                        text = category.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp),
                    )
                    entries.forEach { entry ->
                        val target = Screen.Module(entry.id, entry.title)
                        NavigationDrawerItem(
                            label = { Text(entry.title) },
                            selected = screen == target,
                            onClick = { go(target) },
                        )
                    }
                }
                Text(
                    text = NavCategory.SYSTEM.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp),
                )
                NavigationDrawerItem(
                    label = { Text("Diagnostics") },
                    selected = screen == Screen.Diagnostics,
                    onClick = { go(Screen.Diagnostics) },
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(titleFor(screen)) },
                    navigationIcon = {
                        TextButton(onClick = { scope.launch { drawerState.open() } }) {
                            Text("≡")
                        }
                    },
                )
            },
        ) { inner ->
            Box(
                modifier = Modifier.fillMaxSize().padding(inner).padding(16.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                when (val current = screen) {
                    Screen.Main -> MainScreen(protection) {
                        go(Screen.Module(ProtectionModule.PROTECTED_APPS_ROUTE, "Protected apps"))
                    }
                    Screen.Diagnostics -> DiagnosticsContent(health)
                    is Screen.Module -> when (current.id) {
                        "protection" -> ProtectionScreen(protection)
                        ProtectionModule.PROTECTED_APPS_ROUTE -> ProtectedAppsScreen(protection)
                        else -> Text("Screen for '${current.id}' is not built yet")
                    }
                }
            }
        }
    }
}

private fun titleFor(screen: Screen): String = when (screen) {
    Screen.Main -> "Zero"
    Screen.Diagnostics -> "Diagnostics"
    is Screen.Module -> screen.title
}

/** Live health values, colored by status role. */
@Composable
private fun DiagnosticsContent(health: HealthService) {
    var system by remember(health) { mutableStateOf(health.report()) }

    LaunchedEffect(health) {
        while (true) {
            system = health.report()
            delay(1_000L)
        }
    }

    Column {
        Text(
            text = "Overall: ${system.overall}",
            style = MaterialTheme.typography.titleMedium,
            color = colorFor(system.overall),
        )
        system.components.forEach { report ->
            Text(
                text = "${report.component}: ${report.state} (${report.detail})",
                color = colorFor(report.state),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private fun colorFor(state: HealthState): Color {
    val role = when (state) {
        HealthState.OK -> StatusRole.HEALTHY
        HealthState.DEGRADED -> StatusRole.WARNING
        HealthState.FAILED -> StatusRole.CRITICAL
    }
    return Color(DesignTokens.colorFor(role))
}
