/*
 * MODULE: feature-streaks tests
 * PURPOSE: Verifies the Again importer against the exact export shape, including defaults, dedupe, dates,
 *          re-import, and recalculated streaks.
 * TAKES: Synthetic payloads in the documented Again export shape.
 * GIVES: Pass/fail results for the converter.
 * OWNS: No production state.
 * DOES NOT OWN: Evaluation rules, which have their own tests.
 * DEPENDENCIES: feature-streaks, kotlin.io.path.
 * CONTRACT: Each test asserts one import rule.
 * FAILURE BEHAVIOR: Assertion failures identify the broken rule.
 */
package zero.feature.streaks.importexport

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import zero.feature.streaks.PeriodStatus
import zero.feature.streaks.StreakMode
import zero.feature.streaks.StreakPalette
import zero.feature.streaks.storage.DirectoryStreakFileStore
import zero.feature.streaks.storage.StreakRepository

class AgainImportTest {
    private val today = LocalDate.of(2026, 1, 10)
    private val settings = """{"theme":"system","notifications_enabled":true,"first_day_of_week":7}"""

    private fun wrap(vararg streaks: String, settingsJson: String = settings): String =
        """{"settings":$settingsJson,"streaks":[${streaks.joinToString(",")}]}"""

    private fun streak(
        id: String = "a1",
        name: String = "Reading",
        frequency: String = "DAILY",
        count: Int = 1,
        created: String = "2026-01-03",
        completions: List<String> = listOf("2026-01-03", "2026-01-04", "2026-01-05", "2026-01-05"),
        color: String? = "#FF9900",
        position: Int? = 2,
        reminder: Boolean = true,
    ): String {
        val parts = mutableListOf(
            "\"id\":\"$id\"",
            "\"name\":\"$name\"",
            "\"emoji\":\"B\"",
            "\"frequency\":\"$frequency\"",
            "\"frequencyCount\":$count",
            "\"createdDate\":\"$created\"",
            "\"currentStreak\":0",
            "\"bestStreak\":0",
            "\"completions\":[" + completions.joinToString(",") { "\"$it\"" } + "]",
        )
        if (color != null) parts += "\"color\":\"$color\""
        if (position != null) parts += "\"position\":$position"
        if (reminder) parts += "\"reminder\":{\"time\":\"08:30\",\"days\":[0,1,2,3,4,5,6]}"
        return "{" + parts.joinToString(",") + "}"
    }

    private fun parse(text: String) = AgainImporter.parse(text, today, emptySet())

    /** A bare array is an internal Again file, not an export, and is rejected. */
    @Test
    fun bareArrayIsRejected() {
        assertFailsWith<UnsupportedImportException> { parse("[${streak()}]") }
    }

    /** A wrapper without settings is rejected. */
    @Test
    fun missingSettingsIsRejected() {
        assertFailsWith<UnsupportedImportException> { parse("""{"streaks":[${streak()}]}""") }
    }

    /** A wrapper without streaks is rejected. */
    @Test
    fun missingStreaksIsRejected() {
        assertFailsWith<UnsupportedImportException> { parse("""{"settings":$settings}""") }
    }

    /** Core fields map to a check-in streak with the frequency count as its target. */
    @Test
    fun mapsCoreFields() {
        val s = parse(wrap(streak(count = 3, frequency = "WEEKLY", position = 5))).streaks.single()

        assertEquals("a1", s.definition.id)
        assertEquals("Reading", s.definition.name)
        assertEquals(StreakMode.CHECK_IN, s.definition.mode)
        assertEquals(3, s.definition.target)
        assertEquals(5, s.order)
    }

    /** The first_day_of_week setting becomes the streak's week start. 7 means Sunday. */
    @Test
    fun firstDayOfWeekSettingSetsWeekStart() {
        val s = parse(wrap(streak())).streaks.single()

        assertEquals(DayOfWeek.SUNDAY, s.definition.weekStart)
    }

    /** A missing first_day_of_week falls back to Monday. */
    @Test
    fun missingFirstDayDefaultsToMonday() {
        val s = parse(wrap(streak(), settingsJson = """{"theme":"system"}""")).streaks.single()

        assertEquals(DayOfWeek.MONDAY, s.definition.weekStart)
    }

