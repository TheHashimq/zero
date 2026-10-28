/*
 * MODULE: foundation-clock
 * PURPOSE: Abstracts the platform wall clock.
 * TAKES: Nothing.
 * GIVES: Epoch milliseconds.
 * OWNS: No state.
 * DOES NOT OWN: Enforcement decisions. Wall time is for display and logs only.
 * DEPENDENCIES: None.
 * CONTRACT: May jump when the user or network changes the system time.
 * FAILURE BEHAVIOR: Implementations should not throw; any throw propagates to the caller.
 */
package zero.foundation.clock

/** Source of epoch milliseconds. Not monotonic. */
fun interface WallClockSource {
    /** Returns the current epoch time in milliseconds. */
    fun epochMillis(): Long
}
