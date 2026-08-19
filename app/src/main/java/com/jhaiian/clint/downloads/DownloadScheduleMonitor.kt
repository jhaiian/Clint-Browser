package com.jhaiian.clint.downloads

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.jhaiian.clint.settings.downloads.DownloadSettingsKeys
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

internal object DownloadScheduleMonitor {

    private const val UNIQUE_WORK_NAME = "download_schedule_check"

    internal val scheduleWaitingIds: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    fun isEnabled(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(
            DownloadSettingsKeys.PREF_SCHEDULE_ENABLED, DownloadSettingsKeys.DEFAULT_SCHEDULE_ENABLED
        )
    }

    private fun windowMinutes(context: Context): Pair<Int, Int> {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val start = prefs.getInt(
            DownloadSettingsKeys.PREF_SCHEDULE_START_MINUTES, DownloadSettingsKeys.DEFAULT_SCHEDULE_START_MINUTES
        )
        val end = prefs.getInt(
            DownloadSettingsKeys.PREF_SCHEDULE_END_MINUTES, DownloadSettingsKeys.DEFAULT_SCHEDULE_END_MINUTES
        )
        return start to end
    }

    fun isWithinWindow(context: Context): Boolean {
        if (!isEnabled(context)) return true
        val (start, end) = windowMinutes(context)
        if (start == end) return true
        val now = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
        return if (start < end) now in start until end else now >= start || now < end
    }

    private fun millisUntilNextBoundary(context: Context): Long {
        val (start, end) = windowMinutes(context)
        val cal = Calendar.getInstance()
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val boundary = if (isWithinWindow(context)) end else start
        var deltaMinutes = boundary - nowMinutes
        if (deltaMinutes <= 0) deltaMinutes += 24 * 60
        val secondsIntoMinute = cal.get(Calendar.SECOND)
        return (deltaMinutes * 60_000L - secondsIntoMinute * 1000L).coerceAtLeast(1_000L)
    }

    fun scheduleNextCheck(context: Context) {
        val workManager = WorkManager.getInstance(context)
        if (!isEnabled(context)) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }
        val (start, end) = windowMinutes(context)
        if (start == end) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }
        val request = OneTimeWorkRequestBuilder<DownloadScheduleWorker>()
            .setInitialDelay(millisUntilNextBoundary(context), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun reconcile(context: Context) {
        if (isWithinWindow(context)) {
            val toResume = ClintDownloadManager.downloadsFlow.value
                .filter { it.status == DownloadStatus.PAUSED && it.waitingForSchedule }
                .map { it.id }
            scheduleWaitingIds.clear()
            toResume.forEach { ClintDownloadManager.resume(context, it) }
            ClintDownloadManager.tryDequeueNext(context)
        } else {
            val toPause = ClintDownloadManager.downloadsFlow.value.filter {
                (it.status == DownloadStatus.QUEUED || it.status in DownloadStatus.ACTIVELY_WORKING) &&
                    it.scheduledStartAtMillis == 0L
            }
            toPause.forEach { item ->
                ClintDownloadManager.updateItem(item.id) { it.copy(waitingForSchedule = true) }
                scheduleWaitingIds.add(item.id)
                ClintDownloadManager.pause(context, item.id)
            }
        }
        scheduleNextCheck(context)
    }

    fun onScheduleChanged(context: Context) {
        reconcile(context)
    }
}
