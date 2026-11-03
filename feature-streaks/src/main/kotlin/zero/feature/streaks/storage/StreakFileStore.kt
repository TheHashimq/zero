/*
 * MODULE: feature-streaks
 * PURPOSE: Reads and writes the streak file, with atomic replacement and a corrupt-file move.
 * TAKES: Text to write, or requests to read or set aside the file.
 * GIVES: The file's text, or null when it doesn't exist.
 * OWNS: The file names: streaks.json, streaks.json.tmp, and streaks.json.corrupt.
 * DOES NOT OWN: What the text means. See StreakRepository.
 * DEPENDENCIES: java.io.File only, so it runs on Android API 23.
 * CONTRACT: write replaces the file as a whole, so a reader never sees a half-written file.
 *           moveToCorrupt replaces any previous .corrupt file.
 * FAILURE BEHAVIOR: IO problems throw IOException. A temp file left by a crash is overwritten on the next write.
 */
package zero.feature.streaks.storage

import java.io.File
import java.io.IOException

interface StreakFileStore {
    /** The file's text, or null if it doesn't exist. */
    fun read(): String?

    /** Replaces the file with [text] atomically. */
    fun write(text: String)

    /** Renames the file to streaks.json.corrupt, replacing any earlier one. Does nothing if the file is missing. */
    fun moveToCorrupt()
}

/** A store backed by files in [directory]. */
class DirectoryStreakFileStore(
    directory: File,
    fileName: String = "streaks.json",
) : StreakFileStore {
    private val target = File(directory, fileName)
    private val temp = File(directory, "$fileName.tmp")
    private val corrupt = File(directory, "$fileName.corrupt")
    private val folder = directory

    override fun read(): String? = if (target.exists()) target.readText(Charsets.UTF_8) else null

    override fun write(text: String) {
        folder.mkdirs()
        temp.writeText(text, Charsets.UTF_8)
        replace(temp, target)
    }

    override fun moveToCorrupt() {
        if (!target.exists()) return
        replace(target, corrupt)
    }

    /** Renames [from] to [to]. On systems where rename won't overwrite, deletes [to] first. */
    private fun replace(from: File, to: File) {
        if (from.renameTo(to)) return
        to.delete()
        if (!from.renameTo(to)) throw IOException("could not move ${from.name} to ${to.name}")
    }
}
