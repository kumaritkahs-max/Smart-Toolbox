package com.githubcontrol.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.githubcontrol.data.api.GhRepo
import com.githubcontrol.data.api.GhUser
import com.githubcontrol.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardState(
    val loading: Boolean = false,
    val user: GhUser? = null,
    val recent: List<GhRepo> = emptyList(),
    val totalRepos: Int = 0,
    val totalStars: Int = 0,
    val rateRemaining: Int? = null,
    val unreadNotifications: Int = 0,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(private val repo: GitHubRepository) : ViewModel() {
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state

    fun load() {
        viewModelScope.launch {
            _state.value = DashboardState(loading = true)
            try {
                val u = repo.me()
                val recent = repo.listMyRepos(1, 8, "updated", "desc", null)
                val rl = runCatching { repo.rateLimit().rate.remaining }.getOrNull()
                val notif = runCatching { repo.notifications(false).count { it.unread } }.getOrDefault(0)
                _state.value = DashboardState(
                    loading = false, user = u, recent = recent, rateRemaining = rl,
                    totalRepos = u.publicRepos, totalStars = recent.sumOf { it.stars }, unreadNotifications = notif
                )
            } catch (t: Throwable) { _state.value = _state.value.copy(loading = false, error = t.message) }
        }
    }
}
