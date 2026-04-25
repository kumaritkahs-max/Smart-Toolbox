package com.githubcontrol.widget

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.githubcontrol.MainActivity

/**
 * One place that builds every PendingIntent the widget RemoteViews use. All
 * actions launch [MainActivity] with an `EXTRA_ACTION` string the activity
 * dispatches to its main view-model on resume — that's the only safe path
 * because uploads need the SAF picker, sync needs the active account, and we
 * can't do those reliably from a background broadcast receiver.
 */
object WidgetIntents {
    const val EXTRA_ACTION = "widget_action"
    const val ACTION_OPEN = "open"
    const val ACTION_SYNC = "sync"
    const val ACTION_UPLOAD = "upload"
    const val ACTION_PULL = "pull"
    const val ACTION_PUSH = "push"

    const val BROADCAST_REFRESH = "com.githubcontrol.widget.REFRESH_ALL"

    fun open(ctx: Context, requestCode: Int, action: String): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .putExtra(EXTRA_ACTION, action)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            ctx, requestCode, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun refreshBroadcast(ctx: Context, providerClass: Class<*>, requestCode: Int = 99): PendingIntent {
        val i = Intent(ctx, providerClass)
            .setAction(BROADCAST_REFRESH)
            .setComponent(ComponentName(ctx, providerClass))
        return PendingIntent.getBroadcast(
            ctx, requestCode, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
