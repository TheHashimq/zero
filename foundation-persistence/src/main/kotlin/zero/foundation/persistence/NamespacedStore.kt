/*
 * MODULE: foundation-persistence
 * PURPOSE: Gives each module a store whose keys cannot collide with other modules' keys.
 * TAKES: A ModuleId and a KeyValueBackend.
 * GIVES: A ModuleStore that prefixes every key with "module.<id>.".
 * OWNS: The prefix string for one module.
 * DOES NOT OWN: Storage itself or key validation (see StoreKey).
 * DEPENDENCIES: KeyValueBackend, StoreKey, ModuleId.
 * CONTRACT: Keys from different modules never overlap. Module ids contain no dots, so the prefix
 *           boundary is unambiguous even though keys may contain dots.
 * FAILURE BEHAVIOR: Backend failures propagate unchanged as PersistenceException.
 */
package zero.foundation.persistence

import zero.foundation.contract.ModuleId

/** Module-private store that prefixes every backend key with the module id. */
internal class NamespacedStore(
    moduleId: ModuleId,
    private val backend: KeyValueBackend,
) : ModuleStore {
    private val prefix = "module.${moduleId.value}."

    override fun getString(key: StoreKey): String? = backend.read(prefix + key.value)

    override fun putString(key: StoreKey, value: String) {
        backend.write(prefix + key.value, value)
    }

    override fun getLong(key: StoreKey): Long? = getString(key)?.toLongOrNull()

    override fun putLong(key: StoreKey, value: Long) {
        putString(key, value.toString())
    }

    override fun remove(key: StoreKey) {
        backend.delete(prefix + key.value)
    }
}
