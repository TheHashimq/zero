/*
 * MODULE: foundation-testing
 * PURPOSE: In-memory KeyValueBackend for tests, with optional write failures.
 * TAKES: Writes and deletes; a failWrites flag set by tests.
 * GIVES: Stored values, a snapshot of keys, and simulated PersistenceException failures.
 * OWNS: Its map of values.
 * DOES NOT OWN: Namespacing or typed access.
 * DEPENDENCIES: foundation-persistence.
 * CONTRACT: Reads and writes behave like a map. When failWrites is true, writes and deletes throw
 *           and leave the map unchanged.
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

    /** Snapshot of every key currently stored. */
    val keys: Set<String> get() = values.keys.toSet()

    /** Reads the string stored under [key], or null if absent. */
    override fun read(key: String): String? = values[key]

    /** Writes [value] under [key], or throws when [failWrites] is true. */
    override fun write(key: String, value: String) {
        if (failWrites) throw PersistenceException("simulated write failure for $key")
        values[key] = value
    }

    /** Deletes [key], or throws when [failWrites] is true. */
    override fun delete(key: String) {
        if (failWrites) throw PersistenceException("simulated delete failure for $key")
        values.remove(key)
    }
}
