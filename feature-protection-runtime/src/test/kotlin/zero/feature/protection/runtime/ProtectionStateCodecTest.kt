/*
 * MODULE: feature-protection-runtime tests
 * PURPOSE: Verifies the state codec round trip and rejects malformed input.
 * TAKES: Each protection state and malformed strings.
 * GIVES: Pass/fail results for the codec.
 * OWNS: No production state.
 * DOES NOT OWN: Coordinator behavior.
 * DEPENDENCIES: feature-protection-runtime, feature-protection.
 * CONTRACT: Round trips are exact; malformed strings decode to null.
 * FAILURE BEHAVIOR: Assertion failures identify the broken format rule.
 */
package zero.feature.protection.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import zero.feature.protection.ProtectionState

class ProtectionStateCodecTest {
    /** Armed round-trips. */
    @Test
    fun roundTripsArmed() {
        val s = ProtectionState.Armed
        assertEquals(s, ProtectionStateCodec.decode(ProtectionStateCodec.encode(s)))
    }

    /** Countdown round-trips with its start and duration. */
    @Test
    fun roundTripsCountdown() {
        val s = ProtectionState.Countdown(1_000L, 120_000L)
        assertEquals(s, ProtectionStateCodec.decode(ProtectionStateCodec.encode(s)))
    }

    /** Grace round-trips with its start. */
    @Test
    fun roundTripsGrace() {
        val s = ProtectionState.Grace(5_000L)
        assertEquals(s, ProtectionStateCodec.decode(ProtectionStateCodec.encode(s)))
    }

    /** Malformed strings decode to null. */
    @Test
    fun rejectsMalformedValues() {
        listOf("", "BOGUS", "COUNTDOWN|x|1", "COUNTDOWN|1|0", "GRACE|1|2").forEach { bad ->
            assertNull(ProtectionStateCodec.decode(bad), "expected '$bad' to be rejected")
        }
    }
}
