/*
 * MODULE: foundation-registry
 * PURPOSE: Represents the outcome of starting registered modules.
 * TAKES: Started module ids and startup failures keyed by module id.
 * GIVES: An immutable startup result.
 * OWNS: No lifecycle state.
 * DOES NOT OWN: Executing callbacks or deciding retry behavior.
 * DEPENDENCIES: foundation-contract ModuleId.
 * CONTRACT: Reports successful starts and failures from one startAll call.
 * FAILURE BEHAVIOR: No operations are defined that can fail.
 */
package zero.foundation.registry

import zero.foundation.contract.ModuleId

/** Result of attempting to start every registered module. */
data class StartReport(
    val started: List<ModuleId>,
    val failed: Map<ModuleId, Throwable>,
)
