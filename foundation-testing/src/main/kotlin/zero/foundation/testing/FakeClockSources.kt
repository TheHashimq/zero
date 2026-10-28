/*
 * MODULE: foundation-testing
 * PURPOSE: Provides deterministic fake clock sources for tests.
 * TAKES: Initial values and explicit test-driven changes.
 * GIVES: Controllable monotonic, wall-clock, and boot-ID sources.
 * OWNS: Each fake's current value.
 * DOES NOT OWN: Clock behavior or reboot reconciliation.
 * DEPENDENCIES: foundation-clock.
 * CONTRACT: Values change only through the methods below.
 * FAILURE BEHAVIOR: advance rejects negative amounts with IllegalArgumentException.
 */
package zero.foundation.testing

import zero.foundation.clock.BootIdSource
import zero.foundation.clock.MonotonicSource
import zero.foundation.clock.WallClockSource

/** Monotonic source controlled by the test. */
class FakeMonotonicSource(private var now: Long = 0L) : MonotonicSource {
    override fun elapsedMillis(): Long = now

    /** Moves time forward by [millis]. Rejects negative values. */
    fun advance(millis: Long) {
        require(millis >= 0) { "advance must be non-negative, was $millis" }
        now += millis
    }

    /** Sets the value directly. Allows simulating a reboot or a backward jump. */
    fun setTo(millis: Long) {
        now = millis
    }
}

/** Wall-clock source controlled by the test. */
class FakeWallClockSource(var now: Long = 0L) : WallClockSource {
    override fun epochMillis(): Long = now
}

/** Boot-ID source controlled by the test. Set [bootId] to null to simulate unavailability. */
class FakeBootIdSource(var bootId: String? = "boot-1") : BootIdSource {
    override fun currentBootId(): String? = bootId
}
