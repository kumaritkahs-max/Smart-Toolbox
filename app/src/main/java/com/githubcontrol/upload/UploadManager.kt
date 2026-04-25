package com.githubcontrol.upload

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.githubcontrol.data.api.PutFileRequest
import com.githubcontrol.data.repository.GitHubRepository
import com.githubcontrol.notifications.Notifier
import com.githubcontrol.utils.GitignoreMatcher
import com.githubcontrol.utils.toBase64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

enum class ConflictMode { OVERWRITE, SKIP, RENAME }
enum class UploadFileState { PENDING, UPLOADING, DONE, SKIPPED, FAILED }

data class UploadFile(
    val id: String,
    val displayName: String,
    val targetPath: String,
    val sizeBytes: Long,
    var state: UploadFileState = UploadFileState.PENDING,
    var error: String? = null,
    var bytesDone: Long = 0,
)

data class UploadJob(
    val owner: String,
    val repo: String,
    val branch: String,
    val targetFolder: String,
    val message: String,
    val conflictMode: ConflictMode,
    val authorName: String?,
    val authorEmail: String?,
    val files: List<UploadFile>,
    val totalBytes: Long,
    val dryRun: Boolean = false,
)

data class UploadProgress(
    val job: UploadJob? = null,
    val files: List<UploadFile> = emptyList(),
    val currentFile: String = "",
    val uploaded: Int = 0,
    val total: Int = 0,
    val bytesDone: Long = 0,
    val bytesPerSec: Double = 0.0,
    val etaSeconds: Long = 0,
    val running: Boolean = false,
    val paused: Boolean = false,
    val finished: Boolean = false,
    val error: String? = null
)

