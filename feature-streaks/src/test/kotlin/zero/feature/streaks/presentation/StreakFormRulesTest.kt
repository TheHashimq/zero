/*
 * MODULE: feature-streaks tests
 * PURPOSE: Verifies form validation, building definitions, and edit behavior with existing history.
 * TAKES: Explicit form values and definitions.
 * GIVES: Pass/fail results for the form rules.
 * OWNS: No production state.
 * DOES NOT OWN: Screens or storage.
 * DEPENDENCIES: feature-streaks.
 * CONTRACT: Each test asserts one form rule.
 * FAILURE BEHAVIOR: Assertion failures identify the wrong rule.
 */
package zero.feature.streaks.presentation

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import zero.feature.streaks.ChecklistPart
import zero.feature.streaks.Frequency
import zero.feature.streaks.StreakDefinition
import zero.feature.streaks.StreakMode

class StreakFormRulesTest {
    private val created = LocalDate.of(2026, 1, 1)

    private fun amount(name: String = "Reading", target: Int = 10) = StreakForm(
        name = name,
        emoji = "B",
        colorIndex = 0,
        mode = StreakMode.AMOUNT,
        frequency = Frequency.DAILY,
        target = target,
        parts = emptyList(),
    )

    private fun prayer(vararg parts: StreakFormPart) = StreakForm(
        name = "Fajr",
        emoji = "F",
        colorIndex = 0,
        mode = StreakMode.CHECKLIST,
        frequency = Frequency.DAILY,
        target = 0,
        parts = parts.toList(),
    )

    private fun definition(mode: StreakMode, frequency: Frequency, parts: List<ChecklistPart>) = StreakDefinition(
        id = "s1",
        name = "Fajr",
        emoji = "F",
        colorIndex = 0,
        mode = mode,
        frequency = frequency,
        target = if (mode == StreakMode.CHECKLIST) 0 else 1,
        parts = parts,
        createdOn = created,
    )

    /** A valid amount form has no errors and builds a definition. */
    @Test
    fun validAmountFormBuilds() {
        assertTrue(StreakFormRules.errors(amount()).isEmpty())
        assertTrue(StreakFormRules.build(amount(), "s1", created, DayOfWeek.MONDAY).isSuccess)
    }

    /** A blank name is rejected. */
    @Test
    fun blankNameIsRejected() {
        assertTrue(StreakFormRules.errors(amount(name = "   ")).any { it.contains("name") })
    }

    /** A name longer than the limit is rejected. */
    @Test
    fun longNameIsRejected() {
        assertTrue(StreakFormRules.errors(amount(name = "x".repeat(41))).any { it.contains("40") })
    }

    /** A target below 1 is rejected for non-checklist modes. */
    @Test
    fun zeroTargetIsRejected() {
        assertTrue(StreakFormRules.errors(amount(target = 0)).any { it.contains("target") })
    }

    /** A checklist must be daily. */
    @Test
    fun weeklyChecklistIsRejected() {
        val form = prayer(StreakFormPart("fard", 3)).copy(frequency = Frequency.WEEKLY)

        assertTrue(StreakFormRules.errors(form).any { it.contains("daily") })
    }

    /** A checklist needs at least one part. */
    @Test
    fun checklistWithoutPartsIsRejected() {
        assertTrue(StreakFormRules.errors(prayer()).any { it.contains("at least one part") })
    }

    /** Part names must be different from each other. */
    @Test
    fun duplicatePartNamesAreRejected() {
        val form = prayer(StreakFormPart("fard", 3), StreakFormPart("fard", 1))

        assertTrue(StreakFormRules.errors(form).any { it.contains("different") })
    }

    /** A weight outside 1 to 10 is rejected. */
    @Test
    fun weightOutOfRangeIsRejected() {
        assertTrue(StreakFormRules.errors(prayer(StreakFormPart("fard", 11))).any { it.contains("Weights") })
    }

    /** Mode can change once entries exist. History is converted when the streak is saved. */
    @Test
    fun modeChangeAllowedWhenEntriesExist() {
        val existing = definition(StreakMode.AMOUNT, Frequency.DAILY, emptyList())

        assertTrue(
            StreakFormRules.lockedChangeErrors(
                existing,
                amount().copy(mode = StreakMode.LIMIT),
                hasEntries = true,
            ).isEmpty(),
        )
    }

    /** With no entries, every field may change, including mode and parts. */
    @Test
    fun noEntriesAllowsAnyChange() {
        val existing = definition(StreakMode.AMOUNT, Frequency.DAILY, emptyList())

        assertTrue(
            StreakFormRules.lockedChangeErrors(
                existing,
                prayer(StreakFormPart("fard", 3)),
                hasEntries = false,
            ).isEmpty(),
        )
    }

    /** Checklist parts can change once entries exist. */
    @Test
    fun checklistChangeAllowedWhenEntriesExist() {
        val existing = definition(StreakMode.CHECKLIST, Frequency.DAILY, listOf(ChecklistPart("fard", 3)))

        assertTrue(
            StreakFormRules.lockedChangeErrors(
                existing,
                prayer(StreakFormPart("fard", 3), StreakFormPart("sunnah", 1)),
                hasEntries = true,
            ).isEmpty(),
        )
    }

    /** A target change is allowed once entries exist, since it is the current goal. */
    @Test
    fun targetChangeAllowedWhenEntriesExist() {
        val existing = definition(StreakMode.AMOUNT, Frequency.DAILY, emptyList())

        assertTrue(StreakFormRules.lockedChangeErrors(existing, amount(target = 20), hasEntries = true).isEmpty())
    }

    /** A definition converted to a form and back keeps its identity and fields. */
    @Test
    fun formRoundTripKeepsFields() {
        val existing = definition(StreakMode.CHECKLIST, Frequency.DAILY, listOf(ChecklistPart("fard", 3)))
        val form = StreakFormRules.fromDefinition(existing)

        val rebuilt = StreakFormRules.build(
            form,
            "s1",
            created,
            DayOfWeek.MONDAY,
        ).getOrThrow()

        assertFalse(rebuilt.parts.isEmpty())
        assertEquals(existing.parts, rebuilt.parts)
        assertEquals(existing.mode, rebuilt.mode)
        assertEquals(existing.name, rebuilt.name)
        assertEquals(1, form.target)
    }
}
