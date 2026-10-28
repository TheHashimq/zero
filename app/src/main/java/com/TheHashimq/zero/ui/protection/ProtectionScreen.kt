/*
 * MODULE: app (protection screen)
 * PURPOSE: Shows protection state, the protected-app count, and the commitment minimum. Starts commitments and raises the minimum.
 * TAKES: The ProtectionModule from ZeroApplication.
 * GIVES: A Compose screen that refreshes every second.
 * OWNS: Only the displayed values.
 * DOES NOT OWN: Protection or minimum rules. Every action goes through the coordinator or repository.
 * DEPENDENCIES: feature-protection-runtime, Compose Material3.
 * CONTRACT: Start uses the current committed minimum. Start is enabled only when Armed, connected, and at least one app is protected.
 * FAILURE BEHAVIOR: Storage errors are shown on screen and never crash the UI.
 */
package com.TheHashimq.zero.ui.protection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import zero.feature.protection.CooldownRules
import zero.feature.protection.ProtectionState
import zero.feature.protection.RaiseResult
import zero.feature.protection.runtime.ProtectionModule
import zero.foundation.persistence.PersistenceException

private const val MINUTE_MILLIS = 60_000L
private val RAISE_OPTIONS_MINUTES = listOf(120L, 150L, 180L)

@Composable
fun ProtectionScreen(module: ProtectionModule) {
    val coordinator = module.coordinator
    val repository = module.repository
    val cooldown = module.cooldown
    if (coordinator == null || repository == null || cooldown == null) {
        Text("Protection is starting")
        return
    }

    var state by remember { mutableStateOf(coordinator.state()) }
    var now by remember { mutableLongStateOf(coordinator.nowMillis()) }
    var connected by remember { mutableStateOf(coordinator.isServiceConnected()) }
    var protectedCount by remember { mutableStateOf(0) }
    var minimum by remember { mutableLongStateOf(CooldownRules.FLOOR_MILLIS) }
    var events by remember { mutableStateOf(coordinator.recentEvents(5)) }
    var message by remember { mutableStateOf<String?>(null) }
    var storageError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(coordinator, repository, cooldown) {
        while (true) {
            try {
                state = coordinator.tick()
                protectedCount = repository.packages().size
                minimum = cooldown.minimumMillis()
                events = coordinator.recentEvents(5)
                storageError = null
            } catch (failure: PersistenceException) {
                storageError = "Storage error: ${failure.message}"
            }
            now = coordinator.nowMillis()
            connected = coordinator.isServiceConnected()
            delay(1_000L)
        }
    }

    val canStart = state == ProtectionState.Armed && connected && protectedCount > 0
    val raiseOptions = RAISE_OPTIONS_MINUTES.filter { it * MINUTE_MILLIS > minimum }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("State: ${describe(state, now)}", style = MaterialTheme.typography.titleMedium)
        Text("Protected apps: $protectedCount")
        Text("Minimum commitment: ${minimum / MINUTE_MILLIS} min")

        if (raiseOptions.isEmpty()) {
            Text("Minimum is at its maximum.")
        } else {
            Text("Raise minimum (cannot be lowered)", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                raiseOptions.forEach { minutes ->
                    Button(onClick = {
                        message = try {
                            raiseMessage(cooldown.raise(minutes * MINUTE_MILLIS))
                        } catch (failure: PersistenceException) {
                            storageError = "Storage error: ${failure.message}"
                            null
                        }
                        try {
                            minimum = cooldown.minimumMillis()
                        } catch (failure: PersistenceException) {
                            storageError = "Storage error: ${failure.message}"
                        }
                    }) {
                        Text("$minutes")
                    }
                }
            }
        }

        if (!connected) {
            Text("Turn on Zero's accessibility service in Settings before starting a commitment.")
        }
        if (protectedCount == 0) {
            Text("Protect at least one app first, from Protected apps in the menu.")
        }

        Button(
            onClick = {
                try {
                    val started = coordinator.startCommitment(minimum)
                    message = if (started) null else "Cannot start now: check the boot ID, protected apps, and state."
                } catch (failure: PersistenceException) {
                    storageError = "Storage error: ${failure.message}"
                }
                state = coordinator.state()
            },
            enabled = canStart,
        ) {
            Text("Start commitment (${minimum / MINUTE_MILLIS} min)")
        }

        message?.let { Text(it) }
        storageError?.let { Text(it) }

        Text("Recent events", style = MaterialTheme.typography.labelLarge)
        events.forEach { event ->
            val details = event.payload.entries.joinToString { "${it.key}=${it.value}" }
            Text(if (details.isEmpty()) event.type.value else "${event.type.value}  $details")
        }
    }
}

private fun raiseMessage(result: RaiseResult): String = when (result) {
    is RaiseResult.Raised -> "Minimum raised to ${result.minimum.millis / MINUTE_MILLIS} min."
    RaiseResult.Unchanged -> "Minimum is already that value."
    RaiseResult.RejectedAboveMax -> "The maximum is 180 min."
    RaiseResult.RejectedDecrease -> "The minimum can only go up."
}

private fun describe(state: ProtectionState, now: Long): String = when (state) {
    ProtectionState.Armed -> "Armed (nothing blocked)"
    is ProtectionState.Countdown -> "Countdown, ${maxOf(0L, state.endsAtMillis - now) / 1000} s left"
    is ProtectionState.Grace -> "Grace, ${maxOf(0L, state.endsAtMillis - now) / 1000} s left"
}
