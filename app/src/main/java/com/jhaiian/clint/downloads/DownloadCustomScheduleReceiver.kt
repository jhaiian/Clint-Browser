package com.jhaiian.clint.downloads

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DownloadCustomScheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_ID, -1)
        if (id == -1) return
        val pendingResult = goAsync()
        val job = ClintDownloadManager.init(context)
        job.invokeOnCompletion {
            ClintDownloadManager.resume(context, id)
            pendingResult.finish()
        }
    }

    companion object {
        const val EXTRA_ID = "id"
    }
}
