package com.githubcontrol.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.githubcontrol.data.api.GhFileTreeItem
import com.githubcontrol.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TreeNodeUi(
    val path: String,
    val name: String,
    val depth: Int,
    val type: String, // tree/blob
    val sha: String,
    val expanded: Boolean = false,
    val visible: Boolean = true,
    val size: Long = 0,
    val selected: Boolean = false
)

data class TreeState(
    val loading: Boolean = false,
    val truncated: Boolean = false,
    val items: List<TreeNodeUi> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class TreeViewModel @Inject constructor(private val repo: GitHubRepository) : ViewModel() {
    private val _state = MutableStateFlow(TreeState())
    val state: StateFlow<TreeState> = _state

    fun load(owner: String, name: String, ref: String?) {
        viewModelScope.launch {
            _state.value = TreeState(loading = true)
            try {
                val branchRef = ref?.takeIf { it.isNotBlank() } ?: "HEAD"
                val branch = runCatching { repo.api.branch(owner, name, branchRef) }.getOrNull()
                val sha = branch?.commit?.sha ?: repo.api.repo(owner, name).defaultBranch.let { db ->
                    repo.api.branch(owner, name, db).commit.sha
                }
                val tree = repo.api.gitTree(owner, name, sha, recursive = 1)
                val ui = tree.tree.map { it.toUi() }.sortedBy { it.path.lowercase() }
                val depthZero = ui.map { it.copy(depth = it.path.count { c -> c == '/' }) }
                _state.value = TreeState(loading = false, truncated = tree.truncated, items = computeVisibility(depthZero, mutableSetOf()))
            } catch (t: Throwable) { _state.value = TreeState(loading = false, error = t.message) }
        }
    }

    fun toggle(path: String) {
        val items = _state.value.items.toMutableList()
        val idx = items.indexOfFirst { it.path == path }
        if (idx < 0 || items[idx].type != "tree") return
        items[idx] = items[idx].copy(expanded = !items[idx].expanded)
        val expanded = items.filter { it.expanded && it.type == "tree" }.map { it.path }.toMutableSet()
        _state.value = _state.value.copy(items = computeVisibility(items, expanded))
    }

    fun toggleSelect(path: String) {
        _state.value = _state.value.copy(items = _state.value.items.map {
            if (it.path == path) it.copy(selected = !it.selected) else it
        })
    }

    fun selectedPaths(): List<String> = _state.value.items.filter { it.selected && it.type == "blob" }.map { it.path }

    private fun computeVisibility(items: List<TreeNodeUi>, expanded: MutableSet<String>): List<TreeNodeUi> {
        return items.map { node ->
            val parentPath = node.path.substringBeforeLast('/', "")
            val visible = if (parentPath.isEmpty()) true else {
                var p = parentPath
                var ok = true
                while (p.isNotEmpty()) {
                    if (!expanded.contains(p)) { ok = false; break }
                    p = p.substringBeforeLast('/', "")
                }
                ok
            }
            node.copy(visible = visible)
        }
    }

    private fun GhFileTreeItem.toUi() = TreeNodeUi(
        path = path, name = path.substringAfterLast('/'), depth = path.count { it == '/' },
        type = type, sha = sha, size = size ?: 0
    )
}
