/*
 * MODULE: foundation-persistence tests
 * PURPOSE: Verifies StoreKey validation rules.
 * TAKES: Valid and invalid key strings.
 * GIVES: Pass/fail results for key validation.
 * OWNS: No production state.
 * DOES NOT OWN: Storage behavior.
 * DEPENDENCIES: foundation-persistence, foundation-testing.
 * CONTRACT: Keys are 1 to 64 characters matching the documented pattern.
 * FAILURE BEHAVIOR: Assertion failures identify the wrong validation decision.
 */
package zero.foundation.persistence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Tests StoreKey validation boundaries. */
class StoreKeyTest {
    /** Accepts lowercase keys containing digits, dots, underscores, and hyphens. */
    @Test
    fun acceptsValidKeys() {
        assertEquals("a", StoreKey("a").value)
        assertEquals("count", StoreKey("count").value)
        assertEquals("last.boot-id_2", StoreKey("last.boot-id_2").value)
    }

    /** Rejects empty, uppercase, digit-first, spaced, and slash-containing keys. */
    @Test
    fun rejectsInvalidKeys() {
        listOf("", "Count", "1abc", "has space", "a/b").forEach { bad ->
            assertFailsWith<IllegalArgumentException>("expected '$bad' to be rejected") {
                StoreKey(bad)
            }
        }
    }

    /** Accepts exactly 64 characters and rejects 65. */
    @Test
    fun enforcesLengthBoundary() {
        val longest = "a" + "b".repeat(63)
        val tooLong = "a" + "b".repeat(64)

        assertEquals(64, StoreKey(longest).value.length)
        assertFailsWith<IllegalArgumentException> { StoreKey(tooLong) }
    }
}
