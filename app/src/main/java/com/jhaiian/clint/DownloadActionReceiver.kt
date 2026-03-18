package com.jhaiian.clint

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DownloadActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CANCEL = "com.jhaiian.clint.DOWNLOAD_CANCEL"
        const val EXTRA_ID = "download_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CANCEL) {
            val id = intent.getIntExtra(EXTRA_ID, -1)
            if (id != -1) ClintDownloadManager.cancel(context, id)
        }
    }
}
