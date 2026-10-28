/*
 * MODULE: app (main screen)
 * PURPOSE: Protection state, the timer, and one primary action.
 * TAKES: The ProtectionModule, and a callback to open the protected-apps screen.
 * GIVES: A Compose screen that refreshes every second.
 * OWNS: Only the displayed values and the last message.
 * DOES NOT OWN: Protection rules. Every action goes through the coordinator or repository.
 * DEPENDENCIES: feature-protection, feature-protection-runtime, foundation-design, Compose Material3.
 * CONTRACT: Exactly one primary action is shown, chosen in this order: enable service, choose apps, start, or none.
 * FAILURE BEHAVIOR: Storage errors appear as a message and never crash the screen.
 */
package com.TheHashimq.zero.ui.main

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.TheHashimq.zero.ui.components.PrimaryButton
import com.TheHashimq.zero.ui.components.SectionLabel
import com.TheHashimq.zero.ui.components.StatusLine
import kotlinx.coroutines.delay
import zero.feature.protection.ProtectionState
import zero.feature.protection.TimeFormat
import zero.feature.protection.runtime.ProtectionModule
import zero.foundation.design.DesignTokens
import zero.foundation.persistence.PersistenceException

private const val MINUTE_MILLIS = 60_000L

@Composable
fun MainScreen(module: ProtectionModule, onOpenProtectedApps: () -> Unit) {
    val coordinator = module.coordinator
    val repository = module.repository
    val cooldown = module.cooldown
    if (coordinator == null || repository == null || cooldown == null) {
        Text("Starting Zero")
        return
    }
    val context = LocalContext.current

    var state by remember { mutableStateOf(coordinator.state()) }
    var now by remember { mutableLongStateOf(coordinator.nowMillis()) }
    var connected by remember { mutableStateOf(coordinator.isServiceConnected()) }
    var protectedCount by remember { mutableStateOf(0) }
    var minimum by remember { mutableLongStateOf(90L * MINUTE_MILLIS) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(coordinator, repository, cooldown) {
        while (true) {
            try {
                state = coordinator.tick()
                protectedCount = repository.packages().size
                minimum = cooldown.minimumMillis()
                message = null
            } catch (failure: PersistenceException) {
                message = "Storage error: ${failure.message}"
            }
            now = coordinator.nowMillis()
            connected = coordinator.isServiceConnected()
            delay(1_000L)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(stateLabel(state))
            Text(
                text = timerText(state, now),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = subtitle(state, protectedCount),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "$protectedCount protected app(s)",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!connected) {
                StatusLine(
                    "Protection is off. Turn on Zero's accessibility service to enable it.",
                    Color(DesignTokens.WARNING),
                )
            }
            message?.let { StatusLine(it, MaterialTheme.colorScheme.onSurfaceVariant) }

            when {
                !connected -> PrimaryButton("Enable protection") {
                    context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
                protectedCount == 0 -> PrimaryButton("Choose apps to protect", onOpenProtectedApps)
                state == ProtectionState.Armed -> PrimaryButton(
                    "Start ${minimum / MINUTE_MILLIS} min commitment",
                ) {
                    message = try {
                        if (coordinator.startCommitment(minimum)) null else "Cannot start right now."
                    } catch (failure: PersistenceException) {
                        "Storage error: ${failure.message}"
                    }
                    state = coordinator.state()
                }
                else -> StatusLine("Commitment in progress.", MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun stateLabel(state: ProtectionState): String = when (state) {
    ProtectionState.Armed -> "Armed"
    is ProtectionState.Countdown -> "Committed"
    is ProtectionState.Grace -> "Grace"
}

private fun timerText(state: ProtectionState, now: Long): String = when (state) {
    ProtectionState.Armed -> "Ready"
    is ProtectionState.Countdown -> TimeFormat.remaining(state.endsAtMillis - now)
    is ProtectionState.Grace -> TimeFormat.remaining(state.endsAtMillis - now)
}

private fun subtitle(state: ProtectionState, protectedCount: Int): String = when (state) {
    ProtectionState.Armed ->
        if (protectedCount == 0) "Nothing is blocked yet."
        else "$protectedCount app(s) protected. Nothing is blocked until you start."
    is ProtectionState.Countdown -> "Protected apps are blocked until the timer ends."
    is ProtectionState.Grace -> "Apps are open during the grace period, then protection resumes."
}
