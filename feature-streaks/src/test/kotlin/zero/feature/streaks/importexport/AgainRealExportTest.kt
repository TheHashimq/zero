/*
 * MODULE: feature-streaks tests
 * PURPOSE: Real-file regression scaffold for the Again export snapshot.
 * TAKES: The fixture again_export_2026-11-01.json and the export date 2026-11-01.
 * GIVES: Pass/fail results per streak.
 * OWNS: No production state.
 * DOES NOT OWN: The importer rules, which have their own tests.
 * DEPENDENCIES: feature-streaks, kotlinx-serialization-json.
 * CONTRACT: Every streak imports, and each streak's current and best match Again's saved values.
 * FAILURE BEHAVIOR: Assertion failures name the streak ID and both values.
 */
package zero.feature.streaks.importexport

import java.time.LocalDate
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import zero.feature.streaks.storage.DirectoryStreakFileStore
import zero.feature.streaks.storage.StreakRepository

class AgainRealExportTest {
    private val exportDate = LocalDate.of(2026, 11, 1)

    /**
     * This is a red-state scaffold for the real Again export. Replace the fixture with the real file and
     * the expected saved values will turn green when the mapping is correct.
     */
    @Test
    fun realExportRecalculatesSavedCurrentAndBest() {
        val text = javaClass.classLoader
            .getResource("again_export_2026-11-01.json")!!
            .readText(Charsets.UTF_8)
        val streaks = Json.parseToJsonElement(text).jsonObject["streaks"]!!.jsonArray
        val repo = StreakRepository.open(DirectoryStreakFileStore(createTempDirectory("real").toFile()))

        val result = AgainImporter.importInto(repo, text, exportDate)

        assertEquals(streaks.size, result.added, "every streak should import; skipped: ${result.skipped}")
        streaks.forEach { element ->
            val saved = element.jsonObject
            val id = saved["id"]!!.jsonPrimitive.content
            val savedCurrent = saved["currentStreak"]!!.jsonPrimitive.content.toInt()
            val savedBest = saved["bestStreak"]!!.jsonPrimitive.content.toInt()
            val calc = repo.summary(id, exportDate)
            assertEquals(savedCurrent, calc.current, "current mismatch for $id")
            assertEquals(savedBest, calc.best, "best mismatch for $id")
        }
    }
}
