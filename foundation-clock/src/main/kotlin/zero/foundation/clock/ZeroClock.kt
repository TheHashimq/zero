/*
 * MODULE: foundation-clock
 * PURPOSE: Provides time and boot identity to modules.
 * TAKES: Monotonic, wall-clock, and boot-ID sources.
 * GIVES: Monotonic milliseconds, wall-clock milliseconds, and the current boot ID.
 * OWNS: No state.
 * DOES NOT OWN: Persisting boot IDs or deciding what a reboot means. See BootReconciler.
 * DEPENDENCIES: MonotonicSource, WallClockSource, BootIdSource.
 * CONTRACT: monotonicMillis is non-decreasing within one boot session and resets after a reboot.
 * FAILURE BEHAVIOR: Passes through source values unchanged, including null boot IDs.
 */
package zero.foundation.clock

/**
 * Time service for modules.
 *
 * monotonicMillis measures durations within one boot session. It resets after a reboot,
 * so callers must check the boot ID with BootReconciler. wallClockMillis is for display
 * and logs only and must not be used for enforcement.
 */
interface ZeroClock {
    /** Milliseconds since boot. Resets to a smaller value after a reboot. */
    fun monotonicMillis(): Long

    /** Epoch milliseconds. Not safe for measuring durations. */
    fun wallClockMillis(): Long

    /** Current boot identifier, or null if the platform cannot provide one. */
    fun currentBootId(): String?
}

/**
 * Default ZeroClock composed from platform-provided sources.
 *
 * Stateless. Safe to call from any thread if the underlying sources are.
 */
class DefaultZeroClock(
    private val monotonic: MonotonicSource,
    private val wall: WallClockSource,
    private val bootIds: BootIdSource,
) : ZeroClock {
    override fun monotonicMillis(): Long = monotonic.elapsedMillis()

    override fun wallClockMillis(): Long = wall.epochMillis()

    override fun currentBootId(): String? = bootIds.currentBootId()
}
