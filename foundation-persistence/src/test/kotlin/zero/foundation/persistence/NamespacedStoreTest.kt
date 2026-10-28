/*
 * MODULE: foundation-persistence tests
 * PURPOSE: Verifies namespacing, typed access, malformed-value handling, and write failures.
 * TAKES: ModuleStoreFactory over an InMemoryKeyValueBackend.
 * GIVES: Pass/fail results for store behavior.
 * OWNS: No production state.
 * DOES NOT OWN: Platform storage behavior.
 * DEPENDENCIES: foundation-persistence, foundation-testing.
 * CONTRACT: Each module's keys are isolated and malformed or failed operations are reported safely.
 * FAILURE BEHAVIOR: Assertion failures identify the broken storage guarantee.
 */
package zero.foundation.persistence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import zero.foundation.contract.ModuleId
import zero.foundation.testing.InMemoryKeyValueBackend

/** Tests module namespace isolation and typed storage behavior. */
class NamespacedStoreTest {
    private val backend = InMemoryKeyValueBackend()
    private val factory = ModuleStoreFactory(backend)
    private val alpha = ModuleId("alpha-one")
    private val beta = ModuleId("beta-two")

    /** A string written through a store reads back unchanged. */
    @Test
    fun stringRoundTrip() {
        factory.storeFor(alpha).putString(StoreKey("name"), "zero")

        assertEquals("zero", factory.storeFor(alpha).getString(StoreKey("name")))
    }

    /** A key that was never written reads as null. */
    @Test
    fun missingKeyReturnsNull() {
        assertNull(factory.storeFor(alpha).getString(StoreKey("absent")))
    }

    /** Two modules writing the same key do not see each other's values. */
    @Test
    fun modulesAreIsolated() {
        factory.storeFor(alpha).putString(StoreKey("count"), "1")
        factory.storeFor(beta).putString(StoreKey("count"), "2")

        assertEquals("1", factory.storeFor(alpha).getString(StoreKey("count")))
        assertEquals("2", factory.storeFor(beta).getString(StoreKey("count")))
    }

    /** Backend keys carry the module prefix exactly. */
    @Test
    fun keysAreNamespacedInBackend() {
        factory.storeFor(alpha).putString(StoreKey("count"), "5")

        assertEquals(setOf("module.alpha-one.count"), backend.keys)
    }

    /** A Long written through a store reads back equal. */
    @Test
    fun longRoundTrip() {
        factory.storeFor(alpha).putLong(StoreKey("deadline"), 42L)

        assertEquals(42L, factory.storeFor(alpha).getLong(StoreKey("deadline")))
    }

    /** A non-numeric value reads as null through getLong and unchanged through getString. */
    @Test
    fun malformedLongReturnsNullWithoutThrowing() {
        backend.write("module.alpha-one.count", "not-a-number")
        val store = factory.storeFor(alpha)

        assertNull(store.getLong(StoreKey("count")))
        assertEquals("not-a-number", store.getString(StoreKey("count")))
    }

    /** Removing one module's key leaves the same key in other modules untouched. */
    @Test
    fun removeOnlyAffectsOwnNamespace() {
        factory.storeFor(alpha).putString(StoreKey("x"), "a-value")
        factory.storeFor(beta).putString(StoreKey("x"), "b-value")

        factory.storeFor(alpha).remove(StoreKey("x"))

        assertNull(factory.storeFor(alpha).getString(StoreKey("x")))
        assertEquals("b-value", factory.storeFor(beta).getString(StoreKey("x")))
    }

    /** A failed write throws and leaves the previous value in place. */
    @Test
    fun failedWriteThrowsAndKeepsPreviousValue() {
        val store = factory.storeFor(alpha)
        store.putString(StoreKey("state"), "old")
        backend.failWrites = true

        assertFailsWith<PersistenceException> {
            store.putString(StoreKey("state"), "new")
        }
        backend.failWrites = false

        assertEquals("old", store.getString(StoreKey("state")))
    }
}
