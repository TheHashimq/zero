/*
 * MODULE: feature-protection-runtime tests
 * PURPOSE: Verifies add-only semantics, validation, persistence, read filtering, and write failure.
 * TAKES: An in-memory backend wrapped in a module store.
 * GIVES: Pass/fail results for the repository.
 * OWNS: No production state.
 * DOES NOT OWN: The picker or the coordinator.
 * DEPENDENCIES: feature-protection-runtime, foundation-testing.
 * CONTRACT: Each test asserts one rule.
 * FAILURE BEHAVIOR: Assertion failures identify the broken rule.
 */
package zero.feature.protection.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import zero.foundation.contract.ModuleId
import zero.foundation.persistence.ModuleStoreFactory
import zero.foundation.persistence.PersistenceException
import zero.foundation.testing.InMemoryKeyValueBackend

class ProtectedAppRepositoryTest {
    private val own = "com.TheHashimq.zero"
    private val settings = "com.android.settings"
    private val backend = InMemoryKeyValueBackend()
    private val store = ModuleStoreFactory(backend).storeFor(ModuleId("protection"))

    private fun repo() = ProtectedAppRepository(store, own)

    /** A new store has no protected packages. */
    @Test
    fun emptyStoreHasNoPackages() {
        assertEquals(emptySet(), repo().packages())
    }

    /** Adding a valid package stores it. */
    @Test
    fun addValidPackageStoresIt() {
        assertEquals(ProtectedAppRepository.AddResult.Added, repo().add(settings))

        assertEquals(setOf(settings), repo().packages())
    }

    /** Adding the same package twice keeps one entry and reports AlreadyAdded. */
    @Test
    fun addingSamePackageTwiceKeepsOneEntry() {
        val r = repo()
        r.add(settings)

        assertEquals(ProtectedAppRepository.AddResult.AlreadyAdded, r.add(settings))
        assertEquals(1, r.packages().size)
    }

    /** Zero's own package is rejected and nothing is stored. */
    @Test
    fun ownPackageIsRejectedAndNotStored() {
        assertEquals(ProtectedAppRepository.AddResult.OwnPackage, repo().add(own))

        assertEquals(emptySet(), repo().packages())
        assertNull(backend.read("module.protection.packages"))
    }

    /** A malformed name is rejected and nothing is stored. */
    @Test
    fun invalidNameIsRejected() {
        assertEquals(ProtectedAppRepository.AddResult.InvalidName, repo().add("not a package"))

        assertEquals(emptySet(), repo().packages())
    }

    /** A package added by one repository instance is visible to a new instance over the same store. */
    @Test
    fun packagesSurviveNewRepositoryInstance() {
        repo().add(settings)

        assertEquals(setOf(settings), repo().packages())
    }

    /** The repository has no remove, delete, or clear operation. */
    @Test
    fun noRemoveOperationExists() {
        val names = ProtectedAppRepository::class.java.methods.map { it.name.lowercase() }

        assertTrue(names.none { it.contains("remove") || it.contains("delete") || it.contains("clear") })
    }

    /** Malformed entries in the stored list are ignored; valid entries are kept. */
    @Test
    fun malformedStoredEntriesAreIgnored() {
        backend.write("module.protection.packages", "$settings\nnot valid\ncom.example.notes")

        assertEquals(setOf(settings, "com.example.notes"), repo().packages())
    }

    /** A stored entry naming Zero itself is ignored on read. */
    @Test
    fun storedOwnPackageIsIgnoredOnRead() {
        backend.write("module.protection.packages", "$own\n$settings")

        assertEquals(setOf(settings), repo().packages())
    }

    /** A failed write throws and leaves the stored list unchanged. */
    @Test
    fun writeFailureThrowsAndLeavesListUnchanged() {
        val r = repo()
        r.add(settings)
        backend.failWrites = true

        assertFailsWith<PersistenceException> { r.add("com.example.notes") }
        backend.failWrites = false

        assertEquals(setOf(settings), r.packages())
    }
}
