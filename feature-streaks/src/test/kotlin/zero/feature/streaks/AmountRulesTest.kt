/*
 * MODULE: feature-streaks tests
 * PURPOSE: Verifies amount targets, merged logs, strength, and validation.
 * TAKES: A fixed daily amount streak and entries.
 * GIVES: Pass/fail results for amount rules.
 * OWNS: No production state.
 * DOES NOT OWN: Other modes.
 * DEPENDENCIES: feature-streaks.
 * CONTRACT: Each test asserts one amount rule.
 * FAILURE BEHAVIOR: Assertion failures identify the broken rule.
 */
package zero.feature.streaks

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AmountRulesTest {
    private val today = LocalDate.of(2026, 1, 10)
    private val pastDay = LocalDate.of(2026, 1, 9)

    private val reading = StreakDefinition(
        id = "read",
        name = "Reading",
        emoji = "B",
        colorIndex = 0,
        mode = StreakMode.AMOUNT,
        frequency = Frequency.DAILY,
        target = 10,
        parts = emptyList(),
        createdOn = LocalDate.of(2026, 1, 1),
    )

    private fun dayResult(entries: List<Entry>, date: LocalDate): PeriodResult =
        StreakEngine.summarize(reading, entries, today).periods.first { it.start == date }

    /** Today under target is open, not missed. */
    @Test
    fun underTargetTodayIsOpen() {
        val r = dayResult(listOf(Entry(today, amount = 5)), today)

        assertEquals(PeriodStatus.OPEN, r.status)
        assertEquals(0.5, r.strength, 0.0001)
    }

    /** Two logs on the same day add up to meet the target. */
    @Test
    fun logsOnSameDayAddUp() {
        val r = dayResult(listOf(Entry(today, amount = 5), Entry(today, amount = 5)), today)

        assertEquals(PeriodStatus.MET, r.status)
        assertEquals(10, r.value)
    }

    /** A past day under target is missed. */
    @Test
    fun underTargetPastDayIsMissed() {
        val r = dayResult(listOf(Entry(pastDay, amount = 5)), pastDay)

        assertEquals(PeriodStatus.MISSED, r.status)
    }

    /** Strength is capped at 1.0 even when the amount exceeds the target. */
    @Test
    fun strengthIsCappedAtOne() {
        val r = dayResult(listOf(Entry(pastDay, amount = 25)), pastDay)

        assertEquals(PeriodStatus.MET, r.status)
        assertEquals(1.0, r.strength, 0.0001)
    }

    /** An amount of zero is rejected by validate. */
    @Test
    fun zeroAmountIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            StreakEngine.validate(reading, Entry(today, amount = 0))
        }
    }
}
