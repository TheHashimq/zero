/*
 * MODULE: foundation-events
 * PURPOSE: Delivers events to in-process listeners.
 * TAKES: Subscriptions with optional type filters and published events.
 * GIVES: Delivery to matching active listeners and listener exceptions.
 * OWNS: The subscription list and subscription sequence.
 * DOES NOT OWN: Storage. Publishing does not record anything.
 * DEPENDENCIES: ZeroEvent, EventType.
 * CONTRACT: Listeners run synchronously in subscription order; cancellation during delivery takes effect immediately.
 * FAILURE BEHAVIOR: Listener Exceptions are collected and returned. Errors are not caught.
 */
package zero.foundation.events

/** Synchronous, in-process event delivery. Not thread-safe; call from one thread. */
class EventBus {
    /** Receives published events. */
    fun interface Listener {
        /** Handles [event] synchronously during publish. */
        fun onEvent(event: ZeroEvent)
    }

    /** Handle for removing a subscription. */
    class Subscription internal constructor(private val onCancel: () -> Unit) {
        /** Stops delivery to this subscription. Calling it again does nothing. */
        fun cancel() = onCancel()
    }

    private class Entry(val type: EventType?, val listener: Listener)

    private val entries = LinkedHashMap<Long, Entry>()
    private var nextId = 0L

    /** Subscribes to events of [type], or to all events when [type] is null. */
    fun subscribe(type: EventType?, listener: Listener): Subscription {
        val id = nextId++
        entries[id] = Entry(type, listener)
        return Subscription { entries.remove(id) }
    }

    /** Delivers [event] to matching active listeners and returns their exceptions in delivery order. */
    fun publish(event: ZeroEvent): List<Exception> {
        val failures = mutableListOf<Exception>()
        for ((id, entry) in entries.entries.toList()) {
            if (entries[id] !== entry) continue
            if (entry.type == null || entry.type == event.type) {
                try {
                    entry.listener.onEvent(event)
                } catch (failure: Exception) {
                    failures.add(failure)
                }
            }
        }
        return failures
    }
}
