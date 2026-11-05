/*
 * MODULE: feature-streaks tests
 * PURPOSE: Verifies the Home card values for each mode, the dot limit, limit flags, and empty summaries.
 * TAKES: Fixed definitions and entries with today set to 2026-01-10.
 * GIVES: Pass/fail results for the card model.
 * OWNS: No production state.
 * DOES NOT OWN: Drawing.
 * DEPENDENCIES: feature-streaks.
 * CONTRACT: Each test asserts one display rule.
 * FAILURE BEHAVIOR: Assertion failures identify the wrong value.
 */
package zero.feature.streaks.presentation

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import zero.feature.streaks.ChecklistPart
import zero.feature.streaks.Entry
import zero.feature.streaks.Frequency
import zero.feature.streaks.PeriodStatus
import zero.feature.streaks.StreakDefinition
import zero.feature.streaks.StreakEngine
import zero.feature.streaks.StreakMode
import zero.feature.streaks.StreakSummary

class StreakCardsTest {
    private val today = LocalDate.of(2026, 1, 10)
    private val created = LocalDate.of(2026, 1, 1)

    private fun def(
        mode: StreakMode,
        frequency: Frequency = Frequency.DAILY,
        target: Int = 10,
        parts: List<ChecklistPart> = emptyList(),
    ) = StreakDefinition(
        id = "s1",
        name = "Streak",
        emoji = "B",
        colorIndex = 0,
        mode = mode,
        frequency = frequency,
        target = target,
        parts = parts,
        createdOn = created,
    )

    private fun card(def: StreakDefinition, entries: List<Entry>): StreakCard =
        StreakCards.build(def, StreakEngine.summarize(def, entries, today))

    /** A weekly check-in streak shows dots for the check-ins so far this week. */
    @Test
    fun checkInWeeklyShowsDotsAndText() {
        val c = card(
            def(StreakMode.CHECK_IN, Frequency.WEEKLY, target = 3),
            listOf(Entry(LocalDate.of(2026, 1, 5)), Entry(LocalDate.of(2026, 1, 6))),
        )

        assertEquals(Progress.Dots(2, 3), c.progress)
        assertEquals("2 of 3 this week", c.summaryText)
    }

    /** A check-in target above the dot limit is drawn as a bar instead. */
    @Test
    fun checkInAboveDotLimitUsesBar() {
        val c = card(def(StreakMode.CHECK_IN, Frequency.WEEKLY, target = 20), emptyList())

        assertIs<Progress.Bar>(c.progress)
    }

    /** An amount streak shows the day's total against the target. */
    @Test
    fun amountShowsBarAndText() {
        val c = card(def(StreakMode.AMOUNT), listOf(Entry(today, amount = 4)))

        assertEquals(Progress.Bar(4, 10, over = false), c.progress)
        assertEquals("4 of 10 today", c.summaryText)
    }

    /** A limit day over the limit is flagged and worded as exceeded. */
    @Test
    fun limitOverTargetIsFlagged() {
        val c = card(def(StreakMode.LIMIT, target = 120), listOf(Entry(today, amount = 130)))

        assertEquals(Progress.Bar(130, 120, over = true), c.progress)
        assertEquals("130 of 120 limit exceeded", c.summaryText)
    }

    /** A limit day under the limit shows the usage as a maximum. */
    @Test
    fun limitUnderTargetShowsMaximum() {
        val c = card(def(StreakMode.LIMIT, target = 120), listOf(Entry(today, amount = 60)))

        assertEquals(Progress.Bar(60, 120, over = false), c.progress)
        assertEquals("60 of 120 max today", c.summaryText)
    }

    /** A checklist shows the percentage of weight done. Fard alone, of 3 and 1, is 75%. */
    @Test
    fun checklistShowsPercent() {
        val checklist = def(
            StreakMode.CHECKLIST,
            target = 0,
            parts = listOf(ChecklistPart("fard", 3), ChecklistPart("sunnah", 1)),
        )
        val c = card(checklist, listOf(Entry(today, parts = setOf("fard"))))

        assertEquals(Progress.Percent(75), c.progress)
        assertEquals("75% done today", c.summaryText)
    }

    /** Current and best streaks, status, name, emoji, and color are carried onto the card. */
    @Test
    fun streaksStatusAndIdentityAreCarried() {
        val c = card(def(StreakMode.AMOUNT), listOf(Entry(today.minusDays(1), amount = 10)))

        assertEquals(1, c.current)
        assertEquals(1, c.best)
        assertEquals(PeriodStatus.OPEN, c.status)
        assertEquals("Streak", c.name)
        assertEquals("B", c.emoji)
        assertEquals(0, c.colorIndex)
    }

    /** A summary with no periods gives an open, empty card and does not throw. */
    @Test
    fun emptySummaryGivesOpenEmptyCard() {
        val summary = StreakSummary(
            current = 0,
            best = 0,
            todayStatus = PeriodStatus.OPEN,
            periods = emptyList(),
            lastEntryDate = null,
        )

        val c = StreakCards.build(def(StreakMode.AMOUNT), summary)

        assertEquals(Progress.Bar(0, 10, over = false), c.progress)
        assertEquals(PeriodStatus.OPEN, c.status)
    }
}
