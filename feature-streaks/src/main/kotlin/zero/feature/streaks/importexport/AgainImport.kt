/*
 * MODULE: feature-streaks
 * PURPOSE: Converts a settings export from the Again app into Zero streaks.
 * TAKES: The exported JSON text, today's date, and the repository to import into.
 * GIVES: Imported streak definitions with entries and reminders, a list of skipped items with reasons,
 *        and a summary of how many streaks were added.
 * OWNS: The Again field names and the first_day_of_week and color mappings.
 * DOES NOT OWN: Storage (see StreakRepository) or evaluation (see StreakEngine).
 * DEPENDENCIES: kotlinx-serialization-json, StreakPalette, StreakRepository.
 * CONTRACT: Only the wrapper object with "settings" and "streaks" is accepted. Imported currentStreak and bestStreak
 *           are ignored and recalculated. Completions are deduplicated per streak. Completions before the streak's
 *           start move the start back. Future or invalid dates are skipped with a note. Streaks whose ID already
 *           exists are skipped, so re-importing the same file adds nothing.
 * FAILURE BEHAVIOR: A payload that isn't the wrapper object throws UnsupportedImportException. A bad streak is skipped
 *                   with a note and does not stop the others. Storage failures propagate as StreakStorageException.
 */
package zero.feature.streaks.importexport

import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import zero.feature.streaks.Entry
import zero.feature.streaks.Frequency
import zero.feature.streaks.StreakDefinition
import zero.feature.streaks.StreakMode
import zero.feature.streaks.StreakPalette
import zero.feature.streaks.storage.StreakRepository

class UnsupportedImportException(message: String) : RuntimeException(message)

@Serializable
data class AgainReminder(val time: String, val days: List<Int> = emptyList())

@Serializable
data class AgainSettings(
    val theme: String? = null,
    @SerialName("notifications_enabled") val notificationsEnabled: Boolean? = null,
    @SerialName("first_day_of_week") val firstDayOfWeek: Int? = null,
)

@Serializable
data class AgainStreak(
    val id: String,
    val name: String,
    val emoji: String,
    val frequency: String,
    val frequencyCount: Int,
    val createdDate: String,
    val lastCompletedDate: String? = null,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val completions: List<String> = emptyList(),
    val reminder: AgainReminder? = null,
    val color: String? = null,
    val position: Int? = null,
)

/** A streak ready to be stored, with its entries and the reminder kept for the reminder phase. */
data class ImportedStreak(
    val definition: StreakDefinition,
    val order: Int,
    val entries: List<Pair<String, Entry>>,
    val reminder: AgainReminder?,
)

data class ImportReport(val streaks: List<ImportedStreak>, val skipped: List<String>)

data class ImportSummary(val added: Int, val skipped: List<String>)

object AgainImporter {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private const val DEFAULT_COLOR = "#FF9900"

    /** Parses an Again export without touching storage. Streaks whose ID is in [existingIds] are skipped. */
    fun parse(text: String, today: LocalDate, existingIds: Set<String>): ImportReport {
        val root = try {
            json.parseToJsonElement(text)
        } catch (failure: Exception) {
            throw UnsupportedImportException("the file is not valid JSON")
        }
        val wrapper = root as? JsonObject
            ?: throw UnsupportedImportException("not an Again settings export: expected an object, not a list")
        val settingsElement = wrapper["settings"]
            ?: throw UnsupportedImportException("not an Again settings export: missing 'settings'")
        val streaksElement = wrapper["streaks"]
            ?: throw UnsupportedImportException("not an Again settings export: missing 'streaks'")
        val streaksList = streaksElement as? JsonArray
            ?: throw UnsupportedImportException("'streaks' must be a list")
        val settings = try {
            json.decodeFromJsonElement(AgainSettings.serializer(), settingsElement)
        } catch (failure: Exception) {
            throw UnsupportedImportException("'settings' must be an object")
        }
        val weekStart = weekStartFrom(settings.firstDayOfWeek)

        val imported = mutableListOf<ImportedStreak>()
        val skipped = mutableListOf<String>()
        for (element in streaksList) {
            val dto = try {
                json.decodeFromJsonElement(AgainStreak.serializer(), element)
            } catch (failure: Exception) {
                skipped += "unreadable streak entry"
                continue
            }
            if (dto.id in existingIds) {
                skipped += "${dto.name}: already imported"
                continue
            }
            try {
                imported += map(dto, weekStart, today, skipped)
            } catch (failure: Exception) {
                skipped += "${dto.name}: ${failure.message}"
            }
        }
        return ImportReport(imported, skipped)
    }

