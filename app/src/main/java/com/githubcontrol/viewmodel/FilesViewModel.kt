package com.githubcontrol.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.githubcontrol.data.api.DeleteFileRequest
import com.githubcontrol.data.api.GhBranch
import com.githubcontrol.data.api.GhContent
import com.githubcontrol.data.api.PutFileRequest
import com.githubcontrol.data.repository.GitHubRepository
import com.githubcontrol.utils.toBase64
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FilesState(
    val loading: Boolean = false,
    val owner: String = "",
    val name: String = "",
    val path: String = "",
    val ref: String = "",
    val items: List<GhContent> = emptyList(),
    val branches: List<GhBranch> = emptyList(),
    val selection: Set<String> = emptySet(),
    val multiSelect: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class FilesViewModel @Inject constructor(private val repo: GitHubRepository) : ViewModel() {
    private val _state = MutableStateFlow(FilesState())
    val state: StateFlow<FilesState> = _state

    fun load(owner: String, name: String, path: String, ref: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, owner = owner, name = name, path = path, ref = ref, error = null)
            try {
                val items = repo.contents(owner, name, path, ref.ifBlank { null })
                    .sortedWith(compareByDescending<GhContent> { it.type == "dir" }.thenBy { it.name.lowercase() })
                val branches = if (_state.value.branches.isEmpty()) runCatching { repo.branches(owner, name) }.getOrDefault(emptyList()) else _state.value.branches
                _state.value = _state.value.copy(loading = false, items = items, branches = branches)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(loading = false, error = t.message)
            }
        }
    }

    fun toggleMultiSelect() { _state.value = _state.value.copy(multiSelect = !_state.value.multiSelect, selection = emptySet()) }
    fun toggleSelect(path: String) {
        val cur = _state.value.selection
        _state.value = _state.value.copy(selection = if (cur.contains(path)) cur - path else cur + path)
    }
    fun clearSelection() { _state.value = _state.value.copy(selection = emptySet()) }

    fun setRef(branch: String) {
        val s = _state.value
        load(s.owner, s.name, s.path, branch)
    }

    fun deletePath(path: String, sha: String, message: String, onDone: () -> Unit) {
        val s = _state.value
        viewModelScope.launch {
            try {
                repo.api.deleteFile(s.owner, s.name, path, DeleteFileRequest(message, sha, s.ref.ifBlank { null }))
                load(s.owner, s.name, s.path, s.ref); onDone()
            } catch (t: Throwable) { _state.value = _state.value.copy(error = t.message) }
        }
    }

    fun renamePath(oldPath: String, sha: String, newPath: String, message: String, contentBytes: ByteArray, onDone: () -> Unit) {
        val s = _state.value
        viewModelScope.launch {
            try {
                repo.api.putFile(s.owner, s.name, newPath, PutFileRequest(message, contentBytes.toBase64(), null, s.ref.ifBlank { null }))
                repo.api.deleteFile(s.owner, s.name, oldPath, DeleteFileRequest(message, sha, s.ref.ifBlank { null }))
                load(s.owner, s.name, s.path, s.ref); onDone()
            } catch (t: Throwable) { _state.value = _state.value.copy(error = t.message) }
        }
    }

    fun deleteSelected(message: String, onDone: () -> Unit) {
        val s = _state.value
        val selected = s.items.filter { s.selection.contains(it.path) && it.type == "file" }
        viewModelScope.launch {
            try {
                for (it in selected) {
                    repo.api.deleteFile(s.owner, s.name, it.path, DeleteFileRequest(message, it.sha, s.ref.ifBlank { null }))
                }
                clearSelection()
                load(s.owner, s.name, s.path, s.ref); onDone()
            } catch (t: Throwable) { _state.value = _state.value.copy(error = t.message) }
        }
    }
}
