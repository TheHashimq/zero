/*
 * MODULE: app (protection screen)
 * PURPOSE: Shows protection state, starts the test commitment, and lists recent events.
 * TAKES: The ProtectionModule from ZeroApplication.
 * GIVES: A Compose screen that ticks every second and shows the countdown and events.
 * OWNS: Only the displayed values.
 * DOES NOT OWN: Protection decisions. Every action goes through the coordinator.
 * DEPENDENCIES: feature-protection-runtime, Compose Material3.
 * CONTRACT: Start is enabled only when the service is connected, apps are protected, and state is Armed.
 * FAILURE BEHAVIOR: Storage errors are shown on screen and never crash the UI.
 */
package com.TheHashimq.zero.ui.protection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import zero.feature.protection.ProtectionState
import zero.feature.protection.runtime.ProtectionModule
import zero.foundation.persistence.PersistenceException

/** Phase 8b test duration. Phase 10 replaces this with the configured minimum. */
private const val TEST_COMMITMENT_MILLIS = 2L * 60L * 1000L

@Composable
fun ProtectionScreen(module: ProtectionModule) {
    val coordinator = module.coordinator
    if (coordinator == null) {
        Text("Protection is starting")
        return
    }

    var state by remember { mutableStateOf(coordinator.state()) }
    var now by remember { mutableLongStateOf(coordinator.nowMillis()) }
    var connected by remember { mutableStateOf(coordinator.isServiceConnected()) }
    var protectedCount by remember { mutableIntStateOf(0) }
    var events by remember { mutableStateOf(coordinator.recentEvents(5)) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(coordinator) {
        while (true) {
            try {
                state = coordinator.tick()
            } catch (failure: PersistenceException) {
                message = "Storage error: ${failure.message}"
            }
            now = coordinator.nowMillis()
            connected = coordinator.isServiceConnected()
            try {
                protectedCount = module.repository?.packages()?.size ?: 0
            } catch (failure: PersistenceException) {
                message = "Storage error: ${failure.message}"
            }
            events = coordinator.recentEvents(5)
            delay(1_000L)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("State: ${describe(state, now)}", style = MaterialTheme.typography.titleMedium)

        if (!connected) {
            Text("Turn on Zero's accessibility service in Settings before starting a commitment.")
        }
        Text("Protected apps: $protectedCount")

        Button(
            onClick = {
                try {
                    val started = coordinator.startCommitment(TEST_COMMITMENT_MILLIS)
                    message = if (started) null else "Cannot start now: add a protected app, check boot availability, or finish the running commitment."
                } catch (failure: PersistenceException) {
                    message = "Storage error: ${failure.message}"
                }
                state = coordinator.state()
            },
            enabled = state == ProtectionState.Armed && connected && protectedCount > 0,
        ) {
            Text("Start test commitment (2 min)")
        }

        message?.let { Text(it) }

        Text("Recent events", style = MaterialTheme.typography.labelLarge)
        events.forEach { event ->
            val details = event.payload.entries.joinToString { "${it.key}=${it.value}" }
            Text(if (details.isEmpty()) event.type.value else "${event.type.value}  $details")
        }
    }
}

private fun describe(state: ProtectionState, now: Long): String = when (state) {
    ProtectionState.Armed -> "Armed (nothing blocked)"
    is ProtectionState.Countdown -> "Countdown, ${maxOf(0L, state.endsAtMillis - now) / 1000} s left"
    is ProtectionState.Grace -> "Grace, ${maxOf(0L, state.endsAtMillis - now) / 1000} s left"
}
