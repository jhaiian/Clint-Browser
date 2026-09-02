package com.jhaiian.clint.userscripts

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
import java.io.ByteArrayOutputStream

sealed class UserScriptUpdateItemResult {
    data class UpToDate(val script: UserScript) : UserScriptUpdateItemResult()
    data class Updated(
        val script: UserScript,
        val newCode: String,
        val newRequiresCache: String,
        val newEtag: String?,
        val newLastModified: String?
    ) : UserScriptUpdateItemResult()
    data class Failed(val script: UserScript, val message: String) : UserScriptUpdateItemResult()
}

sealed class UserScriptUpdateEvent {
    data class CheckingScript(val script: UserScript, val index: Int, val total: Int) : UserScriptUpdateEvent()
    data class DownloadingScript(val script: UserScript, val bytesRead: Long, val totalBytes: Long) : UserScriptUpdateEvent()
    data class ItemComplete(val result: UserScriptUpdateItemResult) : UserScriptUpdateEvent()
}

internal object UserScriptUpdateChecker {

    private const val PROGRESS_EMIT_INTERVAL_MS = 80L

    fun checkAndUpdateAll(
        context: Context,
        scripts: List<UserScript>,
        forceUpdate: Boolean = false
    ): Flow<UserScriptUpdateEvent> = flow {
        val appContext = context.applicationContext
        val updatable = scripts.filter { !it.isLocal }
        val total = updatable.size

        for ((index, script) in updatable.withIndex()) {
            currentCoroutineContext().ensureActive()
            emit(UserScriptUpdateEvent.CheckingScript(script, index, total))

            val result = tryCheckAndUpdate(appContext, script, forceUpdate) { bytesRead, totalBytes ->
                emit(UserScriptUpdateEvent.DownloadingScript(script, bytesRead, totalBytes))
            }
            currentCoroutineContext().ensureActive()
            emit(UserScriptUpdateEvent.ItemComplete(result))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun tryCheckAndUpdate(
        context: Context,
        script: UserScript,
        forceUpdate: Boolean,
        onDownloadProgress: suspend (Long, Long) -> Unit
    ): UserScriptUpdateItemResult {
        val url = resolveUpdateUrl(script)
            ?: return UserScriptUpdateItemResult.Failed(script, context.getString(R.string.user_scripts_item_local_no_update))

        return try {
            val cookie = try {
                CookieManager.getInstance().getCookie(url)
            } catch (_: Exception) {
                null
            }
            val userAgent = WebSettings.getDefaultUserAgent(context)

            val requestBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Accept", "text/javascript, application/javascript, text/plain, */*;q=0.8")

            if (!cookie.isNullOrBlank()) {
                requestBuilder.header("Cookie", cookie)
            }

            if (!forceUpdate) {
                if (!script.etag.isNullOrBlank()) {
                    requestBuilder.header("If-None-Match", script.etag)
                } else if (!script.lastModified.isNullOrBlank()) {
                    requestBuilder.header("If-Modified-Since", script.lastModified)
                }
            }

            val call = ClintDownloadManager.httpClient.newCall(requestBuilder.build())
            currentCoroutineContext()[Job]?.invokeOnCompletion { call.cancel() }

            call.execute().use { response ->
                if (response.code == 304) {
                    return UserScriptUpdateItemResult.UpToDate(script)
                }

                if (!response.isSuccessful) {
                    return UserScriptUpdateItemResult.Failed(
                        script,
                        context.getString(R.string.quiver_guard_download_error_http, response.code)
                    )
                }

                val body = response.body

                val responseEtag = response.header("ETag")
                val responseLastModified = response.header("Last-Modified")

                if (!forceUpdate && !responseEtag.isNullOrBlank() && responseEtag == script.etag) {
                    return UserScriptUpdateItemResult.UpToDate(script)
                }

                val totalBytes = body.contentLength()
                var bytesRead = 0L
                var lastEmitMillis = 0L
                onDownloadProgress(0L, totalBytes)

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
                        if (now - lastEmitMillis >= PROGRESS_EMIT_INTERVAL_MS) {
                            lastEmitMillis = now
                            onDownloadProgress(bytesRead, totalBytes)
                        }
                    }
                }
                currentCoroutineContext().ensureActive()

                val newCode = output.toString("UTF-8")
                val lowered = newCode.trimStart().lowercase()
                if (newCode.isBlank() || lowered.startsWith("<!doctype html") || lowered.startsWith("<html")) {
                    return UserScriptUpdateItemResult.Failed(script, context.getString(R.string.user_scripts_add_error_invalid_format))
                }

                if (!forceUpdate && newCode == script.code) {
                    return UserScriptUpdateItemResult.UpToDate(script)
                }

                val newMeta = UserScriptMetadataParser.parse(newCode, "")
                val newRequiresCache = if (newMeta.requires.isEmpty() && newMeta.resources.isEmpty()) "" else UserScriptRequireFetcher.fetchAssets(newMeta)

                UserScriptUpdateItemResult.Updated(script, newCode, newRequiresCache, responseEtag, responseLastModified)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            UserScriptUpdateItemResult.Failed(script, context.getString(R.string.quiver_guard_download_error_network))
        }
    }

    private fun resolveUpdateUrl(script: UserScript): String? {
        val metadata = UserScriptMetadataParser.parse(script.code, "")
        return metadata.downloadUrl.takeIf { it.isNotBlank() }
            ?: metadata.updateUrl.takeIf { it.isNotBlank() }
            ?: script.sourceUrl?.takeIf { it.isNotBlank() }
    }
}
