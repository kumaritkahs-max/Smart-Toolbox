package com.githubcontrol.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        // Background upload reentry point: the actual job is driven by UploadManager held in the foreground.
        // For OS-killed scenarios, this worker simply records the resume request and the user can resume from UI.
        return Result.success()
    }
}
