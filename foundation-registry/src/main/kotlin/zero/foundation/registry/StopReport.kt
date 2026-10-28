/*
 * MODULE: foundation-registry
 * PURPOSE: Represents the outcome of stopping running modules.
 * TAKES: Stopped module ids and shutdown failures keyed by module id.
 * GIVES: An immutable shutdown result.
 * OWNS: No lifecycle state.
 * DOES NOT OWN: Executing callbacks or deciding retry behavior.
 * DEPENDENCIES: foundation-contract ModuleId.
 * CONTRACT: Reports successful stops and failures from one stopAll call.
 * FAILURE BEHAVIOR: No operations are defined that can fail.
 */
package zero.foundation.registry

import zero.foundation.contract.ModuleId

/** Result of attempting to stop every running module. */
data class StopReport(
    val stopped: List<ModuleId>,
    val failed: Map<ModuleId, Throwable>,
)
