/*
 * MODULE: foundation-runtime
 * PURPOSE: Carries the platform-independent services that modules receive at startup.
 * TAKES: A ZeroClock, ModuleStoreFactory, EventService, and HealthService built by the composition root.
 * GIVES: A ModuleContext exposing these services to registered modules.
 * OWNS: No state. References are held unchanged.
 * DOES NOT OWN: Construction of the clock or stores, and platform adapters.
 * DEPENDENCIES: foundation-contract, foundation-clock, foundation-persistence, foundation-events, foundation-health.
 * CONTRACT: Modules receive the same service instances the composition root passed in.
 * FAILURE BEHAVIOR: Never throws. Validation belongs to the services it exposes.
 */
package zero.foundation.runtime

import zero.foundation.clock.ZeroClock
import zero.foundation.contract.ModuleContext
import zero.foundation.events.EventService
import zero.foundation.health.HealthService
import zero.foundation.persistence.ModuleStoreFactory

/** Services available to every module at startup. */
class ZeroServices(
    /** Time and boot identity. */
    val clock: ZeroClock,
    /** Factory for namespaced per-module stores. */
    val stores: ModuleStoreFactory,
    /** Event recording and subscription. */
    val events: EventService,
    /** Aggregated component health. */
    val health: HealthService,
) : ModuleContext
