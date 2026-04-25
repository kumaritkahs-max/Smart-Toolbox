package com.githubcontrol.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.githubcontrol.ai.CommitAi
import com.githubcontrol.data.auth.AccountManager
import com.githubcontrol.data.repository.GitHubRepository
import com.githubcontrol.upload.ConflictMode
import com.githubcontrol.upload.UploadJob
import com.githubcontrol.upload.UploadManager
import com.githubcontrol.upload.UploadProgress
import com.githubcontrol.utils.GitignoreMatcher
import com.githubcontrol.utils.fromBase64
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UploadFormState(
    val owner: String = "",
    val repo: String = "",
    val branch: String = "",
    val targetFolder: String = "",
    val message: String = "",
    val conflictMode: ConflictMode = ConflictMode.OVERWRITE,
    val authorName: String? = null,
    val authorEmail: String? = null,
    val pickedUris: List<Uri> = emptyList(),
    val ignore: GitignoreMatcher? = null,
    val job: UploadJob? = null,
    val totalBytes: Long = 0,
    val totalFiles: Int = 0,
    val branches: List<String> = emptyList(),
    val dryRun: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class UploadViewModel @Inject constructor(
    val uploadManager: UploadManager,
    private val repo: GitHubRepository,
    private val accounts: AccountManager
) : ViewModel() {

    private val _form = MutableStateFlow(UploadFormState())
    val form: StateFlow<UploadFormState> = _form
    val progress: StateFlow<UploadProgress> = uploadManager.state

    fun init(owner: String, repoName: String, path: String, ref: String) {
        viewModelScope.launch {
            val a = accounts.activeAccount()
            val branchList = runCatching { repo.branches(owner, repoName).map { it.name } }.getOrDefault(listOf("main"))
            val ignore = runCatching {
                val g = repo.fileContent(owner, repoName, ".gitignore", ref.ifBlank { null })
                val txt = g.content?.let { String(it.fromBase64()) }
                txt?.split("\n")?.let { GitignoreMatcher(it) }
            }.getOrNull()
            _form.value = _form.value.copy(
                owner = owner, repo = repoName, branch = ref.ifBlank { branchList.firstOrNull() ?: "main" },
                targetFolder = path, branches = branchList, ignore = ignore,
                authorName = a?.name, authorEmail = a?.email
            )
        }
    }

    fun setUris(uris: List<Uri>) {
        val s = _form.value
        val job = uploadManager.buildJob(uris, s.owner, s.repo, s.branch, s.targetFolder, s.message.ifBlank { "Upload via GitHub Control" }, s.conflictMode, s.authorName, s.authorEmail, s.ignore, s.dryRun)
        _form.value = s.copy(pickedUris = uris, job = job, totalBytes = job.totalBytes, totalFiles = job.files.size)
    }

    fun setDryRun(v: Boolean) { _form.value = _form.value.copy(dryRun = v); rebuild() }

    fun aiSuggestMessage() {
        val job = _form.value.job ?: return
        val msg = CommitAi.suggest(job)
        _form.value = _form.value.copy(message = msg)
    }

    fun setMessage(m: String) { _form.value = _form.value.copy(message = m) }
    fun setBranch(b: String) { _form.value = _form.value.copy(branch = b); rebuild() }
    fun setTarget(t: String) { _form.value = _form.value.copy(targetFolder = t); rebuild() }
    fun setMode(m: ConflictMode) { _form.value = _form.value.copy(conflictMode = m); rebuild() }
    fun setAuthor(name: String?, email: String?) { _form.value = _form.value.copy(authorName = name, authorEmail = email); rebuild() }

    private fun rebuild() {
        val s = _form.value
        if (s.pickedUris.isNotEmpty()) setUris(s.pickedUris)
    }

    fun start() {
        val s = _form.value
        val job = s.job ?: return
        viewModelScope.launch {
            uploadManager.runJob(job.copy(message = s.message.ifBlank { "Upload via GitHub Control" }))
        }
    }

    fun pause() = uploadManager.pause()
    fun resume() = uploadManager.resume()
    fun cancel() = uploadManager.cancel()
}
