package com.githubcontrol.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.githubcontrol.data.repository.ReadmeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReadmeState(
    val loading: Boolean = false,
    val html: String? = null,
    val error: String? = null
)

@HiltViewModel
class ReadmeViewModel @Inject constructor(
    private val readmes: ReadmeRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ReadmeState())
    val state: StateFlow<ReadmeState> = _state

    fun load(owner: String, repo: String, ref: String? = null) {
        if (_state.value.loading) return
        _state.value = ReadmeState(loading = true)
        viewModelScope.launch {
            try {
                val html = readmes.renderedReadmeHtml(owner, repo, ref)
                _state.value = ReadmeState(loading = false, html = html)
            } catch (t: Throwable) {
                _state.value = ReadmeState(loading = false, error = t.message ?: "Failed to load README")
            }
        }
    }
}
