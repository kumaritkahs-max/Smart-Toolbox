package com.githubcontrol.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.githubcontrol.data.api.GhCommit
import com.githubcontrol.data.api.GhCommitCompare
import com.githubcontrol.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CommitsState(
    val loading: Boolean = false,
    val commits: List<GhCommit> = emptyList(),
    val branch: String = "",
    val page: Int = 1,
    val endReached: Boolean = false,
    val error: String? = null
)

data class CommitDetailState(
    val loading: Boolean = false,
    val commit: GhCommit? = null,
    val ignoreWhitespace: Boolean = false,
    val sideBySide: Boolean = false,
    val error: String? = null
)

data class CompareState(
    val loading: Boolean = false,
    val compare: GhCommitCompare? = null,
    val error: String? = null
)

@HiltViewModel
class CommitsViewModel @Inject constructor(private val repo: GitHubRepository) : ViewModel() {
    private val _state = MutableStateFlow(CommitsState())
    val state: StateFlow<CommitsState> = _state

    private val _detail = MutableStateFlow(CommitDetailState())
    val detail: StateFlow<CommitDetailState> = _detail

    private val _compare = MutableStateFlow(CompareState())
    val compare: StateFlow<CompareState> = _compare

    fun load(owner: String, name: String, branch: String) {
        viewModelScope.launch {
            _state.value = CommitsState(loading = true, branch = branch)
            try {
                val list = repo.commits(owner, name, branch.ifBlank { null }, 1)
                _state.value = CommitsState(loading = false, commits = list, branch = branch, endReached = list.size < 30)
            } catch (t: Throwable) { _state.value = _state.value.copy(loading = false, error = t.message) }
        }
    }

    fun loadMore(owner: String, name: String) {
        val s = _state.value
        if (s.loading || s.endReached) return
        viewModelScope.launch {
            _state.value = s.copy(loading = true)
            try {
                val nextPage = s.page + 1
                val list = repo.commits(owner, name, s.branch.ifBlank { null }, nextPage)
                _state.value = s.copy(loading = false, commits = (s.commits + list).distinctBy { it.sha }, page = nextPage, endReached = list.size < 30)
            } catch (t: Throwable) { _state.value = s.copy(loading = false, error = t.message) }
        }
    }

    fun loadDetail(owner: String, name: String, sha: String) {
        viewModelScope.launch {
            _detail.value = CommitDetailState(loading = true)
            try {
                val c = repo.commitDetail(owner, name, sha)
                _detail.value = CommitDetailState(loading = false, commit = c)
            } catch (t: Throwable) { _detail.value = _detail.value.copy(loading = false, error = t.message) }
        }
    }

    fun toggleSideBySide() { _detail.value = _detail.value.copy(sideBySide = !_detail.value.sideBySide) }
    fun toggleIgnoreWs() { _detail.value = _detail.value.copy(ignoreWhitespace = !_detail.value.ignoreWhitespace) }

    fun loadCompare(owner: String, name: String, base: String, head: String) {
        viewModelScope.launch {
            _compare.value = CompareState(loading = true)
            try {
                val c = repo.compare(owner, name, base, head)
                _compare.value = CompareState(loading = false, compare = c)
            } catch (t: Throwable) { _compare.value = _compare.value.copy(loading = false, error = t.message) }
        }
    }
}
