/*
 * MODULE: feature-streaks
 * PURPOSE: Builds the year strip, the month calendar, and the history rows for the details screen.
 * TAKES: A streak summary, dates, a week start, and identified entries.
 * GIVES: Cells with strength steps, and history rows newest first.
 * OWNS: The strip length.
 * DOES NOT OWN: Drawing or storage.
 * DEPENDENCIES: StreakSummary, PeriodResult, IdentifiedEntry.
 * CONTRACT: Each day takes its step from the period that contains it. Future days have step 0.
 *           History is newest first, and within a day by ID descending.
 * FAILURE BEHAVIOR: Never throws.
 */
package zero.feature.streaks.presentation

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import zero.feature.streaks.PeriodResult
import zero.feature.streaks.StreakDefinition
import zero.feature.streaks.StreakMode
import zero.feature.streaks.StreakSummary
import zero.feature.streaks.storage.IdentifiedEntry

/** One day in the strip or the calendar. */
data class DayCell(
    val date: LocalDate,
    val step: Int,
    val inMonth: Boolean,
    val isToday: Boolean,
    val isFuture: Boolean,
)

/** One history row. */
data class HistoryRow(val id: String, val date: LocalDate, val label: String)

object DetailViews {
    const val STRIP_DAYS = 182

    /** 0 for no progress, 1 to 4 for increasing strength. */
    fun strengthStep(strength: Double): Int = when {
        strength <= 0.0 -> 0
        strength <= 0.25 -> 1
        strength <= 0.5 -> 2
        strength <= 0.75 -> 3
        else -> 4
    }

    /** The step for [date], taken from the period that contains it. Dates with no period are 0. */
    fun stepOn(summary: StreakSummary, date: LocalDate): Int {
        val period: PeriodResult = summary.periods.firstOrNull {
            !date.isBefore(it.start) && !date.isAfter(it.end)
        } ?: return 0
        return strengthStep(period.strength)
    }

    /** The last [STRIP_DAYS] days, oldest first, ending on [today]. */
    fun strip(summary: StreakSummary, today: LocalDate): List<DayCell> =
        (STRIP_DAYS - 1 downTo 0).map { back ->
            val date = today.minusDays(back.toLong())
            DayCell(date, stepOn(summary, date), inMonth = true, isToday = date == today, isFuture = false)
        }

    /** Six rows of seven days for [month], starting on [weekStart]. */
    fun month(summary: StreakSummary, month: YearMonth, today: LocalDate, weekStart: DayOfWeek): List<List<DayCell>> {
        val first = month.atDay(1)
        val lead = (first.dayOfWeek.value - weekStart.value + 7) % 7
        val start = first.minusDays(lead.toLong())
        return (0 until 42).map { i ->
            val date = start.plusDays(i.toLong())
            val future = date.isAfter(today)
            DayCell(
                date = date,
                step = if (future) 0 else stepOn(summary, date),
                inMonth = YearMonth.from(date) == month,
                isToday = date == today,
                isFuture = future,
            )
        }.chunked(7)
    }

    /** History rows, newest first. */
    fun history(def: StreakDefinition, entries: List<IdentifiedEntry>): List<HistoryRow> =
        entries
            .sortedWith(compareByDescending<IdentifiedEntry> { it.entry.date }.thenByDescending { it.id })
            .map { HistoryRow(it.id, it.entry.date, label(def, it)) }

    private fun label(def: StreakDefinition, item: IdentifiedEntry): String = when (def.mode) {
        StreakMode.CHECK_IN -> "Check-in"
        StreakMode.AMOUNT -> "+${item.entry.amount}"
        StreakMode.LIMIT -> "${item.entry.amount} used"
        StreakMode.CHECKLIST -> item.entry.parts.sorted().joinToString(", ").ifEmpty { "Nothing checked" }
    }
}
