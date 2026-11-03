/*
 * MODULE: feature-streaks
 * PURPOSE: The on-disk format for streaks and their entries, with a schema version and a JSON codec.
 * TAKES: StreakFile values, or JSON text.
 * GIVES: JSON text, or decoded StreakFile values.
 * OWNS: The schema version constant and the JSON settings.
 * DOES NOT OWN: Reading or writing files. See StreakFileStore.
 * DEPENDENCIES: kotlinx-serialization-json.
 * CONTRACT: Unknown JSON fields are ignored. Field names are part of the stored format and must not be renamed.
 * FAILURE BEHAVIOR: decode throws on invalid JSON or a missing schemaVersion.
 */
package zero.feature.streaks.storage

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The schema version this app writes and understands. */
const val SUPPORTED_SCHEMA_VERSION = 1

@Serializable
data class StreakFile(
    val schemaVersion: Int,
    val streaks: List<StoredStreak> = emptyList(),
)

@Serializable
data class StoredStreak(
    val id: String,
    val name: String,
    val emoji: String,
    val colorIndex: Int,
    val mode: String,
    val frequency: String,
    val target: Int,
    val parts: List<StoredPart> = emptyList(),
    val createdOn: String,
    val weekStart: String = "MONDAY",
    val order: Int = 0,
    val entries: List<StoredEntry> = emptyList(),
)

@Serializable
data class StoredPart(val name: String, val weight: Int)

@Serializable
data class StoredEntry(
    val id: String,
    val date: String,
    val amount: Int = 1,
    val parts: List<String> = emptyList(),
    val context: String? = null,
)

/** JSON encoding and decoding for [StreakFile]. */
object StreakCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun encode(file: StreakFile): String = json.encodeToString(StreakFile.serializer(), file)

    fun decode(text: String): StreakFile = json.decodeFromString(StreakFile.serializer(), text)
}
