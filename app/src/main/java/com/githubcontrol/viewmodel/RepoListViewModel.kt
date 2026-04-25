package com.githubcontrol.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.githubcontrol.data.api.GhRepo
import com.githubcontrol.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RepoFilter { ALL, OWNED, FORKS, STARRED, PRIVATE, PUBLIC, ARCHIVED }
enum class RepoSort(val sort: String, val direction: String) {
    UPDATED("updated", "desc"),
    NAME("full_name", "asc"),
    STARS("pushed", "desc"),
    SIZE("created", "desc")
}

data class RepoListState(
    val loading: Boolean = false,
    val repos: List<GhRepo> = emptyList(),
    val filter: RepoFilter = RepoFilter.ALL,
    val sort: RepoSort = RepoSort.UPDATED,
    val search: String = "",
    val page: Int = 1,
    val endReached: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RepoListViewModel @Inject constructor(
    private val repo: GitHubRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RepoListState())
    val state: StateFlow<RepoListState> = _state.asStateFlow()

    init { reload() }

    fun setSearch(q: String) { _state.value = _state.value.copy(search = q) }
    fun setSort(s: RepoSort) { _state.value = _state.value.copy(sort = s); reload() }
    fun setFilter(f: RepoFilter) { _state.value = _state.value.copy(filter = f); reload() }

    fun reload() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, page = 1, endReached = false, error = null, repos = emptyList())
            try {
                val list = if (_state.value.filter == RepoFilter.STARRED) repo.listStarred(1)
                else repo.listMyRepos(1, 30, _state.value.sort.sort, _state.value.sort.direction,
                    when (_state.value.filter) {
                        RepoFilter.PRIVATE -> "private"; RepoFilter.PUBLIC -> "public"
                        else -> null
                    })
                val filtered = applyFilter(list)
                _state.value = _state.value.copy(loading = false, repos = filtered, endReached = list.size < 30)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(loading = false, error = t.message)
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loading || s.endReached) return
        viewModelScope.launch {
            _state.value = s.copy(loading = true)
            try {
                val nextPage = s.page + 1
                val list = if (s.filter == RepoFilter.STARRED) repo.listStarred(nextPage)
                else repo.listMyRepos(nextPage, 30, s.sort.sort, s.sort.direction, null)
                val combined = (s.repos + applyFilter(list)).distinctBy { it.id }
                _state.value = s.copy(loading = false, repos = combined, page = nextPage, endReached = list.size < 30)
            } catch (t: Throwable) {
                _state.value = s.copy(loading = false, error = t.message)
            }
        }
    }

    private fun applyFilter(list: List<GhRepo>): List<GhRepo> = when (_state.value.filter) {
        RepoFilter.OWNED -> list.filter { !it.fork }
        RepoFilter.FORKS -> list.filter { it.fork }
        RepoFilter.PRIVATE -> list.filter { it.private }
        RepoFilter.PUBLIC -> list.filter { !it.private }
        RepoFilter.ARCHIVED -> list.filter { it.archived }
        else -> list
    }
}
