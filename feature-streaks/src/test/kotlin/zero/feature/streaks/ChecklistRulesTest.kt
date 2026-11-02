/*
 * MODULE: feature-streaks tests
 * PURPOSE: Verifies required parts, weighted strength, tied weights, and merged logs for checklists.
 * TAKES: A fixed checklist definition and entries.
 * GIVES: Pass/fail results for checklist rules.
 * OWNS: No production state.
 * DOES NOT OWN: Other modes.
 * DEPENDENCIES: feature-streaks.
 * CONTRACT: Each test asserts one checklist rule.
 * FAILURE BEHAVIOR: Assertion failures identify the broken rule.
 */
package zero.feature.streaks

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ChecklistRulesTest {
    private val today = LocalDate.of(2026, 1, 10)
    private val day = LocalDate.of(2026, 1, 9)

    private fun prayer(fard: Int, sunnah: Int) = StreakDefinition(
        id = "fajr",
        name = "Fajr",
        emoji = "F",
        colorIndex = 0,
        mode = StreakMode.CHECKLIST,
        frequency = Frequency.DAILY,
        target = 0,
        parts = listOf(ChecklistPart("fard", fard), ChecklistPart("sunnah", sunnah)),
        createdOn = LocalDate.of(2026, 1, 1),
    )

    private fun resultFor(def: StreakDefinition, entries: List<Entry>): PeriodResult =
        StreakEngine.summarize(def, entries, today).periods.first { it.start == day }

    /** A sunnah-only day fails, because the highest-weighted part is missing. Strength is 0.25. */
    @Test
    fun sunnahOnlyFailsWithQuarterStrength() {
        val r = resultFor(prayer(3, 1), listOf(Entry(day, parts = setOf("sunnah"))))

        assertEquals(PeriodStatus.MISSED, r.status)
        assertEquals(0.25, r.strength, 0.0001)
    }

    /** A fard-only day passes and has strength 0.75. */
    @Test
    fun fardOnlyPassesWithThreeQuarterStrength() {
        val r = resultFor(prayer(3, 1), listOf(Entry(day, parts = setOf("fard"))))

        assertEquals(PeriodStatus.MET, r.status)
        assertEquals(0.75, r.strength, 0.0001)
    }

    /** Fard and sunnah together pass with full strength. */
    @Test
    fun bothPartsGiveFullStrength() {
        val r = resultFor(prayer(3, 1), listOf(Entry(day, parts = setOf("fard", "sunnah"))))

        assertEquals(PeriodStatus.MET, r.status)
        assertEquals(1.0, r.strength, 0.0001)
    }

    /** Tied top weights are all required, so one of them alone fails. */
    @Test
    fun tiedTopWeightsAreAllRequired() {
        val r = resultFor(prayer(2, 2), listOf(Entry(day, parts = setOf("fard"))))

        assertEquals(PeriodStatus.MISSED, r.status)
    }

    /** Parts logged in separate entries on the same day add up. */
    @Test
    fun partsFromSeparateLogsMerge() {
        val r = resultFor(
            prayer(3, 1),
            listOf(Entry(day, parts = setOf("fard")), Entry(day, parts = setOf("sunnah"))),
        )

        assertEquals(PeriodStatus.MET, r.status)
        assertEquals(1.0, r.strength, 0.0001)
    }

    /** An entry naming a part the streak doesn't have is rejected by validate. */
    @Test
    fun unknownPartIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            StreakEngine.validate(prayer(3, 1), Entry(day, parts = setOf("witr")))
        }
    }
}
