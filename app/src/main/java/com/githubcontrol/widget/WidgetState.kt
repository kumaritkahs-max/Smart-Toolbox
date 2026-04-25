package com.githubcontrol.widget

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Lightweight, JSON-encoded snapshot of everything the widgets need to render
 * — written by [WidgetController] from the app process and read by every
 * widget provider (small / medium / large). Stored in a tiny SharedPreferences
 * file so it works whether or not the app process is alive.
 */
@Serializable
data class WidgetState(
    val status: String = "idle",          // idle | synced | pending | busy | failed
    val statusLabel: String = "● tap to open",
    val repoName: String = "GitHub Control",
    val subtitle: String = "Tap to open",
    val lastCommit: String = "",
    val uploadRunning: Boolean = false,
    val uploadProgressPct: Int = 0,
    val uploadCurrentFile: String = "",
    val uploadDone: Int = 0,
    val uploadTotal: Int = 0,
    val recent: List<String> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
)

object WidgetStore {
    private const val PREFS = "widget_state_v2"
    private const val KEY = "state_json"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Synchronized
    fun read(ctx: Context): WidgetState {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return WidgetState()
        return runCatching { json.decodeFromString<WidgetState>(raw) }.getOrDefault(WidgetState())
    }

    @Synchronized
    fun write(ctx: Context, state: WidgetState) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, json.encodeToString(state))
            .apply()
    }

    @Synchronized
    fun update(ctx: Context, transform: (WidgetState) -> WidgetState) {
        write(ctx, transform(read(ctx)))
    }
}
