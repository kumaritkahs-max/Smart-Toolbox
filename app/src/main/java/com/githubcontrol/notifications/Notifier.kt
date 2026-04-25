package com.githubcontrol.notifications

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.githubcontrol.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Notifier @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val nm: NotificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun upload(progress: Int, max: Int, fileName: String, indeterminate: Boolean = false) {
        val n = NotificationCompat.Builder(ctx, NotificationChannels.UPLOAD)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("Uploading $fileName")
            .setContentText("$progress / $max files")
            .setProgress(max.coerceAtLeast(1), progress, indeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        nm.notify(NID_UPLOAD, n)
    }

    fun uploadDone(success: Boolean, summary: String) {
        val n = NotificationCompat.Builder(ctx, NotificationChannels.UPLOAD)
            .setSmallIcon(if (success) android.R.drawable.stat_sys_upload_done else android.R.drawable.stat_notify_error)
            .setContentTitle(if (success) "Upload complete" else "Upload failed")
            .setContentText(summary)
            .setAutoCancel(true)
            .build()
        nm.notify(NID_UPLOAD, n)
    }

    fun alert(title: String, body: String) {
        val n = NotificationCompat.Builder(ctx, NotificationChannels.ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title).setContentText(body).setAutoCancel(true).build()
        nm.notify(NID_ALERT, n)
    }

    companion object {
        const val NID_UPLOAD = 1001
        const val NID_ALERT = 1002
        const val NID_SYNC = 1003
    }
}
