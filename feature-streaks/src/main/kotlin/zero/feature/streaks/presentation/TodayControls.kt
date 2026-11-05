/*
 * MODULE: feature-streaks
 * PURPOSE: Derives the "today" control for each mode and computes checklist toggles.
 * TAKES: A streak definition and today's entries.
 * GIVES: A TodayControl for the screen, and the replacement entries after toggling a checklist part.
 * OWNS: No state.
 * DOES NOT OWN: Storage (see StreakRepository) or evaluation (see StreakEngine).
 * DEPENDENCIES: StreakDefinition, Entry.
 * CONTRACT: Checklists always produce at most one entry per day. Toggling a part keeps the other parts.
 * FAILURE BEHAVIOR: An unknown checklist part throws IllegalArgumentException.
 */
package zero.feature.streaks.presentation

import java.time.LocalDate
import zero.feature.streaks.Entry
import zero.feature.streaks.StreakDefinition
import zero.feature.streaks.StreakMode

/** What the today control shows for a streak. */
sealed interface TodayControl {
    data class CheckIn(val count: Int, val target: Int) : TodayControl
    data class Amount(val total: Int, val target: Int) : TodayControl
    data class Limit(val total: Int, val limit: Int) : TodayControl {
        val over: Boolean get() = total > limit
    }
    data class Checklist(val parts: List<PartRow>) : TodayControl
}

/** One checklist row for today. */
data class PartRow(val name: String, val weight: Int, val required: Boolean, val checked: Boolean)

object TodayControls {
    fun build(def: StreakDefinition, todayEntries: List<Entry>): TodayControl = when (def.mode) {
        StreakMode.CHECK_IN -> TodayControl.CheckIn(todayEntries.size, def.target)
        StreakMode.AMOUNT -> TodayControl.Amount(todayEntries.sumOf { it.amount }, def.target)
        StreakMode.LIMIT -> TodayControl.Limit(todayEntries.sumOf { it.amount }, def.target)
        StreakMode.CHECKLIST -> {
            val checked = todayEntries.flatMap { it.parts }.toSet()
            val top = def.parts.maxOf { it.weight }
            TodayControl.Checklist(
                def.parts.map { PartRow(it.name, it.weight, it.weight == top, it.name in checked) },
            )
        }
    }

    /** Today's single checklist entry after toggling [part]. Empty when nothing is checked. */
    fun toggleChecklistPart(
        def: StreakDefinition,
        todayEntries: List<Entry>,
        date: LocalDate,
        part: String,
    ): List<Entry> {
        require(def.parts.any { it.name == part }) { "unknown checklist part '$part'" }
        val checked = todayEntries.flatMap { it.parts }.toMutableSet()
        if (!checked.add(part)) checked.remove(part)
        return if (checked.isEmpty()) emptyList() else listOf(Entry(date, parts = checked.toSet()))
    }
}
