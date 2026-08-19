package com.jhaiian.clint.quiver.engine

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import java.util.Base64
import java.util.Collections
import java.util.WeakHashMap

object QuiverGuardWebIntegration {

    private const val JS_BRIDGE_NAME = "__qgBridge"

    @Volatile private var bootstrapScript: String? = null

    private fun bootstrapScript(context: Context): String =
        bootstrapScript ?: synchronized(this) {
            bootstrapScript ?: run {
                val body = context.assets.open("JavaScript/quiver_guard_cosmetic.js")
                    .bufferedReader().use { it.readText() }

                "(function(){\n$body\n})();".also { bootstrapScript = it }
            }
        }

    fun initialize(context: Context): QuiverGuardEngine.PreloadResult {
        return QuiverGuardEngine.preload(context)
    }

    fun onCompileComplete(context: Context) {
        QuiverGuardEngine.activate(QuiverGuardPaths.databaseFile(context).absolutePath)
    }

    fun shouldInterceptRequest(
        context: Context,
        request: WebResourceRequest,
        pageUrl: String?,
        tabId: String,
        isQuiverGuardEnabled: Boolean
    ): WebResourceResponse? {
        if (!isQuiverGuardEnabled) return null
        if (!QuiverGuardEngine.isLoaded) {
            QuiverGuardEngine.preload(context)
            if (!QuiverGuardEngine.isLoaded) return null
        }

        val requestUrl = request.url ?: return null
        val scheme = requestUrl.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https" && scheme != "ws" && scheme != "wss") return null

        val urlStr = requestUrl.toString()
        val resourceType = ResourceTypeDetector.detect(request)

        val sourceUrl = pageUrl ?: urlStr

        val check = QuiverGuardEngine.checkNetworkRequest(urlStr, sourceUrl, resourceType, request.method ?: "GET") ?: return null

        if (check.matched) {
            BlockedRequestCounter.increment(tabId)

            check.redirectDataUrl?.let { decodeDataUrlResponse(it) }?.let { return it }
            return BlockedResponse.forResourceType(resourceType)
        }

        val addHeaders = LinkedHashMap<String, String>()
        check.csp?.let { addHeaders["Content-Security-Policy"] = it }

        if (check.rewrittenUrl != null || addHeaders.isNotEmpty()) {
            val modification = PassthroughFetcher.Modification(
                newUrl = check.rewrittenUrl,
                addResponseHeaders = addHeaders,
            )
            val modifiedResponse = PassthroughFetcher.fetch(request, modification)
            if (modifiedResponse != null) return modifiedResponse
        }

        return null
    }

    private fun decodeDataUrlResponse(dataUrl: String): WebResourceResponse? {
        if (!dataUrl.startsWith("data:")) return null
        val commaIndex = dataUrl.indexOf(',')
        if (commaIndex == -1) return null
        val meta = dataUrl.substring(5, commaIndex)
        if (!meta.endsWith(";base64")) return null
        val mime = meta.removeSuffix(";base64").ifBlank { "application/octet-stream" }
        val bytes = try {
            Base64.getDecoder().decode(dataUrl.substring(commaIndex + 1))
        } catch (_: IllegalArgumentException) {
            return null
        }
        return WebResourceResponse(
            mime, "binary", 200, "OK",
            BlockedResponse.BASE_HEADERS + mapOf("Content-Type" to mime),
            bytes.inputStream()
        )
    }

    fun buildCosmeticFilterScript(
        context: Context,
        @Suppress("UNUSED_PARAMETER") pageUrl: String,
        isQuiverGuardEnabled: Boolean
    ): String? {
        if (!isQuiverGuardEnabled) return null
        return bootstrapScript(context)
    }

    fun applyCosmeticFilterScript(webView: WebView, script: String) {
        ensureBridgeInstalled(webView)
        webView.evaluateJavascript(script, null)
    }

    fun buildDocumentStartScript(
        context: Context,
        @Suppress("UNUSED_PARAMETER") pageUrl: String,
        isQuiverGuardEnabled: Boolean
    ): String? {
        if (!isQuiverGuardEnabled) return null
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return null
        return bootstrapScript(context)
    }

    fun applyDocumentStartScript(
        webView: WebView,
        tabScriptHandlers: ScriptHandlerStore,
        tabId: String,
        script: String
    ) {
        ensureBridgeInstalled(webView)
        tabScriptHandlers.remove(tabId)
        val handler = WebViewCompat.addDocumentStartJavaScript(webView, script, setOf("*"))
        tabScriptHandlers.put(tabId, handler)
    }

    fun installEarly(context: Context, webView: WebView) {
        ensureBridgeInstalled(webView)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(webView, bootstrapScript(context), setOf("*"))
        }
    }

    private val bridgedWebViews = Collections.newSetFromMap(WeakHashMap<WebView, Boolean>())

    private fun ensureBridgeInstalled(webView: WebView) {
        if (bridgedWebViews.add(webView)) {
            webView.addJavascriptInterface(QuiverGuardJsBridge, JS_BRIDGE_NAME)
        }
    }
}
