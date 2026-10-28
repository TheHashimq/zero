/*
 * MODULE: app (installed-app discovery)
 * PURPOSE: Lists launchable apps for the protected-app picker.
 * TAKES: An Android Context.
 * GIVES: Apps sorted by label with one entry per package.
 * OWNS: No state.
 * DOES NOT OWN: Which apps are protected or how they are persisted.
 * DEPENDENCIES: Android PackageManager launcher queries.
 * CONTRACT: Excludes Zero and returns labels only; package names remain internal identifiers.
 * FAILURE BEHAVIOR: PackageManager query failures are surfaced to the caller.
 */
package com.TheHashimq.zero.ui.protection

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import java.util.Locale

/** A launchable app represented by its user-visible label and package identifier. */
data class InstalledApp(val packageName: String, val label: String)

/** Discovers launchable apps visible to this package. */
object InstalledApps {
    fun load(context: Context): List<InstalledApp> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(launcherIntent, 0)
            .asSequence()
            .map { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                InstalledApp(
                    packageName = packageName,
                    label = resolveInfo.loadLabel(packageManager)?.toString()
                        ?.takeIf(String::isNotBlank) ?: packageName,
                )
            }
            .filter { it.packageName != context.packageName }
            .distinctBy(InstalledApp::packageName)
            .sortedWith(
                compareBy<InstalledApp> { it.label.lowercase(Locale.ROOT) }
                    .thenBy(InstalledApp::packageName),
            )
            .toList()
    }
}
