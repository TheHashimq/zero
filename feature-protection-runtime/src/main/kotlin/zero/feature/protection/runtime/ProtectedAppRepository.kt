/*
 * MODULE: feature-protection-runtime
 * PURPOSE: Persistent list of protected packages. Add-only by design.
 * TAKES: A ModuleStore and Zero's own package name.
 * GIVES: The protected set, and the result of each add request.
 * OWNS: The "packages" key in its store, holding one package per line.
 * DOES NOT OWN: Installed-app discovery or the picker UI.
 * DEPENDENCIES: PackageNameRules, foundation-persistence.
 * CONTRACT: There is no remove, delete, or clear operation. Zero's own package is never stored.
 *           Malformed or own-package entries found on read are ignored, not treated as protected.
 * FAILURE BEHAVIOR: A failed write throws PersistenceException and leaves the stored list unchanged.
 */
package zero.feature.protection.runtime

import zero.foundation.persistence.ModuleStore
import zero.foundation.persistence.StoreKey

/** Persistent, add-only list of protected packages. */
class ProtectedAppRepository(
    private val store: ModuleStore,
    private val ownPackage: String,
) {
    /** Result of an add request. */
    enum class AddResult { Added, AlreadyAdded, InvalidName, OwnPackage }

    /** The currently protected packages. */
    fun packages(): Set<String> = decode(store.getString(KEY))

    /** Adds [packageName] if it is valid, not Zero itself, and not already protected. */
    fun add(packageName: String): AddResult {
        if (!PackageNameRules.isValid(packageName)) return AddResult.InvalidName
        if (packageName == ownPackage) return AddResult.OwnPackage
        val current = packages()
        if (packageName in current) return AddResult.AlreadyAdded
        store.putString(KEY, encode(current + packageName))
        return AddResult.Added
    }

    private fun decode(raw: String?): Set<String> {
        if (raw == null) return emptySet()
        return raw.split("\n")
            .filter { PackageNameRules.isValid(it) && it != ownPackage }
            .toSet()
    }

    private fun encode(packages: Set<String>): String = packages.sorted().joinToString("\n")

    private companion object {
        val KEY = StoreKey("packages")
    }
}
