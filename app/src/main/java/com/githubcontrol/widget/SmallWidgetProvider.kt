package com.githubcontrol.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.githubcontrol.utils.Logger

/** 2x1 quick-action widget — sync button + status pill. */
class SmallWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        val state = WidgetStore.read(context)
        val v = WidgetRender.renderSmall(context, state)
        ids.forEach { mgr.updateAppWidget(it, v) }
        Logger.i("Widget", "small rendered ×${ids.size}")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WidgetIntents.BROADCAST_REFRESH) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(android.content.ComponentName(context, SmallWidgetProvider::class.java))
            onUpdate(context, mgr, ids)
        }
    }
}