    /** A repeated completion date becomes one entry. */
    @Test
    fun duplicateCompletionsAreMerged() {
        val s = parse(wrap(streak())).streaks.single()

        assertEquals(3, s.entries.size)
    }

    /** A missing color maps to the nearest palette color, which is yellow for Again's default orange. */
    @Test
    fun missingColorDefaultsToNearestPaletteColor() {
        val s = parse(wrap(streak(color = null))).streaks.single()

        assertEquals(2, s.definition.colorIndex)
        assertEquals(2, AgainImporter.nearestPaletteIndex("#FF9900"))
    }

    /** A missing position defaults to zero. */
    @Test
    fun missingPositionDefaultsToZero() {
        val s = parse(wrap(streak(position = null))).streaks.single()

        assertEquals(0, s.order)
    }

    /** A completion before the creation date moves the start back to that completion. */
    @Test
    fun earlyCompletionMovesStartBack() {
        val s = parse(wrap(streak(created = "2026-01-03", completions = listOf("2025-12-30")))).streaks.single()

        assertEquals(LocalDate.of(2025, 12, 30), s.definition.createdOn)
    }

    /** A completion after today is skipped and reported, not imported. */
    @Test
    fun futureCompletionIsSkippedWithNote() {
        val report = parse(wrap(streak(completions = listOf("2026-01-09", "2026-01-11"))))

        assertEquals(1, report.streaks.single().entries.size)
        assertTrue(report.skipped.any { it.contains("future") })
    }

    /** An unparseable completion date is skipped and reported. */
    @Test
    fun invalidDateIsSkippedWithNote() {
        val report = parse(wrap(streak(completions = listOf("not-a-date", "2026-01-05"))))

        assertEquals(1, report.streaks.single().entries.size)
        assertTrue(report.skipped.any { it.contains("invalid date") })
    }

    /** An unknown frequency drops that streak only, and the others still import. */
    @Test
    fun unknownFrequencyDropsOnlyThatStreak() {
        val report = parse(wrap(streak(id = "bad", frequency = "FORTNIGHTLY"), streak(id = "good")))

        assertEquals(listOf("good"), report.streaks.map { it.definition.id })
        assertTrue(report.skipped.any { it.contains("unknown frequency") })
    }

    /** The reminder is kept in the parsed result for the reminder phase. */
    @Test
    fun reminderIsKeptForLaterPhase() {
        val reminder = parse(wrap(streak())).streaks.single().reminder

        assertEquals("08:30", reminder?.time)
        assertEquals(7, reminder?.days?.size)
    }

    /** Importing the same file twice adds nothing the second time. */
    @Test
    fun reimportIsNoOp() {
        val repo = StreakRepository.open(DirectoryStreakFileStore(createTempDirectory("again").toFile()))
        val text = wrap(streak())

        val first = AgainImporter.importInto(repo, text, today)
        val second = AgainImporter.importInto(repo, text, today)

        assertEquals(1, first.added)
        assertEquals(0, second.added)
        assertEquals(3, repo.entries("a1").size)
        assertTrue(second.skipped.any { it.contains("already imported") })
    }

    /** The imported streak recalculates to the same current and best streaks Again saved (3 and 3). */
    @Test
    fun recalculatedStreaksMatchAgainSaved() {
        val repo = StreakRepository.open(DirectoryStreakFileStore(createTempDirectory("again2").toFile()))
        AgainImporter.importInto(repo, wrap(streak()), LocalDate.of(2026, 1, 5))

        val summary = repo.summary("a1", LocalDate.of(2026, 1, 5))

        assertEquals(3, summary.current)
        assertEquals(3, summary.best)
        assertEquals(PeriodStatus.MET, summary.todayStatus)
    }

    /** The palette mapping never returns an index outside the palette. */
    @Test
    fun nearestIndexStaysInPalette() {
        val index = AgainImporter.nearestPaletteIndex("#123456")

        assertTrue(index in 0 until StreakPalette.SIZE)
    }
}
