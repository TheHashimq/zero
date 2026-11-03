/*
 * MODULE: feature-streaks tests
 * PURPOSE: Verifies persistence, validation, recovery from corrupt and newer files, and rollback on failed saves.
 * TAKES: Temporary directories and streak definitions.
 * GIVES: Pass/fail results for the repository.
 * OWNS: No production state.
 * DOES NOT OWN: Evaluation rules, which have their own tests.
 * DEPENDENCIES: feature-streaks.
 * CONTRACT: Each test asserts one repository rule.
 * FAILURE BEHAVIOR: Assertion failures identify the broken rule.
 */
package zero.feature.streaks.storage

import java.io.File
import java.io.IOException
import java.time.LocalDate
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import zero.feature.streaks.Entry
import zero.feature.streaks.Frequency
import zero.feature.streaks.PeriodStatus
import zero.feature.streaks.StreakDefinition
import zero.feature.streaks.StreakMode

class StreakRepositoryTest {
    private val today = LocalDate.of(2026, 1, 10)
    private val created = LocalDate.of(2026, 1, 1)

    private fun store(dir: File) = DirectoryStreakFileStore(dir)

    private fun reading() = StreakDefinition(
        id = "read",
        name = "Reading",
        emoji = "B",
        colorIndex = 0,
        mode = StreakMode.AMOUNT,
        frequency = Frequency.DAILY,
        target = 10,
        parts = emptyList(),
        createdOn = created,
    )

    /** A streak added to a new repository is listed. */
    @Test
    fun addedStreakIsListed() {
        val repo = StreakRepository.open(store(createTempDirectory("a").toFile()))
        repo.addStreak(reading())

        assertEquals(listOf("read"), repo.definitions().map { it.id })
    }

    /** A second streak with the same ID is rejected. */
    @Test
    fun duplicateStreakIdIsRejected() {
        val repo = StreakRepository.open(store(createTempDirectory("b").toFile()))
        repo.addStreak(reading())

        assertFailsWith<IllegalArgumentException> { repo.addStreak(reading()) }
    }

    /** Two logs on the same day add up in the summary. */
    @Test
    fun twoLogsOnSameDayAddUp() {
        val repo = StreakRepository.open(store(createTempDirectory("c").toFile()))
        repo.addStreak(reading())
        repo.addEntry("read", Entry(today, amount = 5), today)
        repo.addEntry("read", Entry(today, amount = 5), today)

        assertEquals(PeriodStatus.MET, repo.summary("read", today).periods.last().status)
    }

    /** Deleting an entry updates the summary. */
    @Test
    fun deletingEntryUpdatesSummary() {
        val repo = StreakRepository.open(store(createTempDirectory("d").toFile()))
        repo.addStreak(reading())
        val id = repo.addEntry("read", Entry(today.minusDays(1), amount = 10), today)
        repo.deleteEntry("read", id)

        assertEquals(0, repo.summary("read", today).best)
    }

    /** Deleting a streak removes it from disk as well as from memory. */
    @Test
    fun deletingStreakRemovesItFromDisk() {
        val dir = createTempDirectory("e").toFile()
        val repo = StreakRepository.open(store(dir))
        repo.addStreak(reading())
        repo.addEntry("read", Entry(today), today)
        repo.deleteStreak("read")

        assertTrue(StreakRepository.open(store(dir)).definitions().isEmpty())
    }

    /** Data written by one repository is read back by a new one. */
    @Test
    fun dataSurvivesReopen() {
        val dir = createTempDirectory("f").toFile()
        val repo = StreakRepository.open(store(dir))
        repo.addStreak(reading())
        repo.addEntry("read", Entry(today.minusDays(1), amount = 10), today)

        assertEquals(1, StreakRepository.open(store(dir)).summary("read", today).best)
    }

