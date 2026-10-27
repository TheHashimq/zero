/*
 * MODULE: foundation-events tests
 * PURPOSE: Verifies EventType validation rules.
 * TAKES: Valid and invalid event type names.
 * GIVES: Pass/fail results for event type validation.
 * OWNS: No production state.
 * DOES NOT OWN: Event payload or log behavior.
 * DEPENDENCIES: foundation-events and Kotlin test/JUnit.
 * CONTRACT: Event types follow the documented lowercase pattern and length.
 * FAILURE BEHAVIOR: Assertion failures identify an incorrect validation decision.
 */
package zero.foundation.events

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Tests EventType validation. */
class EventTypeTest {
    /** Accepts lowercase names with dots and hyphens. */
    @Test
    fun acceptsValidTypes() {
        assertEquals("boot", EventType("boot").value)
        assertEquals("protection.escape-attempt", EventType("protection.escape-attempt").value)
    }

    /** Rejects empty, uppercase, digit-first, spaced, and underscored names. */
    @Test
    fun rejectsInvalidTypes() {
        listOf("", "Boot", "1boot", "has space", "snake_case").forEach { bad ->
            assertFailsWith<IllegalArgumentException>("expected '$bad' to be rejected") {
                EventType(bad)
            }
        }
    }

    /** Accepts 48 characters and rejects 49. */
    @Test
    fun enforcesLengthBoundary() {
        assertEquals(48, EventType("a" + "b".repeat(47)).value.length)
        assertFailsWith<IllegalArgumentException> { EventType("a" + "b".repeat(48)) }
    }
}
