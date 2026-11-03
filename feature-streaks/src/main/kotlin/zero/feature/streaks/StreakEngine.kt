package zero.feature.streaks

import java.time.LocalDate

object StreakEngine {
    fun validate(definition: StreakDefinition, entry: Entry) {
        require(!entry.date.isBefore(definition.createdOn)) {
            "Entries before the streak creation date are ignored."
        }

        when (definition.mode) {
            StreakMode.CHECKLIST -> {
                val validNames = definition.parts.map { it.name.trim() }.toSet()
                require(entry.parts.all { it.trim() in validNames }) {
                    "Entry contains part names that do not exist on this streak."
                }
                require(entry.amount == 0 || entry.parts.isEmpty()) {
                    "Checklist entries should not provide both amount and part logs."
                }
            }
            StreakMode.AMOUNT -> require(entry.amount > 0) { "Amount streak entries must be positive." }
            StreakMode.LIMIT -> require(entry.amount > 0) { "Limit streak entries must be positive." }
            StreakMode.CHECK_IN -> require(entry.amount == 0 || entry.parts.isEmpty()) {
                "Check-in entries should not provide both amount and part logs."
            }
        }
    }

    fun summarize(definition: StreakDefinition, entries: List<Entry>, today: LocalDate): StreakSummary {
        val validEntries = entries
            .filter { it.date <= today }
            .filter { !it.date.isBefore(definition.createdOn) }
            .filter { entry -> runCatching { validate(definition, entry) }.isSuccess }

        val periods = generatePeriods(definition, definition.createdOn, today)
            .map { periodStart ->
                val periodEntries = validEntries.filter { entry ->
                    val end = periodEnd(definition, periodStart)
                    !entry.date.isBefore(periodStart) && !entry.date.isAfter(end)
                }
                evaluatePeriod(definition, periodStart, periodEnd(definition, periodStart), periodEntries, today)
            }

        val current = computeCurrent(periods, today)
        val best = computeBest(periods)
        val todayStatus = periods.firstOrNull { it.start <= today && it.end >= today }?.status ?: PeriodStatus.MISSED
        val lastEntryDate = validEntries.maxOfOrNull { it.date }

        return StreakSummary(
            current = current,
            best = best,
            todayStatus = todayStatus,
            periods = periods,
            lastEntryDate = lastEntryDate,
        )
    }

    private fun evaluatePeriod(
        definition: StreakDefinition,
        start: LocalDate,
        end: LocalDate,
        entries: List<Entry>,
        today: LocalDate,
    ): PeriodResult {
        val isCurrentPeriod = start <= today && end >= today
        val value = when (definition.mode) {
            StreakMode.CHECKLIST -> {
                val merged = entries.flatMap { it.parts }.toSet()
                val partWeights = definition.parts.associate { it.name.trim() to it.weight }
                val total = partWeights.filterKeys { it in merged }.values.sum()
                total
            }
            else -> when {
                entries.isEmpty() -> 0
                definition.mode == StreakMode.CHECK_IN -> entries.size
                else -> entries.sumOf { it.amount }
            }
        }

        val (status, strength) = when (definition.mode) {
            StreakMode.AMOUNT -> {
                val target = definition.target
                val strengthValue = (value.toDouble() / target).coerceAtMost(1.0)
                if (value >= target) PeriodStatus.MET to strengthValue
                else if (isCurrentPeriod) PeriodStatus.OPEN to strengthValue
                else PeriodStatus.MISSED to strengthValue
            }
            StreakMode.LIMIT -> {
                val target = definition.target
                val strengthValue = (value.toDouble() / target).coerceAtMost(1.0)
                when {
                    value > target -> PeriodStatus.MISSED to strengthValue
                    entries.isEmpty() && !isCurrentPeriod -> PeriodStatus.MISSED to strengthValue
                    isCurrentPeriod -> PeriodStatus.OPEN to strengthValue
                    else -> PeriodStatus.MET to strengthValue
                }
            }
            StreakMode.CHECK_IN -> {
                val target = definition.target
                val strengthValue = (value.toDouble() / target).coerceAtMost(1.0)
                when {
                    value >= target -> PeriodStatus.MET to strengthValue
                    isCurrentPeriod -> PeriodStatus.OPEN to strengthValue
                    else -> PeriodStatus.MISSED to strengthValue
                }
            }
            StreakMode.CHECKLIST -> {
                val totalWeight = definition.parts.sumOf { it.weight }
                val maxWeight = definition.parts.maxOf { it.weight }
                val requiredNames = definition.parts.filter { it.weight == maxWeight }.map { it.name.trim() }
                val checkedNames = entries.flatMap { it.parts }.map { it.trim() }.toSet()
                val achievedWeight = value
                val strengthValue = (achievedWeight.toDouble() / totalWeight.coerceAtLeast(1)).coerceAtMost(1.0)
                when {
                    requiredNames.all { it in checkedNames } -> PeriodStatus.MET to strengthValue
                    isCurrentPeriod -> PeriodStatus.OPEN to strengthValue
                    else -> PeriodStatus.MISSED to strengthValue
                }
            }
        }

        return PeriodResult(
            start = start,
            end = end,
            status = status,
            value = value,
            strength = strength,
        )
    }

    private fun generatePeriods(definition: StreakDefinition, start: LocalDate, end: LocalDate): List<LocalDate> {
        val periods = mutableListOf<LocalDate>()
        var cursor = firstPeriodStart(definition, start)
        while (!cursor.isAfter(end)) {
            periods += cursor
            cursor = nextPeriodStart(definition, cursor)
        }
        return periods
    }

    private fun firstPeriodStart(definition: StreakDefinition, date: LocalDate): LocalDate = when (definition.frequency) {
        Frequency.DAILY -> date
        Frequency.WEEKLY -> date.with(java.time.temporal.TemporalAdjusters.previousOrSame(definition.weekStart))
        Frequency.MONTHLY -> date.withDayOfMonth(1)
        Frequency.YEARLY -> LocalDate.of(date.year, 1, 1)
    }

    private fun nextPeriodStart(definition: StreakDefinition, current: LocalDate): LocalDate = when (definition.frequency) {
        Frequency.DAILY -> current.plusDays(1)
        Frequency.WEEKLY -> current.plusWeeks(1)
        Frequency.MONTHLY -> current.plusMonths(1).withDayOfMonth(1)
        Frequency.YEARLY -> LocalDate.of(current.year + 1, 1, 1)
    }

    private fun periodEnd(definition: StreakDefinition, start: LocalDate): LocalDate = when (definition.frequency) {
        Frequency.DAILY -> start
        Frequency.WEEKLY -> start.plusDays(6)
        Frequency.MONTHLY -> start.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth())
        Frequency.YEARLY -> LocalDate.of(start.year, 12, 31)
    }

    private fun computeCurrent(periods: List<PeriodResult>, today: LocalDate): Int {
        var current = 0
        for (period in periods.asReversed()) {
            when (period.status) {
                PeriodStatus.MET -> current += 1
                PeriodStatus.OPEN -> {
                    if (period.start <= today && period.end >= today) {
                        continue
                    }
                    break
                }
                PeriodStatus.MISSED -> break
            }
        }
        return current
    }

    private fun computeBest(periods: List<PeriodResult>): Int {
        var best = 0
        var run = 0
        for (period in periods) {
            when (period.status) {
                PeriodStatus.MET -> {
                    run += 1
                    best = maxOf(best, run)
                }
                PeriodStatus.MISSED -> run = 0
                PeriodStatus.OPEN -> Unit
            }
        }
        return best
    }
}
