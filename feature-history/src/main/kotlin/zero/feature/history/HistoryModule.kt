/*
 * MODULE: feature-history
 * PURPOSE: Registers the History navigation entry and reads recent events for the screen.
 * TAKES: ZeroServices during startup.
 * GIVES: A History entry under Activity, and a read-only event source.
 * OWNS: The reference to the services.
 * DOES NOT OWN: Recording events, or the screen itself.
 * DEPENDENCIES: foundation-runtime, foundation-navigation.
 * CONTRACT: The module only reads events. It has no write or delete operation.
 * FAILURE BEHAVIOR: A context that is not ZeroServices fails the module with ModuleException.
 */
package zero.feature.history

import zero.foundation.contract.ModuleContext
import zero.foundation.contract.ModuleException
import zero.foundation.contract.ModuleId
import zero.foundation.contract.ZeroModule
import zero.foundation.events.ZeroEvent
import zero.foundation.navigation.NavCategory
import zero.foundation.navigation.NavEntry
import zero.foundation.runtime.ZeroServices

/** Runtime module for the event history. */
class HistoryModule : ZeroModule {
    override val id: ModuleId = ModuleId("history")
    override val displayName: String = "History"

    @Volatile
    private var services: ZeroServices? = null

    /** Most recent events, newest first. Empty before startup. */
    fun recent(limit: Int): List<ZeroEvent> = services?.events?.recent(limit) ?: emptyList()

    override fun onStart(context: ModuleContext) {
        val zero = context as? ZeroServices
            ?: throw ModuleException("HistoryModule requires ZeroServices")
        services = zero
        zero.navigation.register(NavEntry(id.value, displayName, NavCategory.ACTIVITY))
    }

    override fun onStop() {
        services = null
    }
}
