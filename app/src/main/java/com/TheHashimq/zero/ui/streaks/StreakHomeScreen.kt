/*
 * MODULE: app (streaks home screen)
 * PURPOSE: Shows every streak as a clean row: emoji, name, ongoing status, and a round check for today.
 * TAKES: The StreakStore from ZeroApplication, and callbacks for opening a streak and adding one.
 * GIVES: A reorderable list. The check logs or removes today's check-in for check-in streaks; other rows open details.
 * OWNS: The import dialog, the More menu, and the message line.
 * DOES NOT OWN: Streak rules or storage. Those live in feature-streaks.
 * DEPENDENCIES: feature-streaks, foundation-design, Compose Material3, reorderable, shared components.
 * CONTRACT: Every change goes through StreakStore.update. Rows show no progress detail; that lives in the details screen.
 * FAILURE BEHAVIOR: Load and save errors are shown as messages. Nothing crashes if storage is unavailable.
 */
package com.TheHashimq.zero.ui.streaks

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.TheHashimq.zero.platform.streaks.StreakStore
import com.TheHashimq.zero.ui.components.PrimaryButton
import com.TheHashimq.zero.ui.components.SectionLabel
import com.TheHashimq.zero.ui.components.StatusLine
import java.io.IOException
import java.time.LocalDate
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import zero.feature.streaks.Entry
import zero.feature.streaks.PeriodStatus
import zero.feature.streaks.StreakMode
import zero.feature.streaks.StreakPalette
import zero.feature.streaks.importexport.AgainImporter
import zero.feature.streaks.presentation.StreakCard
import zero.feature.streaks.presentation.StreakCards
import zero.foundation.design.DesignTokens

@Composable
fun StreakHomeScreen(
    store: StreakStore,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val context = LocalContext.current
    var importText by remember { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
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

    val today = store.today()
    val cards = store.read { repo ->
        repo.definitions().map { definition ->
            StreakCards.build(definition, repo.summary(definition.id, today))
        }
    }.orEmpty()
    val modes = store.read { repo -> repo.definitions().associate { it.id to it.mode } }.orEmpty()
    val loadError = store.loadError
    val listState = rememberLazyListState()
    var ordered by remember(cards) { mutableStateOf(cards) }
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val moved = ordered.toMutableList().apply { add(to.index, removeAt(from.index)) }
        val result = store.update { repo -> repo.reorder(moved.map { it.id }) }
        if (result.isSuccess) {
            ordered = moved
            message = null
        } else {
            message = result.exceptionOrNull()?.message ?: "Could not save the streak order."
        }
    }

    fun toggleToday(card: StreakCard) {
        if (modes[card.id] != StreakMode.CHECK_IN) {
            onOpen(card.id)
            return
        }
        if (card.status == PeriodStatus.MET) {
            val last = store.read { repo ->
                repo.identifiedEntries(card.id).lastOrNull { it.entry.date == today }
            }
            if (last != null) {
                message = store.update { repo -> repo.deleteEntry(card.id, last.id) }.exceptionOrNull()?.message
            }
        } else {
            message = store.update { repo -> repo.addEntry(card.id, Entry(today), today) }
                .exceptionOrNull()?.message
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) { SectionLabel("Streaks") }
            Box {
                OutlinedButton(onClick = { menuOpen = true }) { Text("More") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Import from Again") },
                        onClick = {
                            menuOpen = false
                            importLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                    )
                }
            }
        }
        loadError?.let { StatusLine(it, Color(DesignTokens.WARNING)) }
        message?.let { StatusLine(it, Color(DesignTokens.CRITICAL)) }
        if (cards.isEmpty() && loadError == null) {
            StatusLine("No streaks yet.", MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(ordered, key = { it.id }) { card ->
                ReorderableItem(reorderState, key = card.id) { isDragging ->
                    StreakRow(
                        card = card,
                        dragging = isDragging,
                        dragHandle = Modifier.longPressDraggableHandle(),
                        onOpen = { onOpen(card.id) },
                        onCheck = { toggleToday(card) },
                    )
                }
            }
        }
        PrimaryButton("Add a streak") { onAdd() }
    }
}

@Composable
private fun StreakRow(
    card: StreakCard,
    dragging: Boolean,
    dragHandle: Modifier,
    onOpen: () -> Unit,
    onCheck: () -> Unit,
) {
    val accent = Color(StreakPalette.COLORS[card.colorIndex])
    val done = card.status == PeriodStatus.MET
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (dragging) 10.dp else 0.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(dragHandle)
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = card.emoji, fontSize = 22.sp)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = card.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = ongoingText(card.current),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        CheckCircle(done = done, accent = accent, onClick = onCheck)
    }
}

@Composable
private fun CheckCircle(done: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (done) accent else Color.Transparent)
            .border(2.dp, if (done) accent else MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            Text(
                text = "✓",
                color = Color(DesignTokens.BACKGROUND),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun ongoingText(current: Int): String = when {
    current <= 0 -> "No ongoing streak"
    current == 1 -> "1 day"
    else -> "$current days"
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
