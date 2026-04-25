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

data class IssuesState(
    val loading: Boolean = false,
    val state: String = "open",
    val list: List<GhIssue> = emptyList(),
    val page: Int = 1,
    val endReached: Boolean = false,
    val error: String? = null
)

data class IssueDetailState(
    val loading: Boolean = false,
    val issue: GhIssue? = null,
    val comments: List<IssueComment> = emptyList(),
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class IssuesViewModel @Inject constructor(private val repo: GitHubRepository) : ViewModel() {
    private val _state = MutableStateFlow(IssuesState())
    val state: StateFlow<IssuesState> = _state

    private val _detail = MutableStateFlow(IssueDetailState())
    val detail: StateFlow<IssueDetailState> = _detail

    fun load(owner: String, name: String, st: String) {
        viewModelScope.launch {
            _state.value = IssuesState(loading = true, state = st)
            try {
                val list = repo.issues(owner, name, st, 1).filter { it.pullRequest == null }
                _state.value = IssuesState(loading = false, state = st, list = list, endReached = list.size < 30)
            } catch (t: Throwable) { _state.value = _state.value.copy(loading = false, error = t.message) }
        }
    }

    fun loadMore(owner: String, name: String) {
        val s = _state.value
        if (s.loading || s.endReached) return
        viewModelScope.launch {
            _state.value = s.copy(loading = true)
            try {
                val list = repo.issues(owner, name, s.state, s.page + 1).filter { it.pullRequest == null }
                _state.value = s.copy(loading = false, list = (s.list + list).distinctBy { it.id }, page = s.page + 1, endReached = list.size < 30)
            } catch (t: Throwable) { _state.value = s.copy(loading = false, error = t.message) }
        }
    }

    fun loadDetail(owner: String, name: String, number: Int) {
        viewModelScope.launch {
            _detail.value = IssueDetailState(loading = true)
            try {
                val i = repo.issue(owner, name, number)
                val c = runCatching { repo.issueComments(owner, name, number) }.getOrDefault(emptyList())
                _detail.value = IssueDetailState(loading = false, issue = i, comments = c)
            } catch (t: Throwable) { _detail.value = _detail.value.copy(loading = false, error = t.message) }
        }
    }

    fun create(owner: String, name: String, req: CreateIssueRequest, onDone: (GhIssue?) -> Unit) {
        viewModelScope.launch {
            try { onDone(repo.createIssue(owner, name, req)) }
            catch (t: Throwable) { _state.value = _state.value.copy(error = t.message); onDone(null) }
        }
    }

    fun close(owner: String, name: String, number: Int) {
        viewModelScope.launch {
            runCatching { repo.updateIssue(owner, name, number, UpdateIssueRequest(state = "closed")) }
                .onSuccess { _detail.value = _detail.value.copy(issue = it, message = "Closed") }
                .onFailure { _detail.value = _detail.value.copy(error = it.message) }
        }
    }

    fun reopen(owner: String, name: String, number: Int) {
        viewModelScope.launch {
            runCatching { repo.updateIssue(owner, name, number, UpdateIssueRequest(state = "open")) }
                .onSuccess { _detail.value = _detail.value.copy(issue = it, message = "Reopened") }
                .onFailure { _detail.value = _detail.value.copy(error = it.message) }
        }
    }

    fun comment(owner: String, name: String, number: Int, body: String) {
        viewModelScope.launch {
            runCatching { repo.addIssueComment(owner, name, number, body) }
                .onSuccess { _detail.value = _detail.value.copy(comments = _detail.value.comments + it) }
                .onFailure { _detail.value = _detail.value.copy(error = it.message) }
        }
    }
}
