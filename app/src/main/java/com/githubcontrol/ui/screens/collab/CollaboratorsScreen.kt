package com.githubcontrol.ui.screens.collab

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.githubcontrol.data.api.GhCollaborator
import com.githubcontrol.data.repository.GitHubRepository
import com.githubcontrol.ui.components.EmbeddedTerminal
import com.githubcontrol.ui.components.GhBadge
import com.githubcontrol.ui.components.GhCard
import com.githubcontrol.utils.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollabState(
    val loading: Boolean = true,
    val list: List<GhCollaborator> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class CollaboratorsViewModel @Inject constructor(private val repo: GitHubRepository) : ViewModel() {
    val state = MutableStateFlow(CollabState())
    private var owner = ""; private var name = ""
    fun load(o: String, n: String) {
        owner = o; name = n
        viewModelScope.launch {
            state.value = state.value.copy(loading = true, error = null)
            try { state.value = CollabState(loading = false, list = repo.collaborators(o, n)) }
            catch (t: Throwable) { state.value = CollabState(loading = false, error = t.message) }
        }
    }
    fun add(login: String, permission: String) { viewModelScope.launch {
        runCatching {
            repo.addCollaborator(owner, name, login.trim(), permission)
            Logger.i("Collaborators", "invited $login as $permission")
        }.onFailure { Logger.e("Collaborators", "invite failed", it) }
        load(owner, name)
    } }
    fun remove(login: String) { viewModelScope.launch {
        runCatching { repo.removeCollaborator(owner, name, login); Logger.w("Collaborators", "removed $login") }
            .onFailure { Logger.e("Collaborators", "remove failed", it) }
        load(owner, name)
    } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollaboratorsScreen(owner: String, name: String, onBack: () -> Unit, vm: CollaboratorsViewModel = hiltViewModel()) {
    LaunchedEffect(owner, name) { vm.load(owner, name) }
    val s by vm.state.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var loginField by remember { mutableStateOf("") }
    var permission by remember { mutableStateOf("push") }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Collaborators") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
            actions = { IconButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, null) } }
        )
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (s.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                items(s.list, key = { it.id }) { c ->
                    GhCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(model = c.avatarUrl, contentDescription = null, modifier = Modifier.size(36.dp).clip(CircleShape))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.login, fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (c.permissions.admin) GhBadge("admin", MaterialTheme.colorScheme.error)
                                    else if (c.permissions.maintain) GhBadge("maintain", MaterialTheme.colorScheme.tertiary)
                                    else if (c.permissions.push) GhBadge("write")
                                    else if (c.permissions.triage) GhBadge("triage")
                                    else if (c.permissions.pull) GhBadge("read")
                                    c.roleName?.let { GhBadge(it) }
                                }
                            }
                            IconButton(onClick = { vm.remove(c.login) }) { Icon(Icons.Filled.PersonRemove, null) }
                        }
                    }
                }
            }
            EmbeddedTerminal(section = "Collaborators")
        }
    }
    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false; loginField = "" },
            title = { Text("Invite collaborator") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(loginField, { loginField = it }, label = { Text("GitHub username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Permission", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("pull", "triage", "push", "maintain", "admin").forEach { p ->
                            FilterChip(permission == p, { permission = p }, label = { Text(p) })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = loginField.isNotBlank(), onClick = {
                    vm.add(loginField, permission); showAdd = false; loginField = ""
                }) { Text("Invite") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}
