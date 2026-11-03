/*
 * MODULE: app (platform adapter for streaks)
 * PURPOSE: Opens the streak repository from the app's private storage and gives screens a way to read and change it.
 * TAKES: The app's files directory.
 * GIVES: Read access for screens, a guarded update function, and a revision counter that makes screens refresh.
 * OWNS: The repository reference, the load error message, and the revision counter.
 * DOES NOT OWN: Streak rules or storage format, which live in feature-streaks.
 * DEPENDENCIES: feature-streaks, Compose runtime.
 * CONTRACT: Opening never throws. If the file can't be used, repository is null and loadError explains why.
 *           Every change refreshes screens, including failed changes, so the UI shows the stored state.
 * FAILURE BEHAVIOR: A newer-version file or a storage error sets loadError. Update returns a failed Result.
 */
package com.TheHashimq.zero.platform.streaks

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.time.LocalDate
import zero.feature.streaks.storage.DirectoryStreakFileStore
import zero.feature.streaks.storage.StreakRepository
import zero.feature.streaks.storage.StreakStorageException
import zero.feature.streaks.storage.UnsupportedSchemaException

class StreakStore(directory: File) {
    val repository: StreakRepository?
    val loadError: String?

    private var revisionValue by mutableIntStateOf(0)

    init {
        var opened: StreakRepository? = null
        var error: String? = null
        try {
            opened = StreakRepository.open(DirectoryStreakFileStore(directory))
        } catch (failure: UnsupportedSchemaException) {
            error = "These streaks were saved by a newer version of Zero. Update the app to see them."
        } catch (failure: StreakStorageException) {
            error = "Streaks could not be loaded: ${failure.message}"
        }
        repository = opened
        loadError = error
    }

    /** Reads from the repository. Screens that call this recompose when the store changes. */
    fun <T> read(block: (StreakRepository) -> T): T? {
        val current = revisionValue
        return repository?.let { if (current >= 0) block(it) else null }
    }

    /** Changes the repository and refreshes every screen that reads it. */
    fun <T> update(block: (StreakRepository) -> T): Result<T> {
        val repo = repository ?: return Result.failure(IllegalStateException(loadError ?: "Streaks unavailable"))
        val result = runCatching { block(repo) }
        revisionValue++
        return result
    }

    fun today(): LocalDate = LocalDate.now()
}
