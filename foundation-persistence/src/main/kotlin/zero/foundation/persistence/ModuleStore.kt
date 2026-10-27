/*
 * MODULE: foundation-persistence
 * PURPOSE: Typed access to one module's private values.
 * TAKES: StoreKey values and typed payloads.
 * GIVES: Strings and Longs, or null when a value is absent or malformed.
 * OWNS: No state.
 * DOES NOT OWN: Storage location or other modules' values.
 * DEPENDENCIES: StoreKey, PersistenceException.
 * CONTRACT: Reads never throw for missing or malformed values. Writes either complete or throw.
 * FAILURE BEHAVIOR: Malformed stored numbers read as null. Failed writes throw PersistenceException.
 */
package zero.foundation.persistence

/** A single module's private key-value store. */
interface ModuleStore {
    /** Returns the stored string, or null if absent. */
    fun getString(key: StoreKey): String?

    /** Stores a string. Throws PersistenceException if the write fails. */
    fun putString(key: StoreKey, value: String)

    /** Returns the stored Long, or null if absent or not a valid Long. */
    fun getLong(key: StoreKey): Long?

    /** Stores a Long. Throws PersistenceException if the write fails. */
    fun putLong(key: StoreKey, value: Long)

    /** Removes a value. Removing an absent key does nothing. Throws PersistenceException on failure. */
    fun remove(key: StoreKey)
}
