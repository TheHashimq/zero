/*
 * MODULE: feature-streaks tests
 * PURPOSE: Verifies current and best streaks, today's status, and creation and future-date filtering.
 * TAKES: Fixed daily amount streaks and entries.
 * GIVES: Pass/fail results for the summary.
 * OWNS: No production state.
 * DOES NOT OWN: Per-mode rules, which have their own tests.
 * DEPENDENCIES: feature-streaks.
 * CONTRACT: Each test asserts one streak rule.
 * FAILURE BEHAVIOR: Assertion failures identify the broken rule.
 */
package zero.feature.streaks

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StreakEngineTest {
    private val today = LocalDate.of(2026, 1, 10)

    private fun def(createdOn: LocalDate = LocalDate.of(2026, 1, 1)) = StreakDefinition(
        id = "read",
        name = "Reading",
        emoji = "B",
        colorIndex = 0,
        mode = StreakMode.AMOUNT,
        frequency = Frequency.DAILY,
        target = 10,
        parts = emptyList(),
        createdOn = createdOn,
    )

    private fun met(vararg dates: LocalDate) = dates.map { Entry(it, amount = 10) }

    /** Yesterday and the days before it count, and open today does not break the run. */
    @Test
    fun currentCountsThroughOpenToday() {
        val s = StreakEngine.summarize(
            def(),
            met(LocalDate.of(2026, 1, 7), LocalDate.of(2026, 1, 8), LocalDate.of(2026, 1, 9)),
            today,
        )

        assertEquals(3, s.current)
        assertEquals(3, s.best)
        assertEquals(PeriodStatus.OPEN, s.todayStatus)
    }

    /** A missed day breaks the current streak, so only the run after it counts. */
    @Test
    fun currentResetsAfterMissedDay() {
        val s = StreakEngine.summarize(
            def(),
            met(LocalDate.of(2026, 1, 7), LocalDate.of(2026, 1, 9)),
            today,
        )

        assertEquals(1, s.current)
        assertEquals(1, s.best)
    }

    /** Completing today as well extends the current streak. */
    @Test
    fun todayCompletedIsIncludedInCurrent() {
        val s = StreakEngine.summarize(
            def(),
            met(LocalDate.of(2026, 1, 9), today),
            today,
        )

        assertEquals(2, s.current)
        assertEquals(PeriodStatus.MET, s.todayStatus)
    }

    /** A missed yesterday leaves the current streak at zero, even with today still open. */
    @Test
    fun missedYesterdayMeansCurrentZero() {
        val s = StreakEngine.summarize(
            def(),
            met(LocalDate.of(2026, 1, 8)),
            today,
        )

        assertEquals(0, s.current)
        assertEquals(1, s.best)
    }

    /** Best is the longest run ever, even when it's no longer current. */
    @Test
    fun bestIsLongestRunInHistory() {
        val s = StreakEngine.summarize(
            def(),
            met(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 2),
                LocalDate.of(2026, 1, 3),
                LocalDate.of(2026, 1, 5),
                LocalDate.of(2026, 1, 6),
            ),
            today,
        )

        assertEquals(3, s.best)
        assertEquals(0, s.current)
    }

    /** Entries before the streak was created are ignored, and the first period starts on creation day. */
    @Test
    fun entriesBeforeCreationAreIgnored() {
        val s = StreakEngine.summarize(
            def(createdOn = LocalDate.of(2026, 1, 5)),
            met(LocalDate.of(2026, 1, 2)),
            today,
        )

        assertEquals(LocalDate.of(2026, 1, 5), s.periods.first().start)
        assertEquals(0, s.best)
        assertEquals(0, s.current)
    }

    /** Entries after today are ignored, so they don't count and don't become the last entry. */
    @Test
    fun entriesAfterTodayAreIgnored() {
        val s = StreakEngine.summarize(
            def(),
            met(LocalDate.of(2026, 1, 11)),
            today,
        )

        assertNull(s.lastEntryDate)
        assertEquals(PeriodStatus.OPEN, s.todayStatus)
    }

    /** With no entries, both streaks are zero and every day since creation is listed. */
    @Test
    fun noEntriesGivesZeroStreaks() {
        val s = StreakEngine.summarize(def(), emptyList(), today)

        assertEquals(0, s.current)
        assertEquals(0, s.best)
        assertEquals(10, s.periods.size)
    }
}
