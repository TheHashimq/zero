/*
 * MODULE: feature-protection-runtime
 * PURPOSE: Registers protection with the runtime and exposes its coordinator to the app.
 * TAKES: The own package and the protected package set, plus ZeroServices during startup.
 * GIVES: A "Protection" navigation entry, a ProtectionCoordinator, and the service-connected status.
 * OWNS: The coordinator reference.
 * DOES NOT OWN: The accessibility connection or the screen.
 * DEPENDENCIES: foundation-runtime, foundation-contract, foundation-navigation, feature-protection-runtime.
 * CONTRACT: After a successful start, coordinator is non-null and the navigation entry is registered.
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
class ProtectionModule(
    private val ownPackage: String,
    private val protectedPackages: Set<String>,
) : ZeroModule {
    override val id: ModuleId = ModuleId("protection")
    override val displayName: String = "Protection"

    /** Available after a successful start. */
    @Volatile
    var coordinator: ProtectionCoordinator? = null
        private set

    /** True when the accessibility service is connected. */
    fun isServiceConnected(): Boolean = coordinator?.isServiceConnected() == true

    override fun onStart(context: ModuleContext) {
        val services = context as? ZeroServices
            ?: throw ModuleException("ProtectionModule requires ZeroServices")
        services.navigation.register(NavEntry(id.value, displayName, NavCategory.PROTECTION))
        val created = ProtectionCoordinator(
            services.clock,
            services.stores.storeFor(id),
            services.events,
            id,
            ownPackage,
            protectedPackages,
        )
        created.reconcileBoot()
        coordinator = created
    }

    override fun onStop() {
        coordinator?.setServiceConnected(false)
    }
}
