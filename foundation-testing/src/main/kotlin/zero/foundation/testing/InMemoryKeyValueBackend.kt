/*
 * MODULE: foundation-testing
 * PURPOSE: In-memory KeyValueBackend for tests, with optional write failures.
 * TAKES: Writes and deletes; failure flags set by tests.
 * GIVES: Stored values, a snapshot of keys, and simulated PersistenceException failures.
 * OWNS: Its map of values.
 * DOES NOT OWN: Namespacing or typed access.
 * DEPENDENCIES: foundation-persistence.
 * CONTRACT: Reads and writes behave like a map. Configured write failures leave the map unchanged.
 * FAILURE BEHAVIOR: Simulated failures throw PersistenceException.
 */
package zero.foundation.testing

import zero.foundation.persistence.KeyValueBackend
import zero.foundation.persistence.PersistenceException

/** Map-backed KeyValueBackend that can simulate write failures. */
class InMemoryKeyValueBackend : KeyValueBackend {
    private val values = mutableMapOf<String, String>()

    /** When true, writes and deletes throw without changing stored values. */
    var failWrites: Boolean = false

    /** When true, the next write throws without changing stored values, then resets to false. */
    var failNextWrite: Boolean = false

    /** Key whose next write throws, then resets to null. */
    var failWriteForKey: String? = null

    /** Snapshot of every key currently stored. */
    val keys: Set<String> get() = values.keys.toSet()

    /** Reads the string stored under [key], or null if absent. */
    override fun read(key: String): String? = values[key]

    /** Writes [value] under [key], or throws when a write failure is configured. */
    override fun write(key: String, value: String) {
        if (failWriteForKey == key) {
            failWriteForKey = null
            throw PersistenceException("simulated write failure for $key")
        }
        if (failNextWrite) {
            failNextWrite = false
            throw PersistenceException("simulated write failure for $key")
        }
        if (failWrites) throw PersistenceException("simulated write failure for $key")
        values[key] = value
    }

    /** Deletes [key], or throws when [failWrites] is true. */
    override fun delete(key: String) {
        if (failWrites) throw PersistenceException("simulated delete failure for $key")
        values.remove(key)
    }
}
