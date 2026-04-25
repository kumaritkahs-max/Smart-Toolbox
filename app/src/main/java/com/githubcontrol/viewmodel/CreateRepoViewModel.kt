package com.githubcontrol.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.githubcontrol.data.api.CreateRepoRequest
import com.githubcontrol.data.api.GhRepo
import com.githubcontrol.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateRepoState(
    val name: String = "",
    val description: String = "",
    val private: Boolean = false,
    val autoInit: Boolean = true,
    val gitignore: String = "",
    val license: String = "",
    val hasIssues: Boolean = true,
    val hasWiki: Boolean = true,
    val hasProjects: Boolean = true,
    val busy: Boolean = false,
    val created: GhRepo? = null,
    val error: String? = null
)

@HiltViewModel
class CreateRepoViewModel @Inject constructor(private val repo: GitHubRepository) : ViewModel() {
    private val _state = MutableStateFlow(CreateRepoState())
    val state: StateFlow<CreateRepoState> = _state

    fun update(s: CreateRepoState) { _state.value = s }

    fun submit() {
        val s = _state.value
        if (s.name.isBlank()) { _state.value = s.copy(error = "Name required"); return }
        viewModelScope.launch {
            _state.value = s.copy(busy = true, error = null)
            try {
                val r = repo.createRepo(CreateRepoRequest(
                    name = s.name, description = s.description.ifBlank { null }, private = s.private,
                    autoInit = s.autoInit,
                    gitignoreTemplate = s.gitignore.ifBlank { null },
                    licenseTemplate = s.license.ifBlank { null },
                    hasIssues = s.hasIssues, hasWiki = s.hasWiki, hasProjects = s.hasProjects
                ))
                _state.value = _state.value.copy(busy = false, created = r)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(busy = false, error = t.message)
            }
        }
    }
}
