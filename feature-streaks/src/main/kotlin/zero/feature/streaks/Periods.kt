package zero.feature.streaks

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

object Periods {
    fun containing(date: LocalDate, definition: StreakDefinition): Pair<LocalDate, LocalDate> = when (definition.frequency) {
        Frequency.DAILY -> date to date
        Frequency.WEEKLY -> {
            val start = date.with(TemporalAdjusters.previousOrSame(definition.weekStart))
            start to start.plusDays(6)
        }
        Frequency.MONTHLY -> {
            val start = date.withDayOfMonth(1)
            start to start.with(TemporalAdjusters.lastDayOfMonth())
        }
        Frequency.YEARLY -> {
            val start = LocalDate.of(date.year, 1, 1)
            start to LocalDate.of(date.year, 12, 31)
        }
    }
}
