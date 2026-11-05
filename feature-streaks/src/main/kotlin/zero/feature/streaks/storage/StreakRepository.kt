/*
 * MODULE: feature-streaks
 * PURPOSE: Stores streaks and entries in one versioned file, and derives summaries from entries.
 * TAKES: A StreakFileStore, and streak definitions and entries from the caller.
 * GIVES: Definitions, entries with IDs, summaries, and a recovery flag after a corrupt file.
 * OWNS: The in-memory list of stored streaks, mirrored to the file after each change.
 * DOES NOT OWN: Evaluation rules (StreakEngine) and file details (StreakFileStore).
 * DEPENDENCIES: StreakEngine, StreakDefinition, Entry, StreakFile, StreakFileStore.
 * CONTRACT: Every change is written before it becomes visible. A failed write restores the previous state.
 *           Entries are added, deleted, or replaced atomically. Updating a streak converts history if its mode
 *           or checklist parts change.
 *           A file with a newer schema is never overwritten.
 * FAILURE BEHAVIOR: An unreadable or invalid file is renamed to streaks.json.corrupt, and the repository opens
 *                   empty with recoveredFromCorrupt = true. A newer schema throws UnsupportedSchemaException and
 *                   leaves the file alone. Save failures throw StreakStorageException. Invalid input throws
 *                   IllegalArgumentException.
 */
package zero.feature.streaks.storage

import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import zero.feature.streaks.ChecklistPart
import zero.feature.streaks.Entry
import zero.feature.streaks.Frequency
import zero.feature.streaks.StreakDefinition
import zero.feature.streaks.StreakEngine
import zero.feature.streaks.StreakMode
import zero.feature.streaks.StreakSummary

/** A stored entry with its ID, so a screen can delete a single entry. */
data class IdentifiedEntry(val id: String, val entry: Entry)

class StreakStorageException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class UnsupportedSchemaException(found: Int) :
    RuntimeException("streak file schema $found is newer than the supported $SUPPORTED_SCHEMA_VERSION")

