/*
 * MODULE: feature-streaks tests
 * PURPOSE: Verifies limit streaks, including unlogged days and immediate failure when exceeded.
 * TAKES: A fixed daily limit streak and entries.
 * GIVES: Pass/fail results for limit rules.
 * OWNS: No production state.
 * DOES NOT OWN: Other modes.
 * DEPENDENCIES: feature-streaks.
 * CONTRACT: Each test asserts one limit rule.
 * FAILURE BEHAVIOR: Assertion failures identify the broken rule.
 */
package zero.feature.streaks

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class LimitRulesTest {
    private val today = LocalDate.of(2026, 1, 10)
    private val pastDay = LocalDate.of(2026, 1, 9)

    private val screenTime = StreakDefinition(
        id = "screen",
        name = "Screen time",
        emoji = "S",
        colorIndex = 0,
        mode = StreakMode.LIMIT,
        frequency = Frequency.DAILY,
        target = 120,
        parts = emptyList(),
        createdOn = LocalDate.of(2026, 1, 1),
    )

    private fun dayResult(entries: List<Entry>, date: LocalDate): PeriodResult =
        StreakEngine.summarize(screenTime, entries, today).periods.first { it.start == date }

    /** An unlogged today is open, not missed. */
    @Test
    fun unloggedTodayIsOpen() {
        assertEquals(PeriodStatus.OPEN, dayResult(emptyList(), today).status)
    }

    /** An unlogged past day counts as missed. */
    @Test
    fun unloggedPastDayIsMissed() {
        assertEquals(PeriodStatus.MISSED, dayResult(emptyList(), pastDay).status)
    }

    /** A logged total within the limit passes once the day is over. Equal to the limit still passes. */
    @Test
    fun withinLimitPastDayIsMet() {
        assertEquals(PeriodStatus.MET, dayResult(listOf(Entry(pastDay, amount = 120)), pastDay).status)
    }

    /** Exceeding the limit fails at once, even while today is still open. */
    @Test
    fun exceedingLimitIsMissedEvenToday() {
        assertEquals(PeriodStatus.MISSED, dayResult(listOf(Entry(today, amount = 130)), today).status)
    }

    /** A logged total within the limit today is open until the day ends. */
    @Test
    fun withinLimitTodayIsOpen() {
        assertEquals(PeriodStatus.OPEN, dayResult(listOf(Entry(today, amount = 60)), today).status)
    }
}
