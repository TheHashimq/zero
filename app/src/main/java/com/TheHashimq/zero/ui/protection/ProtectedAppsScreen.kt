/*
 * MODULE: app (protected apps screen)
 * PURPOSE: Displays the protected list and adds launchable apps after confirmation.
 * TAKES: The protection module, its repository, and launchable apps from PackageManager.
 * GIVES: A sorted protected-app list and an add-only app picker.
 * OWNS: Picker, confirmation, and feedback UI state only.
 * DOES NOT OWN: Package discovery, persistence, or protection decisions.
 * DEPENDENCIES: feature-protection-runtime, InstalledApps, Compose Material3.
 * CONTRACT: Apps can be added only after confirmation; this screen exposes no removal action.
 * FAILURE BEHAVIOR: Persistence failures are shown in the screen.
 */
package com.TheHashimq.zero.ui.protection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import java.util.Locale
import zero.feature.protection.runtime.ProtectedAppRepository
import zero.feature.protection.runtime.ProtectionModule
import zero.foundation.persistence.PersistenceException

@Composable
fun ProtectedAppsScreen(module: ProtectionModule) {
    val repository = module.repository
    if (repository == null) {
        Text("Protected apps are starting")
        return
    }

    val context = LocalContext.current
    val installedApps = remember(context) { InstalledApps.load(context) }
    var protectedPackages by remember(repository) { mutableStateOf<Set<String>>(emptySet()) }
    var selectedApp by remember { mutableStateOf<InstalledApp?>(null) }
    var pickerOpen by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(repository) {
        try {
            protectedPackages = repository.packages()
        } catch (failure: PersistenceException) {
            message = "Storage error: ${failure.message}"
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Protected apps (${protectedPackages.size})",
            style = MaterialTheme.typography.titleMedium,
        )
        if (protectedPackages.isEmpty()) {
            Text("No apps are protected yet.")
        } else {
            val protectedApps = protectedPackages
                .map { packageName ->
                    packageName to (installedApps.firstOrNull { it.packageName == packageName }?.label ?: packageName)
                }
                .sortedWith(
                    compareBy<Pair<String, String>> { it.second.lowercase(Locale.ROOT) }
                        .thenBy { it.first },
                )
            LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                items(protectedApps, key = { it.first }) { (_, label) -> Text(label) }
            }
        }
        Button(onClick = { pickerOpen = true }) {
            Text("Add protected app")
        }
        message?.let { Text(it) }
    }

    if (pickerOpen) {
        AlertDialog(
            onDismissRequest = { pickerOpen = false },
            title = { Text("Choose an app") },
            text = {
                if (installedApps.isEmpty()) {
                    Text("No launchable apps are available.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(installedApps, key = InstalledApp::packageName) { app ->
                            TextButton(
                                onClick = {
                                    pickerOpen = false
                                    selectedApp = app
                                },
                            ) {
                                Text(app.label)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pickerOpen = false }) {
                    Text("Close")
                }
            },
        )
    }

    selectedApp?.let { app ->
        AlertDialog(
            onDismissRequest = { selectedApp = null },
            title = { Text("Protect ${app.label}?") },
            text = { Text("Zero will send you Home when this app opens during a commitment.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            when (repository.add(app.packageName)) {
                                ProtectedAppRepository.AddResult.Added -> {
                                    protectedPackages = repository.packages()
                                    message = "${app.label} added to protected apps."
                                }
                                ProtectedAppRepository.AddResult.AlreadyAdded ->
                                    message = "${app.label} is already protected."
                                ProtectedAppRepository.AddResult.InvalidName ->
                                    message = "This app has an invalid package name."
                                ProtectedAppRepository.AddResult.OwnPackage ->
                                    message = "Zero cannot protect itself."
                            }
                            selectedApp = null
                        } catch (failure: PersistenceException) {
                            message = "Storage error: ${failure.message}"
                            selectedApp = null
                        }
                    },
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedApp = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}
