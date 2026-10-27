/*
 * MODULE: foundation-events
 * PURPOSE: Provides the single entry point modules use to record and observe events.
 * TAKES: Record requests from modules and subscriptions.
 * GIVES: Recorded events, listener failures, and recent history.
 * OWNS: Nothing directly; it coordinates EventLog and EventBus.
 * DOES NOT OWN: Storage format or listener logic.
 * DEPENDENCIES: EventLog, EventBus, foundation-contract ModuleId.
 * CONTRACT: An event is recorded before listeners see it; a listener can read it from the log.
 * FAILURE BEHAVIOR: Persistence failures propagate without notifying listeners; listener exceptions are returned.
 */
package zero.foundation.events

import zero.foundation.contract.ModuleId

/** Result of recording an event. */
data class RecordResult(
    /** Event stored in the log. */
    val event: ZeroEvent,
    /** Exceptions thrown by listeners while this event was published. */
    val listenerFailures: List<Exception>,
)

/** Records events, then notifies listeners. */
class EventService(
    private val log: EventLog,
    private val bus: EventBus,
) {
    /** Records an event and publishes it to matching listeners. */
    fun record(
        source: ModuleId,
        type: EventType,
        payload: Map<String, String> = emptyMap(),
    ): RecordResult {
        val event = log.append(source, type, payload)
        val failures = bus.publish(event)
        return RecordResult(event, failures)
    }

    /** Subscribes to events of [type], or to all events when [type] is null. */
    fun subscribe(type: EventType?, listener: EventBus.Listener): EventBus.Subscription =
        bus.subscribe(type, listener)

    /** Returns up to [limit] recent events, newest first. */
    fun recent(limit: Int): List<ZeroEvent> = log.recent(limit)
}
