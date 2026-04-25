package com.githubcontrol.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.githubcontrol.data.api.GhBranch
import com.githubcontrol.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BranchesState(
    val loading: Boolean = false,
    val items: List<GhBranch> = emptyList(),
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class BranchesViewModel @Inject constructor(private val repo: GitHubRepository) : ViewModel() {
    private val _state = MutableStateFlow(BranchesState())
    val state: StateFlow<BranchesState> = _state

    fun load(owner: String, name: String) {
        viewModelScope.launch {
            _state.value = BranchesState(loading = true)
            try {
                _state.value = BranchesState(loading = false, items = repo.branches(owner, name))
            } catch (t: Throwable) { _state.value = BranchesState(loading = false, error = t.message) }
        }
    }

    fun create(owner: String, name: String, newBranch: String, fromBranch: String) {
        viewModelScope.launch {
            runCatching { repo.createBranch(owner, name, newBranch, fromBranch) }
                .onSuccess { _state.value = _state.value.copy(message = "Created $newBranch"); load(owner, name) }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun delete(owner: String, name: String, branch: String) {
        viewModelScope.launch {
            runCatching { repo.deleteBranch(owner, name, branch) }
                .onSuccess { _state.value = _state.value.copy(message = "Deleted $branch"); load(owner, name) }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun rename(owner: String, name: String, oldBranch: String, newBranch: String) {
        viewModelScope.launch {
            runCatching { repo.renameBranch(owner, name, oldBranch, newBranch) }
                .onSuccess { _state.value = _state.value.copy(message = "Renamed"); load(owner, name) }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }
}
