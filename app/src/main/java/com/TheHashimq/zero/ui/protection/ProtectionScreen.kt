/*
 * MODULE: app (protection screen)
 * PURPOSE: Shows protection status, the minimum commitment, controls to raise it, start, and recent events.
 * TAKES: The ProtectionModule from ZeroApplication.
 * GIVES: A Compose screen that refreshes every second.
 * OWNS: Only the displayed values and the last message.
 * DOES NOT OWN: Protection or minimum rules. Every action goes through the coordinator or repository.
 * DEPENDENCIES: feature-protection-runtime, foundation-design, Compose Material3, shared components.
 * CONTRACT: Start is enabled only when Armed, connected, and at least one app is protected.
 * FAILURE BEHAVIOR: Storage errors are shown on screen and never crash the UI.
 */
package com.TheHashimq.zero.ui.protection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.TheHashimq.zero.ui.components.PrimaryButton
import com.TheHashimq.zero.ui.components.SectionLabel
import com.TheHashimq.zero.ui.components.SecondaryButton
import com.TheHashimq.zero.ui.components.StatusLine
import kotlinx.coroutines.delay
import zero.feature.protection.CooldownRules
import zero.feature.protection.ProtectionState
import zero.feature.protection.RaiseResult
import zero.feature.protection.TimeFormat
import zero.feature.protection.runtime.ProtectionModule
import zero.foundation.design.DesignTokens
import zero.foundation.persistence.PersistenceException

private const val MINUTE_MILLIS = 60_000L
private val RAISE_OPTIONS_MINUTES = listOf(120L, 150L, 180L)

@Composable
fun ProtectionScreen(module: ProtectionModule) {
    val coordinator = module.coordinator
    val repository = module.repository
    val cooldown = module.cooldown
    if (coordinator == null || repository == null || cooldown == null) {
        StatusLine("Protection is starting", Color(DesignTokens.WARNING))
        return
    }

    var state by remember { mutableStateOf(coordinator.state()) }
    var now by remember { mutableLongStateOf(coordinator.nowMillis()) }
    var connected by remember { mutableStateOf(coordinator.isServiceConnected()) }
    var protectedCount by remember { mutableStateOf(0) }
    var minimum by remember { mutableLongStateOf(CooldownRules.FLOOR_MILLIS) }
    var events by remember { mutableStateOf(coordinator.recentEvents(5)) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(coordinator, repository, cooldown) {
        while (true) {
            try {
                state = coordinator.tick()
                protectedCount = repository.packages().size
                minimum = cooldown.minimumMillis()
                events = coordinator.recentEvents(5)
            } catch (failure: PersistenceException) {
                message = "Storage error: ${failure.message}"
            }
            now = coordinator.nowMillis()
            connected = coordinator.isServiceConnected()
            delay(1_000L)
        }
    }

    val canStart = state == ProtectionState.Armed && connected && protectedCount > 0
    val raiseOptions = RAISE_OPTIONS_MINUTES.filter { it * MINUTE_MILLIS > minimum }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionLabel("Status")
            StatusLine(describe(state, now), MaterialTheme.colorScheme.onBackground)
            StatusLine("Protected apps: $protectedCount", MaterialTheme.colorScheme.onSurfaceVariant)
            StatusLine(
                "Minimum commitment: ${minimum / MINUTE_MILLIS} min",
                MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (!connected) {
            StatusLine(
                "Turn on Zero's accessibility service in Settings before starting a commitment.",
                Color(DesignTokens.WARNING),
            )
        }
        if (protectedCount == 0) {
            StatusLine(
                "Protect at least one app first, from Protected apps in the menu.",
                MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Minimum")
            if (raiseOptions.isEmpty()) {
                StatusLine("Minimum is at its maximum.", MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                StatusLine(
                    "Raise only. The minimum cannot be lowered.",
                    MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    raiseOptions.forEach { minutes ->
                        SecondaryButton(
                            text = "$minutes min",
                            modifier = Modifier.weight(1f),
                        ) {
                            message = try {
                                raiseMessage(cooldown.raise(minutes * MINUTE_MILLIS))
                            } catch (failure: PersistenceException) {
                                "Storage error: ${failure.message}"
                            }
                            try {
                                minimum = cooldown.minimumMillis()
                            } catch (failure: PersistenceException) {
                                message = "Storage error: ${failure.message}"
                            }
                        }
                    }
                }
            }
        }

        PrimaryButton(
            text = "Start ${minimum / MINUTE_MILLIS} min commitment",
            enabled = canStart,
        ) {
            message = try {
                val started = coordinator.startCommitment(minimum)
                if (started) null else "Cannot start right now: check the boot ID, protected apps, and state."
            } catch (failure: PersistenceException) {
                "Storage error: ${failure.message}"
            }
            state = coordinator.state()
        }

        message?.let { StatusLine(it, MaterialTheme.colorScheme.onBackground) }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionLabel("Recent events")
            if (events.isEmpty()) {
                StatusLine("No events yet.", MaterialTheme.colorScheme.onSurfaceVariant)
            }
            events.forEach { event ->
                val details = event.payload.entries.joinToString { "${it.key}=${it.value}" }
                StatusLine(
                    if (details.isEmpty()) event.type.value else "${event.type.value}  $details",
                    MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
    ProtectionState.Armed -> "State: Armed (nothing blocked)"
    is ProtectionState.Countdown -> "State: Committed, ${TimeFormat.remaining(state.endsAtMillis - now)} left"
    is ProtectionState.Grace -> "State: Grace, ${TimeFormat.remaining(state.endsAtMillis - now)} left"
}
