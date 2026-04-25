package com.githubcontrol.ui.screens.branches

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.githubcontrol.data.api.RequiredStatusChecks
import com.githubcontrol.data.api.UpdateBranchProtectionRequest
import com.githubcontrol.data.api.UpdateRequiredReviews
import com.githubcontrol.data.repository.GitHubRepository
import com.githubcontrol.ui.components.EmbeddedTerminal
import com.githubcontrol.ui.components.GhCard
import com.githubcontrol.utils.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BPState(
    val loading: Boolean = true,
    val protected: Boolean = false,
    val requireReviews: Boolean = false,
    val reviewerCount: Int = 1,
    val dismissStale: Boolean = false,
    val requireCodeOwners: Boolean = false,
    val requireStatusChecks: Boolean = false,
    val strictStatusChecks: Boolean = false,
    val statusContexts: String = "",
    val enforceAdmins: Boolean = false,
    val linearHistory: Boolean = false,
    val allowForcePushes: Boolean = false,
    val allowDeletions: Boolean = false,
    val requireConvResolution: Boolean = false,
    val lockBranch: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class BranchProtectionViewModel @Inject constructor(private val repo: GitHubRepository) : ViewModel() {
    val state = MutableStateFlow(BPState())
    private var owner = ""; private var name = ""; private var branch = ""
    fun load(o: String, n: String, b: String) {
        owner = o; name = n; branch = b
        viewModelScope.launch {
            state.value = state.value.copy(loading = true, error = null)
            try {
                val p = repo.branchProtection(o, n, b)
                if (p == null) {
                    state.value = BPState(loading = false, protected = false)
                    return@launch
                }
                val rev = p.requiredReviews
                val sc = p.requiredStatusChecks
                state.value = BPState(
                    loading = false, protected = true,
                    requireReviews = rev != null,
                    reviewerCount = rev?.requiredApprovingCount ?: 1,
                    dismissStale = rev?.dismissStale ?: false,
                    requireCodeOwners = rev?.requireCodeOwners ?: false,
                    requireStatusChecks = sc != null,
                    strictStatusChecks = sc?.strict ?: false,
                    statusContexts = sc?.contexts.orEmpty().joinToString(", "),
                    enforceAdmins = p.enforceAdmins?.enabled ?: false,
                    linearHistory = p.requiredLinearHistory?.enabled ?: false,
                    allowForcePushes = p.allowForcePushes?.enabled ?: false,
                    allowDeletions = p.allowDeletions?.enabled ?: false,
                    requireConvResolution = p.requiredConversationResolution?.enabled ?: false,
                    lockBranch = p.lockBranch?.enabled ?: false
                )
            } catch (t: Throwable) {
                state.value = state.value.copy(loading = false, error = t.message)
            }
        }
    }
    fun update(transform: (BPState) -> BPState) { state.value = transform(state.value) }
    fun save() {
        val s = state.value
        viewModelScope.launch {
            state.value = s.copy(saving = true, error = null, message = null)
            try {
                val req = UpdateBranchProtectionRequest(
                    requiredStatusChecks = if (s.requireStatusChecks) RequiredStatusChecks(
                        strict = s.strictStatusChecks,
                        contexts = s.statusContexts.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    ) else null,
                    requiredReviews = if (s.requireReviews) UpdateRequiredReviews(
                        requiredApprovingCount = s.reviewerCount.coerceIn(0, 6),
                        dismissStale = s.dismissStale,
                        requireCodeOwners = s.requireCodeOwners
                    ) else null,
                    enforceAdmins = s.enforceAdmins,
                    requiredLinearHistory = s.linearHistory,
                    allowForcePushes = s.allowForcePushes,
                    allowDeletions = s.allowDeletions,
                    requiredConversationResolution = s.requireConvResolution,
                    lockBranch = s.lockBranch
                )
                repo.setBranchProtection(owner, name, branch, req)
                Logger.i("BranchProtection", "saved $owner/$name@$branch")
                state.value = s.copy(saving = false, protected = true, message = "Saved.")
            } catch (t: Throwable) {
                Logger.e("BranchProtection", "save failed", t)
                state.value = s.copy(saving = false, error = t.message)
            }
        }
    }
    fun remove() {
        viewModelScope.launch {
            try {
                repo.removeBranchProtection(owner, name, branch)
                Logger.w("BranchProtection", "removed $owner/$name@$branch")
                state.value = BPState(loading = false, protected = false, message = "Protection removed.")
            } catch (t: Throwable) {
                state.value = state.value.copy(error = t.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchProtectionScreen(
    owner: String, name: String, branch: String,
    onBack: () -> Unit, vm: BranchProtectionViewModel = hiltViewModel()
) {
    LaunchedEffect(owner, name, branch) { vm.load(owner, name, branch) }
    val s by vm.state.collectAsState()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Protection · $branch") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
        )
    }) { pad ->
        Column(Modifier.padding(pad).padding(12.dp).fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (s.loading) { LinearProgressIndicator(Modifier.fillMaxWidth()); return@Column }
            s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            s.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            GhCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, null, tint = if (s.protected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (s.protected) "Branch is protected" else "Branch is not protected", style = MaterialTheme.typography.titleMedium)
                        Text("$owner/$name", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            GhCard {
                Text("Pull request reviews", style = MaterialTheme.typography.titleMedium)
                ListItem(headlineContent = { Text("Require approving reviews") }, trailingContent = { Switch(s.requireReviews, { v -> vm.update { it.copy(requireReviews = v) } }) })
                if (s.requireReviews) {
                    ListItem(headlineContent = { Text("Required reviewers") }, trailingContent = {
                        OutlinedTextField(s.reviewerCount.toString(), { v -> v.toIntOrNull()?.let { vm.update { st -> st.copy(reviewerCount = it.coerceIn(0,6)) } } },
                            singleLine = true, modifier = Modifier.width(80.dp))
                    })
                    ListItem(headlineContent = { Text("Dismiss stale reviews on new commits") }, trailingContent = { Switch(s.dismissStale, { v -> vm.update { it.copy(dismissStale = v) } }) })
                    ListItem(headlineContent = { Text("Require code owner reviews") }, trailingContent = { Switch(s.requireCodeOwners, { v -> vm.update { it.copy(requireCodeOwners = v) } }) })
                }
            }

            GhCard {
                Text("Status checks", style = MaterialTheme.typography.titleMedium)
                ListItem(headlineContent = { Text("Require status checks to pass") }, trailingContent = { Switch(s.requireStatusChecks, { v -> vm.update { it.copy(requireStatusChecks = v) } }) })
                if (s.requireStatusChecks) {
                    ListItem(headlineContent = { Text("Require branch up to date (strict)") }, trailingContent = { Switch(s.strictStatusChecks, { v -> vm.update { it.copy(strictStatusChecks = v) } }) })
                    OutlinedTextField(s.statusContexts, { v -> vm.update { it.copy(statusContexts = v) } },
                        label = { Text("Required check contexts (comma separated)") }, modifier = Modifier.fillMaxWidth())
                }
            }

            GhCard {
                Text("Other rules", style = MaterialTheme.typography.titleMedium)
                ListItem(headlineContent = { Text("Include administrators") }, trailingContent = { Switch(s.enforceAdmins, { v -> vm.update { it.copy(enforceAdmins = v) } }) })
                ListItem(headlineContent = { Text("Require linear history") }, trailingContent = { Switch(s.linearHistory, { v -> vm.update { it.copy(linearHistory = v) } }) })
                ListItem(headlineContent = { Text("Require conversation resolution") }, trailingContent = { Switch(s.requireConvResolution, { v -> vm.update { it.copy(requireConvResolution = v) } }) })
                ListItem(headlineContent = { Text("Lock branch (read-only)") }, trailingContent = { Switch(s.lockBranch, { v -> vm.update { it.copy(lockBranch = v) } }) })
                ListItem(headlineContent = { Text("Allow force pushes") }, trailingContent = { Switch(s.allowForcePushes, { v -> vm.update { it.copy(allowForcePushes = v) } }) })
                ListItem(headlineContent = { Text("Allow deletions") }, trailingContent = { Switch(s.allowDeletions, { v -> vm.update { it.copy(allowDeletions = v) } }) })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.save() }, enabled = !s.saving, modifier = Modifier.weight(1f)) {
                    if (s.saving) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    else { Icon(Icons.Filled.Save, null); Spacer(Modifier.width(6.dp)); Text("Save protection") }
                }
                if (s.protected) {
                    OutlinedButton(onClick = { vm.remove() }, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Remove")
                    }
                }
            }
            EmbeddedTerminal(section = "BranchProtection")
        }
    }
}
