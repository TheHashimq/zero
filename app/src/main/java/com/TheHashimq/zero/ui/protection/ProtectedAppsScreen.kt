/*
 * MODULE: app (protected-apps screen)
 * PURPOSE: Lists protected apps and lets the user add launchable apps. There is no remove control.
 * TAKES: The ProtectionModule from ZeroApplication.
 * GIVES: A Compose screen with a confirmation dialog for each addition.
 * OWNS: Only the displayed list and dialog state.
 * DOES NOT OWN: Protection decisions or storage rules. The repository enforces them.
 * DEPENDENCIES: feature-protection-runtime, Compose Material3, InstalledApps, shared components.
 * CONTRACT: Every addition requires confirmation. Results from the repository are shown as messages.
 * FAILURE BEHAVIOR: Storage errors are shown as messages and never crash the UI.
 */
package com.TheHashimq.zero.ui.protection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.TheHashimq.zero.ui.components.SectionLabel
import com.TheHashimq.zero.ui.components.StatusLine
import java.util.Locale
import zero.feature.protection.runtime.ProtectedAppRepository
import zero.feature.protection.runtime.ProtectionModule
import zero.foundation.persistence.PersistenceException

@Composable
fun ProtectedAppsScreen(module: ProtectionModule) {
    val repository = module.repository
    if (repository == null) {
        StatusLine("Protection is starting", MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val context = LocalContext.current
    val installed = remember(context) { InstalledApps.load(context) }
    val labels = installed.associate { it.packageName to it.label }
    var protectedPackages by remember(repository) { mutableStateOf<Set<String>>(emptySet()) }
    var pending by remember { mutableStateOf<InstalledApp?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(repository) {
        try {
            protectedPackages = repository.packages()
        } catch (failure: PersistenceException) {
            message = "Storage error: ${failure.message}"
        }
    }

    val protectedApps = protectedPackages
        .map { packageName -> packageName to (labels[packageName] ?: packageName) }
        .sortedWith(
            compareBy<Pair<String, String>> { it.second.lowercase(Locale.ROOT) }
                .thenBy { it.first },
        )
    val available = installed.filter { it.packageName !in protectedPackages }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Protected (${protectedPackages.size})")
                if (protectedApps.isEmpty()) {
                    StatusLine("No apps protected yet.", MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(protectedApps, key = { it.first }) { (packageName, label) ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                StatusLine(label, MaterialTheme.colorScheme.onBackground)
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusLine(
                    "Protected apps cannot be removed from Zero.",
                    MaterialTheme.colorScheme.onSurfaceVariant,
                )
                message?.let { StatusLine(it, MaterialTheme.colorScheme.onBackground) }
                SectionLabel("Add an app")
            }
        }
        if (available.isEmpty()) {
            item {
                StatusLine("No more launchable apps are available.", MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(available, key = InstalledApp::packageName) { app ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { pending = app }
                        .padding(vertical = 14.dp),
                ) {
                    StatusLine(app.label, MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }

    pending?.let { app ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Protect ${app.label}?") },
            text = {
                Text(
                    "Once added, ${app.label} cannot be removed from Zero. " +
                        "It will be blocked during every commitment.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        message = when (repository.add(app.packageName)) {
                            ProtectedAppRepository.AddResult.Added -> "Added ${app.label}."
                            ProtectedAppRepository.AddResult.AlreadyAdded -> "${app.label} is already protected."
                            ProtectedAppRepository.AddResult.InvalidName -> "That app's package name is not valid."
                            ProtectedAppRepository.AddResult.OwnPackage -> "Zero cannot protect itself."
                        }
                        protectedPackages = repository.packages()
                    } catch (failure: PersistenceException) {
                        message = "Storage error: ${failure.message}"
                    } finally {
                        pending = null
                    }
                }) { Text("Protect") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel") } },
        )
    }
}
