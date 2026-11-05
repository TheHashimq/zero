/*
 * MODULE: app (streaks home screen)
 * PURPOSE: Shows every streak and routes to add, edit, and details screens.
 * TAKES: The StreakStore from ZeroApplication.
 * GIVES: A Compose list that refreshes when the store changes or the date changes.
 * OWNS: Only the displayed notice.
 * DOES NOT OWN: Streak rules or storage. Those live in feature-streaks.
 * DEPENDENCIES: feature-streaks, foundation-design, Compose Material3, shared components.
 * CONTRACT: Home cards open the streak details; add and edit use the streak form.
 * FAILURE BEHAVIOR: A load error is shown as a message. Nothing crashes if storage is unavailable.
 */
package com.TheHashimq.zero.ui.streaks

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.TheHashimq.zero.platform.streaks.StreakStore
import com.TheHashimq.zero.ui.components.PrimaryButton
import com.TheHashimq.zero.ui.components.SecondaryButton
import com.TheHashimq.zero.ui.components.SectionLabel
import com.TheHashimq.zero.ui.components.StatusLine
import java.io.IOException
import java.time.LocalDate
import zero.feature.streaks.PeriodStatus
import zero.feature.streaks.StreakDefinition
import zero.feature.streaks.StreakPalette
import zero.feature.streaks.presentation.Progress
import zero.feature.streaks.presentation.StreakCard
import zero.feature.streaks.presentation.StreakCards
import zero.feature.streaks.importexport.AgainImporter
import zero.foundation.design.DesignTokens

@Composable
fun StreakHomeScreen(store: StreakStore) {
    var editing by remember { mutableStateOf<StreakDefinition?>(null) }
    var creating by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    var importText by remember { mutableStateOf<String?>(null) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) importText = runImport(context, store, uri)
    }
    importText?.let { text ->
        AlertDialog(
            onDismissRequest = { importText = null },
            title = { Text("Import from Again") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(text)
                }
            },
            confirmButton = { TextButton(onClick = { importText = null }) { Text("OK") } },
        )
    }
    if (creating || editing != null) {
        StreakFormScreen(store, editing) {
            creating = false
            editing = null
        }
        return
    }
    viewing?.let { id ->
        StreakDetailsScreen(
            store = store,
            streakId = id,
            onEdit = {
                editing = store.read { repo -> repo.definitions().firstOrNull { it.id == id } }
                viewing = null
            },
            onBack = { viewing = null },
        )
        return
    }

    val today = store.today()
    val cards = store.read { repo ->
        repo.definitions().map { definition ->
            StreakCards.build(definition, repo.summary(definition.id, today))
        }
    }.orEmpty()
    val loadError = store.loadError

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionLabel("Streaks")
        loadError?.let { StatusLine(it, Color(DesignTokens.WARNING)) }
        if (cards.isEmpty() && loadError == null) {
            StatusLine("No streaks yet.", MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(cards, key = { it.id }) { card -> StreakCardView(card) { viewing = card.id } }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrimaryButton("Add a streak", modifier = Modifier.weight(1f)) { creating = true }
            SecondaryButton(
                "Import from Again",
                modifier = Modifier.weight(1f),
            ) { importLauncher.launch(arrayOf("application/json", "*/*")) }
        }
    }
}

@Composable
private fun StreakCardView(card: StreakCard, onClick: () -> Unit) {
    val accent = Color(StreakPalette.COLORS[card.colorIndex])
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            Text(
                text = "${card.emoji}  ${card.name}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        StatusLine(card.summaryText, MaterialTheme.colorScheme.onSurfaceVariant)
        ProgressView(card.progress, accent)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatusLine("Current ${card.current}", MaterialTheme.colorScheme.onSurface)
            StatusLine("Best ${card.best}", MaterialTheme.colorScheme.onSurfaceVariant)
            StatusLine(statusText(card.status), statusColor(card.status))
        }
    }
}

@Composable
private fun ProgressView(progress: Progress, accent: Color) {
    when (progress) {
        is Progress.Dots -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(progress.total) { index ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (index < progress.filled) accent else MaterialTheme.colorScheme.outline),
                )
            }
        }
        is Progress.Bar -> {
            val fraction = (progress.value.toFloat() / progress.target).coerceIn(0f, 1f)
            val color = if (progress.over) Color(DesignTokens.CRITICAL) else accent
            BarTrack(fraction, color)
        }
        is Progress.Percent -> BarTrack(progress.percent / 100f, accent)
    }
}

@Composable
private fun BarTrack(fraction: Float, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.outline),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(color),
        )
    }
}

private fun statusText(status: PeriodStatus): String = when (status) {
    PeriodStatus.MET -> "Met"
    PeriodStatus.OPEN -> "In progress"
    PeriodStatus.MISSED -> "Missed"
}

private fun statusColor(status: PeriodStatus): Color = when (status) {
    PeriodStatus.MET -> Color(DesignTokens.HEALTHY)
    PeriodStatus.OPEN -> Color(DesignTokens.TEXT_SECONDARY)
    PeriodStatus.MISSED -> Color(DesignTokens.CRITICAL)
}

/** Reads the picked file and imports it. Returns the text to show the user. */
private fun runImport(context: Context, store: StreakStore, uri: Uri): String {
    val text = try {
        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: return "Could not open the file."
    } catch (failure: IOException) {
        return "Could not read the file: ${failure.message}"
    } catch (failure: SecurityException) {
        return "Could not read the file: ${failure.message}"
    }
    val result = store.update { repo -> AgainImporter.importInto(repo, text, LocalDate.now()) }
    return result.fold(
        onSuccess = { AgainImporter.describe(it) },
        onFailure = { "Import failed: ${it.message}" },
    )
}
