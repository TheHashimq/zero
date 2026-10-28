/*
 * MODULE: feature-protection-runtime
 * PURPOSE: Registers protection with the runtime and exposes its coordinator and protected-app repository.
 * TAKES: Zero's own package name, plus ZeroServices during startup.
 * GIVES: The coordinator, the repository, and two navigation entries.
 * OWNS: The coordinator and repository references.
 * DOES NOT OWN: The accessibility connection or the screens.
 * DEPENDENCIES: foundation-runtime, foundation-navigation, feature-protection-runtime.
 * CONTRACT: Navigation entries are registered last, so a failed start leaves no menu entries.
 * FAILURE BEHAVIOR: A context that is not ZeroServices fails the module with ModuleException.
 *                   A failed boot reconciliation fails the module with its PersistenceException.
 */
package zero.feature.protection.runtime

import zero.foundation.contract.ModuleContext
import zero.foundation.contract.ModuleException
import zero.foundation.contract.ModuleId
import zero.foundation.contract.ZeroModule
import zero.foundation.navigation.NavCategory
import zero.foundation.navigation.NavEntry
import zero.foundation.runtime.ZeroServices

/** Runtime module for protection. */
class ProtectionModule(private val ownPackage: String) : ZeroModule {
    override val id: ModuleId = ModuleId("protection")
    override val displayName: String = "Protection"

    /** Available after a successful start. */
    @Volatile
    var coordinator: ProtectionCoordinator? = null
        private set

    /** Available after a successful start. */
    @Volatile
    var repository: ProtectedAppRepository? = null
        private set

    /** True when the accessibility service is connected. */
    fun isServiceConnected(): Boolean = coordinator?.isServiceConnected() == true

    override fun onStart(context: ModuleContext) {
        val services = context as? ZeroServices
            ?: throw ModuleException("ProtectionModule requires ZeroServices")
        val store = services.stores.storeFor(id)
        val protectedApps = ProtectedAppRepository(store, ownPackage)
        val created = ProtectionCoordinator(services.clock, store, services.events, id, ownPackage) {
            protectedApps.packages()
        }
        created.reconcileBoot()
        repository = protectedApps
        coordinator = created
        services.navigation.register(NavEntry(id.value, displayName, NavCategory.PROTECTION))
        services.navigation.register(
            NavEntry(PROTECTED_APPS_ROUTE, "Protected apps", NavCategory.PROTECTION, order = 1),
        )
    }

    override fun onStop() {
        coordinator?.setServiceConnected(false)
    }

    companion object {
        /** Navigation route for the protected-apps screen. */
        const val PROTECTED_APPS_ROUTE = "protected-apps"
    }
}
