package com.jhaiian.clint.quiver

import android.content.Context
import android.os.SystemClock
import android.webkit.CookieManager
import android.webkit.WebSettings
import com.jhaiian.clint.R
import com.jhaiian.clint.downloads.ClintDownloadManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

sealed class FilterListUpdateItemResult {

    data class Skipped(val filterList: FilterList) : FilterListUpdateItemResult()

    data class UpToDate(val filterList: FilterList) : FilterListUpdateItemResult()
    data class Updated(
        val filterList: FilterList,
        val newRuleCount: Long,
        val newFileSizeBytes: Long,
        val newEtag: String?,
        val newLastModified: String?
    ) : FilterListUpdateItemResult()
    data class Failed(val filterList: FilterList, val message: String) : FilterListUpdateItemResult()
}

sealed class FilterListUpdateEvent {
    data class CheckingList(
        val filterList: FilterList,
        val index: Int,
        val total: Int
    ) : FilterListUpdateEvent()
    data class DownloadingList(
        val filterList: FilterList,
        val bytesRead: Long,
        val totalBytes: Long
    ) : FilterListUpdateEvent()
    data class ItemComplete(val result: FilterListUpdateItemResult) : FilterListUpdateEvent()
}

internal object FilterListUpdateChecker {

    private const val MIN_VALID_FILE_BYTES = 100L
    private const val MIN_VALID_LINE_COUNT = 3
    private const val PROGRESS_EMIT_INTERVAL_MS = 80L

    fun checkAndUpdateAll(
        context: Context,
        filterLists: List<FilterList>,
        forceUpdate: Boolean = false
    ): Flow<FilterListUpdateEvent> = flow {
        val appContext = context.applicationContext
        val downloadedLists = filterLists.filter { it.isDownloaded && !it.isLocal }
        val total = downloadedLists.size

        for ((index, filterList) in downloadedLists.withIndex()) {
            currentCoroutineContext().ensureActive()
            emit(FilterListUpdateEvent.CheckingList(filterList, index, total))

            val result = tryCheckAndUpdate(appContext, filterList, forceUpdate) { bytesRead, totalBytes ->
                emit(FilterListUpdateEvent.DownloadingList(filterList, bytesRead, totalBytes))
            }
            currentCoroutineContext().ensureActive()
            emit(FilterListUpdateEvent.ItemComplete(result))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun tryCheckAndUpdate(
        context: Context,
        filterList: FilterList,
        forceUpdate: Boolean = false,
        onDownloadProgress: suspend (Long, Long) -> Unit
    ): FilterListUpdateItemResult {
        val targetFile = FilterListDownloader.localFileFor(context, filterList.id)
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.update.part")

        return try {
            val cookie = try {
                CookieManager.getInstance().getCookie(filterList.downloadUrl)
            } catch (_: Exception) {
                null
            }
            val userAgent = WebSettings.getDefaultUserAgent(context)

            val requestBuilder = Request.Builder()
                .url(filterList.downloadUrl)
                .header("User-Agent", userAgent)
                .header("Accept", "text/plain, */*;q=0.8")

            if (!cookie.isNullOrBlank()) {
                requestBuilder.header("Cookie", cookie)
            }

            if (!forceUpdate) {
                if (!filterList.etag.isNullOrBlank()) {
                    requestBuilder.header("If-None-Match", filterList.etag)
                } else if (!filterList.lastModified.isNullOrBlank()) {
                    requestBuilder.header("If-Modified-Since", filterList.lastModified)
                }
            }

            val call = ClintDownloadManager.httpClient.newCall(requestBuilder.build())
            currentCoroutineContext()[Job]?.invokeOnCompletion { call.cancel() }

            call.execute().use { response ->
                if (response.code == 304) {
                    return FilterListUpdateItemResult.UpToDate(filterList)
                }

                if (!response.isSuccessful) {
                    return FilterListUpdateItemResult.Failed(
                        filterList,
                        context.getString(R.string.quiver_guard_download_error_http, response.code)
                    )
                }

                val body = response.body

                val responseEtag = response.header("ETag")
                val responseLastModified = response.header("Last-Modified")

                if (!forceUpdate &&
                    !responseEtag.isNullOrBlank() &&
                    responseEtag == filterList.etag &&
                    !filterList.etag.isNullOrBlank()
                ) {
                    return FilterListUpdateItemResult.UpToDate(filterList)
                }

                val totalBytes = body.contentLength()
                var bytesRead = 0L
                var lastEmitMillis = 0L
                tempFile.parentFile?.mkdirs()
                onDownloadProgress(0L, totalBytes)

                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            bytesRead += read
                            val now = SystemClock.elapsedRealtime()
                            if (now - lastEmitMillis >= PROGRESS_EMIT_INTERVAL_MS) {
                                lastEmitMillis = now
                                onDownloadProgress(bytesRead, totalBytes)
                            }
                        }
                        output.flush()
                    }
                }

                if (!isValidFilterFile(tempFile)) {
                    tempFile.delete()
                    return FilterListUpdateItemResult.Failed(
                        filterList,
                        context.getString(R.string.filter_list_update_validation_failed, filterList.name)
                    )
                }

                val newRuleCount = countFilterRules(tempFile)
                val newFileSize = tempFile.length()

                if (targetFile.exists()) targetFile.delete()
                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }

                FilterListUpdateItemResult.Updated(
                    filterList,
                    newRuleCount,
                    newFileSize,
                    responseEtag,
                    responseLastModified
                )
            }
        } catch (e: CancellationException) {
            tempFile.delete()
            throw e
        } catch (_: Exception) {
            tempFile.delete()
            FilterListUpdateItemResult.Failed(
                filterList,
                context.getString(R.string.quiver_guard_download_error_network)
            )
        }
    }

    private fun isValidFilterFile(file: File): Boolean {
        if (!file.exists() || file.length() < MIN_VALID_FILE_BYTES) return false
        var lineCount = 0
        return try {
            file.bufferedReader().useLines { lines ->
                for (line in lines) {
                    if (line.isNotBlank()) lineCount++
                    if (lineCount >= MIN_VALID_LINE_COUNT) return@useLines
                }
            }
            lineCount > 0
        } catch (_: Exception) {
            false
        }
    }

    private fun countFilterRules(file: File): Long {
        var count = 0L
        file.bufferedReader().useLines { lines ->
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue
                if (trimmed.startsWith("!")) continue
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) continue
                count++
            }
        }
        return count
    }
}
