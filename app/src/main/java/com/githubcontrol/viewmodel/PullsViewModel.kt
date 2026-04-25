package com.githubcontrol.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.githubcontrol.data.api.*
import com.githubcontrol.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PullsState(
    val loading: Boolean = false,
    val state: String = "open",
    val list: List<GhPullRequest> = emptyList(),
    val page: Int = 1,
    val endReached: Boolean = false,
    val error: String? = null
)

data class PullDetailState(
    val loading: Boolean = false,
    val pull: GhPullRequest? = null,
    val files: List<GhCommitFile> = emptyList(),
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class PullsViewModel @Inject constructor(private val repo: GitHubRepository) : ViewModel() {
    private val _state = MutableStateFlow(PullsState())
    val state: StateFlow<PullsState> = _state

    private val _detail = MutableStateFlow(PullDetailState())
    val detail: StateFlow<PullDetailState> = _detail

    fun load(owner: String, name: String, st: String) {
        viewModelScope.launch {
            _state.value = PullsState(loading = true, state = st)
            try {
                val list = repo.pulls(owner, name, st, 1)
                _state.value = PullsState(loading = false, state = st, list = list, endReached = list.size < 30)
            } catch (t: Throwable) { _state.value = _state.value.copy(loading = false, error = t.message) }
        }
    }

    fun loadMore(owner: String, name: String) {
        val s = _state.value
        if (s.loading || s.endReached) return
        viewModelScope.launch {
            _state.value = s.copy(loading = true)
            try {
                val list = repo.pulls(owner, name, s.state, s.page + 1)
                _state.value = s.copy(loading = false, list = (s.list + list).distinctBy { it.id }, page = s.page + 1, endReached = list.size < 30)
            } catch (t: Throwable) { _state.value = s.copy(loading = false, error = t.message) }
        }
    }

    fun loadDetail(owner: String, name: String, number: Int) {
        viewModelScope.launch {
            _detail.value = PullDetailState(loading = true)
            try {
                val pr = repo.pull(owner, name, number)
                val files = runCatching { repo.pullFiles(owner, name, number) }.getOrDefault(emptyList())
                _detail.value = PullDetailState(loading = false, pull = pr, files = files)
            } catch (t: Throwable) { _detail.value = _detail.value.copy(loading = false, error = t.message) }
        }
    }

    fun create(owner: String, name: String, req: CreatePRRequest, onDone: (GhPullRequest?) -> Unit) {
        viewModelScope.launch {
            try { onDone(repo.createPull(owner, name, req)) }
            catch (t: Throwable) { _state.value = _state.value.copy(error = t.message); onDone(null) }
        }
    }

    fun close(owner: String, name: String, number: Int) {
        viewModelScope.launch {
            runCatching { repo.updatePull(owner, name, number, UpdatePRRequest(state = "closed")) }
                .onSuccess { _detail.value = _detail.value.copy(pull = it, message = "Closed") }
                .onFailure { _detail.value = _detail.value.copy(error = it.message) }
        }
    }

    fun reopen(owner: String, name: String, number: Int) {
        viewModelScope.launch {
            runCatching { repo.updatePull(owner, name, number, UpdatePRRequest(state = "open")) }
                .onSuccess { _detail.value = _detail.value.copy(pull = it, message = "Reopened") }
                .onFailure { _detail.value = _detail.value.copy(error = it.message) }
        }
    }

    fun merge(owner: String, name: String, number: Int, method: String) {
        viewModelScope.launch {
            runCatching { repo.mergePull(owner, name, number, MergePRRequest(mergeMethod = method)) }
                .onSuccess { _detail.value = _detail.value.copy(message = if (it.isSuccessful) "Merged" else "Failed: ${it.code()}") }
                .onFailure { _detail.value = _detail.value.copy(error = it.message) }
        }
    }
}
