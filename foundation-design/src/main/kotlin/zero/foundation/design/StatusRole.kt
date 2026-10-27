/*
 * MODULE: foundation-design
 * PURPOSE: Names the semantic role a status color plays.
 * TAKES: Nothing; an enum.
 * GIVES: HEALTHY, WARNING, or CRITICAL.
 * OWNS: No state.
 * DOES NOT OWN: Mapping from health states. The shell does that.
 * DEPENDENCIES: None.
 * CONTRACT: Each role maps to exactly one token color.
 * FAILURE BEHAVIOR: Not applicable.
 */
package zero.foundation.design

/** Semantic status roles. */
enum class StatusRole { HEALTHY, WARNING, CRITICAL }
