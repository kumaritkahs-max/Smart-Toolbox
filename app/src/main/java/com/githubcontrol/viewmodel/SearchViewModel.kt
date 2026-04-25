package com.githubcontrol.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.githubcontrol.data.api.GhCodeItem
import com.githubcontrol.data.api.GhRepo
import com.githubcontrol.data.api.GhUser
import com.githubcontrol.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchKind { REPOS, CODE, USERS }

data class SearchState(
    val q: String = "",
    val kind: SearchKind = SearchKind.REPOS,
    val loading: Boolean = false,
    val repos: List<GhRepo> = emptyList(),
    val code: List<GhCodeItem> = emptyList(),
    val users: List<GhUser> = emptyList(),
    val total: Int = 0,
    val error: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(private val repo: GitHubRepository) : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state

    fun setKind(k: SearchKind) { _state.value = _state.value.copy(kind = k) }
    fun setQ(q: String) { _state.value = _state.value.copy(q = q) }

    fun search() {
        val s = _state.value
        if (s.q.isBlank()) return
        viewModelScope.launch {
            _state.value = s.copy(loading = true, error = null)
            try {
                when (s.kind) {
                    SearchKind.REPOS -> {
                        val r = repo.searchRepos(s.q)
                        _state.value = _state.value.copy(loading = false, repos = r.items, total = r.totalCount, code = emptyList(), users = emptyList())
                    }
                    SearchKind.CODE -> {
                        val r = repo.searchCode(s.q)
                        _state.value = _state.value.copy(loading = false, code = r.items, total = r.totalCount, repos = emptyList(), users = emptyList())
                    }
                    SearchKind.USERS -> {
                        val r = repo.searchUsers(s.q)
                        _state.value = _state.value.copy(loading = false, users = r.items, total = r.totalCount, repos = emptyList(), code = emptyList())
                    }
                }
            } catch (t: Throwable) { _state.value = _state.value.copy(loading = false, error = t.message) }
        }
    }
}
