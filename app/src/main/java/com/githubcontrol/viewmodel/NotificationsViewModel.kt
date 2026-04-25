package com.githubcontrol.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.githubcontrol.data.api.GhNotification
import com.githubcontrol.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsState(
    val loading: Boolean = false,
    val all: Boolean = false,
    val items: List<GhNotification> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(private val repo: GitHubRepository) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsState())
    val state: StateFlow<NotificationsState> = _state

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            try {
                val list = repo.notifications(_state.value.all)
                _state.value = _state.value.copy(loading = false, items = list)
            } catch (t: Throwable) { _state.value = _state.value.copy(loading = false, error = t.message) }
        }
    }

    fun toggleAll() { _state.value = _state.value.copy(all = !_state.value.all); load() }
    fun markRead(id: String) {
        viewModelScope.launch {
            runCatching { repo.markNotification(id) }
            _state.value = _state.value.copy(items = _state.value.items.map { if (it.id == id) it.copy(unread = false) else it })
        }
    }
}
