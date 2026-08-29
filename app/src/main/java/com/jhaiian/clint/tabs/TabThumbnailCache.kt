package com.jhaiian.clint.tabs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.webkit.WebView
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

object TabThumbnailCache {
    private const val MAX_DIMENSION_PX = 360
    private const val DIR_NAME = "tab_thumbnails"

    private val cache = ConcurrentHashMap<String, Bitmap>()

    fun capture(tabId: String, webView: WebView, isIncognito: Boolean = false) {
        val width = webView.width
        val height = webView.height
        if (width <= 0 || height <= 0) return
        val scale = (MAX_DIMENSION_PX.toFloat() / maxOf(width, height)).coerceAtMost(1f)
        val scaledWidth = (width * scale).toInt().coerceAtLeast(1)
        val scaledHeight = (height * scale).toInt().coerceAtLeast(1)
        runCatching {
            val bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.scale(scale, scale)
            webView.draw(canvas)
            cache[tabId] = bitmap
            if (!isIncognito) writeToDisk(webView.context.applicationContext, tabId, bitmap)
        }
    }

    fun get(context: Context, tabId: String): Bitmap? {
        cache[tabId]?.let { return it }
        val fromDisk = readFromDisk(context.applicationContext, tabId) ?: return null
        cache[tabId] = fromDisk
        return fromDisk
    }

    fun evict(context: Context, tabId: String) {
        cache.remove(tabId)
        diskFile(context.applicationContext, tabId).delete()
    }

    fun clear() {
        cache.clear()
    }

    fun pruneDisk(context: Context, keepTabIds: Set<String>) {
        runCatching {
            diskDir(context.applicationContext).listFiles()?.forEach { file ->
                if (file.nameWithoutExtension !in keepTabIds) file.delete()
            }
        }
    }

    private fun diskDir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { mkdirs() }

    private fun diskFile(context: Context, tabId: String): File =
        File(diskDir(context), "$tabId.png")

    private fun writeToDisk(context: Context, tabId: String, bitmap: Bitmap) {
        runCatching {
            FileOutputStream(diskFile(context, tabId)).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
        }
    }

    private fun readFromDisk(context: Context, tabId: String): Bitmap? {
        val file = diskFile(context, tabId)
        if (!file.exists()) return null
        return runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
    }
}
