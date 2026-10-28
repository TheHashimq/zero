/*
 * MODULE: foundation-persistence
 * PURPOSE: The only way modules obtain a store.
 * TAKES: A KeyValueBackend and a ModuleId.
 * GIVES: A ModuleStore namespaced to that ModuleId.
 * OWNS: The backend reference.
 * DOES NOT OWN: Module lifecycle or registration.
 * DEPENDENCIES: KeyValueBackend, NamespacedStore, ModuleStore.
 * CONTRACT: A store returned for id X can only read and write keys under X's namespace.
 * FAILURE BEHAVIOR: Never throws. Backend failures surface through the returned store.
 */
package zero.foundation.persistence

import zero.foundation.contract.ModuleId

/** Creates namespaced stores over one shared backend. */
class ModuleStoreFactory(private val backend: KeyValueBackend) {
    /** Returns a store that only accesses keys belonging to [moduleId]. */
    fun storeFor(moduleId: ModuleId): ModuleStore = NamespacedStore(moduleId, backend)
}
