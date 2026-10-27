/*
 * MODULE: foundation-events
 * PURPOSE: Defines an append-only event record interface.
 * TAKES: Source module, event type, and payload for new events.
 * GIVES: The stored event and recent events newest first.
 * OWNS: Nothing directly; implementations own storage.
 * DOES NOT OWN: Delivery to listeners. See EventBus and EventService.
 * DEPENDENCIES: ZeroEvent, EventType, foundation-contract ModuleId.
 * CONTRACT: There is no edit or delete operation; retention is implementation-managed.
 * FAILURE BEHAVIOR: append throws PersistenceException when it cannot store the event.
 */
package zero.foundation.events

import zero.foundation.contract.ModuleId

/** Append-only event record. */
interface EventLog {
    /** Records an event and returns it with its assigned sequence and timestamps. */
    fun append(source: ModuleId, type: EventType, payload: Map<String, String>): ZeroEvent

    /** Returns up to [limit] retained events, newest first. Malformed entries are skipped. */
    fun recent(limit: Int): List<ZeroEvent>
}
