/*
 * MODULE: feature-streaks
 * PURPOSE: Turns a streak definition and its summary into the values the Home card displays.
 * TAKES: A StreakDefinition and its StreakSummary.
 * GIVES: A StreakCard with today's progress, status, current and best streaks, color index, and emoji.
 * OWNS: The dot limit and the summary wording.
 * DOES NOT OWN: Evaluation rules (StreakEngine) or any drawing.
 * DEPENDENCIES: StreakEngine types only.
 * CONTRACT: Check-ins up to 14 show dots; larger targets show a bar. Limit days over the limit are flagged.
 *           Checklists show the percentage of weight done. A summary with no periods gives an open, empty card.
 * FAILURE BEHAVIOR: Never throws.
 */
package zero.feature.streaks.presentation

import kotlin.math.roundToInt
import zero.feature.streaks.Frequency
import zero.feature.streaks.PeriodResult
import zero.feature.streaks.PeriodStatus
import zero.feature.streaks.StreakDefinition
import zero.feature.streaks.StreakMode
import zero.feature.streaks.StreakSummary

/** How today's progress is drawn. */
sealed interface Progress {
    data class Dots(val filled: Int, val total: Int) : Progress
    data class Bar(val value: Int, val target: Int, val over: Boolean) : Progress
    data class Percent(val percent: Int) : Progress
}

/** Everything one Home card shows. */
data class StreakCard(
    val id: String,
    val name: String,
    val emoji: String,
    val colorIndex: Int,
    val summaryText: String,
    val progress: Progress,
    val status: PeriodStatus,
    val current: Int,
    val best: Int,
)

object StreakCards {
    /** Check-in targets up to this value are drawn as dots; larger ones use a bar. */
    const val MAX_DOTS = 14

    fun build(def: StreakDefinition, summary: StreakSummary): StreakCard {
        val period = summary.periods.lastOrNull() ?: PeriodResult(
            def.createdOn,
            def.createdOn,
            PeriodStatus.OPEN,
            value = 0,
            strength = 0.0,
        )
        val label = periodLabel(def.frequency)
        val progress = when (def.mode) {
            StreakMode.CHECK_IN ->
                if (def.target <= MAX_DOTS) Progress.Dots(minOf(period.value, def.target), def.target)
                else Progress.Bar(period.value, def.target, over = false)
            StreakMode.AMOUNT -> Progress.Bar(period.value, def.target, over = false)
            StreakMode.LIMIT -> Progress.Bar(period.value, def.target, over = period.value > def.target)
            StreakMode.CHECKLIST -> Progress.Percent((period.strength * 100).roundToInt())
        }
        return StreakCard(
            id = def.id,
            name = def.name,
            emoji = def.emoji,
            colorIndex = def.colorIndex,
            summaryText = summaryText(def, period, label),
            progress = progress,
            status = period.status,
            current = summary.current,
            best = summary.best,
        )
    }

    fun periodLabel(frequency: Frequency): String = when (frequency) {
        Frequency.DAILY -> "today"
        Frequency.WEEKLY -> "this week"
        Frequency.MONTHLY -> "this month"
        Frequency.YEARLY -> "this year"
    }

    private fun summaryText(def: StreakDefinition, period: PeriodResult, label: String): String = when (def.mode) {
        StreakMode.CHECK_IN -> "${period.value} of ${def.target} $label"
        StreakMode.AMOUNT -> "${period.value} of ${def.target} $label"
        StreakMode.LIMIT ->
            if (period.value > def.target) "${period.value} of ${def.target} limit exceeded"
            else "${period.value} of ${def.target} max $label"
        StreakMode.CHECKLIST -> "${(period.strength * 100).roundToInt()}% done $label"
    }
}
