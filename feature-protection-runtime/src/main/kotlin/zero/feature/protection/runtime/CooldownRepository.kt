/*
 * MODULE: feature-protection-runtime
 * PURPOSE: Persists the one-way committed minimum.
 * TAKES: A ModuleStore.
 * GIVES: The current minimum in milliseconds, and the result of each raise request.
 * OWNS: The "minimum-ms" key in its store.
 * DOES NOT OWN: The rules. See CooldownRules.
 * DEPENDENCIES: feature-protection, foundation-persistence.
 * CONTRACT: The stored minimum never goes down. A missing value means the floor.
 *           An unreadable or out-of-range value means the cap, so damage cannot shorten commitments.
 * FAILURE BEHAVIOR: A failed write throws PersistenceException and leaves the stored value unchanged.
 */
package zero.feature.protection.runtime

import zero.feature.protection.CommittedMinimum
import zero.feature.protection.CooldownRules
import zero.feature.protection.RaiseResult
import zero.foundation.persistence.ModuleStore
import zero.foundation.persistence.StoreKey

/** Persistent one-way commitment minimum. */
class CooldownRepository(private val store: ModuleStore) {
    /** The current committed minimum in milliseconds. */
    fun minimumMillis(): Long {
        val raw = store.getString(KEY) ?: return CooldownRules.FLOOR_MILLIS
        val value = raw.toLongOrNull() ?: return CooldownRules.MAX_MILLIS
        return if (value in CooldownRules.FLOOR_MILLIS..CooldownRules.MAX_MILLIS) {
            value
        } else {
            CooldownRules.MAX_MILLIS
        }
    }

    /** Asks to raise the minimum to [requested] milliseconds. Persists only when the result is Raised. */
    fun raise(requested: Long): RaiseResult {
        val result = CooldownRules.raise(CommittedMinimum(minimumMillis()), requested)
        if (result is RaiseResult.Raised) {
            store.putString(KEY, result.minimum.millis.toString())
        }
        return result
    }

    private companion object {
        val KEY = StoreKey("minimum-ms")
    }
}
