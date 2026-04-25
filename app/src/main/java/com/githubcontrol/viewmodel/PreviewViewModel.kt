package com.githubcontrol.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.githubcontrol.data.api.GhContent
import com.githubcontrol.data.api.PutFileRequest
import com.githubcontrol.data.repository.GitHubRepository
import com.githubcontrol.utils.fromBase64
import com.githubcontrol.utils.toBase64
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PreviewState(
    val loading: Boolean = false,
    val content: GhContent? = null,
    val text: String? = null,
    val isImage: Boolean = false,
    val error: String? = null,
    val saving: Boolean = false
)

@HiltViewModel
class PreviewViewModel @Inject constructor(private val repo: GitHubRepository) : ViewModel() {
    private val _state = MutableStateFlow(PreviewState())
    val state: StateFlow<PreviewState> = _state

    fun load(owner: String, name: String, path: String, ref: String) {
        viewModelScope.launch {
            _state.value = PreviewState(loading = true)
            try {
                val c = repo.fileContent(owner, name, path, ref.ifBlank { null })
                val ext = path.substringAfterLast('.', "").lowercase()
                val isImage = ext in setOf("png", "jpg", "jpeg", "gif", "webp", "svg", "bmp", "ico")
                val text = if (!isImage && c.encoding == "base64" && c.content != null) {
                    runCatching { String(c.content!!.fromBase64()) }.getOrNull()
                } else null
                _state.value = PreviewState(loading = false, content = c, text = text, isImage = isImage)
            } catch (t: Throwable) {
                _state.value = PreviewState(loading = false, error = t.message)
            }
        }
    }

    fun save(owner: String, name: String, path: String, ref: String, newText: String, message: String, onDone: () -> Unit) {
        val sha = _state.value.content?.sha ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true)
            try {
                repo.api.putFile(owner, name, path, PutFileRequest(message, newText.toByteArray().toBase64(), sha, ref.ifBlank { null }))
                onDone()
            } catch (t: Throwable) {
                _state.value = _state.value.copy(error = t.message)
            } finally {
                _state.value = _state.value.copy(saving = false)
            }
        }
    }
}
