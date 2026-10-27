/*
 * MODULE: foundation-events
 * PURPOSE: Represents one immutable recorded event.
 * TAKES: Sequence, timestamps, source module, type, and string payload.
 * GIVES: An immutable event value.
 * OWNS: No mutable state.
 * DOES NOT OWN: Storage or delivery.
 * DEPENDENCIES: foundation-contract ModuleId and EventType.
 * CONTRACT: Sequence numbers start at 0 and increase by one per recorded event.
 * FAILURE BEHAVIOR: Not applicable; a value type.
 */
package zero.foundation.events

import zero.foundation.contract.ModuleId

/** A single recorded event. */
data class ZeroEvent(
    /** Position in the log. Starts at 0 and increases by 1 per event. */
    val sequence: Long,
    /** Monotonic milliseconds at recording. Only comparable within one boot session. */
    val monotonicMillis: Long,
    /** Wall-clock milliseconds at recording. For display only. */
    val wallMillis: Long,
    /** Module that recorded the event. */
    val source: ModuleId,
    /** Kind of event. */
    val type: EventType,
    /** Small string key-value details. */
    val payload: Map<String, String>,
)