@Singleton
class UploadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: GitHubRepository,
    private val notifier: Notifier
) {
    private val _state = MutableStateFlow(UploadProgress())
    val state: StateFlow<UploadProgress> = _state

    @Volatile var paused: Boolean = false
    @Volatile var cancelled: Boolean = false

    fun pause() { paused = true; _state.value = _state.value.copy(paused = true) }
    fun resume() { paused = false; _state.value = _state.value.copy(paused = false) }
    fun cancel() { cancelled = true; paused = false }

    /** Build an upload job from the given URIs (files and/or directories). */
    fun buildJob(
        uris: List<Uri>, owner: String, repo: String, branch: String, targetFolder: String,
        message: String, conflictMode: ConflictMode,
        authorName: String?, authorEmail: String?, gitignore: GitignoreMatcher? = null,
        dryRun: Boolean = false
    ): UploadJob {
        val files = mutableListOf<UploadFile>()
        var total = 0L
        for (uri in uris) {
            val df = DocumentFile.fromTreeUri(context, uri) ?: DocumentFile.fromSingleUri(context, uri)
            if (df != null && df.isDirectory) collectDir(df, targetFolder, files, gitignore) else if (df != null) {
                val tname = df.name ?: "file"
                val target = joinPath(targetFolder, tname)
                if (gitignore?.isIgnored(target, false) != true) {
                    files += UploadFile(uri.toString(), tname, target, df.length())
                    total += df.length()
                }
            }
        }
        return UploadJob(owner, repo, branch, targetFolder, message, conflictMode, authorName, authorEmail, files, total, dryRun)
    }

    private fun collectDir(dir: DocumentFile, base: String, into: MutableList<UploadFile>, gitignore: GitignoreMatcher?) {
        val dname = dir.name ?: return
        val newBase = joinPath(base, dname)
        for (child in dir.listFiles()) {
            if (child.isDirectory) collectDir(child, newBase, into, gitignore)
            else {
                val target = joinPath(newBase, child.name ?: "file")
                if (gitignore?.isIgnored(target, false) != true) {
                    into += UploadFile(child.uri.toString(), child.name ?: "file", target, child.length())
                }
            }
        }
    }

    suspend fun runJob(job: UploadJob) = withContext(Dispatchers.IO) {
        cancelled = false; paused = false
        val start = System.currentTimeMillis()
        var bytesDone = 0L
        var done = 0
        _state.value = UploadProgress(job, job.files, "", 0, job.files.size, 0, 0.0, 0, true)

        for (uf in job.files) {
            if (cancelled) break
            while (paused && !cancelled) Thread.sleep(150)
            uf.state = UploadFileState.UPLOADING
            _state.value = _state.value.copy(currentFile = uf.targetPath, files = job.files.toList())
            try {
                if (job.dryRun) {
                    // Preview-only: do not contact the API, just mark the file as resolved.
                    uf.state = UploadFileState.DONE
                    uf.bytesDone = uf.sizeBytes
                    bytesDone += uf.sizeBytes
                    done++
                    _state.value = _state.value.copy(files = job.files.toList(), uploaded = done, bytesDone = bytesDone)
                    continue
                }
                val bytes = readUri(Uri.parse(uf.id)) ?: throw IllegalStateException("Cannot read")
                val targetSha = if (job.conflictMode == ConflictMode.OVERWRITE) {
                    runCatching { repo.fileContent(job.owner, job.repo, uf.targetPath, job.branch).sha }.getOrNull()
                } else null

                if (job.conflictMode == ConflictMode.SKIP) {
                    val exists = runCatching { repo.fileContent(job.owner, job.repo, uf.targetPath, job.branch) }.isSuccess
                    if (exists) { uf.state = UploadFileState.SKIPPED; done++; continue }
                }

                val finalPath = if (job.conflictMode == ConflictMode.RENAME) renameIfExists(job.owner, job.repo, uf.targetPath, job.branch) else uf.targetPath
                val req = PutFileRequest(
                    message = job.message,
                    content = bytes.toBase64(),
                    sha = targetSha,
                    branch = job.branch,
                    author = if (job.authorName != null && job.authorEmail != null)
                        com.githubcontrol.data.api.GhCommitAuthor(job.authorName, job.authorEmail, java.time.Instant.now().toString()) else null,
                    committer = if (job.authorName != null && job.authorEmail != null)
                        com.githubcontrol.data.api.GhCommitAuthor(job.authorName, job.authorEmail, java.time.Instant.now().toString()) else null,
                )
                repo.api.putFile(job.owner, job.repo, finalPath, req)
                uf.state = UploadFileState.DONE
                uf.bytesDone = uf.sizeBytes
                bytesDone += uf.sizeBytes
            } catch (t: Throwable) {
                uf.state = UploadFileState.FAILED
                uf.error = t.message
            }
            done++
            val elapsed = (System.currentTimeMillis() - start) / 1000.0
            val rate = if (elapsed > 0) bytesDone / elapsed else 0.0
            val eta = if (rate > 0) ((job.totalBytes - bytesDone) / rate).toLong() else 0
            notifier.upload(done, job.files.size, uf.displayName)
            _state.value = _state.value.copy(
                files = job.files.toList(), uploaded = done, bytesDone = bytesDone,
                bytesPerSec = rate, etaSeconds = eta
            )
        }
        val ok = job.files.none { it.state == UploadFileState.FAILED }
        if (!job.dryRun) {
            notifier.uploadDone(ok, "${job.files.count { it.state == UploadFileState.DONE }}/${job.files.size} files")
        }
        _state.value = _state.value.copy(running = false, finished = true, error = if (ok) null else "Some files failed")
    }

    private suspend fun renameIfExists(owner: String, repoName: String, path: String, branch: String): String {
        var candidate = path
        var i = 1
        while (runCatching { repo.fileContent(owner, repoName, candidate, branch) }.isSuccess) {
            val dot = path.lastIndexOf('.')
            candidate = if (dot > 0 && dot > path.lastIndexOf('/')) {
                path.substring(0, dot) + "-$i" + path.substring(dot)
            } else "$path-$i"
            i++
            if (i > 99) break
        }
        return candidate
    }

    suspend fun uploadZipExpanded(
        owner: String, repoName: String, branch: String, targetFolder: String,
        zipUri: Uri, message: String, authorName: String?, authorEmail: String?
    ) = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val files = mutableListOf<Pair<String, ByteArray>>()
        resolver.openInputStream(zipUri)?.use { input ->
            ZipInputStream(input).use { zin ->
                var entry = zin.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val baos = ByteArrayOutputStream()
                        zin.copyTo(baos)
                        files += joinPath(targetFolder, entry.name) to baos.toByteArray()
                    }
                    zin.closeEntry()
                    entry = zin.nextEntry
                }
            }
        }
        // Atomic multi-file commit via Git data API
        repo.commitFiles(owner, repoName, branch, files.map { it.first to it.second as ByteArray? }, message, authorName, authorEmail)
    }

    private fun readUri(uri: Uri): ByteArray? = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }

    private fun joinPath(base: String, name: String): String {
        val b = base.trim('/')
        val n = name.trim('/')
        return if (b.isEmpty()) n else "$b/$n"
    }
}
