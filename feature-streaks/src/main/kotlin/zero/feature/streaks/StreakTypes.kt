package zero.feature.streaks

import java.time.DayOfWeek
import java.time.LocalDate

enum class StreakMode {
    CHECKLIST,
    AMOUNT,
    LIMIT,
    CHECK_IN,
}

enum class Frequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
}

enum class PeriodStatus {
    MET,
    OPEN,
    MISSED,
}

data class ChecklistPart(
    val name: String,
    val weight: Int,
) {
    init {
        require(name.isNotBlank()) { "Checklist part names must not be blank." }
        require(weight in 1..10) { "Checklist part weights must be between 1 and 10." }
    }
}

data class StreakDefinition(
    val id: String,
    val name: String,
    val emoji: String,
    val colorIndex: Int,
    val mode: StreakMode,
    val frequency: Frequency,
    val target: Int,
    val parts: List<ChecklistPart>,
    val createdOn: LocalDate,
    val weekStart: DayOfWeek = DayOfWeek.MONDAY,
) {
    init {
        require(id.isNotBlank()) { "Streak id must not be blank." }
        require(name.isNotBlank()) { "Streak name must not be blank." }
        require(emoji.isNotBlank()) { "Streak emoji must not be blank." }
        require(colorIndex in 0 until StreakPalette.SIZE) { "Color index must be within the available palette." }
        require(mode != StreakMode.CHECKLIST || frequency == Frequency.DAILY) {
            "Checklist streaks must be daily."
        }
        require(mode == StreakMode.CHECKLIST || target > 0) {
            "Targets must be positive for non-checklist streaks."
        }
        val duplicates = parts
            .map { it.name.trim() }
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
        require(duplicates.isEmpty()) { "Checklist parts must have unique names." }
        require(parts.all { it.weight in 1..10 }) { "Checklist weights must be between 1 and 10." }
        require(parts.all { it.name.isNotBlank() }) { "Checklist part names must not be blank." }
        if (mode == StreakMode.CHECKLIST) {
            require(parts.isNotEmpty()) { "Checklist streaks must define at least one part." }
            require(target == 0) { "Checklist streaks use the part-weight target; target should be 0." }
        }
    }

    val partMap: Map<String, ChecklistPart> = parts.associateBy { it.name.trim() }
}

data class Entry(
    val date: LocalDate,
    val amount: Int = 0,
    val parts: Set<String> = emptySet(),
    val context: String? = null,
) {
    init {
        require(amount >= 0) { "Entry amount must not be negative." }
        require(parts.all { it.isNotBlank() }) { "Entry part names must not be blank." }
    }

    val day: LocalDate
        get() = date
}

data class PeriodResult(
    val start: LocalDate,
    val end: LocalDate,
    val status: PeriodStatus,
    val value: Int = 0,
    val strength: Double = 0.0,
)

data class StreakSummary(
    val current: Int,
    val best: Int,
    val todayStatus: PeriodStatus,
    val periods: List<PeriodResult>,
    val lastEntryDate: LocalDate?,
)
