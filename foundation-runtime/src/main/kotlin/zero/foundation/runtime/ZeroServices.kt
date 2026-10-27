/*
 * MODULE: foundation-runtime
 * PURPOSE: Carries the platform-independent services that modules receive at startup.
 * TAKES: A ZeroClock and a ModuleStoreFactory, both constructed by the composition root.
 * GIVES: A ModuleContext exposing clock and stores to registered modules.
 * OWNS: No state. References are held unchanged.
 * DOES NOT OWN: Construction of the clock or stores, and platform adapters.
 * DEPENDENCIES: foundation-contract, foundation-clock, foundation-persistence.
 * CONTRACT: Modules receive the same clock and store factory instances the composition root passed in.
 * FAILURE BEHAVIOR: Never throws. Validation belongs to the services it exposes.
 */
package zero.foundation.runtime

import zero.foundation.clock.ZeroClock
import zero.foundation.contract.ModuleContext
import zero.foundation.persistence.ModuleStoreFactory

/** Services available to every module at startup. */
class ZeroServices(
    /** Time and boot identity. */
    val clock: ZeroClock,
    /** Factory for namespaced per-module stores. */
    val stores: ModuleStoreFactory,
) : ModuleContext
