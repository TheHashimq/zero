/*
 * MODULE: foundation-clock tests
 * PURPOSE: Verifies the fake monotonic source's test controls.
 * TAKES: Fake monotonic source calls.
 * GIVES: Pass/fail results for fake behavior.
 * OWNS: No production state.
 * DOES NOT OWN: Clock logic.
 * DEPENDENCIES: foundation-clock and foundation-testing.
 * CONTRACT: advance rejects negative values; setTo allows backward movement.
 * FAILURE BEHAVIOR: Assertion failures identify a broken fake that would invalidate other tests.
 */
package zero.foundation.clock

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import zero.foundation.testing.FakeMonotonicSource

/** Tests controllable behavior used by deterministic clock tests. */
class FakeSourcesTest {
    /** Negative advance is rejected. */
    @Test
    fun advanceRejectsNegativeValues() {
        val mono = FakeMonotonicSource()

        assertFailsWith<IllegalArgumentException> {
            mono.advance(-1L)
        }
    }

    /** setTo can move the value backward so tests can simulate resets. */
    @Test
    fun setToCanMoveBackward() {
        val mono = FakeMonotonicSource()
        mono.setTo(100L)

        mono.setTo(10L)

        assertEquals(10L, mono.elapsedMillis())
    }
}
