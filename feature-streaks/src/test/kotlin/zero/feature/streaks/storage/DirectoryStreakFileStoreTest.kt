/*
 * MODULE: feature-streaks tests
 * PURPOSE: Verifies file reads, writes, replacement, the corrupt-file move, and temp-file cleanup.
 * TAKES: Temporary directories.
 * GIVES: Pass/fail results for the file store.
 * OWNS: No production state.
 * DOES NOT OWN: JSON content.
 * DEPENDENCIES: feature-streaks.
 * CONTRACT: Each test asserts one file rule.
 * FAILURE BEHAVIOR: Assertion failures identify the broken rule.
 */
package zero.feature.streaks.storage

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class DirectoryStreakFileStoreTest {
    private fun store(): DirectoryStreakFileStore = DirectoryStreakFileStore(createTempDirectory("store").toFile())

    /** A missing file reads as null. */
    @Test
    fun missingFileReadsNull() {
        assertNull(store().read())
    }

    /** Written text reads back unchanged. */
    @Test
    fun writeThenRead() {
        val s = store()
        s.write("abc")

        assertEquals("abc", s.read())
    }

    /** A second write replaces the first. */
    @Test
    fun writeReplacesExisting() {
        val s = store()
        s.write("one")
        s.write("two")

        assertEquals("two", s.read())
    }

    /** Moving to corrupt removes the active file. */
    @Test
    fun moveToCorruptRemovesActiveFile() {
        val s = store()
        s.write("bad")
        s.moveToCorrupt()

        assertNull(s.read())
    }

    /** A completed write leaves no temp file behind. */
    @Test
    fun writeLeavesNoTempFile() {
        val dir = createTempDirectory("tmp").toFile()
        DirectoryStreakFileStore(dir).write("x")

        assertFalse(File(dir, "streaks.json.tmp").exists())
    }
}
