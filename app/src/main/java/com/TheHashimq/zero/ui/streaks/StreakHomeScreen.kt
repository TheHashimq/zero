package com.TheHashimq.zero.ui.streaks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.TheHashimq.zero.platform.streaks.StreakStore
import zero.feature.streaks.StreakDefinition
import zero.feature.streaks.presentation.Progress
import zero.feature.streaks.presentation.StreakCard
import zero.feature.streaks.presentation.StreakCards
import java.time.LocalDate

@Composable
fun StreakHomeScreen(store: StreakStore) {
    var today by remember { mutableStateOf(LocalDate.now()) }
    val repo = store.repository

    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            store.loadError != null -> {
                Text("Unable to load streaks: ${store.loadError}")
            }
            repo == null -> {
                Text("No streaks saved yet.")
            }
            repo.definitions().isEmpty() -> {
                Text("No streaks yet. Add one to start tracking your habits.")
            }
            else -> {
                val first = repo.definitions().first()
                val summary = repo.summary(first.id, today)
                val card = StreakCards.build(first, summary)
                StreakCardView(card)
            }
        }
    }
}

@Composable
private fun StreakCardView(card: StreakCard) {
    Card(
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(card.title, style = MaterialTheme.typography.titleMedium)
            Text(card.summaryText, style = MaterialTheme.typography.bodyMedium)
            when (val progress = card.progress) {
                is Progress.Dots -> Text("${progress.done}/${progress.total}")
                is Progress.Bar -> Text("${progress.value}/${progress.target}")
                is Progress.Percent -> Text("${progress.value}%")
            }
        }
    }
}
