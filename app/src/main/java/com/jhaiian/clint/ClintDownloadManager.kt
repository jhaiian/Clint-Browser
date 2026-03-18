package com.jhaiian.clint

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger

object ClintDownloadManager {

    enum class DownloadStatus { DOWNLOADING, COMPLETE, FAILED, CANCELLED }

    data class DownloadItem(
        val id: Int,
        val url: String,
        val filename: String,
        val userAgent: String,
        var bytesDownloaded: Long = 0L,
        var totalBytes: Long = -1L,
        var status: DownloadStatus = DownloadStatus.DOWNLOADING,
        var file: File? = null,
        var errorMessage: String? = null
    ) {
        val progressPercent: Int
            get() = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else -1
    }

    private val CHANNEL_ID = "clint_downloads"
    private val executor = Executors.newFixedThreadPool(4)
    private val idCounter = AtomicInteger(1)
    private val futures = mutableMapOf<Int, Future<*>>()

    val downloads = mutableListOf<DownloadItem>()
    var onDownloadsChanged: (() -> Unit)? = null

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Clint Downloads",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "File download progress"
            setSound(null, null)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun enqueue(context: Context, url: String, filename: String, userAgent: String) {
        val id = idCounter.getAndIncrement()
        val item = DownloadItem(id = id, url = url, filename = filename, userAgent = userAgent)
        synchronized(downloads) { downloads.add(0, item) }
        onDownloadsChanged?.invoke()
        showProgressNotification(context, item)

        val future = executor.submit {
            runDownload(context, item)
        }
        futures[id] = future
    }

    fun cancel(context: Context, id: Int) {
        futures[id]?.cancel(true)
        futures.remove(id)
        synchronized(downloads) {
            downloads.find { it.id == id }?.let { it.status = DownloadStatus.CANCELLED }
        }
        context.getSystemService(NotificationManager::class.java).cancel(id)
        onDownloadsChanged?.invoke()
    }

    fun clearCompleted() {
        synchronized(downloads) {
            downloads.removeAll { it.status != DownloadStatus.DOWNLOADING }
        }
        onDownloadsChanged?.invoke()
    }

    private fun runDownload(context: Context, item: DownloadItem) {
        try {
            val client = OkHttpClient.Builder().build()
            val request = Request.Builder()
                .url(item.url)
                .header("User-Agent", item.userAgent)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                fail(context, item, "Server error ${response.code}")
                return
            }

            val body = response.body ?: run { fail(context, item, "Empty response"); return }
            item.totalBytes = body.contentLength()

            val destDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            destDir.mkdirs()
            val destFile = uniqueFile(destDir, item.filename)
            item.file = destFile

            val buffer = ByteArray(8192)
            var lastNotifyBytes = 0L
            body.byteStream().use { input ->
                FileOutputStream(destFile).use { output ->
                    while (true) {
                        if (Thread.currentThread().isInterrupted) {
                            destFile.delete()
                            item.status = DownloadStatus.CANCELLED
                            context.getSystemService(NotificationManager::class.java).cancel(item.id)
                            onDownloadsChanged?.invoke()
                            return
                        }
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        item.bytesDownloaded += read
                        if (item.bytesDownloaded - lastNotifyBytes > 65536) {
                            lastNotifyBytes = item.bytesDownloaded
                            showProgressNotification(context, item)
                            onDownloadsChanged?.invoke()
                        }
                    }
                }
            }

            item.status = DownloadStatus.COMPLETE
            onDownloadsChanged?.invoke()
            showCompleteNotification(context, item)
        } catch (e: Exception) {
            if (item.status != DownloadStatus.CANCELLED) {
                fail(context, item, e.message ?: "Unknown error")
            }
        }
    }

    private fun fail(context: Context, item: DownloadItem, msg: String) {
        item.status = DownloadStatus.FAILED
        item.errorMessage = msg
        onDownloadsChanged?.invoke()
        showFailedNotification(context, item)
    }

    private fun showProgressNotification(context: Context, item: DownloadItem) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val progress = item.progressPercent
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(item.filename)
            .setContentText(if (progress >= 0) "$progress%" else "Downloading…")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setProgress(100, progress.coerceAtLeast(0), progress < 0)

        val cancelIntent = Intent(context, DownloadActionReceiver::class.java).apply {
            action = DownloadActionReceiver.ACTION_CANCEL
            putExtra(DownloadActionReceiver.EXTRA_ID, item.id)
        }
        val cancelPi = PendingIntent.getBroadcast(
            context, item.id, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(0, "Cancel", cancelPi)
        nm.notify(item.id, builder.build())
    }

    private fun showCompleteNotification(context: Context, item: DownloadItem) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val file = item.file ?: run { nm.cancel(item.id); return }
        val uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) { null }

        val openIntent = if (uri != null) {
            val ext = file.extension.lowercase()
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else null

        val openPi = openIntent?.let {
            PendingIntent.getActivity(
                context, item.id + 10000, it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(item.filename)
            .setContentText("Download complete")
            .setAutoCancel(true)
            .setOngoing(false)
        if (openPi != null) {
            builder.setContentIntent(openPi)
            builder.addAction(0, "Open", openPi)
        }
        nm.notify(item.id, builder.build())
    }

    private fun showFailedNotification(context: Context, item: DownloadItem) {
        val nm = context.getSystemService(NotificationManager::class.java)
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(item.filename)
            .setContentText("Download failed")
            .setAutoCancel(true)
            .build()
            .let { nm.notify(item.id, it) }
    }

    private fun uniqueFile(dir: File, name: String): File {
        var file = File(dir, name)
        if (!file.exists()) return file
        val dot = name.lastIndexOf('.')
        val base = if (dot >= 0) name.substring(0, dot) else name
        val ext  = if (dot >= 0) name.substring(dot) else ""
        var i = 1
        while (file.exists()) { file = File(dir, "$base($i)$ext"); i++ }
        return file
    }
}
