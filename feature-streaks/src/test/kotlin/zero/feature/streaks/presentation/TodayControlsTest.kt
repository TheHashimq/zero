/*
 * MODULE: feature-streaks tests
 * PURPOSE: Verifies the today control for each mode and checklist toggles.
 * TAKES: Fixed definitions and today's entries.
 * GIVES: Pass/fail results for the today rules.
 * OWNS: No production state.
 * DOES NOT OWN: Storage or screens.
 * DEPENDENCIES: feature-streaks.
 * CONTRACT: Each test asserts one rule.
 * FAILURE BEHAVIOR: Assertion failures identify the wrong value.
 */
package zero.feature.streaks.presentation

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import zero.feature.streaks.ChecklistPart
import zero.feature.streaks.Entry
import zero.feature.streaks.Frequency
import zero.feature.streaks.StreakDefinition
import zero.feature.streaks.StreakMode

class TodayControlsTest {
    private val today = LocalDate.of(2026, 1, 10)

    private fun def(
        mode: StreakMode,
        target: Int = 10,
        limit: Int = 120,
        parts: List<ChecklistPart> = emptyList(),
    ) = StreakDefinition(
        id = "s1",
        name = "S",
        emoji = "B",
        colorIndex = 0,
        mode = mode,
        frequency = Frequency.DAILY,
        target = when (mode) {
            StreakMode.CHECKLIST -> 0
            StreakMode.LIMIT -> limit
            else -> target
        },
        parts = parts,
        createdOn = LocalDate.of(2026, 1, 1),
    )

    private val checklist = def(
        StreakMode.CHECKLIST,
        parts = listOf(ChecklistPart("fard", 3), ChecklistPart("sunnah", 1)),
    )

    /** Check-in counts today's entries against the target. */
    @Test
    fun checkInCountsTodayEntries() {
        val control = TodayControls.build(
            def(StreakMode.CHECK_IN, target = 3),
            listOf(Entry(today), Entry(today)),
        )

        assertEquals(TodayControl.CheckIn(2, 3), control)
    }

    /** Amount sums today's logs. */
    @Test
    fun amountSumsTodayLogs() {
        val control = TodayControls.build(
            def(StreakMode.AMOUNT),
            listOf(Entry(today, amount = 4), Entry(today, amount = 5)),
        )

        assertEquals(TodayControl.Amount(9, 10), control)
    }

    /** Limit over the limit is flagged. */
    @Test
    fun limitOverIsFlagged() {
        val control = TodayControls.build(def(StreakMode.LIMIT, limit = 120), listOf(Entry(today, amount = 130)))

        assertTrue((control as TodayControl.Limit).over)
    }

    /** Limit at or under the limit is not flagged. */
    @Test
    fun limitUnderIsNotFlagged() {
        val control = TodayControls.build(def(StreakMode.LIMIT, limit = 120), listOf(Entry(today, amount = 120)))

        assertTrue(!(control as TodayControl.Limit).over)
    }

    /** The highest-weighted part is marked required; the others are not. */
    @Test
    fun checklistMarksRequiredParts() {
        val control = TodayControls.build(checklist, emptyList()) as TodayControl.Checklist

        assertTrue(control.parts.first { it.name == "fard" }.required)
        assertTrue(!control.parts.first { it.name == "sunnah" }.required)
    }

    /** Parts logged today are shown as checked. */
    @Test
    fun checklistShowsCheckedParts() {
        val control =
            TodayControls.build(checklist, listOf(Entry(today, parts = setOf("fard")))) as TodayControl.Checklist

        assertTrue(control.parts.first { it.name == "fard" }.checked)
        assertTrue(!control.parts.first { it.name == "sunnah" }.checked)
    }

    /** Toggling an unchecked part on today creates one entry with that part. */
    @Test
    fun toggleAddsPart() {
        val result = TodayControls.toggleChecklistPart(checklist, emptyList(), today, "fard")

        assertEquals(listOf(Entry(today, parts = setOf("fard"))), result)
    }

    /** Toggling the only checked part off leaves no entry for today. */
    @Test
    fun toggleRemovesLastPartAndClearsDay() {
        val result = TodayControls.toggleChecklistPart(
            checklist,
            listOf(Entry(today, parts = setOf("fard"))),
            today,
            "fard",
        )

        assertTrue(result.isEmpty())
    }

    /** Toggling another part keeps the parts already checked. */
    @Test
    fun toggleKeepsOtherParts() {
        val result = TodayControls.toggleChecklistPart(
            checklist,
            listOf(Entry(today, parts = setOf("fard"))),
            today,
            "sunnah",
        )

        assertEquals(setOf("fard", "sunnah"), result.single().parts)
    }

    /** Toggling a part the streak doesn't have is rejected. */
    @Test
    fun toggleUnknownPartIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            TodayControls.toggleChecklistPart(checklist, emptyList(), today, "witr")
        }
    }
}
