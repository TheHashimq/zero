/*
 * MODULE: feature-streaks tests
 * PURPOSE: Verifies definition validation.
 * TAKES: Valid and invalid definitions.
 * GIVES: Pass/fail results for validation.
 * OWNS: No production state.
 * DOES NOT OWN: Evaluation.
 * DEPENDENCIES: feature-streaks.
 * CONTRACT: Each test asserts one validation rule.
 * FAILURE BEHAVIOR: Assertion failures identify the broken rule.
 */
package zero.feature.streaks

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertFailsWith

class StreakDefinitionTest {
    private val created = LocalDate.of(2026, 1, 1)

    private fun def(
        name: String = "Reading",
        mode: StreakMode = StreakMode.AMOUNT,
        frequency: Frequency = Frequency.DAILY,
        target: Int = 10,
        parts: List<ChecklistPart> = emptyList(),
        colorIndex: Int = 0,
    ) = StreakDefinition("s1", name, "B", colorIndex, mode, frequency, target, parts, created)

    /** A valid amount streak is accepted. */
    @Test
    fun validAmountDefinitionIsAccepted() {
        def()
    }

    /** A blank name is rejected. */
    @Test
    fun blankNameIsRejected() {
        assertFailsWith<IllegalArgumentException> { def(name = "  ") }
    }

    /** A zero target is rejected for non-checklist modes. */
    @Test
    fun zeroTargetIsRejected() {
        assertFailsWith<IllegalArgumentException> { def(target = 0) }
    }

    /** A checklist that is not daily is rejected. */
    @Test
    fun checklistMustBeDaily() {
        assertFailsWith<IllegalArgumentException> {
            def(mode = StreakMode.CHECKLIST, frequency = Frequency.WEEKLY, parts = listOf(ChecklistPart("fard", 3)))
        }
    }

    /** A checklist with no parts is rejected. */
    @Test
    fun checklistNeedsParts() {
        assertFailsWith<IllegalArgumentException> {
            def(mode = StreakMode.CHECKLIST, parts = emptyList())
        }
    }

    /** Checklist part names must be unique. */
    @Test
    fun duplicatePartNamesAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            def(mode = StreakMode.CHECKLIST, parts = listOf(ChecklistPart("fard", 3), ChecklistPart("fard", 1)))
        }
    }

    /** A color index outside the palette is rejected. */
    @Test
    fun colorIndexOutOfRangeIsRejected() {
        assertFailsWith<IllegalArgumentException> { def(colorIndex = StreakPalette.SIZE) }
    }

    /** A part weight outside 1 to 10 is rejected. */
    @Test
    fun weightOutOfRangeIsRejected() {
        assertFailsWith<IllegalArgumentException> { ChecklistPart("sunnah", 11) }
    }
}
