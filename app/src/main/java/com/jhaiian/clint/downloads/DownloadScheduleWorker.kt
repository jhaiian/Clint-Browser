package com.jhaiian.clint.downloads

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters

class DownloadScheduleWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        setForeground(foregroundInfo())
        ClintDownloadManager.init(applicationContext).join()
        DownloadScheduleMonitor.reconcile(applicationContext)
        return Result.success()
    }

    private fun foregroundInfo(): ForegroundInfo {
        ClintDownloadManager.createNotificationChannel(applicationContext)
        val notification = DownloadNotificationHelper.buildSummaryNotification(applicationContext, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(DownloadForegroundService.FOREGROUND_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(DownloadForegroundService.FOREGROUND_ID, notification)
        }
    }
}
