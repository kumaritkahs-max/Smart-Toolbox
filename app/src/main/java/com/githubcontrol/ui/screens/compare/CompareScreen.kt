package com.githubcontrol.ui.screens.compare

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.SwapHoriz
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
import com.githubcontrol.data.api.GhCommitCompare
import com.githubcontrol.data.repository.GitHubRepository
import com.githubcontrol.ui.components.EmbeddedTerminal
import com.githubcontrol.ui.components.GhBadge
import com.githubcontrol.ui.components.GhCard
import com.githubcontrol.utils.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompareState(
    val base: String = "",
    val head: String = "",
    val branches: List<String> = emptyList(),
    val loading: Boolean = false,
    val result: GhCommitCompare? = null,
    val error: String? = null
)

@HiltViewModel
class CompareViewModel @Inject constructor(private val repo: GitHubRepository) : ViewModel() {
    val state = MutableStateFlow(CompareState())
    private var owner = ""; private var name = ""
    fun init(o: String, n: String, base: String, head: String) {
        owner = o; name = n
        state.value = state.value.copy(base = base, head = head)
        viewModelScope.launch {
            runCatching {
                val br = repo.branches(o, n).map { it.name }
                state.value = state.value.copy(
                    branches = br,
                    base = state.value.base.ifBlank { runCatching { repo.api.repo(o, n).defaultBranch }.getOrNull() ?: br.firstOrNull().orEmpty() },
                    head = state.value.head
                )
            }
        }
    }
    fun setBase(v: String) { state.value = state.value.copy(base = v) }
    fun setHead(v: String) { state.value = state.value.copy(head = v) }
    fun swap() { state.value = state.value.copy(base = state.value.head, head = state.value.base, result = null) }
    fun run() {
        val s = state.value
        if (s.base.isBlank() || s.head.isBlank()) { state.value = s.copy(error = "Pick base and head"); return }
        viewModelScope.launch {
            state.value = s.copy(loading = true, error = null, result = null)
            try {
                val res = repo.api.compare(owner, name, s.base, s.head)
                Logger.i("Compare", "${s.base}...${s.head}: ${res.aheadBy} ahead / ${res.behindBy} behind, ${res.files.size} files")
                state.value = state.value.copy(loading = false, result = res)
            } catch (t: Throwable) {
                Logger.e("Compare", "compare failed", t)
                state.value = state.value.copy(loading = false, error = t.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    owner: String, name: String, base: String, head: String,
    onBack: () -> Unit, vm: CompareViewModel = hiltViewModel()
) {
    LaunchedEffect(owner, name) { vm.init(owner, name, base, head) }
    val s by vm.state.collectAsState()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Compare · $name") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
            actions = { IconButton(onClick = { vm.swap() }) { Icon(Icons.Filled.SwapHoriz, null) } }
        )
    }) { pad ->
        Column(Modifier.padding(pad).padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GhCard {
                Text("Pick refs", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(s.base, { vm.setBase(it) }, label = { Text("base") }, singleLine = true, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.CompareArrows, null)
                    OutlinedTextField(s.head, { vm.setHead(it) }, label = { Text("head") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                if (s.branches.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Available: ${s.branches.joinToString(", ").take(120)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
                Button(onClick = { vm.run() }, enabled = !s.loading) {
                    if (s.loading) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    else Text("Compare")
                }
            }
            s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            s.result?.let { res ->
                GhCard {
                    Text("${res.status.uppercase()} · ${res.aheadBy} ahead, ${res.behindBy} behind", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GhBadge("${res.totalCommits} commits")
                        GhBadge("${res.files.size} files")
                    }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    if (res.files.isNotEmpty()) item {
                        Text("Files", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    items(res.files, key = { it.sha + it.filename }) { f ->
                        GhCard {
                            Text(f.filename, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GhBadge(f.status)
                                Text("+${f.additions}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                                Text("-${f.deletions}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    if (res.commits.isNotEmpty()) item {
                        Text("Commits", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    items(res.commits, key = { it.sha }) { c ->
                        GhCard {
                            Text(c.commit.message.lineSequence().firstOrNull().orEmpty(), maxLines = 2)
                            Text("${c.sha.take(7)} · ${c.commit.author?.name ?: c.commit.committer?.name ?: ""}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            EmbeddedTerminal(section = "Compare")
        }
    }
}
