/*
 * MODULE: feature-streaks tests
 * PURPOSE: Verifies period boundaries for each frequency and week start.
 * TAKES: Fixed dates and frequencies.
 * GIVES: Pass/fail results for period boundaries.
 * OWNS: No production state.
 * DOES NOT OWN: Period status.
 * DEPENDENCIES: feature-streaks.
 * CONTRACT: Each test asserts one boundary rule.
 * FAILURE BEHAVIOR: Assertion failures identify the wrong boundary.
 */
package zero.feature.streaks

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class PeriodTest {
    private fun def(frequency: Frequency, weekStart: DayOfWeek = DayOfWeek.MONDAY) = StreakDefinition(
        id = "p",
        name = "Period",
        emoji = "P",
        colorIndex = 0,
        mode = StreakMode.CHECK_IN,
        frequency = frequency,
        target = 1,
        parts = emptyList(),
        createdOn = LocalDate.of(2026, 1, 1),
        weekStart = weekStart,
    )

    /** A daily period is exactly one day. */
    @Test
    fun dailyPeriodIsOneDay() {
        val (start, end) = Periods.containing(LocalDate.of(2026, 1, 7), def(Frequency.DAILY))

        assertEquals(LocalDate.of(2026, 1, 7), start)
        assertEquals(LocalDate.of(2026, 1, 7), end)
    }

    /** Weekly periods start on Monday by default. */
    @Test
    fun weeklyStartsOnMondayByDefault() {
        val (start, end) = Periods.containing(LocalDate.of(2026, 1, 7), def(Frequency.WEEKLY))

        assertEquals(LocalDate.of(2026, 1, 5), start)
        assertEquals(LocalDate.of(2026, 1, 11), end)
    }

    /** A Sunday week start moves the boundaries. */
    @Test
    fun weeklyCanStartOnSunday() {
        val (start, end) = Periods.containing(
            LocalDate.of(2026, 1, 7),
            def(Frequency.WEEKLY, DayOfWeek.SUNDAY),
        )

        assertEquals(LocalDate.of(2026, 1, 4), start)
        assertEquals(LocalDate.of(2026, 1, 10), end)
    }

    /** A monthly period in a leap year ends on February 29. */
    @Test
    fun monthlyBoundsIncludeLeapFebruary() {
        val (start, end) = Periods.containing(LocalDate.of(2000, 2, 15), def(Frequency.MONTHLY))

        assertEquals(LocalDate.of(2000, 2, 1), start)
        assertEquals(LocalDate.of(2000, 2, 29), end)
    }

    /** A yearly period runs from January 1 to December 31. */
    @Test
    fun yearlyBoundsCoverTheYear() {
        val (start, end) = Periods.containing(LocalDate.of(2026, 6, 1), def(Frequency.YEARLY))

        assertEquals(LocalDate.of(2026, 1, 1), start)
        assertEquals(LocalDate.of(2026, 12, 31), end)
    }
}
