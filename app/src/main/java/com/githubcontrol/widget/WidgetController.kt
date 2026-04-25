package com.githubcontrol.widget

import android.content.Context
import com.githubcontrol.upload.UploadFileState
import com.githubcontrol.upload.UploadManager
import com.githubcontrol.upload.UploadProgress
import com.githubcontrol.utils.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridge between the live in-app state and every home-screen widget. Listens to
 * [UploadManager.state] and pushes a fresh [WidgetState] snapshot to every
 * widget size whenever something changes. Idempotent: safe to call [start]
 * multiple times.
 */
@Singleton
class WidgetController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val uploadManager: UploadManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            uploadManager.state
                .distinctUntilChanged { a, b ->
                    a.uploaded == b.uploaded && a.total == b.total &&
                        a.running == b.running && a.finished == b.finished &&
                        a.currentFile == b.currentFile && a.error == b.error
                }
                .collect { onUploadStateChanged(it) }
        }
        Logger.i("WidgetCtl", "started — observing upload state")
    }

    /** Public hook for non-upload events (login, sync done, repo opened). */
    fun publish(
        repoName: String? = null,
        lastCommit: String? = null,
        status: String? = null,
        subtitle: String? = null,
        recent: List<String>? = null
    ) {
        WidgetStore.update(context) { cur ->
            cur.copy(
                repoName = repoName ?: cur.repoName,
                lastCommit = lastCommit ?: cur.lastCommit,
                status = status ?: cur.status,
                subtitle = subtitle ?: cur.subtitle,
                recent = recent ?: cur.recent,
                updatedAt = System.currentTimeMillis()
            )
        }
        WidgetRender.pushAll(context, WidgetStore.read(context))
    }

    fun pushRecent(line: String) {
        WidgetStore.update(context) { cur ->
            val merged = (listOf(line) + cur.recent).distinct().take(6)
            cur.copy(recent = merged, updatedAt = System.currentTimeMillis())
        }
        WidgetRender.pushAll(context, WidgetStore.read(context))
    }

    private fun onUploadStateChanged(p: UploadProgress) {
        val pct = if (p.total > 0) (p.uploaded * 100 / p.total) else 0
        val failed = p.files.count { it.state == UploadFileState.FAILED }
        val status = when {
            p.running -> "busy"
            p.error != null -> "failed"
            failed > 0 -> "failed"
            p.finished && p.total > 0 -> "synced"
            else -> "idle"
        }
        val subtitle = when {
            p.running -> "${p.uploaded}/${p.total} · ${p.currentFile.takeLast(40)}"
            p.finished && p.total > 0 -> "Done — ${p.uploaded}/${p.total} files"
            p.error != null -> "Last upload failed"
            else -> "Tap to open"
        }
        WidgetStore.update(context) { cur ->
            cur.copy(
                status = status,
                subtitle = subtitle,
                uploadRunning = p.running,
                uploadProgressPct = pct,
                uploadCurrentFile = p.currentFile,
                uploadDone = p.uploaded,
                uploadTotal = p.total,
                updatedAt = System.currentTimeMillis()
            )
        }
        WidgetRender.pushAll(context, WidgetStore.read(context))
        if (p.finished) {
            val name = p.job?.let { "${it.owner}/${it.repo}" } ?: "uploads"
            pushRecent("Upload → $name")
        }
    }
}
