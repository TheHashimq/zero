/*
 * MODULE: foundation-contract
 * PURPOSE: Defines the lifecycle callbacks implemented by a Zero module.
 * TAKES: A stable id, display name, startup context, and lifecycle calls.
 * GIVES: A module contract for registry-managed implementations.
 * OWNS: No mutable state.
 * DOES NOT OWN: Lifecycle ordering, status tracking, or dependency lookup.
 * DEPENDENCIES: ModuleId and ModuleContext from this module.
 * CONTRACT: Modules expose identity and implement start and stop callbacks.
 * FAILURE BEHAVIOR: Lifecycle callback failures are propagated to the caller.
 */
package zero.foundation.contract

/** Lifecycle contract for a module managed by the registry. */
interface ZeroModule {
    /** Stable, unique identifier. Used by the registry for duplicate detection. */
    val id: ModuleId

    /** Human-readable name. Must not be blank. */
    val displayName: String

    /**
     * Called once by the registry at startup.
     * May throw. A throw marks this module FAILED and does not stop other modules.
     */
    fun onStart(context: ModuleContext)

    /**
     * Called once by the registry at shutdown, in reverse registration order.
     * May throw. A throw is recorded and does not stop other modules from stopping.
     */
    fun onStop()
}
