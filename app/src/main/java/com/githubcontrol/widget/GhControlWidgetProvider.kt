package com.githubcontrol.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.RemoteViews
import com.githubcontrol.MainActivity
import com.githubcontrol.R
import com.githubcontrol.utils.Logger

/**
 * Lightweight home-screen widget. Two buttons: open the app, or trigger a refresh
 * (which just re-renders the widget with the latest cached subtitle). The subtitle
 * is read from a tiny shared-prefs key that the app updates on important events
 * (login change, sync completed, upload finished). If nothing has been written
 * yet we fall back to a friendly default — so the widget never shows "loading".
 */
class GhControlWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        val views = buildViews(context)
        ids.forEach { mgr.updateAppWidget(it, views) }
        Logger.i("Widget", "rendered for ${ids.size} instance(s)")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, GhControlWidgetProvider::class.java))
            onUpdate(context, mgr, ids)
        }
    }

    private fun buildViews(ctx: Context): RemoteViews {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val subtitle = prefs.getString(KEY_SUBTITLE, ctx.getString(R.string.widget_subtitle))
            ?: ctx.getString(R.string.widget_subtitle)

        val views = RemoteViews(ctx.packageName, R.layout.widget_control)
        views.setTextViewText(R.id.widget_title, ctx.getString(R.string.widget_label))
        views.setTextViewText(R.id.widget_subtitle, subtitle)

        val openIntent = Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val openPi = PendingIntent.getActivity(
            ctx, 1, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, openPi)
        views.setOnClickPendingIntent(R.id.widget_open, openPi)

        val refreshIntent = Intent(ctx, GhControlWidgetProvider::class.java).setAction(ACTION_REFRESH)
        val refreshPi = PendingIntent.getBroadcast(
            ctx, 2, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_refresh, refreshPi)
        return views
    }

    companion object {
        private const val PREFS = "widget_state"
        private const val KEY_SUBTITLE = "subtitle"
        private const val ACTION_REFRESH = "com.githubcontrol.widget.REFRESH"

        /** Apps can call this from anywhere to update what the widget shows. */
        fun publishStatus(context: Context, subtitle: String) {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_SUBTITLE, subtitle).apply()
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, GhControlWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                mgr.updateAppWidget(ids, RemoteViews(context.packageName, R.layout.widget_control).also { v ->
                    v.setTextViewText(R.id.widget_subtitle, subtitle)
                    v.setTextViewText(R.id.widget_title, context.getString(R.string.widget_label))
                })
            }
        }
    }
}
