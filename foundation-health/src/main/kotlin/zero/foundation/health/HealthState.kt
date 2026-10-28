/*
 * MODULE: foundation-health
 * PURPOSE: Defines ordered severity for a component or the whole system.
 * TAKES: No inputs.
 * GIVES: OK, DEGRADED, or FAILED health states.
 * OWNS: No state.
 * DOES NOT OWN: Feature-specific behavior for any state.
 * DEPENDENCIES: None.
 * CONTRACT: Declaration order is least to most severe.
 * FAILURE BEHAVIOR: Not applicable.
 */
package zero.foundation.health

/** Severity of a component. Declaration order is least to most severe. */
enum class HealthState { OK, DEGRADED, FAILED }
