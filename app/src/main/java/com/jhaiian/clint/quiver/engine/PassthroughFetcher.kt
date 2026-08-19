package com.jhaiian.clint.quiver.engine

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request

object PassthroughFetcher {

    private val client = OkHttpClient()

    private val SKIPPED_REQUEST_HEADERS = setOf("host", "accept-encoding", "content-length")

    data class Modification(

        val newUrl: String? = null,
        val addResponseHeaders: Map<String, String> = emptyMap(),
    ) {
        val isNoOp: Boolean
            get() = newUrl == null && addResponseHeaders.isEmpty()
    }

    fun fetch(request: WebResourceRequest, modification: Modification): WebResourceResponse? {
        if (modification.isNoOp) return null
        val method = request.method?.uppercase() ?: "GET"
        if (method != "GET" && method != "HEAD") return null

        return try {
            val urlToFetch = modification.newUrl ?: request.url.toString()
            val reqBuilder = Request.Builder().url(urlToFetch)
            if (method == "HEAD") reqBuilder.head() else reqBuilder.get()
            request.requestHeaders?.forEach { (name, value) ->
                if (name.lowercase() !in SKIPPED_REQUEST_HEADERS) {
                    reqBuilder.header(name, value)
                }
            }

            client.newCall(reqBuilder.build()).execute().use { resp ->
                val body = resp.body
                val bytes = body.bytes()

                val contentType = resp.header("Content-Type")
                val mimeType = contentType?.substringBefore(";")?.trim()?.takeIf { it.isNotEmpty() }
                    ?: "application/octet-stream"
                val encoding = contentType?.substringAfter("charset=", "")?.trim()?.takeIf { it.isNotEmpty() }

                val headers = LinkedHashMap<String, String>()
                for (name in resp.headers.names()) {
                    if (name.equals("Set-Cookie", ignoreCase = true)) continue
                    resp.header(name)?.let { headers[name] = it }
                }

                val setCookies = resp.headers.values("Set-Cookie")
                if (setCookies.isNotEmpty()) {
                    headers["Set-Cookie"] = setCookies.joinToString(", ")
                }

                modification.addResponseHeaders.forEach { (k, v) -> headers[k] = v }

                WebResourceResponse(
                    mimeType,
                    encoding,
                    resp.code,
                    resp.message.ifEmpty { "OK" },
                    headers,
                    bytes.inputStream()
                )
            }
        } catch (_: IOException) {
            null
        } catch (_: Exception) {
            null
        }
    }
}
