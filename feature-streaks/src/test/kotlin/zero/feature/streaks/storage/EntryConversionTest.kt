/*
 * MODULE: feature-streaks tests
 * PURPOSE: Verifies how history converts between modes and checklist part changes.
 * TAKES: Explicit stored entries and modes.
 * GIVES: Pass/fail results for the conversion rules.
 * OWNS: No production state.
 * DOES NOT OWN: Evaluation.
 * DEPENDENCIES: feature-streaks.
 * CONTRACT: Each test asserts one conversion rule.
 * FAILURE BEHAVIOR: Assertion failures identify the wrong conversion.
 */
package zero.feature.streaks.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import zero.feature.streaks.StreakMode

class EntryConversionTest {
    private fun e(id: String, date: String, amount: Int = 0, parts: List<String> = emptyList()) =
        StoredEntry(id, date, amount, parts)

    /** A check-in day becomes one unit when the streak becomes an amount streak. */
    @Test
    fun checkInToAmountMakesEachDayOneUnit() {
        val out = EntryConversion.convert(
            listOf(e("a", "2026-01-01"), e("b", "2026-01-02")),
            StreakMode.CHECK_IN,
            StreakMode.AMOUNT,
            emptyList(),
        )

        assertEquals(listOf(1, 1), out.map { it.amount })
        assertEquals(listOf("2026-01-01", "2026-01-02"), out.map { it.date })
    }

    /** Several amount entries on one day become a single check-in. */
    @Test
    fun amountToCheckInCollapsesDays() {
        val out = EntryConversion.convert(
            listOf(e("a", "2026-01-01", 5), e("b", "2026-01-01", 7), e("c", "2026-01-02", 3)),
            StreakMode.AMOUNT,
            StreakMode.CHECK_IN,
            emptyList(),
        )

        assertEquals(2, out.size)
        assertTrue(out.all { it.amount == 0 })
    }

    /** A check-in day counts every checklist part as done. */
    @Test
    fun checkInToChecklistMarksAllParts() {
        val out = EntryConversion.convert(
            listOf(e("a", "2026-01-01")),
            StreakMode.CHECK_IN,
            StreakMode.CHECKLIST,
            listOf("fard", "sunnah"),
        )

        assertEquals(setOf("fard", "sunnah"), out.single().parts.toSet())
    }

    /** Checklist days become one unit each when the streak becomes an amount streak. */
    @Test
    fun checklistToAmountGivesOneUnitPerDay() {
        val out = EntryConversion.convert(
            listOf(e("a", "2026-01-01", 0, listOf("fard"))),
            StreakMode.CHECKLIST,
            StreakMode.AMOUNT,
            emptyList(),
        )

        assertEquals(1, out.single().amount)
        assertTrue(out.single().parts.isEmpty())
    }

    /** Staying in the same mode keeps every entry exactly as it was. */
    @Test
    fun sameModeAmountKeepsEntries() {
        val input = listOf(e("a", "2026-01-01", 5), e("b", "2026-01-01", 7))

        assertEquals(input, EntryConversion.convert(input, StreakMode.AMOUNT, StreakMode.AMOUNT, emptyList()))
    }

    /** Checklist parts that no longer exist are dropped from history. */
    @Test
    fun removedChecklistPartsAreDropped() {
        val out = EntryConversion.convert(
            listOf(e("a", "2026-01-01", 0, listOf("fard", "witr"))),
            StreakMode.CHECKLIST,
            StreakMode.CHECKLIST,
            listOf("fard", "sunnah"),
        )

        assertEquals(listOf("fard"), out.single().parts)
    }

    /** Parts checked in separate entries on the same day are combined. */
    @Test
    fun checklistPartsFromSameDayAreMerged() {
        val out = EntryConversion.convert(
            listOf(e("a", "2026-01-01", 0, listOf("fard")), e("b", "2026-01-01", 0, listOf("sunnah"))),
            StreakMode.CHECKLIST,
            StreakMode.CHECKLIST,
            listOf("fard", "sunnah"),
        )

        assertEquals(setOf("fard", "sunnah"), out.single().parts.toSet())
    }
}