class StreakRepository private constructor(
    private val files: StreakFileStore,
    private var streaks: List<StoredStreak>,
    val recoveredFromCorrupt: Boolean,
) {
    companion object {
        /** Opens the repository, recovering from a damaged file where safe to do so. */
        fun open(files: StreakFileStore): StreakRepository {
            val text = readOrThrow(files) ?: return StreakRepository(files, emptyList(), false)

            val loaded: List<StoredStreak>? = try {
                val file = StreakCodec.decode(text)
                if (file.schemaVersion > SUPPORTED_SCHEMA_VERSION) {
                    throw UnsupportedSchemaException(file.schemaVersion)
                }
                if (file.schemaVersion < 1) throw IllegalStateException("invalid schema version")
                file.streaks.forEach { it.toDefinition() }
                file.streaks
            } catch (failure: UnsupportedSchemaException) {
                throw failure
            } catch (failure: Exception) {
                null
            }

            if (loaded != null) return StreakRepository(files, loaded, false)

            try {
                files.moveToCorrupt()
            } catch (failure: IOException) {
                throw StreakStorageException("could not set the damaged file aside", failure)
            }
            return StreakRepository(files, emptyList(), true)
        }

        private fun readOrThrow(files: StreakFileStore): String? = try {
            files.read()
        } catch (failure: IOException) {
            throw StreakStorageException("could not read the streak file", failure)
        }
    }

    /** Streak definitions, ordered by their position and then by ID. */
    fun definitions(): List<StreakDefinition> =
        streaks.sortedWith(compareBy({ it.order }, { it.id })).map { it.toDefinition() }

    /** Entries for one streak, sorted by date and then by ID. */
    fun entries(streakId: String): List<StoredEntry> =
        find(streakId).entries.sortedWith(compareBy({ it.date }, { it.id }))

    /** Current summary of one streak, derived from its entries. */
    fun summary(streakId: String, today: LocalDate): StreakSummary {
        val stored = find(streakId)
        return StreakEngine.summarize(stored.toDefinition(), stored.entries.map { it.toEntry() }, today)
    }

    fun addStreak(def: StreakDefinition) {
        require(streaks.none { it.id == def.id }) { "duplicate streak id ${def.id}" }
        commit(streaks + def.toStored(order = streaks.size, entries = emptyList()))
    }

    fun updateStreak(def: StreakDefinition) {
        val current = find(def.id)
        val converted = EntryConversion.convert(
            entries = current.entries,
            fromMode = current.toDefinition().mode,
            toMode = def.mode,
            toPartNames = def.parts.map { it.name },
        )
        commit(streaks.map { if (it.id == def.id) def.toStored(current.order, converted) else it })
    }

    fun deleteStreak(streakId: String) {
        find(streakId)
        commit(streaks.filterNot { it.id == streakId })
    }

    /** Adds an entry and returns its generated ID. */
    fun addEntry(streakId: String, entry: Entry, today: LocalDate): String {
        val stored = find(streakId)
        val def = stored.toDefinition()
        require(!entry.date.isAfter(today)) { "entries cannot be dated after today" }
        require(!entry.date.isBefore(def.createdOn)) { "entries cannot be dated before the streak was created" }

        val normalized = when {
            entry.amount == 0 && entry.parts.isEmpty() && (def.mode == StreakMode.AMOUNT || def.mode == StreakMode.LIMIT) ->
                entry.copy(amount = 1)
            else -> entry
        }

        StreakEngine.validate(def, normalized)
        val id = UUID.randomUUID().toString()
        val updated = stored.copy(entries = stored.entries + storedEntryFrom(id, normalized))
        commit(streaks.map { if (it.id == streakId) updated else it })
        return id
    }

    fun deleteEntry(streakId: String, entryId: String) {
        val stored = find(streakId)
        require(stored.entries.any { it.id == entryId }) { "unknown entry $entryId" }
        val updated = stored.copy(entries = stored.entries.filterNot { it.id == entryId })
        commit(streaks.map { if (it.id == streakId) updated else it })
    }

    /** Entries for one streak with their IDs, oldest first. */
    fun identifiedEntries(streakId: String): List<IdentifiedEntry> =
        find(streakId).entries
            .sortedWith(compareBy({ it.date }, { it.id }))
            .map { IdentifiedEntry(it.id, it.toEntry()) }

    /**
     * Replaces every entry dated [date] with [entries] in one save. Checklists use this to keep one entry per day.
     * Other days are untouched. Entries must be dated [date], and the date must be valid.
     */
    fun replaceDayEntries(streakId: String, date: LocalDate, entries: List<Entry>, today: LocalDate) {
        val stored = find(streakId)
        val def = stored.toDefinition()
        require(!date.isAfter(today)) { "entries cannot be dated after today" }
        require(!date.isBefore(def.createdOn)) { "entries cannot be dated before the streak was created" }
        entries.forEach { entry ->
            require(entry.date == date) { "entry dated ${entry.date} does not belong to $date" }
            StreakEngine.validate(def, entry)
        }
        val kept = stored.entries.filterNot { it.date == date.toString() }
        val added = entries.map { storedEntryFrom(UUID.randomUUID().toString(), it) }
        val updated = stored.copy(entries = kept + added)
        commit(streaks.map { if (it.id == streakId) updated else it })
    }

    /**
     * Imports a streak with its entries, keeping the given entry IDs so re-import is a no-op.
     * Returns false, and changes nothing, if a streak with the same ID already exists.
     */
    fun importStreak(def: StreakDefinition, order: Int, entries: List<Pair<String, Entry>>): Boolean {
        if (streaks.any { it.id == def.id }) return false
        val stored = def.toStored(order = order, entries = entries.map { (id, entry) -> storedEntryFrom(id, entry) })
        commit(streaks + stored)
        return true
    }

    /** Sets the display order to [orderedIds]. Every streak must be listed exactly once. */
    fun reorder(orderedIds: List<String>) {
        require(orderedIds.size == streaks.size && orderedIds.toSet() == streaks.map { it.id }.toSet()) {
            "reorder must list every streak exactly once"
        }
        val position = orderedIds.withIndex().associate { it.value to it.index }
        commit(streaks.map { it.copy(order = position.getValue(it.id)) })
    }

    private fun find(streakId: String): StoredStreak =
        streaks.firstOrNull { it.id == streakId } ?: throw IllegalArgumentException("unknown streak $streakId")

    private fun commit(next: List<StoredStreak>) {
        val previous = streaks
        streaks = next
        try {
            files.write(StreakCodec.encode(StreakFile(SUPPORTED_SCHEMA_VERSION, next)))
        } catch (failure: IOException) {
            streaks = previous
            throw StreakStorageException("could not save streaks; the change was not kept", failure)
        }
    }
}

private fun StreakDefinition.toStored(order: Int, entries: List<StoredEntry>) = StoredStreak(
    id = id,
    name = name,
    emoji = emoji,
    colorIndex = colorIndex,
    mode = mode.name,
    frequency = frequency.name,
    target = target,
    parts = parts.map { StoredPart(it.name, it.weight) },
    createdOn = createdOn.toString(),
    weekStart = weekStart.name,
    order = order,
    entries = entries,
)

internal fun StoredStreak.toDefinition(): StreakDefinition = StreakDefinition(
    id = id,
    name = name,
    emoji = emoji,
    colorIndex = colorIndex,
    mode = StreakMode.valueOf(mode),
    frequency = Frequency.valueOf(frequency),
    target = target,
    parts = parts.map { ChecklistPart(it.name, it.weight) },
    createdOn = LocalDate.parse(createdOn),
    weekStart = DayOfWeek.valueOf(weekStart),
)

internal fun StoredEntry.toEntry(): Entry = Entry(LocalDate.parse(date), amount, parts.toSet(), context)

private fun storedEntryFrom(id: String, entry: Entry): StoredEntry = StoredEntry(
    id = id,
    date = entry.date.toString(),
    amount = entry.amount,
    parts = entry.parts.sorted(),
    context = entry.context,
)
