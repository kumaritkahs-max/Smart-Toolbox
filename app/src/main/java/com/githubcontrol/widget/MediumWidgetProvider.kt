package com.githubcontrol.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.githubcontrol.utils.Logger

/** 4x2 status + actions widget — repo name, status, last commit, Upload / Sync / Open. */
class MediumWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        val state = WidgetStore.read(context)
        val v = WidgetRender.renderMedium(context, state)
        ids.forEach { mgr.updateAppWidget(it, v) }
        Logger.i("Widget", "medium rendered ×${ids.size}")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WidgetIntents.BROADCAST_REFRESH) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(android.content.ComponentName(context, MediumWidgetProvider::class.java))
            onUpdate(context, mgr, ids)
        }
    }
}
