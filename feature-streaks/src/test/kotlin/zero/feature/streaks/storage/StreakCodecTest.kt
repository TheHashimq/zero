/*
 * MODULE: feature-streaks tests
 * PURPOSE: Verifies the JSON round trip, tolerance of unknown fields, and rejection of invalid input.
 * TAKES: A sample file and malformed text.
 * GIVES: Pass/fail results for the codec.
 * OWNS: No production state.
 * DOES NOT OWN: File access.
 * DEPENDENCIES: feature-streaks.
 * CONTRACT: Each test asserts one codec rule.
 * FAILURE BEHAVIOR: Assertion failures identify the broken rule.
 */
package zero.feature.streaks.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StreakCodecTest {
    private val sample = StreakFile(
        schemaVersion = 1,
        streaks = listOf(
            StoredStreak(
                id = "read",
                name = "Reading",
                emoji = "B",
                colorIndex = 0,
                mode = "AMOUNT",
                frequency = "DAILY",
                target = 10,
                createdOn = "2026-01-01",
                entries = listOf(StoredEntry("e1", "2026-01-05", 10)),
            ),
        ),
    )

    /** Encoding then decoding returns an equal file. */
    @Test
    fun roundTripIsExact() {
        assertEquals(sample, StreakCodec.decode(StreakCodec.encode(sample)))
    }

    /** Fields this version doesn't know are ignored, so newer files can still be read. */
    @Test
    fun unknownFieldsAreIgnored() {
        val text = StreakCodec.encode(sample).replaceFirst("{", "{\"futureField\": 1,")

        assertEquals(sample, StreakCodec.decode(text))
    }

    /** A file without a schemaVersion is rejected. */
    @Test
    fun missingSchemaVersionFails() {
        assertFailsWith<Exception> { StreakCodec.decode("""{"streaks": []}""") }
    }

    /** Text that isn't JSON is rejected. */
    @Test
    fun notJsonFails() {
        assertFailsWith<Exception> { StreakCodec.decode("garbage") }
    }
}
