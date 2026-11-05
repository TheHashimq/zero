/*
 * MODULE: app (streak details screen)
 * PURPOSE: Shows one streak centered: current and best, today's control, the six-month strip,
 *          the month calendar, and history with single-entry delete.
 * TAKES: The StreakStore, the streak ID, and callbacks for edit and back.
 * GIVES: Logging for today, checklist toggles, and deletion of single entries, all through StreakStore.update.
 * OWNS: The amount draft and the message line.
 * DOES NOT OWN: Rules (feature-streaks) and storage (StreakRepository).
 * DEPENDENCIES: feature-streaks, foundation-design, Compose Material3, shared components.
 * CONTRACT: Every change goes through StreakStore.update and refreshes the screen. Past days are changed only by
 *           deleting their history rows. This file changes layout and styling only.
 * FAILURE BEHAVIOR: Errors are shown as messages and the screen stays open.
 */
package com.TheHashimq.zero.ui.streaks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.TheHashimq.zero.platform.streaks.StreakStore
import com.TheHashimq.zero.ui.components.PrimaryButton
import com.TheHashimq.zero.ui.components.SecondaryButton
import com.TheHashimq.zero.ui.components.StatusLine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import zero.feature.streaks.Entry
import zero.feature.streaks.StreakDefinition
import zero.feature.streaks.StreakPalette
import zero.feature.streaks.StreakSummary
import zero.feature.streaks.presentation.DayCell
import zero.feature.streaks.presentation.DetailViews
import zero.feature.streaks.presentation.TodayControl
import zero.feature.streaks.presentation.TodayControls
import zero.feature.streaks.storage.IdentifiedEntry
import zero.feature.streaks.storage.StreakRepository
import zero.foundation.design.DesignTokens

private val STEP_ALPHA = floatArrayOf(0f, 0.3f, 0.5f, 0.75f, 1f)

private data class StreakDetailsData(
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
    val data = store.read { repository ->
        repository.definitions().firstOrNull { it.id == streakId }?.let { definition ->
            StreakDetailsData(
                definition = definition,
                entries = repository.identifiedEntries(streakId),
                summary = repository.summary(streakId, today),
            )
        }
    }
    var message by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableIntStateOf(0) }

    if (data == null) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusLine("This streak is no longer available.", MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onBack) { Text("Back") }
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
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = def.emoji, fontSize = 40.sp)
            Text(
                text = def.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(56.dp, Alignment.CenterHorizontally),
        ) {
            StatBlock(value = summary.current.toString(), label = "Current")
            StatBlock(value = summary.best.toString(), label = "Best")
        }

        message?.let { StatusLine(it, Color(DesignTokens.CRITICAL)) }

        SectionCard(title = "Today") {
            when (control) {
                is TodayControl.CheckIn -> {
                    Text(
                        text = "${control.count} of ${control.target}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    PrimaryButton("Check in") { act { it.addEntry(def.id, Entry(today), today) } }
                    OutlinedButton(
                        onClick = {
                            identified.lastOrNull { it.entry.date == today }?.let { last ->
                                act { it.deleteEntry(def.id, last.id) }
                            }
                        },
                        enabled = control.count > 0,
                    ) { Text("Undo last check-in") }
                }

                is TodayControl.Amount -> AmountBlock(
                    label = "${control.total} of ${control.target}",
                    draft = draft,
                    onDraft = { draft = it },
                    onLog = { if (act { it.addEntry(def.id, Entry(today, amount = draft), today) }) draft = 0 },
                )

                is TodayControl.Limit -> AmountBlock(
                    label = if (control.over) "${control.total} of ${control.limit} · over the limit"
                    else "${control.total} of ${control.limit} used",
                    draft = draft,
                    over = control.over,
                    onDraft = { draft = it },
                    onLog = { if (act { it.addEntry(def.id, Entry(today, amount = draft), today) }) draft = 0 },
                )

                is TodayControl.Checklist -> {
                    control.parts.forEach { part ->
                        ChecklistRow(
                            name = part.name,
                            required = part.required,
                            checked = part.checked,
                            accent = accent,
                            onToggle = {
                                act {
                                    it.replaceDayEntries(
                                        def.id,
                                        today,
                                        TodayControls.toggleChecklistPart(def, todayEntries, today, part.name),
                                        today,
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }

        SectionCard(title = "Last six months") {
            SixMonthStrip(summary = summary, today = today, accent = accent)
        }

        SectionCard(title = monthTitle(YearMonth.now())) {
            CalendarGrid(
                summary = summary,
                month = YearMonth.now(),
                today = today,
                weekStart = def.weekStart,
                accent = accent,
            )
        }

        SectionCard(title = "History") {
            val rows = DetailViews.history(def, identified)
            if (rows.isEmpty()) {
                StatusLine("No entries yet.", MaterialTheme.colorScheme.onSurfaceVariant)
            }
            rows.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = row.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = row.date.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(onClick = { act { it.deleteEntry(def.id, row.id) } }) { Text("Delete") }
                }
                if (index < rows.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(DesignTokens.BORDER)),
                    )
                }
            }
        }

        SecondaryButton("Edit streak", modifier = Modifier.fillMaxWidth()) { onEdit() }
        Box(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StatBlock(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(DesignTokens.ELEVATED))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        content()
    }
}

@Composable
private fun ChecklistRow(
    name: String,
    required: Boolean,
    checked: Boolean,
    accent: Color,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (checked) accent.copy(alpha = 0.16f) else Color(DesignTokens.SURFACE))
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (checked) accent else Color.Transparent)
                .border(2.dp, if (checked) accent else MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(DesignTokens.BACKGROUND),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (required) {
                Text(
                    text = "Required",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                )
            }
        }
    }
}

