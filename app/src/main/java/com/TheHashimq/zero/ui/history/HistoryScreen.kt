/*
 * MODULE: app (history screen)
 * PURPOSE: Shows recent protection events, grouped by day, newest first.
 * TAKES: The HistoryModule from ZeroApplication.
 * GIVES: A read-only Compose list.
 * OWNS: Only the loaded rows.
 * DOES NOT OWN: Recording events or any protection decision.
 * DEPENDENCIES: feature-history, Compose Material3, shared components.
 * CONTRACT: Read-only. No control here changes stored data.
 * FAILURE BEHAVIOR: Storage read errors show a message; the screen never crashes.
 */
package com.TheHashimq.zero.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.TheHashimq.zero.ui.components.SectionLabel
import com.TheHashimq.zero.ui.components.StatusLine
import java.util.TimeZone
import zero.feature.history.HistoryDay
import zero.feature.history.HistoryFormatter
import zero.feature.history.HistoryModule
import zero.foundation.persistence.PersistenceException

private const val LOAD_LIMIT = 200

@Composable
fun HistoryScreen(module: HistoryModule) {
    var days by remember(module) { mutableStateOf<List<HistoryDay>?>(null) }
    var message by remember(module) { mutableStateOf<String?>(null) }

    LaunchedEffect(module) {
        try {
            val offset = TimeZone.getDefault().getOffset(System.currentTimeMillis()).toLong()
            days = HistoryFormatter.days(module.recent(LOAD_LIMIT), offset)
        } catch (failure: PersistenceException) {
            message = "Storage error: ${failure.message}"
        }
    }

    val loadedDays = days
    val loadError = message
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            loadError != null -> {
                item {
                    StatusLine(loadError, MaterialTheme.colorScheme.onBackground)
                }
            }
            loadedDays == null -> {
                item {
                    StatusLine("Loading history...", MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            loadedDays.isEmpty() -> {
                item {
                    StatusLine("No events yet.", MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
                items(loadedDays, key = HistoryDay::dateText) { day ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SectionLabel(day.dateText)
                        day.rows.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = row.timeText,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Column {
                                    StatusLine(row.title, MaterialTheme.colorScheme.onBackground)
                                    row.detail?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
