/*
 * MODULE: feature-protection-runtime tests
 * PURPOSE: Verifies the one-way minimum persists, never decreases, and fails closed when damaged.
 * TAKES: An in-memory backend wrapped in a module store.
 * GIVES: Pass/fail results for the repository.
 * OWNS: No production state.
 * DOES NOT OWN: The rules themselves.
 * DEPENDENCIES: feature-protection-runtime, foundation-testing.
 * CONTRACT: Each test asserts one rule.
 * FAILURE BEHAVIOR: Assertion failures identify the broken rule.
 */
package zero.feature.protection.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import zero.feature.protection.CooldownRules
import zero.feature.protection.RaiseResult
import zero.foundation.contract.ModuleId
import zero.foundation.persistence.ModuleStoreFactory
import zero.foundation.testing.InMemoryKeyValueBackend

class CooldownRepositoryTest {
    private val minute = 60_000L
    private val backend = InMemoryKeyValueBackend()
    private val store = ModuleStoreFactory(backend).storeFor(ModuleId("protection"))

    /** An empty store reports the floor. */
    @Test
    fun emptyStoreReportsFloor() {
        assertEquals(CooldownRules.FLOOR_MILLIS, CooldownRepository(store).minimumMillis())
    }

    /** A raise persists and a new repository instance sees it. */
    @Test
    fun raisePersistsAcrossInstances() {
        CooldownRepository(store).raise(120 * minute)

        assertEquals(120 * minute, CooldownRepository(store).minimumMillis())
    }

    /** A decrease is rejected and the stored value is not written. */
    @Test
    fun decreaseIsRejectedAndNotStored() {
        val repo = CooldownRepository(store)
        repo.raise(120 * minute)

        assertEquals(RaiseResult.RejectedDecrease, repo.raise(90 * minute))
        assertEquals(120 * minute, repo.minimumMillis())
    }

    /** A request above 180 minutes is rejected and nothing changes. */
    @Test
    fun aboveMaxIsRejected() {
        val repo = CooldownRepository(store)

        assertEquals(RaiseResult.RejectedAboveMax, repo.raise(181 * minute))
        assertEquals(CooldownRules.FLOOR_MILLIS, repo.minimumMillis())
    }

    /** Requesting the current value stores nothing. */
    @Test
    fun sameValueStoresNothing() {
        val repo = CooldownRepository(store)

        assertEquals(RaiseResult.Unchanged, repo.raise(CooldownRules.FLOOR_MILLIS))
        assertNull(backend.read("module.protection.minimum-ms"))
    }

    /** A damaged stored value reads as the cap, not the floor. */
    @Test
    fun damagedStoredValueFailsClosedAtMax() {
        backend.write("module.protection.minimum-ms", "not-a-number")

        assertEquals(CooldownRules.MAX_MILLIS, CooldownRepository(store).minimumMillis())
    }
}
