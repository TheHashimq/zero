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

    @Test
    fun checkInWeeklyShowsDotsAndText() {
        val c = card(
            def(StreakMode.CHECK_IN, Frequency.WEEKLY, target = 3),
            listOf(Entry(LocalDate.of(2026, 1, 5)), Entry(LocalDate.of(2026, 1, 6))),
        )

        assertEquals(Progress.Dots(2, 3), c.progress)
        assertEquals("2 of 3 this week", c.summaryText)
    }

    @Test
    fun checkInAboveDotLimitUsesBar() {
        val c = card(def(StreakMode.CHECK_IN, Frequency.WEEKLY, target = 20), emptyList())

        assertIs<Progress.Bar>(c.progress)
    }

    @Test
    fun amountShowsBarAndText() {
        val c = card(def(StreakMode.AMOUNT), listOf(Entry(today, amount = 4)))

        assertEquals(Progress.Bar(4, 10, over = false), c.progress)
        assertEquals("4 of 10 today", c.summaryText)
    }

    @Test
    fun limitOverTargetIsFlagged() {
        val c = card(def(StreakMode.LIMIT, target = 120), listOf(Entry(today, amount = 130)))

        assertEquals(Progress.Bar(130, 120, over = true), c.progress)
        assertEquals("130 of 120 limit exceeded", c.summaryText)
    }

    @Test
    fun limitUnderTargetShowsMaximum() {
        val c = card(def(StreakMode.LIMIT, target = 120), listOf(Entry(today, amount = 60)))

        assertEquals(Progress.Bar(60, 120, over = false), c.progress)
        assertEquals("60 of 120 max today", c.summaryText)
    }

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

    @Test
    fun streaksAndStatusAreCarried() {
        val c = card(def(StreakMode.AMOUNT), listOf(Entry(today.minusDays(1), amount = 10)))

        assertEquals(1, c.current)
        assertEquals(1, c.best)
        assertEquals(PeriodStatus.OPEN, c.status)
    }

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
