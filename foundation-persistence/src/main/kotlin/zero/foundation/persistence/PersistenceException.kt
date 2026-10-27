/*
 * MODULE: foundation-persistence
 * PURPOSE: Error raised when a persistence write or delete cannot complete.
 * TAKES: A message and an optional cause.
 * GIVES: A RuntimeException subtype.
 * OWNS: No state.
 * DOES NOT OWN: Retry policy. Callers decide whether to retry.
 * DEPENDENCIES: None.
 * CONTRACT: Thrown only for failed writes or deletes, never for missing keys.
 * FAILURE BEHAVIOR: Is itself the failure report.
 */
package zero.foundation.persistence

/** Raised when a value cannot be durably written or deleted. */
open class PersistenceException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
