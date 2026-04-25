package com.githubcontrol.worker

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.githubcontrol.data.db.AppDatabase
import com.githubcontrol.data.repository.GitHubRepository
import com.githubcontrol.notifications.Notifier
import com.githubcontrol.upload.UploadManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.security.MessageDigest

/**
 * Mirrors a local SAF folder tree into a remote GitHub repo path.
 * For each enabled job:
 *   1. Walk local tree → map<relativePath, bytes>.
 *   2. Fetch remote tree (recursive) → map<relativePath, blobSha>.
 *   3. Compute git blob SHA-1 for each local file. Anything that differs or
 *      does not exist remotely is ADDED/MODIFIED. Anything in remote that no
 *      longer exists locally is DELETED.
 *   4. Send a single atomic tree commit via the Git Data API.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val db: AppDatabase,
    private val repo: GitHubRepository,
    private val uploads: UploadManager,
    private val notifier: Notifier
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val jobs = db.syncJobs().observeEnabled().first()
        val now = System.currentTimeMillis()
        for (j in jobs) {
            val due = j.lastRun + j.intervalMinutes * 60_000L
            if (now < due) continue
            runCatching { syncOne(j.owner, j.repo, j.branch, Uri.parse(j.localUri), j.remotePath) }
                .onFailure { notifier.alert("Sync failed", "${j.owner}/${j.repo}: ${it.message}") }
            db.syncJobs().update(j.copy(lastRun = now))
        }
        return Result.success()
    }

    private suspend fun syncOne(owner: String, repoName: String, branch: String, localUri: Uri, remoteBase: String) {
        val local = mutableMapOf<String, ByteArray>()
        val root = DocumentFile.fromTreeUri(applicationContext, localUri) ?: return
        walk(root, "", local)

        val branchInfo = repo.branches(owner, repoName).firstOrNull { it.name == branch } ?: return
        val tree = runCatching { repo.api.gitTree(owner, repoName, branchInfo.commit.sha, recursive = 1) }.getOrNull() ?: return
        val remote = tree.tree.filter { it.type == "blob" }.associate { it.path to it.sha }

        val base = remoteBase.trim('/')
        val joined = mutableListOf<Pair<String, ByteArray?>>()

        // Added or modified
        for ((rel, bytes) in local) {
            val remotePath = if (base.isEmpty()) rel else "$base/$rel"
            val sha = blobSha(bytes)
            if (remote[remotePath] != sha) joined += remotePath to bytes
        }
        // Deleted (only inside the synced sub-tree)
        for ((rpath, _) in remote) {
            val inScope = base.isEmpty() || rpath == base || rpath.startsWith("$base/")
            if (!inScope) continue
            val rel = if (base.isEmpty()) rpath else rpath.removePrefix("$base/")
            if (!local.containsKey(rel)) joined += rpath to null
        }
        if (joined.isEmpty()) return

        repo.commitFiles(owner, repoName, branch, joined, "Sync from device", null, null)
        notifier.alert("Sync complete", "$owner/$repoName: ${joined.count { it.second != null }} updated, ${joined.count { it.second == null }} removed")
    }

    private fun walk(dir: DocumentFile, prefix: String, into: MutableMap<String, ByteArray>) {
        for (child in dir.listFiles()) {
            val name = child.name ?: continue
            val key = if (prefix.isEmpty()) name else "$prefix/$name"
            if (child.isDirectory) walk(child, key, into)
            else applicationContext.contentResolver.openInputStream(child.uri)?.use { into[key] = it.readBytes() }
        }
    }

    /** Compute Git's blob SHA-1: `sha1("blob " + size + "\0" + content)` */
    private fun blobSha(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-1")
        val header = "blob ${bytes.size}\u0000".toByteArray(Charsets.UTF_8)
        md.update(header); md.update(bytes)
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
