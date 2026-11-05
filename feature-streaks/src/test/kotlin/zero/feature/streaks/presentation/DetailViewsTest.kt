/*
 * MODULE: feature-streaks tests
 * PURPOSE: Verifies strength steps, day-to-period mapping, the strip, the month grid, and history order and labels.
 * TAKES: Fixed periods, summaries, and identified entries.
 * GIVES: Pass/fail results for the detail views.
 * OWNS: No production state.
 * DOES NOT OWN: Drawing.
 * DEPENDENCIES: feature-streaks.
 * CONTRACT: Each test asserts one display rule.
 * FAILURE BEHAVIOR: Assertion failures identify the wrong value.
 */
package zero.feature.streaks.presentation

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import zero.feature.streaks.ChecklistPart
import zero.feature.streaks.Entry
import zero.feature.streaks.Frequency
import zero.feature.streaks.PeriodResult
import zero.feature.streaks.PeriodStatus
import zero.feature.streaks.StreakDefinition
import zero.feature.streaks.StreakMode
import zero.feature.streaks.StreakSummary
import zero.feature.streaks.storage.IdentifiedEntry

class DetailViewsTest {
    private val today = LocalDate.of(2026, 1, 10)

    private fun day(date: LocalDate, strength: Double) =
        PeriodResult(date, date, PeriodStatus.MET, value = 0, strength = strength)

    private fun summary(vararg periods: PeriodResult) =
        StreakSummary(0, 0, PeriodStatus.OPEN, periods.toList(), null)

    private fun def(mode: StreakMode, parts: List<ChecklistPart> = emptyList()) = StreakDefinition(
        id = "s1",
        name = "S",
        emoji = "B",
        colorIndex = 0,
        mode = mode,
        frequency = Frequency.DAILY,
        target = if (mode == StreakMode.CHECKLIST) 0 else 10,
        parts = parts,
        createdOn = LocalDate.of(2026, 1, 1),
    )

    /** Strength buckets map to steps 0 to 4. */
    @Test
    fun strengthBucketsMapToSteps() {
        assertEquals(0, DetailViews.strengthStep(0.0))
        assertEquals(1, DetailViews.strengthStep(0.1))
        assertEquals(1, DetailViews.strengthStep(0.25))
        assertEquals(2, DetailViews.strengthStep(0.5))
        assertEquals(3, DetailViews.strengthStep(0.75))
        assertEquals(4, DetailViews.strengthStep(1.0))
    }

    /** A day takes its step from the period that contains it. */
    @Test
    fun stepComesFromContainingPeriod() {
        val s = summary(day(LocalDate.of(2026, 1, 9), 1.0))

        assertEquals(4, DetailViews.stepOn(s, LocalDate.of(2026, 1, 9)))
        assertEquals(0, DetailViews.stepOn(s, LocalDate.of(2026, 1, 8)))
    }

    /** A weekly period colors every day it covers. */
    @Test
    fun weeklyPeriodColorsEveryDayItCovers() {
        val weekly = StreakSummary(
            1,
            1,
            PeriodStatus.OPEN,
            listOf(
                PeriodResult(
                    LocalDate.of(2026, 1, 5),
                    LocalDate.of(2026, 1, 11),
                    PeriodStatus.MET,
                    1,
                    1.0,
                ),
            ),
            null,
        )

        assertEquals(4, DetailViews.stepOn(weekly, LocalDate.of(2026, 1, 5)))
        assertEquals(4, DetailViews.stepOn(weekly, LocalDate.of(2026, 1, 11)))
    }

    /** The strip has 182 days, oldest first, ending today. */
    @Test
    fun stripEndsToday() {
        val strip = DetailViews.strip(summary(), today)

        assertEquals(182, strip.size)
        assertTrue(strip.last().isToday)
        assertEquals(today.minusDays(181), strip.first().date)
    }

    /** January 2026 with a Monday week start begins on Monday, December 29, 2025. */
    @Test
    fun monthGridStartsOnWeekStart() {
        val grid = DetailViews.month(summary(), YearMonth.of(2026, 1), today, DayOfWeek.MONDAY)

        assertEquals(6, grid.size)
        assertEquals(LocalDate.of(2025, 12, 29), grid.first().first().date)
    }

    /** Days outside the displayed month are marked; days inside are not. */
    @Test
    fun outsideMonthDaysAreMarked() {
        val grid = DetailViews.month(summary(), YearMonth.of(2026, 1), today, DayOfWeek.MONDAY).flatten()

        assertFalse(grid.first { it.date == LocalDate.of(2025, 12, 29) }.inMonth)
        assertTrue(grid.first { it.date == LocalDate.of(2026, 1, 1) }.inMonth)
    }

    /** Future days show no progress, even when a period claims them. */
    @Test
    fun futureDaysHaveNoStep() {
        val s = summary(day(LocalDate.of(2026, 1, 20), 1.0))
        val future = DetailViews.month(s, YearMonth.of(2026, 1), today, DayOfWeek.MONDAY)
            .flatten()
            .first { it.date == LocalDate.of(2026, 1, 20) }

        assertEquals(0, future.step)
        assertTrue(future.isFuture)
    }

    /** History is newest first, and within a day by ID descending. */
    @Test
    fun historyNewestFirst() {
        val rows = DetailViews.history(
            def(StreakMode.CHECK_IN),
            listOf(
                IdentifiedEntry("a", Entry(LocalDate.of(2026, 1, 8))),
                IdentifiedEntry("b", Entry(LocalDate.of(2026, 1, 9))),
                IdentifiedEntry("c", Entry(LocalDate.of(2026, 1, 9))),
            ),
        )

        assertEquals(listOf("c", "b", "a"), rows.map { it.id })
    }

    /** Checklist history lists the checked parts alphabetically. */
    @Test
    fun checklistHistoryListsPartsAlphabetically() {
        val prayer = def(
            StreakMode.CHECKLIST,
            listOf(ChecklistPart("sunnah", 1), ChecklistPart("fard", 3)),
        )

        val row = DetailViews.history(
            prayer,
            listOf(IdentifiedEntry("a", Entry(today, parts = setOf("sunnah", "fard")))),
        ).single()

        assertEquals("fard, sunnah", row.label)
    }

    /** Amount history shows the logged amount with a plus sign. */
    @Test
    fun amountHistoryShowsPlusAmount() {
        val row = DetailViews.history(
            def(StreakMode.AMOUNT),
            listOf(IdentifiedEntry("a", Entry(today, amount = 5))),
        ).single()

        assertEquals("+5", row.label)
    }
}
