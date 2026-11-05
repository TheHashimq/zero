/*
 * MODULE: feature-streaks
 * PURPOSE: Validates the add and edit form and turns it into a StreakDefinition.
 * TAKES: A StreakForm, the existing definition when editing, and whether the streak already has entries.
 * GIVES: A list of readable errors, a built definition, or the locked-field errors for an edit.
 * OWNS: The name, weight, and target limits.
 * DOES NOT OWN: Storage or screens.
 * DEPENDENCIES: StreakDefinition types, StreakPalette.
 * CONTRACT: Checklists are daily, have at least one part, and have unique non-blank names with weights 1 to 10.
 *           Other modes need a target of at least 1. Edits are allowed with history; saving converts entries.
 * FAILURE BEHAVIOR: Never throws. build returns a failure when the form is invalid.
 */
package zero.feature.streaks.presentation

import java.time.DayOfWeek
import java.time.LocalDate
import zero.feature.streaks.ChecklistPart
import zero.feature.streaks.Frequency
import zero.feature.streaks.StreakDefinition
import zero.feature.streaks.StreakMode
import zero.feature.streaks.StreakPalette

/** Unvalidated checklist input, including temporarily blank names and out-of-range weights. */
data class StreakFormPart(
    val name: String,
    val weight: Int,
)

/** The values the add and edit form collects. */
data class StreakForm(
    val name: String,
    val emoji: String,
    val colorIndex: Int,
    val mode: StreakMode,
    val frequency: Frequency,
    val target: Int,
    val parts: List<StreakFormPart>,
)

object StreakFormRules {
    const val MAX_NAME = 40
    const val MAX_WEIGHT = 10

    /** Readable problems with [form], or an empty list when it is valid. */
    fun errors(form: StreakForm): List<String> {
        val problems = mutableListOf<String>()
        val name = form.name.trim()
        if (name.isEmpty()) problems += "Give the streak a name."
        if (name.length > MAX_NAME) problems += "Keep the name to $MAX_NAME characters."
        if (form.emoji.isBlank()) problems += "Choose an emoji."
        if (form.colorIndex !in 0 until StreakPalette.SIZE) problems += "Choose a color."
        if (form.mode == StreakMode.CHECKLIST) {
            if (form.frequency != Frequency.DAILY) problems += "Checklists repeat daily."
            if (form.parts.isEmpty()) problems += "Add at least one part."
            val names = form.parts.map { it.name.trim() }
            if (names.any { it.isEmpty() }) problems += "Every part needs a name."
            val filled = names.filter { it.isNotEmpty() }
            if (filled.toSet().size != filled.size) problems += "Part names must be different."
            if (form.parts.any { it.weight !in 1..MAX_WEIGHT }) problems += "Weights are 1 to $MAX_WEIGHT."
        } else if (form.target < 1) {
            problems += "The target must be at least 1."
        }
        return problems
    }

    /** Builds a definition from a valid form. Fails with the readable errors when the form is invalid. */
    fun build(
        form: StreakForm,
        id: String,
        createdOn: LocalDate,
        weekStart: DayOfWeek,
    ): Result<StreakDefinition> {
        val problems = errors(form)
        if (problems.isNotEmpty()) return Result.failure(IllegalArgumentException(problems.joinToString(" ")))
        return runCatching {
            StreakDefinition(
                id = id,
                name = form.name.trim(),
                emoji = form.emoji.trim(),
                colorIndex = form.colorIndex,
                mode = form.mode,
                frequency = if (form.mode == StreakMode.CHECKLIST) Frequency.DAILY else form.frequency,
                target = if (form.mode == StreakMode.CHECKLIST) 0 else form.target,
                parts = if (form.mode == StreakMode.CHECKLIST) {
                    form.parts.map { ChecklistPart(it.name.trim(), it.weight) }
                } else {
                    emptyList()
                },
                createdOn = createdOn,
                weekStart = weekStart,
            )
        }
    }

    /** The form values for an existing streak. Checklist targets are shown as 1 for mode switching. */
    fun fromDefinition(def: StreakDefinition): StreakForm = StreakForm(
        name = def.name,
        emoji = def.emoji,
        colorIndex = def.colorIndex,
        mode = def.mode,
        frequency = def.frequency,
        target = if (def.mode == StreakMode.CHECKLIST) 1 else def.target,
        parts = def.parts.map { StreakFormPart(it.name, it.weight) },
    )

    /** Edits are always allowed. Saving converts existing history to the new mode. */
    fun lockedChangeErrors(existing: StreakDefinition, form: StreakForm, hasEntries: Boolean): List<String> = emptyList()
}
