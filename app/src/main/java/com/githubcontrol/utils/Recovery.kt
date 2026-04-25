package com.githubcontrol.utils

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Tiny resume layer — persists the bare minimum state needed to recover after
 * a crash, app kill, or abrupt network loss. Intentionally backed by plain
 * SharedPreferences (no Hilt, no DataStore) so it can be invoked from
 * background callbacks like the crash handler or the upload service.
 */
object Recovery {
    private const val PREFS = "recovery_state"
    private const val KEY_LAST_ROUTE = "last_route"
    private const val KEY_PENDING_UPLOAD = "pending_upload_json"
    private const val KEY_LAST_ERROR = "last_error"
    private const val KEY_SESSION_TS = "session_ts"

    @Serializable
    data class PendingUpload(
        val owner: String,
        val repo: String,
        val branch: String,
        val targetFolder: String,
        val totalFiles: Int,
        val completedFiles: Int,
        val message: String,
        val updatedAt: Long = System.currentTimeMillis()
    )

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun rememberRoute(ctx: Context, route: String) =
        prefs(ctx).edit().putString(KEY_LAST_ROUTE, route).putLong(KEY_SESSION_TS, System.currentTimeMillis()).apply()

    fun lastRoute(ctx: Context): String? = prefs(ctx).getString(KEY_LAST_ROUTE, null)

    fun rememberPendingUpload(ctx: Context, p: PendingUpload) =
        prefs(ctx).edit().putString(KEY_PENDING_UPLOAD, json.encodeToString(p)).apply()

    fun pendingUpload(ctx: Context): PendingUpload? = runCatching {
        prefs(ctx).getString(KEY_PENDING_UPLOAD, null)?.let { json.decodeFromString<PendingUpload>(it) }
    }.getOrNull()

    fun clearPendingUpload(ctx: Context) =
        prefs(ctx).edit().remove(KEY_PENDING_UPLOAD).apply()

    fun rememberLastError(ctx: Context, message: String) =
        prefs(ctx).edit().putString(KEY_LAST_ERROR, message).apply()

    fun lastError(ctx: Context): String? = prefs(ctx).getString(KEY_LAST_ERROR, null)

    fun lastSessionAt(ctx: Context): Long = prefs(ctx).getLong(KEY_SESSION_TS, 0L)
}