@Composable
private fun AmountBlock(
    label: String,
    draft: Int,
    over: Boolean = false,
    onDraft: (Int) -> Unit,
    onLog: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = if (over) Color(DesignTokens.CRITICAL) else MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(onClick = { onDraft((draft - 10).coerceAtLeast(0)) }) { Text("−10") }
        OutlinedButton(onClick = { onDraft((draft - 1).coerceAtLeast(0)) }) { Text("−1") }
        Text(
            text = "$draft",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(72.dp),
        )
        OutlinedButton(onClick = { onDraft(draft + 1) }) { Text("+1") }
        OutlinedButton(onClick = { onDraft(draft + 10) }) { Text("+10") }
    }
    PrimaryButton(if (draft > 0) "Log $draft" else "Log amount", enabled = draft > 0) { onLog() }
}

@Composable
private fun SixMonthStrip(summary: StreakSummary, today: LocalDate, accent: Color) {
    val weeks = DetailViews.strip(summary, today).chunked(7)
    val scroll = rememberScrollState()
    LaunchedEffect(Unit) { scroll.scrollTo(scroll.maxValue) }
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        weeks.forEach { week ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { cell ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(fillFor(cell.step, accent))
                            .then(
                                if (cell.isToday) {
                                    Modifier.border(
                                        1.5.dp,
                                        MaterialTheme.colorScheme.onSurface,
                                        RoundedCornerShape(3.dp),
                                    )
                                } else {
                                    Modifier
                                },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    summary: StreakSummary,
    month: YearMonth,
    today: LocalDate,
    weekStart: DayOfWeek,
    accent: Color,
) {
    val weekdays = (0L..6L).map { weekStart.plus(it) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        weekdays.forEach { day ->
            Text(
                text = day.name.take(2).lowercase().replaceFirstChar { it.uppercaseChar() },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
    DetailViews.month(summary, month, today, weekStart).forEach { week ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            week.forEach { cell -> DayTile(cell = cell, accent = accent, modifier = Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun DayTile(cell: DayCell, accent: Color, modifier: Modifier) {
    val fill = when {
        cell.isFuture -> Color.Transparent
        cell.step == 0 -> Color(DesignTokens.SURFACE)
        else -> accent.copy(alpha = STEP_ALPHA[cell.step])
    }
    val textColor = when {
        !cell.inMonth || cell.isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        cell.step >= 3 -> Color(DesignTokens.BACKGROUND)
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(fill)
            .then(
                if (cell.isToday) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(10.dp))
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = cell.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}

private fun monthTitle(month: YearMonth): String =
    "${month.month.name.lowercase().replaceFirstChar { it.uppercaseChar() }} ${month.year}"

private fun fillFor(step: Int, accent: Color): Color =
    if (step == 0) Color(DesignTokens.SURFACE) else accent.copy(alpha = STEP_ALPHA[step])
