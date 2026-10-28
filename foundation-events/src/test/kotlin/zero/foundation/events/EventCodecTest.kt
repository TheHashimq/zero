/*
 * MODULE: foundation-events tests
 * PURPOSE: Verifies EventCodec round trips and malformed-input handling.
 * TAKES: ZeroEvent values and malformed serialized strings.
 * GIVES: Pass/fail results for encoding and decoding.
 * OWNS: No production state.
 * DOES NOT OWN: Persistence or event delivery.
 * DEPENDENCIES: foundation-events and Kotlin test/JUnit.
 * CONTRACT: Valid events round-trip, and malformed strings decode to null.
 * FAILURE BEHAVIOR: Assertion failures identify an encoding or decoding regression.
 */
package zero.foundation.events

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import zero.foundation.contract.ModuleId

/** Tests the private persistent representation through the module test friend path. */
class EventCodecTest {
    private val sample = ZeroEvent(
        sequence = 7L,
        monotonicMillis = 1_000L,
        wallMillis = 2_000L,
        source = ModuleId("alpha-one"),
        type = EventType("sample.kind"),
        payload = linkedMapOf("key|one" to "a,b=c%d", "plain" to "value"),
    )

    /** Encoding then decoding returns an equal event, including separators and percent signs. */
    @Test
    fun roundTripsSpecialCharacters() {
        assertEquals(sample, EventCodec.decode(EventCodec.encode(sample)))
    }

    /** An event with no payload round-trips. */
    @Test
    fun roundTripsEmptyPayload() {
        val bare = sample.copy(payload = emptyMap())

        assertEquals(bare, EventCodec.decode(EventCodec.encode(bare)))
    }

    /** Wrong version, wrong field count, and non-numeric fields decode to null. */
    @Test
    fun malformedInputDecodesToNull() {
        val good = EventCodec.encode(sample)
        assertNull(EventCodec.decode(good.replaceFirst("v1|", "v9|")))
        assertNull(EventCodec.decode("v1|only|three"))
        assertNull(EventCodec.decode(good.replaceFirst("|7|", "|seven|")))
    }
}
