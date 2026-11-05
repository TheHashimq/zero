/*
 * MODULE: feature-streaks
 * PURPOSE: Converts stored entries when a streak's mode or checklist parts change, so history fits the new mode.
 * TAKES: The existing entries, the old and new modes, and the new checklist part names.
 * GIVES: Converted entries. One entry per date for mode changes; the first entry's ID is kept.
 * OWNS: No state.
 * DOES NOT OWN: Evaluation (StreakEngine) or saving (StreakRepository).
 * DEPENDENCIES: StoredEntry, StreakMode.
 * CONTRACT: Same mode with no checklist change returns entries unchanged.
 *           Check-in days become one unit for amount and limit, and all parts for checklists.
 *           Amount, limit, and checklist days become check-ins when converted to check-in.
 *           Checklist parts not in the new list are dropped.
 * FAILURE BEHAVIOR: Never throws.
 */
package zero.feature.streaks.storage

import zero.feature.streaks.StreakMode

internal object EntryConversion {
    fun convert(
        entries: List<StoredEntry>,
        fromMode: StreakMode,
        toMode: StreakMode,
        toPartNames: List<String>,
    ): List<StoredEntry> {
        if (fromMode == toMode && toMode != StreakMode.CHECKLIST) return entries
        val byDate = entries.sortedWith(compareBy({ it.date }, { it.id })).groupBy { it.date }
        return byDate.map { (_, group) ->
            val first = group.first()
            when (toMode) {
                StreakMode.CHECK_IN -> first.copy(amount = 0, parts = emptyList())
                StreakMode.AMOUNT, StreakMode.LIMIT -> first.copy(amount = 1, parts = emptyList())
                StreakMode.CHECKLIST -> {
                    val checked = when (fromMode) {
                        StreakMode.CHECK_IN -> toPartNames
                        StreakMode.CHECKLIST -> group.flatMap { it.parts }.distinct().filter { it in toPartNames }
                        else -> emptyList()
                    }
                    first.copy(amount = 0, parts = checked)
                }
            }
        }
    }
}
