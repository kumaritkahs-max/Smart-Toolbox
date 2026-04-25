package com.githubcontrol.ui.screens.keys

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.githubcontrol.data.api.GhSshKey
import com.githubcontrol.data.repository.GitHubRepository
import com.githubcontrol.ui.components.EmbeddedTerminal
import com.githubcontrol.ui.components.GhBadge
import com.githubcontrol.ui.components.GhCard
import com.githubcontrol.utils.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SshKeysState(
    val loading: Boolean = true,
    val keys: List<GhSshKey> = emptyList(),
    val error: String? = null,
    val saving: Boolean = false
)

@HiltViewModel
class SshKeysViewModel @Inject constructor(private val repo: GitHubRepository) : ViewModel() {
    val state = MutableStateFlow(SshKeysState())
    init { reload() }
    fun reload() { viewModelScope.launch {
        state.value = state.value.copy(loading = true, error = null)
        try { state.value = SshKeysState(loading = false, keys = repo.sshKeys()) }
        catch (t: Throwable) { state.value = SshKeysState(loading = false, error = t.message) }
    } }
    fun add(title: String, body: String, onDone: () -> Unit) { viewModelScope.launch {
        state.value = state.value.copy(saving = true, error = null)
        try {
            repo.addSshKey(title.trim(), body.trim())
            Logger.i("SshKeys", "added key '${title.trim()}'")
            reload(); onDone()
        } catch (t: Throwable) {
            Logger.e("SshKeys", "add failed", t)
            state.value = state.value.copy(saving = false, error = t.message)
        }
    } }
    fun delete(id: Long) { viewModelScope.launch {
        runCatching { repo.deleteSshKey(id); Logger.i("SshKeys", "deleted key #$id") }
            .onFailure { Logger.e("SshKeys", "delete failed", it) }
        reload()
    } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SshKeysScreen(onBack: () -> Unit, vm: SshKeysViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var titleField by remember { mutableStateOf("") }
    var keyField by remember { mutableStateOf("") }

    Scaffold(topBar = {
        TopAppBar(title = { Text("SSH keys") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
            actions = { IconButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, null) } })
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (s.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (!s.loading && s.keys.isEmpty()) Text("No SSH keys on this account.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(s.keys, key = { it.id }) { k ->
                    GhCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Key, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(k.title, fontWeight = FontWeight.SemiBold)
                                Text("added ${k.createdAt}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    k.key.take(48) + if (k.key.length > 48) "…" else "",
                                    fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (k.verified) GhBadge("verified", MaterialTheme.colorScheme.primary)
                                    if (k.readOnly) GhBadge("read-only")
                                }
                            }
                            IconButton(onClick = { vm.delete(k.id) }) { Icon(Icons.Filled.Delete, null) }
                        }
                    }
                }
            }
            EmbeddedTerminal(section = "SshKeys")
        }
    }
    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false; titleField = ""; keyField = "" },
            title = { Text("Add SSH key") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(titleField, { titleField = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        keyField, { keyField = it },
                        label = { Text("Public key (ssh-rsa AAAAB3…)") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = titleField.isNotBlank() && keyField.isNotBlank() && !s.saving,
                    onClick = { vm.add(titleField, keyField) { showAdd = false; titleField = ""; keyField = "" } }
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}
