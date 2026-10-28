/*
 * MODULE: foundation-contract
 * PURPOSE: Marks the services supplied to a module during startup.
 * TAKES: No values in Phase 1.
 * GIVES: An empty startup context interface.
 * OWNS: No state.
 * DOES NOT OWN: Service implementations or context construction.
 * DEPENDENCIES: None.
 * CONTRACT: Registry startup passes the same context instance to each module.
 * FAILURE BEHAVIOR: No operations are defined that can fail in Phase 1.
 */
package zero.foundation.contract

/**
 * Services given to modules at start. Empty in Phase 1.
 * Each later phase adds exactly one service here, with its own test.
 */
interface ModuleContext
