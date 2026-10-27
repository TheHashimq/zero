/*
 * MODULE: foundation-clock
 * PURPOSE: Names the outcome of comparing a stored boot ID with the current one.
 * TAKES: Nothing; a value type.
 * GIVES: FIRST_SEEN, SAME_BOOT, REBOOTED, or UNKNOWN.
 * OWNS: No state.
 * DOES NOT OWN: Storage of boot IDs.
 * DEPENDENCIES: None.
 * CONTRACT: Each value has one meaning, documented below.
 * FAILURE BEHAVIOR: Not applicable.
 */
package zero.foundation.clock

/** Result of reconciling a stored boot ID with the current boot ID. */
enum class BootState {
    /** No boot ID was stored before. This is the first observation. */
    FIRST_SEEN,

    /** The stored and current boot IDs are equal. */
    SAME_BOOT,

    /** The IDs differ, so the device rebooted since the stored value was written. */
    REBOOTED,

    /** The current boot ID is unavailable. Monotonic durations must not be trusted. */
    UNKNOWN,
}
