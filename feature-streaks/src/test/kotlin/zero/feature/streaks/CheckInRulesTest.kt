/*
 * MODULE: feature-streaks tests
 * PURPOSE: Verifies check-in streaks over a weekly period.
 * TAKES: A fixed weekly check-in streak and entries.
 * GIVES: Pass/fail results for check-in rules.
 * OWNS: No production state.
 * DOES NOT OWN: Other modes.
 * DEPENDENCIES: feature-streaks.
 * CONTRACT: Each test asserts one check-in rule.
 * FAILURE BEHAVIOR: Assertion failures identify the broken rule.
 */
package zero.feature.streaks

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class CheckInRulesTest {
    private val today = LocalDate.of(2026, 1, 10)
    private val thisWeek = LocalDate.of(2026, 1, 5)
    private val lastWeek = LocalDate.of(2025, 12, 29)

    private val gym = StreakDefinition(
        id = "gym",
        name = "Gym",
        emoji = "G",
        colorIndex = 0,
        mode = StreakMode.CHECK_IN,
        frequency = Frequency.WEEKLY,
        target = 3,
        parts = emptyList(),
        createdOn = LocalDate.of(2026, 1, 1),
    )

    private fun week(entries: List<Entry>, start: LocalDate): PeriodResult =
        StreakEngine.summarize(gym, entries, today).periods.first { it.start == start }

    /** Reaching the target early in an open week is met straight away. */
    @Test
    fun metEarlyInOpenWeek() {
        val entries = listOf(Entry(thisWeek), Entry(thisWeek.plusDays(1)), Entry(thisWeek.plusDays(2)))

        assertEquals(PeriodStatus.MET, week(entries, thisWeek).status)
    }

    /** Fewer check-ins than the target in an open week stays open. */
    @Test
    fun fewerThanTargetInOpenWeekIsOpen() {
        val entries = listOf(Entry(thisWeek), Entry(thisWeek.plusDays(1)))

        assertEquals(PeriodStatus.OPEN, week(entries, thisWeek).status)
    }

    /** Fewer check-ins than the target in a closed week is missed. */
    @Test
    fun fewerThanTargetInClosedWeekIsMissed() {
        val entries = listOf(Entry(lastWeek.plusDays(3)), Entry(lastWeek.plusDays(4)))

        assertEquals(PeriodStatus.MISSED, week(entries, lastWeek).status)
    }
}
