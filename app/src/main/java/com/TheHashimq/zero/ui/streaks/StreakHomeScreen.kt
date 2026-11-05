/*
 * MODULE: app (streaks home screen)
 * PURPOSE: Shows a card for each streak with today's progress, current and best streaks, and an Add action.
 * TAKES: The StreakStore from ZeroApplication.
 * GIVES: A Compose list that refreshes when the store changes or the date changes.
 * OWNS: Only the displayed notice.
 * DOES NOT OWN: Streak rules or storage. Those live in feature-streaks.
 * DEPENDENCIES: feature-streaks, foundation-design, Compose Material3, shared components.
 * CONTRACT: Read-only in this phase. Add shows a notice until the add form arrives in S3c.
 * FAILURE BEHAVIOR: A load error is shown as a message. Nothing crashes if storage is unavailable.
 */
package com.TheHashimq.zero.ui.streaks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.TheHashimq.zero.platform.streaks.StreakStore
import com.TheHashimq.zero.ui.components.PrimaryButton
import com.TheHashimq.zero.ui.components.SectionLabel
import com.TheHashimq.zero.ui.components.StatusLine
import zero.feature.streaks.PeriodStatus
import zero.feature.streaks.StreakPalette
import zero.feature.streaks.presentation.Progress
import zero.feature.streaks.presentation.StreakCard
import zero.feature.streaks.presentation.StreakCards
import zero.foundation.design.DesignTokens

@Composable
fun StreakHomeScreen(store: StreakStore) {
    val today = store.today()
    var notice by remember { mutableStateOf<String?>(null) }
    val cards = store.read { repo ->
        repo.definitions().map { definition ->
            StreakCards.build(definition, repo.summary(definition.id, today))
        }
    }.orEmpty()
    val loadError = store.loadError

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionLabel("Streaks")
        loadError?.let { StatusLine(it, Color(DesignTokens.WARNING)) }
        if (cards.isEmpty() && loadError == null) {
            StatusLine("No streaks yet.", MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(cards, key = { it.id }) { card -> StreakCardView(card) }
        }
        notice?.let { StatusLine(it, MaterialTheme.colorScheme.onSurfaceVariant) }
        PrimaryButton("Add a streak") {
            notice = "Adding streaks arrives in the next step."
        }
    }
}

@Composable
private fun StreakCardView(card: StreakCard) {
    val accent = Color(StreakPalette.COLORS[card.colorIndex])
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            Text(
                text = "${card.emoji}  ${card.name}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        StatusLine(card.summaryText, MaterialTheme.colorScheme.onSurfaceVariant)
        ProgressView(card.progress, accent)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatusLine("Current ${card.current}", MaterialTheme.colorScheme.onSurface)
            StatusLine("Best ${card.best}", MaterialTheme.colorScheme.onSurfaceVariant)
            StatusLine(statusText(card.status), statusColor(card.status))
        }
    }
}

@Composable
private fun ProgressView(progress: Progress, accent: Color) {
    when (progress) {
        is Progress.Dots -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(progress.total) { index ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (index < progress.filled) accent else MaterialTheme.colorScheme.outline),
                )
            }
        }
        is Progress.Bar -> {
            val fraction = (progress.value.toFloat() / progress.target).coerceIn(0f, 1f)
            val color = if (progress.over) Color(DesignTokens.CRITICAL) else accent
            BarTrack(fraction, color)
        }
        is Progress.Percent -> BarTrack(progress.percent / 100f, accent)
    }
}

@Composable
private fun BarTrack(fraction: Float, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.outline),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(color),
        )
    }
}

private fun statusText(status: PeriodStatus): String = when (status) {
    PeriodStatus.MET -> "Met"
    PeriodStatus.OPEN -> "In progress"
    PeriodStatus.MISSED -> "Missed"
}

private fun statusColor(status: PeriodStatus): Color = when (status) {
    PeriodStatus.MET -> Color(DesignTokens.HEALTHY)
    PeriodStatus.OPEN -> Color(DesignTokens.TEXT_SECONDARY)
    PeriodStatus.MISSED -> Color(DesignTokens.CRITICAL)
}
