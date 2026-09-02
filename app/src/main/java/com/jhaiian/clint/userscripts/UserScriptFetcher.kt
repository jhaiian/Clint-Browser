package com.jhaiian.clint.userscripts

import android.content.Context
import android.net.Uri
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
import java.io.ByteArrayOutputStream

sealed class UserScriptFetchProgress {
    data class Progress(val bytesRead: Long, val totalBytes: Long) : UserScriptFetchProgress()
    data class Success(val code: String, val metadata: UserScriptMetadata) : UserScriptFetchProgress()
}

class UserScriptFetchException(message: String) : Exception(message)

internal object UserScriptFetcher {

    private const val PROGRESS_EMIT_INTERVAL_MS = 80L

    fun isValidUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return false
        }
        val host = Uri.parse(trimmed).host
        return !host.isNullOrBlank()
    }

    fun fetch(context: Context, url: String): Flow<UserScriptFetchProgress> = flow {
        val appContext = context.applicationContext
        val cookie = try {
            CookieManager.getInstance().getCookie(url)
        } catch (_: Exception) {
            null
        }
        val userAgent = WebSettings.getDefaultUserAgent(appContext)

        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "text/javascript, application/javascript, text/plain, */*;q=0.8")
        if (!cookie.isNullOrBlank()) {
            requestBuilder.header("Cookie", cookie)
        }

        val call = ClintDownloadManager.httpClient.newCall(requestBuilder.build())
        currentCoroutineContext()[Job]?.invokeOnCompletion { call.cancel() }

        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    throw UserScriptFetchException(
                        appContext.getString(R.string.quiver_guard_download_error_http, response.code)
                    )
                }
                val body = response.body
                val totalBytes = body.contentLength()
                var bytesRead = 0L
                var lastEmitMillis = 0L
                emit(UserScriptFetchProgress.Progress(0L, totalBytes))

                val output = ByteArrayOutputStream()
                body.byteStream().use { input ->
                    val buffer = ByteArray(8 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        bytesRead += read
                        val now = SystemClock.elapsedRealtime()
                        if (now - lastEmitMillis >= PROGRESS_EMIT_INTERVAL_MS || bytesRead == totalBytes) {
                            lastEmitMillis = now
                            emit(UserScriptFetchProgress.Progress(bytesRead, totalBytes))
                        }
                    }
                }
                currentCoroutineContext().ensureActive()

                val code = output.toString("UTF-8")
                val lowered = code.trimStart().lowercase()
                if (lowered.startsWith("<!doctype html") || lowered.startsWith("<html") || code.isBlank()) {
                    throw UserScriptFetchException(
                        appContext.getString(R.string.user_scripts_add_error_invalid_format)
                    )
                }

                val metadata = UserScriptMetadataParser.parse(code, "Untitled Script")
                emit(UserScriptFetchProgress.Success(code, metadata))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: UserScriptFetchException) {
            throw e
        } catch (_: Exception) {
            throw UserScriptFetchException(appContext.getString(R.string.quiver_guard_download_error_network))
        }
    }.flowOn(Dispatchers.IO)
}
