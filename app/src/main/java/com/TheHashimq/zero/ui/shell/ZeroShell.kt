/*
 * MODULE: app (navigation shell)
 * PURPOSE: Renders the drawer menu, back stack, Diagnostics, and every module and streak screen.
 * TAKES: A frozen NavigationRegistry, HealthService, module references, and StreakStore.
 * GIVES: The current screen, with Back in the top bar and system gesture popping one level.
 * OWNS: The back stack and drawer state.
 * DOES NOT OWN: Screen content, health checks, or protection behavior.
 * DEPENDENCIES: foundation-navigation, foundation-health, foundation-design, ui/theme.
 * CONTRACT: Drawer selections start fresh from Main. In-app navigation pushes. Back pops one level.
 *           At Main with the drawer closed, Back is not handled here, so the system closes the app.
 * FAILURE BEHAVIOR: Health reports never throw. A missing streak shows a message with Back.
 */
package com.TheHashimq.zero.ui.shell

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.sp
import com.TheHashimq.zero.platform.streaks.StreakStore
import com.TheHashimq.zero.ui.history.HistoryScreen
import com.TheHashimq.zero.ui.main.MainScreen
import com.TheHashimq.zero.ui.protection.ProtectedAppsScreen
import com.TheHashimq.zero.ui.protection.ProtectionScreen
import com.TheHashimq.zero.ui.streaks.StreakDetailsScreen
import com.TheHashimq.zero.ui.streaks.StreakFormScreen
import com.TheHashimq.zero.ui.streaks.StreakHomeScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import zero.feature.history.HistoryModule
import zero.feature.protection.runtime.ProtectionModule
import zero.foundation.design.DesignTokens
import zero.foundation.design.StatusRole
import zero.foundation.health.HealthService
import zero.foundation.health.HealthState
import zero.foundation.navigation.NavCategory
import zero.foundation.navigation.NavigationRegistry

/** Screens the shell can show. Streak screens carry their identity so Back returns to them. */
private sealed interface Screen {
    data object Main : Screen
    data object Diagnostics : Screen
    data class Module(val id: String, val title: String) : Screen
    data class StreakDetail(val id: String) : Screen
    data class StreakForm(val id: String?) : Screen
}

/** Root composable: drawer menu, back stack, and the current screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZeroShell(
    navigation: NavigationRegistry,
    health: HealthService,
    protection: ProtectionModule,
    history: HistoryModule,
    streaks: StreakStore,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var stack by remember { mutableStateOf<List<Screen>>(listOf(Screen.Main)) }
    val top = stack.last()

    fun openFromDrawer(next: Screen) {
        stack = if (next == Screen.Main) listOf(Screen.Main) else listOf(Screen.Main, next)
        scope.launch { drawerState.close() }
    }

    fun push(next: Screen) {
        stack = stack + next
    }

    fun back() {
        if (stack.size > 1) stack = stack.dropLast(1)
    }

    fun backToStreaks() {
        stack = listOf(Screen.Main, Screen.Module("streaks", "Streaks"))
    }

    BackHandler(enabled = drawerState.isOpen || stack.size > 1) {
        if (drawerState.isOpen) scope.launch { drawerState.close() } else back()
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
                    selected = top == Screen.Main,
                    onClick = { openFromDrawer(Screen.Main) },
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
                            selected = top == target,
                            onClick = { openFromDrawer(target) },
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
                    selected = top == Screen.Diagnostics,
                    onClick = { openFromDrawer(Screen.Diagnostics) },
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(titleFor(top)) },
                    navigationIcon = {
                        if (stack.size > 1) {
                            TextButton(onClick = { back() }) { Text("‹ Back") }
                        } else {
                            TextButton(onClick = { scope.launch { drawerState.open() } }) {
                                Text("≡", fontSize = 30.sp)
                            }
                        }
                    },
                )
            },
        ) { inner ->
            Box(
                modifier = Modifier.fillMaxSize().padding(inner).padding(16.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                when (top) {
                    Screen.Main -> MainScreen(protection) {
                        push(Screen.Module(ProtectionModule.PROTECTED_APPS_ROUTE, "Protected apps"))
                    }
                    Screen.Diagnostics -> DiagnosticsContent(health)
                    is Screen.Module -> when (top.id) {
                        "protection" -> ProtectionScreen(protection)
                        ProtectionModule.PROTECTED_APPS_ROUTE -> ProtectedAppsScreen(protection)
                        "history" -> HistoryScreen(history)
                        "streaks" -> StreakHomeScreen(
                            store = streaks,
                            onOpen = { id -> push(Screen.StreakDetail(id)) },
                            onAdd = { push(Screen.StreakForm(null)) },
                        )
                        else -> Text("Screen for '${top.id}' is not built yet")
                    }
                    is Screen.StreakDetail -> StreakDetailsScreen(
                        store = streaks,
                        streakId = top.id,
                        onEdit = { push(Screen.StreakForm(top.id)) },
                        onBack = { back() },
                    )
                    is Screen.StreakForm -> StreakFormRoute(
                        store = streaks,
                        id = top.id,
                        onDone = { back() },
                        onDeleted = { backToStreaks() },
                    )
                }
            }
        }
    }
}

/** Loads the streak being edited, or shows a message with Back if it no longer exists. */
@Composable
private fun StreakFormRoute(
    store: StreakStore,
    id: String?,
    onDone: () -> Unit,
    onDeleted: () -> Unit,
) {
    if (id == null) {
        StreakFormScreen(store, null, onDone, onDeleted)
        return
    }
    val existing = store.read { repo -> repo.definitions().firstOrNull { it.id == id } }
    if (existing == null) {
        Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)) {
            Text("This streak is no longer available.")
            OutlinedButton(onClick = onDone) { Text("Back") }
        }
    } else {
        StreakFormScreen(store, existing, onDone, onDeleted)
    }
}

private fun titleFor(screen: Screen): String = when (screen) {
    Screen.Main -> "Zero"
    Screen.Diagnostics -> "Diagnostics"
    is Screen.Module -> screen.title
    is Screen.StreakDetail -> "Streak"
    is Screen.StreakForm -> if (screen.id == null) "New streak" else "Edit streak"
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
