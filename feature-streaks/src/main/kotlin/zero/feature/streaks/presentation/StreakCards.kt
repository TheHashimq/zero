package zero.feature.streaks.presentation

import java.time.LocalDate
import kotlin.math.roundToInt
import zero.feature.streaks.Frequency
import zero.feature.streaks.PeriodResult
import zero.feature.streaks.PeriodStatus
import zero.feature.streaks.StreakDefinition
import zero.feature.streaks.StreakMode
import zero.feature.streaks.StreakSummary

sealed interface Progress {
    data class Dots(val done: Int, val total: Int) : Progress
    data class Bar(val value: Int, val target: Int, val over: Boolean = false) : Progress
    data class Percent(val value: Int) : Progress
}

data class StreakCard(
    val title: String,
    val status: PeriodStatus,
    val current: Int,
    val best: Int,
    val progress: Progress,
    val summaryText: String,
)

object StreakCards {
    fun build(definition: StreakDefinition, summary: StreakSummary): StreakCard {
        val currentPeriod = findActivePeriod(definition, summary)
        val value = currentPeriod?.value ?: 0
        val status = summary.todayStatus

        val progress = when (definition.mode) {
            StreakMode.CHECK_IN -> if (definition.target <= 3) {
                Progress.Dots(value.coerceAtMost(definition.target), definition.target)
            } else {
                Progress.Bar(value, definition.target, over = false)
            }
            StreakMode.AMOUNT -> Progress.Bar(value, definition.target, over = value > definition.target)
            StreakMode.LIMIT -> Progress.Bar(value, definition.target, over = value > definition.target)
            StreakMode.CHECKLIST -> {
                val totalWeight = definition.parts.sumOf { it.weight }
                val percentValue = if (totalWeight <= 0) 0 else ((value.toDouble() / totalWeight) * 100.0).roundToInt()
                Progress.Percent(percentValue.coerceIn(0, 100))
            }
        }

        val summaryText = when (definition.mode) {
            StreakMode.CHECK_IN -> if (definition.target <= 3) {
                "$value of ${definition.target} this ${periodLabel(definition.frequency)}"
            } else {
                "$value of ${definition.target} this ${periodLabel(definition.frequency)}"
            }
            StreakMode.AMOUNT -> "$value of ${definition.target} today"
            StreakMode.LIMIT -> if (value > definition.target) {
                "$value of ${definition.target} limit exceeded"
            } else {
                "$value of ${definition.target} max today"
            }
            StreakMode.CHECKLIST -> {
                val totalWeight = definition.parts.sumOf { it.weight }
                val percentValue = if (totalWeight <= 0) 0 else ((value.toDouble() / totalWeight) * 100.0).roundToInt()
                "${percentValue.coerceIn(0, 100)}% done today"
            }
        }

        return StreakCard(
            title = definition.name,
            status = status,
            current = summary.current,
            best = summary.best,
            progress = progress,
            summaryText = summaryText,
        )
    }

    private fun findActivePeriod(definition: StreakDefinition, summary: StreakSummary): PeriodResult? {
        if (summary.periods.isEmpty()) return null
        return summary.periods.lastOrNull()
    }

    private fun periodLabel(frequency: Frequency): String = when (frequency) {
        Frequency.DAILY -> "day"
        Frequency.WEEKLY -> "week"
        Frequency.MONTHLY -> "month"
        Frequency.YEARLY -> "year"
    }
}
