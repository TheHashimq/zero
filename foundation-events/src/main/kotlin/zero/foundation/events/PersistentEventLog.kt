/*
 * MODULE: foundation-events
 * PURPOSE: Stores events in a module store with bounded retention.
 * TAKES: A ModuleStore, a ZeroClock, and a retention capacity.
 * GIVES: EventLog behavior backed by persistence.
 * OWNS: The "next-seq" counter and the "event.<seq>" entries in its store.
 * DOES NOT OWN: The backend or listener delivery.
 * DEPENDENCIES: ModuleStore, ZeroClock, EventCodec.
 * CONTRACT: The event is written before the counter advances; only the newest capacity sequences are retained.
 * FAILURE BEHAVIOR: Write and delete failures throw PersistenceException. Missing or malformed counters mean 0.
 *                   Malformed entries are skipped by recent().
 */
package zero.foundation.events

import zero.foundation.clock.ZeroClock
import zero.foundation.contract.ModuleId
import zero.foundation.persistence.ModuleStore
import zero.foundation.persistence.StoreKey

/** Persistent, bounded, append-only event log. */
class PersistentEventLog(
    private val store: ModuleStore,
    private val clock: ZeroClock,
    private val capacity: Int = DEFAULT_CAPACITY,
) : EventLog {
    init {
        require(capacity in 1..MAX_CAPACITY) { "capacity must be in 1..$MAX_CAPACITY, was $capacity" }
    }

    /** Persists an event, assigns its sequence and timestamps, and applies retention. */
    override fun append(source: ModuleId, type: EventType, payload: Map<String, String>): ZeroEvent {
        val sequence = store.getLong(NEXT_SEQUENCE) ?: 0L
        val event = ZeroEvent(
            sequence = sequence,
            monotonicMillis = clock.monotonicMillis(),
            wallMillis = clock.wallClockMillis(),
            source = source,
            type = type,
            payload = payload.toMap(),
        )
        store.putString(entryKey(sequence), EventCodec.encode(event))
        store.putLong(NEXT_SEQUENCE, sequence + 1)
        if (sequence >= capacity) {
            store.remove(entryKey(sequence - capacity))
        }
        return event
    }

    /** Returns up to [limit] valid events newest first, skipping malformed stored entries. */
    override fun recent(limit: Int): List<ZeroEvent> {
        require(limit >= 0) { "limit must be non-negative, was $limit" }
        val next = store.getLong(NEXT_SEQUENCE) ?: 0L
        val oldest = maxOf(0L, next - capacity)
        val result = mutableListOf<ZeroEvent>()
        var sequence = next - 1
        while (sequence >= oldest && result.size < limit) {
            store.getString(entryKey(sequence))?.let { EventCodec.decode(it) }?.let { result.add(it) }
            sequence--
        }
        return result
    }

    private fun entryKey(sequence: Long): StoreKey = StoreKey("event.$sequence")

    companion object {
        /** Default number of retained events. */
        const val DEFAULT_CAPACITY = 500

        /** Largest allowed retention. */
        const val MAX_CAPACITY = 10_000

        private val NEXT_SEQUENCE = StoreKey("next-seq")
    }
}