    /** A corrupt file is renamed and the repository starts empty. */
    @Test
    fun corruptFileIsSetAsideAndRepositoryStartsEmpty() {
        val dir = createTempDirectory("g").toFile()
        File(dir, "streaks.json").writeText("{ this is not json")

        val repo = StreakRepository.open(store(dir))

        assertTrue(repo.recoveredFromCorrupt)
        assertTrue(repo.definitions().isEmpty())
        assertTrue(File(dir, "streaks.json.corrupt").exists())
        assertFalse(File(dir, "streaks.json").exists())
    }

    /** A file with a newer schema is refused and left exactly as it was. */
    @Test
    fun newerSchemaIsRefusedAndFileIsUntouched() {
        val dir = createTempDirectory("h").toFile()
        val original = """{"schemaVersion": 99, "streaks": []}"""
        File(dir, "streaks.json").writeText(original)

        assertFailsWith<UnsupportedSchemaException> { StreakRepository.open(store(dir)) }
        assertEquals(original, File(dir, "streaks.json").readText())
        assertFalse(File(dir, "streaks.json.corrupt").exists())
    }

    /** An entry dated after today is rejected. */
    @Test
    fun futureDatedEntryIsRejected() {
        val repo = StreakRepository.open(store(createTempDirectory("i").toFile()))
        repo.addStreak(reading())

        assertFailsWith<IllegalArgumentException> {
            repo.addEntry("read", Entry(today.plusDays(1)), today)
        }
    }

    /** An entry dated before the streak was created is rejected. */
    @Test
    fun entryBeforeCreationIsRejected() {
        val repo = StreakRepository.open(store(createTempDirectory("j").toFile()))
        repo.addStreak(reading())

        assertFailsWith<IllegalArgumentException> {
            repo.addEntry("read", Entry(created.minusDays(1)), today)
        }
    }

    /** Editing a streak's name keeps its entries. */
    @Test
    fun updateKeepsEntries() {
        val repo = StreakRepository.open(store(createTempDirectory("k").toFile()))
        repo.addStreak(reading())
        repo.addEntry("read", Entry(today), today)
        repo.updateStreak(reading().copy(name = "Quran"))

        assertEquals(1, repo.entries("read").size)
        assertEquals("Quran", repo.definitions().single().name)
    }

    /** Changing a streak's mode once it has entries is rejected. */
    @Test
    fun changingModeWithEntriesIsRejected() {
        val repo = StreakRepository.open(store(createTempDirectory("l").toFile()))
        repo.addStreak(reading())
        repo.addEntry("read", Entry(today), today)

        assertFailsWith<IllegalArgumentException> {
            repo.updateStreak(reading().copy(mode = StreakMode.LIMIT))
        }
    }

    /** A failed save throws and leaves the in-memory list unchanged. */
    @Test
    fun failedSaveKeepsMemoryUnchanged() {
        val failing = object : StreakFileStore {
            override fun read(): String? = null
            override fun write(text: String) {
                throw IOException("disk full")
            }
            override fun moveToCorrupt() {}
        }
        val repo = StreakRepository.open(failing)

        assertFailsWith<StreakStorageException> { repo.addStreak(reading()) }
        assertTrue(repo.definitions().isEmpty())
    }

    /** Context text round-trips through storage and reload. */
    @Test
    fun contextRoundTripsThroughStorage() {
        val dir = createTempDirectory("m").toFile()
        val repo = StreakRepository.open(store(dir))
        repo.addStreak(reading())
        repo.addEntry("read", Entry(today, context = "after sunrise"), today)

        assertEquals("after sunrise", StreakRepository.open(store(dir)).entries("read").single().context)
    }

    /** Entries are listed by date, whatever order they were added in. */
    @Test
    fun entriesAreSortedByDate() {
        val repo = StreakRepository.open(store(createTempDirectory("n").toFile()))
        repo.addStreak(reading())
        repo.addEntry("read", Entry(today), today)
        repo.addEntry("read", Entry(today.minusDays(2)), today)

        assertEquals(
            listOf(today.minusDays(2), today),
            repo.entries("read").map { LocalDate.parse(it.date) },
        )
    }
}