    /** Parses [text] and imports every new streak into [repository]. Re-importing the same text adds nothing. */
    fun importInto(repository: StreakRepository, text: String, today: LocalDate): ImportSummary {
        val report = parse(text, today, repository.definitions().map { it.id }.toSet())
        var added = 0
        for (streak in report.streaks) {
            if (repository.importStreak(streak.definition, streak.order, streak.entries)) added++
        }
        return ImportSummary(added, report.skipped)
    }

    /** The text shown after an import: the count added, and each skipped item with its reason. */
    fun describe(summary: ImportSummary): String {
        val head = if (summary.added == 1) "Added 1 streak." else "Added ${summary.added} streaks."
        if (summary.skipped.isEmpty()) return head
        return head + "\n\nSkipped:\n" + summary.skipped.joinToString("\n") { "• $it" }
    }

    private fun map(dto: AgainStreak, weekStart: DayOfWeek, today: LocalDate, notes: MutableList<String>): ImportedStreak {
        val frequency = try {
            Frequency.valueOf(dto.frequency)
        } catch (failure: IllegalArgumentException) {
            throw IllegalArgumentException("unknown frequency '${dto.frequency}'")
        }
        require(dto.frequencyCount >= 1) { "frequencyCount must be at least 1" }
        require(dto.name.isNotBlank()) { "name is blank" }
        val created = LocalDate.parse(dto.createdDate)
        require(!created.isAfter(today)) { "creation date is in the future" }

        val dates = sortedSetOf<LocalDate>()
        for (raw in dto.completions) {
            val date = try {
                LocalDate.parse(raw)
            } catch (failure: Exception) {
                notes += "${dto.name}: skipped invalid date '$raw'"
                continue
            }
            if (date.isAfter(today)) {
                notes += "${dto.name}: skipped future date $date"
                continue
            }
            dates += date
        }

        val earliest = dates.firstOrNull()
        val startDate = if (earliest != null && earliest.isBefore(created)) earliest else created

        val definition = StreakDefinition(
            id = dto.id,
            name = dto.name.trim(),
            emoji = dto.emoji,
            colorIndex = nearestPaletteIndex(dto.color),
            mode = StreakMode.CHECK_IN,
            frequency = frequency,
            target = dto.frequencyCount,
            parts = emptyList(),
            createdOn = startDate,
            weekStart = weekStart,
        )
        val entries = dates.map { date -> "again:${dto.id}:$date" to Entry(date) }
        return ImportedStreak(definition, dto.position ?: 0, entries, dto.reminder)
    }

    private fun weekStartFrom(value: Int?): DayOfWeek =
        if (value != null && value in 1..7) DayOfWeek.of(value) else DayOfWeek.MONDAY

    /** The palette index whose color is closest to [hex]. Missing or invalid colors use Again's default. */
    fun nearestPaletteIndex(hex: String?): Int {
        val target = runCatching { rgb(hex ?: DEFAULT_COLOR) }.getOrElse { rgb(DEFAULT_COLOR) }
        return StreakPalette.COLORS.indices.minBy { index -> distance(target, paletteRgb(index)) }
    }

    private fun paletteRgb(index: Int): Triple<Int, Int, Int> {
        val argb = StreakPalette.COLORS[index]
        return Triple((argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF)
    }

    private fun rgb(hex: String): Triple<Int, Int, Int> {
        val digits = hex.removePrefix("#")
        require(digits.length == 6 || digits.length == 8) { "bad color $hex" }
        val value = digits.takeLast(6).toLong(16)
        return Triple(((value shr 16) and 0xFF).toInt(), ((value shr 8) and 0xFF).toInt(), (value and 0xFF).toInt())
    }

    private fun distance(a: Triple<Int, Int, Int>, b: Triple<Int, Int, Int>): Int {
        val dr = a.first - b.first
        val dg = a.second - b.second
        val db = a.third - b.third
        return dr * dr + dg * dg + db * db
    }
}
