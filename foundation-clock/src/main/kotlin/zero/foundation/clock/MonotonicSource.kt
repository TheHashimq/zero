/*
 * MODULE: foundation-clock
 * PURPOSE: Abstracts the platform monotonic clock.
 * TAKES: Nothing.
 * GIVES: Milliseconds elapsed since boot.
 * OWNS: No state.
 * DOES NOT OWN: Boot detection, wall-clock time, or caching.
 * DEPENDENCIES: None.
 * CONTRACT: Values are non-negative and non-decreasing within one boot session.
 * FAILURE BEHAVIOR: Implementations should not throw; any throw propagates to the caller.
 */
package zero.foundation.clock

/** Source of monotonic milliseconds since boot. */
fun interface MonotonicSource {
    /** Returns milliseconds elapsed since boot. */
    fun elapsedMillis(): Long
}
