package com.githubcontrol.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.githubcontrol.data.api.GhWorkflow
import com.githubcontrol.data.api.GhWorkflowRun
import com.githubcontrol.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActionsState(
    val loading: Boolean = false,
    val workflows: List<GhWorkflow> = emptyList(),
    val runs: List<GhWorkflowRun> = emptyList(),
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class ActionsViewModel @Inject constructor(private val repo: GitHubRepository) : ViewModel() {
    private val _state = MutableStateFlow(ActionsState())
    val state: StateFlow<ActionsState> = _state

    fun load(owner: String, name: String) {
        viewModelScope.launch {
            _state.value = ActionsState(loading = true)
            try {
                val w = repo.workflows(owner, name).workflows
                val r = repo.workflowRuns(owner, name).runs
                _state.value = ActionsState(loading = false, workflows = w, runs = r)
            } catch (t: Throwable) { _state.value = _state.value.copy(loading = false, error = t.message) }
        }
    }

    fun dispatch(owner: String, name: String, id: Long, ref: String) {
        viewModelScope.launch {
            runCatching { repo.dispatchWorkflow(owner, name, id, ref) }
                .onSuccess { _state.value = _state.value.copy(message = if (it.isSuccessful) "Dispatched" else "Failed ${it.code()}") }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
            load(owner, name)
        }
    }
}
