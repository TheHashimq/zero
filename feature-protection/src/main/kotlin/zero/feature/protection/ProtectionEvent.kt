/*
 * MODULE: feature-protection
 * PURPOSE: Inputs that can change the protection state.
 * TAKES: Nothing; value types supplied by the caller.
 * GIVES: Start, Tick, ScreenOff, and BootChanged events.
 * OWNS: No state.
 * DOES NOT OWN: Deciding how events are produced. The Android adapter does that in Phase 8b.
 * DEPENDENCIES: None.
 * CONTRACT: nowMillis is monotonic milliseconds.
 * FAILURE BEHAVIOR: Not applicable.
 */
package zero.feature.protection

/** Inputs to the protection state machine. */
sealed interface ProtectionEvent {
    /** Request to begin a commitment. Accepted only from Armed, and only when [bootKnown] is true. */
    data class Start(
        val durationMillis: Long,
        val nowMillis: Long,
        val bootKnown: Boolean,
    ) : ProtectionEvent

    /** Periodic check of the current time. Advances Countdown to Grace, or Grace to Armed. */
    data class Tick(val nowMillis: Long) : ProtectionEvent

    /** Screen turned off. Revokes an active Grace window. */
    data object ScreenOff : ProtectionEvent

    /** The boot ID changed, meaning the device rebooted. */
    data class BootChanged(val nowMillis: Long) : ProtectionEvent
}
