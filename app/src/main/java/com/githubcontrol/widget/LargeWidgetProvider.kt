package com.githubcontrol.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.githubcontrol.utils.Logger

/** 4x3+ dashboard widget — full status, live upload progress, recent activity, action grid. */
class LargeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        val state = WidgetStore.read(context)
        val v = WidgetRender.renderLarge(context, state)
        ids.forEach { mgr.updateAppWidget(it, v) }
        Logger.i("Widget", "large rendered ×${ids.size}")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WidgetIntents.BROADCAST_REFRESH) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(android.content.ComponentName(context, LargeWidgetProvider::class.java))
            onUpdate(context, mgr, ids)
        }
    }
}
