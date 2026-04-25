package com.githubcontrol.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.githubcontrol.R
import com.githubcontrol.utils.Logger

/**
 * Pure renderers that build a [RemoteViews] for each widget size from a single
 * [WidgetState] snapshot. Keeping the rendering logic separate from the
 * [android.appwidget.AppWidgetProvider] subclasses lets [WidgetController]
 * push live updates without going through `onUpdate`.
 */
object WidgetRender {

    fun pushAll(ctx: Context, state: WidgetState) {
        val mgr = AppWidgetManager.getInstance(ctx)
        push(ctx, mgr, state, SmallWidgetProvider::class.java) { renderSmall(ctx, it) }
        push(ctx, mgr, state, MediumWidgetProvider::class.java) { renderMedium(ctx, it) }
        push(ctx, mgr, state, LargeWidgetProvider::class.java) { renderLarge(ctx, it) }
    }

    private fun push(
        ctx: Context,
        mgr: AppWidgetManager,
        state: WidgetState,
        providerClass: Class<*>,
        builder: (WidgetState) -> RemoteViews
    ) {
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, providerClass))
        if (ids.isEmpty()) return
        val views = builder(state)
        runCatching { mgr.updateAppWidget(ids, views) }
            .onFailure { Logger.w("Widget", "${providerClass.simpleName} update failed: ${it.message}") }
    }

    fun renderSmall(ctx: Context, state: WidgetState): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget_small)
        v.setTextViewText(R.id.widget_pill, statusLabel(state))
        v.setInt(R.id.widget_pill, "setBackgroundResource", pillDrawable(state))
        v.setTextViewText(R.id.widget_repo, state.repoName.ifBlank { ctx.getString(R.string.widget_label) })
        v.setOnClickPendingIntent(R.id.widget_root, WidgetIntents.open(ctx, 11, WidgetIntents.ACTION_OPEN))
        v.setOnClickPendingIntent(R.id.widget_sync, WidgetIntents.open(ctx, 12, WidgetIntents.ACTION_SYNC))
        return v
    }

    fun renderMedium(ctx: Context, state: WidgetState): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget_medium)
        v.setTextViewText(R.id.widget_repo, state.repoName.ifBlank { ctx.getString(R.string.widget_label) })
        v.setTextViewText(R.id.widget_pill, statusLabel(state))
        v.setInt(R.id.widget_pill, "setBackgroundResource", pillDrawable(state))
        val sub = when {
            state.uploadRunning -> "${state.uploadDone}/${state.uploadTotal} · ${state.uploadCurrentFile.takeLast(40)}"
            state.lastCommit.isNotBlank() -> state.lastCommit.lineSequence().first().take(80)
            else -> state.subtitle
        }
        v.setTextViewText(R.id.widget_subtitle, sub)
        if (state.uploadRunning) {
            v.setViewVisibility(R.id.widget_progress, View.VISIBLE)
            v.setProgressBar(R.id.widget_progress, 100, state.uploadProgressPct.coerceIn(0, 100), false)
        } else {
            v.setViewVisibility(R.id.widget_progress, View.GONE)
        }
        v.setOnClickPendingIntent(R.id.widget_root, WidgetIntents.open(ctx, 21, WidgetIntents.ACTION_OPEN))
        v.setOnClickPendingIntent(R.id.widget_upload, WidgetIntents.open(ctx, 22, WidgetIntents.ACTION_UPLOAD))
        v.setOnClickPendingIntent(R.id.widget_sync, WidgetIntents.open(ctx, 23, WidgetIntents.ACTION_SYNC))
        v.setOnClickPendingIntent(R.id.widget_open, WidgetIntents.open(ctx, 24, WidgetIntents.ACTION_OPEN))
        return v
    }

    fun renderLarge(ctx: Context, state: WidgetState): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget_large)
        v.setTextViewText(R.id.widget_title, ctx.getString(R.string.widget_label))
        v.setTextViewText(R.id.widget_pill, statusLabel(state))
        v.setInt(R.id.widget_pill, "setBackgroundResource", pillDrawable(state))
        v.setTextViewText(R.id.widget_repo, state.repoName)
        v.setTextViewText(
            R.id.widget_subtitle,
            state.lastCommit.ifBlank { state.subtitle }.lineSequence().first().take(100)
        )
        if (state.uploadRunning && state.uploadTotal > 0) {
            v.setViewVisibility(R.id.widget_progress_block, View.VISIBLE)
            v.setTextViewText(
                R.id.widget_progress_label,
                "Uploading ${state.uploadDone}/${state.uploadTotal} · ${state.uploadProgressPct}%"
            )
            v.setProgressBar(R.id.widget_progress, 100, state.uploadProgressPct.coerceIn(0, 100), false)
        } else {
            v.setViewVisibility(R.id.widget_progress_block, View.GONE)
        }
        val recent = if (state.recent.isEmpty()) "No recent activity yet — tap any button to start."
                     else state.recent.take(4).joinToString("\n") { "• $it" }
        v.setTextViewText(R.id.widget_pinned, recent)

        v.setOnClickPendingIntent(R.id.widget_root, WidgetIntents.open(ctx, 31, WidgetIntents.ACTION_OPEN))
        v.setOnClickPendingIntent(R.id.widget_upload, WidgetIntents.open(ctx, 32, WidgetIntents.ACTION_UPLOAD))
        v.setOnClickPendingIntent(R.id.widget_sync, WidgetIntents.open(ctx, 33, WidgetIntents.ACTION_SYNC))
        v.setOnClickPendingIntent(R.id.widget_pull, WidgetIntents.open(ctx, 34, WidgetIntents.ACTION_PULL))
        v.setOnClickPendingIntent(R.id.widget_push, WidgetIntents.open(ctx, 35, WidgetIntents.ACTION_PUSH))
        v.setOnClickPendingIntent(R.id.widget_open, WidgetIntents.open(ctx, 36, WidgetIntents.ACTION_OPEN))
        return v
    }

    private fun statusLabel(s: WidgetState): String = when (s.status) {
        "synced"  -> "● synced"
        "pending" -> "▲ pending"
        "busy"    -> "↻ working"
        "failed"  -> "✖ failed"
        else      -> s.statusLabel.ifBlank { "● tap" }
    }

    private fun pillDrawable(s: WidgetState): Int = when (s.status) {
        "synced"  -> R.drawable.widget_pill_synced
        "pending" -> R.drawable.widget_pill_pending
        "busy"    -> R.drawable.widget_pill_busy
        "failed"  -> R.drawable.widget_pill_failed
        else      -> R.drawable.widget_pill_synced
    }
}
