package com.githubcontrol.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.githubcontrol.data.api.GhContributor
import com.githubcontrol.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyticsState(
    val loading: Boolean = false,
    val contributors: List<GhContributor> = emptyList(),
    val languages: Map<String, Long> = emptyMap(),
    val rateRemaining: Int? = null,
    val error: String? = null
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(private val repo: GitHubRepository) : ViewModel() {
    private val _state = MutableStateFlow(AnalyticsState())
    val state: StateFlow<AnalyticsState> = _state

    fun load(owner: String, name: String) {
        viewModelScope.launch {
            _state.value = AnalyticsState(loading = true)
            try {
                val c = runCatching { repo.contributors(owner, name) }.getOrDefault(emptyList())
                val l = runCatching { repo.languages(owner, name) }.getOrDefault(emptyMap())
                val rl = runCatching { repo.rateLimit().rate.remaining }.getOrNull()
                _state.value = AnalyticsState(loading = false, contributors = c, languages = l, rateRemaining = rl)
            } catch (t: Throwable) { _state.value = _state.value.copy(loading = false, error = t.message) }
        }
    }
}
