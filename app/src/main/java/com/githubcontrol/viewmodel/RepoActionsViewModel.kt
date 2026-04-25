package com.githubcontrol.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.githubcontrol.data.api.*
import com.githubcontrol.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RepoDetailState(
    val loading: Boolean = false,
    val repo: GhRepo? = null,
    val starred: Boolean = false,
    val branches: List<GhBranch> = emptyList(),
    val contributors: List<GhContributor> = emptyList(),
    val languages: Map<String, Long> = emptyMap(),
    val error: String? = null,
    val actionMessage: String? = null
)

@HiltViewModel
class RepoActionsViewModel @Inject constructor(
    private val repo: GitHubRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RepoDetailState())
    val state: StateFlow<RepoDetailState> = _state.asStateFlow()

    fun load(owner: String, name: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val r = repo.repo(owner, name)
                val starred = repo.isStarred(owner, name)
                val branches = repo.branches(owner, name)
                val contributors = runCatching { repo.contributors(owner, name) }.getOrDefault(emptyList())
                val languages = runCatching { repo.languages(owner, name) }.getOrDefault(emptyMap())
                _state.value = RepoDetailState(false, r, starred, branches, contributors, languages)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(loading = false, error = t.message)
            }
        }
    }

    fun toggleStar() {
        val r = _state.value.repo ?: return
        viewModelScope.launch {
            try {
                if (_state.value.starred) repo.unstar(r.owner.login, r.name) else repo.star(r.owner.login, r.name)
                _state.value = _state.value.copy(starred = !_state.value.starred)
            } catch (t: Throwable) { _state.value = _state.value.copy(error = t.message) }
        }
    }

    fun watch(subscribed: Boolean) {
        val r = _state.value.repo ?: return
        viewModelScope.launch {
            runCatching {
                if (subscribed) repo.watch(r.owner.login, r.name, true) else repo.unwatch(r.owner.login, r.name)
            }.onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun setArchived(archived: Boolean) {
        val r = _state.value.repo ?: return
        viewModelScope.launch {
            runCatching { repo.updateRepo(r.owner.login, r.name, UpdateRepoRequest(archived = archived)) }
                .onSuccess { _state.value = _state.value.copy(repo = it, actionMessage = if (archived) "Archived" else "Unarchived") }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun setVisibility(private: Boolean) {
        val r = _state.value.repo ?: return
        viewModelScope.launch {
            runCatching { repo.updateRepo(r.owner.login, r.name, UpdateRepoRequest(private = private)) }
                .onSuccess { _state.value = _state.value.copy(repo = it, actionMessage = if (private) "Set private" else "Set public") }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun rename(newName: String, onDone: (GhRepo?) -> Unit) {
        val r = _state.value.repo ?: return
        viewModelScope.launch {
            runCatching { repo.updateRepo(r.owner.login, r.name, UpdateRepoRequest(name = newName)) }
                .onSuccess { _state.value = _state.value.copy(repo = it, actionMessage = "Renamed"); onDone(it) }
                .onFailure { _state.value = _state.value.copy(error = it.message); onDone(null) }
        }
    }

    fun toggleFeatures(issues: Boolean? = null, wiki: Boolean? = null, projects: Boolean? = null) {
        val r = _state.value.repo ?: return
        viewModelScope.launch {
            runCatching {
                repo.updateRepo(r.owner.login, r.name, UpdateRepoRequest(hasIssues = issues, hasWiki = wiki, hasProjects = projects))
            }.onSuccess { _state.value = _state.value.copy(repo = it, actionMessage = "Features updated") }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun fork() {
        val r = _state.value.repo ?: return
        viewModelScope.launch {
            runCatching { repo.forkRepo(r.owner.login, r.name) }
                .onSuccess { _state.value = _state.value.copy(actionMessage = "Forked to ${it.fullName}") }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun transfer(newOwner: String) {
        val r = _state.value.repo ?: return
        viewModelScope.launch {
            runCatching { repo.transfer(r.owner.login, r.name, newOwner) }
                .onSuccess { _state.value = _state.value.copy(actionMessage = "Transferred to $newOwner") }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun delete(confirmName: String, onDone: (Boolean) -> Unit) {
        val r = _state.value.repo ?: return
        if (confirmName != r.name) { _state.value = _state.value.copy(error = "Name mismatch"); onDone(false); return }
        viewModelScope.launch {
            runCatching { repo.deleteRepo(r.owner.login, r.name) }
                .onSuccess { onDone(it.isSuccessful) }
                .onFailure { _state.value = _state.value.copy(error = it.message); onDone(false) }
        }
    }

    fun clearMessage() { _state.value = _state.value.copy(actionMessage = null, error = null) }
}
