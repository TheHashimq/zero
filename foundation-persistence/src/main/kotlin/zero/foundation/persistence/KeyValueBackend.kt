/*
 * MODULE: foundation-persistence
 * PURPOSE: Defines the raw string storage supplied by the platform.
 * TAKES: Fully namespaced string keys and string values.
 * GIVES: Stored strings, or a PersistenceException when a write cannot be made durable.
 * OWNS: No state. Implementations own their storage.
 * DOES NOT OWN: Namespacing, validation, or typed values.
 * DEPENDENCIES: PersistenceException.
 * CONTRACT: A successful write is durable before the call returns. Deleting an absent key is a no-op.
 * FAILURE BEHAVIOR: Failed writes or deletes throw PersistenceException and leave prior values intact.
 */
package zero.foundation.persistence

/** Raw string storage supplied by the platform. Callers pass already-namespaced keys. */
interface KeyValueBackend {
    /** Returns the stored string, or null if [key] is absent. */
    fun read(key: String): String?

    /** Stores [value] under [key]. Throws PersistenceException if the write fails. */
    fun write(key: String, value: String)

    /** Removes [key]. Removing an absent key does nothing. Throws PersistenceException on failure. */
    fun delete(key: String)
}
