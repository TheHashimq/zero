/*
 * MODULE: app (streak form screen)
 * PURPOSE: Add and edit form for a streak, with mode, frequency, target, checklist parts, color, and delete.
 * TAKES: The StreakStore, the streak being edited or null for a new one, and callbacks for done and cancel.
 * GIVES: Saves through StreakStore, shows validation errors, and asks before deleting.
 * OWNS: Only the form field state and the error message.
 * DOES NOT OWN: Validation rules (StreakFormRules) and storage (StreakRepository).
 * DEPENDENCIES: feature-streaks, Compose Material3, shared components.
 * CONTRACT: Save and delete go through StreakStore.update. Locked fields are disabled when the streak has entries.
 * FAILURE BEHAVIOR: Save and delete failures are shown as messages; the form stays open.
 */
package com.TheHashimq.zero.ui.streaks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.TheHashimq.zero.ui.components.SecondaryButton
import com.TheHashimq.zero.ui.components.StatusLine
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random
import zero.feature.streaks.Frequency
import zero.feature.streaks.StreakDefinition
import zero.feature.streaks.StreakMode
import zero.feature.streaks.StreakPalette
import zero.feature.streaks.presentation.StreakForm
import zero.feature.streaks.presentation.StreakFormPart
import zero.feature.streaks.presentation.StreakFormRules
import zero.foundation.design.DesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakFormScreen(
    store: StreakStore,
    existing: StreakDefinition?,
    onDone: () -> Unit,
    onDeleted: () -> Unit,
) {
    val repo = store.repository
    val hasEntries = existing != null && repo?.entries(existing.id)?.isNotEmpty() == true
    val usedColors = repo?.definitions()?.map { it.colorIndex }.orEmpty()
    val initial = existing?.let { StreakFormRules.fromDefinition(it) } ?: StreakForm(
        name = "",
        emoji = "B",
        colorIndex = StreakPalette.pickIndex(usedColors, Random.Default),
        mode = StreakMode.AMOUNT,
        frequency = Frequency.DAILY,
        target = 1,
        parts = emptyList(),
    )

    var name by remember { mutableStateOf(initial.name) }
    var emoji by remember { mutableStateOf(initial.emoji) }
    var colorIndex by remember { mutableStateOf(initial.colorIndex) }
    var mode by remember { mutableStateOf(initial.mode) }
    var frequency by remember { mutableStateOf(initial.frequency) }
    var target by remember { mutableStateOf(initial.target) }
    val parts = remember { mutableStateListOf<StreakFormPart>().apply { addAll(initial.parts) } }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val form = StreakForm(name, emoji, colorIndex, mode, frequency, target, parts.toList())
    val lockMode = hasEntries
    val lockParts = hasEntries

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionLabel(if (existing == null) "New streak" else "Edit streak")

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = emoji,
            onValueChange = { emoji = it.take(2) },
            label = { Text("Emoji") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        SectionLabel("Color")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StreakPalette.COLORS.indices.chunked(6).forEach { rowIndices ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowIndices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(StreakPalette.COLORS[index]))
                                .border(
                                    width = if (index == colorIndex) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    shape = CircleShape,
                                )
                                .clickable { colorIndex = index },
                        )
                    }
                }
            }
        }

        SectionLabel("Mode")
        if (lockMode) {
            StatusLine("Locked: this streak already has entries.", MaterialTheme.colorScheme.onSurfaceVariant)
        }
        StreakMode.entries.toList().chunked(2).forEach { rowModes ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowModes.forEach { option ->
                    FilterChip(
                        selected = mode == option,
                        onClick = {
                            if (!lockMode) {
                                mode = option
                                if (option == StreakMode.CHECKLIST) frequency = Frequency.DAILY
                            }
                        },
                        label = { Text(modeLabel(option)) },
                        enabled = !lockMode,
                    )
                }
            }
        }

        if (mode != StreakMode.CHECKLIST) {
            SectionLabel("Repeats")
            listOf(Frequency.DAILY, Frequency.WEEKLY, Frequency.MONTHLY, Frequency.YEARLY)
                .chunked(2)
                .forEach { rowFrequencies ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowFrequencies.forEach { option ->
                            FilterChip(
                                selected = frequency == option,
                                onClick = { if (!lockMode) frequency = option },
                                label = { Text(option.name.lowercase()) },
                                enabled = !lockMode,
                            )
                        }
                    }
                }
            SectionLabel(targetLabel(mode))
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = { if (target > 1) target-- }) { Text("−") }
                Text(target.toString(), style = MaterialTheme.typography.titleLarge)
                OutlinedButton(onClick = { if (target < Int.MAX_VALUE) target++ }) { Text("+") }
            }
            if (hasEntries) {
                StatusLine(
                    "Changing the target may change how past periods are evaluated.",
                    MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            SectionLabel("Parts")
            if (lockParts) {
                StatusLine("Locked: this streak already has entries.", MaterialTheme.colorScheme.onSurfaceVariant)
            }
            parts.forEachIndexed { index, part ->
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = part.name,
                        onValueChange = { parts[index] = part.copy(name = it) },
                        label = { Text("Part") },
                        singleLine = true,
                        enabled = !lockParts,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(
                        onClick = { if (part.weight > 1) parts[index] = part.copy(weight = part.weight - 1) },
                        enabled = !lockParts,
                    ) { Text("−") }
                    Text("${part.weight}")
                    OutlinedButton(
                        onClick = {
                            if (part.weight < StreakFormRules.MAX_WEIGHT) {
                                parts[index] = part.copy(weight = part.weight + 1)
                            }
                        },
                        enabled = !lockParts,
                    ) { Text("+") }
                    OutlinedButton(onClick = { parts.removeAt(index) }, enabled = !lockParts) { Text("Remove") }
                }
            }
            if (!lockParts) {
                SecondaryButton("Add part") { parts.add(StreakFormPart("", 1)) }
            }
            StatusLine("Required parts are the ones with the highest weight.", MaterialTheme.colorScheme.onSurfaceVariant)
        }

        message?.let { StatusLine(it, Color(DesignTokens.CRITICAL)) }

        PrimaryButton(if (existing == null) "Add streak" else "Save changes") {
            message = save(store, existing, form, hasEntries)
            if (message == null) onDone()
        }
        SecondaryButton("Cancel") { onDone() }

        if (existing != null) {
            OutlinedButton(onClick = { confirmDelete = true }) { Text("Delete streak") }
        }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${existing.name}?") },
            text = { Text("Its history will be lost. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    val result = store.update { it.deleteStreak(existing.id) }
                    if (result.isSuccess) {
                        onDeleted()
                    } else {
                        message = result.exceptionOrNull()?.message ?: "Could not delete the streak."
                    }
                    confirmDelete = false
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

/** Returns an error message, or null when the streak was saved. */
private fun save(store: StreakStore, existing: StreakDefinition?, form: StreakForm, hasEntries: Boolean): String? {
    if (existing == null) {
        val built = StreakFormRules.build(form, UUID.randomUUID().toString(), LocalDate.now(), DayOfWeek.MONDAY)
        val def = built.getOrElse { return it.message ?: "Invalid streak details." }
        val result = store.update { it.addStreak(def) }
        return if (result.isSuccess) null else result.exceptionOrNull()?.message ?: "Could not save the streak."
    }
    val locked = StreakFormRules.lockedChangeErrors(existing, form, hasEntries)
    if (locked.isNotEmpty()) return locked.joinToString(" ")
    val built = StreakFormRules.build(form, existing.id, existing.createdOn, existing.weekStart)
    val def = built.getOrElse { return it.message ?: "Invalid streak details." }
    val result = store.update { it.updateStreak(def) }
    return if (result.isSuccess) null else result.exceptionOrNull()?.message ?: "Could not save the streak."
}

private fun modeLabel(mode: StreakMode): String = when (mode) {
    StreakMode.CHECK_IN -> "Check-in"
    StreakMode.AMOUNT -> "Amount"
    StreakMode.LIMIT -> "Limit"
    StreakMode.CHECKLIST -> "Checklist"
}

private fun targetLabel(mode: StreakMode): String = when (mode) {
    StreakMode.CHECK_IN -> "Check-ins per period"
    StreakMode.AMOUNT -> "Amount per period"
    StreakMode.LIMIT -> "Maximum per period"
    StreakMode.CHECKLIST -> "Target"
}
