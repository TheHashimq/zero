/*
 * MODULE: app (streak details screen)
 * PURPOSE: Shows one streak: current and best, the today control, the year strip, the month calendar,
 *          and history with single-entry delete.
 * TAKES: The StreakStore, the streak ID, and callbacks for edit and back.
 * GIVES: Logging for today, checklist toggles, and deletion of single entries, all through StreakStore.update.
 * OWNS: The amount draft and the message line.
 * DOES NOT OWN: Rules (feature-streaks) and storage (StreakRepository).
 * DEPENDENCIES: feature-streaks, foundation-design, Compose Material3, shared components.
 * CONTRACT: Every change goes through StreakStore.update and refreshes the screen. Past days are changed only by
 *           deleting their history rows.
 * FAILURE BEHAVIOR: Errors are shown as messages and the screen stays open.
 */
package com.TheHashimq.zero.ui.streaks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.TheHashimq.zero.platform.streaks.StreakStore
import com.TheHashimq.zero.ui.components.PrimaryButton
import com.TheHashimq.zero.ui.components.SecondaryButton
import com.TheHashimq.zero.ui.components.SectionLabel
import com.TheHashimq.zero.ui.components.StatusLine
import java.time.YearMonth
import zero.feature.streaks.Entry
import zero.feature.streaks.StreakDefinition
import zero.feature.streaks.StreakPalette
import zero.feature.streaks.presentation.DayCell
import zero.feature.streaks.presentation.DetailViews
import zero.feature.streaks.presentation.TodayControl
import zero.feature.streaks.presentation.TodayControls
import zero.feature.streaks.storage.IdentifiedEntry
import zero.feature.streaks.storage.StreakRepository
import zero.feature.streaks.StreakSummary
import zero.foundation.design.DesignTokens

private val STEP_ALPHA = floatArrayOf(0f, 0.3f, 0.5f, 0.75f, 1f)

private data class StreakDetailData(
    val definition: StreakDefinition,
    val entries: List<IdentifiedEntry>,
    val summary: StreakSummary,
)

@Composable
fun StreakDetailsScreen(
    store: StreakStore,
    streakId: String,
    onEdit: () -> Unit,
    onBack: () -> Unit,
) {
    val today = store.today()
    val data = store.read { repo ->
        val definition = repo.definitions().firstOrNull { it.id == streakId }
        definition?.let {
            StreakDetailData(it, repo.identifiedEntries(streakId), repo.summary(streakId, today))
        }
    }
    var message by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableIntStateOf(0) }

    if (data == null) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            StatusLine("This streak is no longer available.", MaterialTheme.colorScheme.onSurfaceVariant)
            SecondaryButton("Back") { onBack() }
        }
        return
    }

    val def = data.definition
    val identified = data.entries
    val todayEntries = identified.filter { it.entry.date == today }.map { it.entry }
    val summary = data.summary
    val control = TodayControls.build(def, todayEntries)
    val accent = Color(StreakPalette.COLORS[def.colorIndex])

    fun act(block: (StreakRepository) -> Unit): Boolean {
        val result = store.update { block(it) }
        message = result.exceptionOrNull()?.message
        return result.isSuccess
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
    ) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Text(
            text = "${def.emoji}  ${def.name}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column {
                Text("${summary.current}", style = MaterialTheme.typography.headlineMedium)
                SectionLabel("Current")
            }
            Column {
                Text("${summary.best}", style = MaterialTheme.typography.headlineMedium)
                SectionLabel("Best")
            }
        }
        message?.let { StatusLine(it, Color(DesignTokens.CRITICAL)) }

        SectionLabel("Today")
        when (control) {
            is TodayControl.CheckIn -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusLine("${control.count} of ${control.target} today", MaterialTheme.colorScheme.onBackground)
                PrimaryButton("Check in") { act { it.addEntry(def.id, Entry(today), today) } }
                SecondaryButton("Undo last check-in", enabled = control.count > 0) {
                    identified.lastOrNull { it.entry.date == today }?.let { last ->
                        act { it.deleteEntry(def.id, last.id) }
                    }
                }
            }

            is TodayControl.Amount -> AmountBlock(
                label = "${control.total} of ${control.target} today",
                draft = draft,
                over = false,
                onDraft = { draft = it },
                onLog = {
                    if (act { it.addEntry(def.id, Entry(today, amount = draft), today) }) draft = 0
                },
            )

            is TodayControl.Limit -> AmountBlock(
                label = "${control.total} of ${control.limit} used today" +
                    if (control.over) " · over the limit" else "",
                draft = draft,
                over = control.over,
                onDraft = { draft = it },
                onLog = {
                    if (act { it.addEntry(def.id, Entry(today, amount = draft), today) }) draft = 0
                },
            )

            is TodayControl.Checklist -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                control.parts.forEach { part ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                act {
                                    it.replaceDayEntries(
                                        def.id,
                                        today,
                                        TodayControls.toggleChecklistPart(def, todayEntries, today, part.name),
                                        today,
                                    )
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (part.checked) accent else MaterialTheme.colorScheme.outline),
                        )
                        Text(
                            text = if (part.required) "${part.name}  · required" else part.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
        }

        SectionLabel("Last six months")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            DetailViews.strip(summary, today).forEach { cell ->
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(fillFor(cell.step, accent)),
                )
            }
        }

        val month = YearMonth.now()
        SectionLabel("${month.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${month.year}")
        DetailViews.month(summary, month, today, def.weekStart).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { cell -> DayBox(cell, accent) }
            }
        }

        SectionLabel("History")
        val rows = DetailViews.history(def, identified)
        if (rows.isEmpty()) StatusLine("No entries yet.", MaterialTheme.colorScheme.onSurfaceVariant)
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    StatusLine(row.label, MaterialTheme.colorScheme.onBackground)
                    StatusLine(row.date.toString(), MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = { act { it.deleteEntry(def.id, row.id) } }) { Text("Delete") }
            }
        }

        SecondaryButton("Edit streak") { onEdit() }
    }
}

@Composable
private fun AmountBlock(
    label: String,
    draft: Int,
    over: Boolean,
    onDraft: (Int) -> Unit,
    onLog: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StatusLine(label, if (over) Color(DesignTokens.CRITICAL) else MaterialTheme.colorScheme.onBackground)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { onDraft((draft - 10).coerceAtLeast(0)) }) { Text("−10") }
            OutlinedButton(onClick = { onDraft((draft - 1).coerceAtLeast(0)) }) { Text("−1") }
            Text("$draft", style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = { onDraft((draft + 1).coerceAtMost(Int.MAX_VALUE)) }) { Text("+1") }
            OutlinedButton(onClick = { onDraft((draft + 10).coerceAtMost(Int.MAX_VALUE)) }) { Text("+10") }
        }
        PrimaryButton(if (draft > 0) "Log $draft" else "Log amount", enabled = draft > 0) { onLog() }
    }
}

@Composable
private fun DayBox(cell: DayCell, accent: Color) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(fillFor(cell.step, accent)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = cell.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = if (cell.inMonth) MaterialTheme.colorScheme.onBackground
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun fillFor(step: Int, accent: Color): Color =
    if (step == 0) Color(0xFF252B32) else accent.copy(alpha = STEP_ALPHA[step])
