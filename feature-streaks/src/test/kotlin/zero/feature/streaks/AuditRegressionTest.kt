/*
 * MODULE: feature-streaks tests
 * PURPOSE: Regression tests for the audit findings: limit days under the limit, required checklist parts,
 *          open limit days at the target, and skipping invalid stored entries.
 * TAKES: Fixed streak definitions and entries.
 * GIVES: Pass/fail results for each corrected rule.
 * OWNS: No production state.
 * DOES NOT OWN: Other engine behavior, covered by the original test classes.
 * DEPENDENCIES: feature-streaks.
 * CONTRACT: Each test asserts one corrected rule.
 * FAILURE BEHAVIOR: Assertion failures identify the rule still broken.
 */
package zero.feature.streaks

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class AuditRegressionTest {
    private val today = LocalDate.of(2026, 1, 10)
    private val pastDay = LocalDate.of(2026, 1, 9)
    private val created = LocalDate.of(2026, 1, 1)

    private fun limit(target: Int) = StreakDefinition(
        id = "screen",
        name = "Screen",
        emoji = "S",
        colorIndex = 0,
        mode = StreakMode.LIMIT,
        frequency = Frequency.DAILY,
        target = target,
        parts = emptyList(),
        createdOn = created,
    )

    private fun prayer() = StreakDefinition(
        id = "fajr",
        name = "Fajr",
        emoji = "F",
        colorIndex = 0,
        mode = StreakMode.CHECKLIST,
        frequency = Frequency.DAILY,
        target = 0,
        parts = listOf(
            ChecklistPart("fard", 3),
            ChecklistPart("sunnah", 1),
            ChecklistPart("witr", 2),
        ),
        createdOn = created,
    )

    private fun dayStatus(def: StreakDefinition, entries: List<Entry>, day: LocalDate): PeriodStatus =
        StreakEngine.summarize(def, entries, today).periods.first { it.start == day }.status

    /** A past limit day with usage under the limit passes. */
    @Test
    fun pastLimitDayUnderLimitIsMet() {
        assertEquals(PeriodStatus.MET, dayStatus(limit(120), listOf(Entry(pastDay, amount = 60)), pastDay))
    }

    /** An open limit day exactly at the limit stays open until the day ends. */
    @Test
    fun openLimitDayAtTargetStaysOpen() {
        assertEquals(PeriodStatus.OPEN, dayStatus(limit(120), listOf(Entry(today, amount = 120)), today))
    }

    /** A past limit day exactly at the limit passes. */
    @Test
    fun pastLimitDayAtTargetIsMet() {
        assertEquals(PeriodStatus.MET, dayStatus(limit(120), listOf(Entry(pastDay, amount = 120)), pastDay))
    }

    /** A checklist fails when the required fard is missing, even if optional parts add up to the required weight. */
    @Test
    fun checklistFailsWithoutRequiredPart() {
        val entries = listOf(Entry(pastDay, parts = setOf("sunnah", "witr")))

        assertEquals(PeriodStatus.MISSED, dayStatus(prayer(), entries, pastDay))
    }

    /** A checklist passes when every required part is checked. */
    @Test
    fun checklistPassesWithRequiredPart() {
        val entries = listOf(Entry(pastDay, parts = setOf("fard")))

        assertEquals(PeriodStatus.MET, dayStatus(prayer(), entries, pastDay))
    }

    /** With tied top weights, every tied part is required. */
    @Test
    fun tiedTopPartsAreAllRequired() {
        val tied = prayer().copy(
            parts = listOf(ChecklistPart("fard", 3), ChecklistPart("fard2", 3), ChecklistPart("sunnah", 1)),
        )
        val entries = listOf(Entry(pastDay, parts = setOf("fard", "sunnah")))

        assertEquals(PeriodStatus.MISSED, dayStatus(tied, entries, pastDay))
    }

    /** An invalid stored entry is skipped, and the rest of the summary still works. */
    @Test
    fun invalidEntryIsSkippedNotFatal() {
        val entries = listOf(
            Entry(pastDay, amount = 120),
            Entry(pastDay, parts = setOf("not-a-part")),
        )

        val summary = StreakEngine.summarize(limit(120), entries, today)

        assertEquals(PeriodStatus.MET, summary.periods.first { it.start == pastDay }.status)
    }

    /** The StreakMode declaration order matches the agreed order. */
    @Test
    fun modeOrderMatchesAgreedOrder() {
        assertEquals(
            listOf(StreakMode.CHECK_IN, StreakMode.AMOUNT, StreakMode.LIMIT, StreakMode.CHECKLIST),
            StreakMode.entries,
        )
    }
}
