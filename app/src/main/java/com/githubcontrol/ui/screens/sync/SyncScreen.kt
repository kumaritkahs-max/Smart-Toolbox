package com.githubcontrol.ui.screens.sync

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.githubcontrol.data.db.AppDatabase
import com.githubcontrol.ui.components.EmbeddedTerminal
import com.githubcontrol.ui.components.GhCard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.collect

@HiltViewModel
class SyncViewModel @Inject constructor(val db: AppDatabase) : androidx.lifecycle.ViewModel()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(onBack: () -> Unit, vm: SyncViewModel = hiltViewModel()) {
    val items by vm.db.syncJobs().observeAll().collectAsState(initial = emptyList())
    Scaffold(topBar = {
        TopAppBar(title = { Text("Sync jobs") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } })
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (items.isEmpty()) {
                Text("No sync jobs yet.", modifier = Modifier.padding(8.dp))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    items(items, key = { it.id }) { j ->
                        GhCard {
                            Text("${j.owner}/${j.repo}", style = MaterialTheme.typography.titleMedium)
                            Text("Local: ${j.localUri}", style = MaterialTheme.typography.bodySmall)
                            Text("Branch: ${j.branch} • every ${j.intervalMinutes} min", style = MaterialTheme.typography.labelSmall)
                            Switch(checked = j.enabled, onCheckedChange = { /* TODO toggle via DAO */ })
                        }
                    }
                }
            }
            EmbeddedTerminal(section = "Sync", initiallyExpanded = true)
        }
    }
}
