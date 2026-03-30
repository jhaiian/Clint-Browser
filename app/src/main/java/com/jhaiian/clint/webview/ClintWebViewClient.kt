package com.jhaiian.clint.webview

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.jhaiian.clint.activities.MainActivity
import com.jhaiian.clint.network.DohManager

class ClintWebViewClient(
    private val prefs: SharedPreferences,
    private val isActive: () -> Boolean = { true }
) : WebViewClient() {

    private val trackerHosts = setOf(
        "googletagmanager.com", "google-analytics.com", "analytics.google.com",
        "doubleclick.net", "googlesyndication.com", "adservice.google.com",
        "connect.facebook.net", "scorecardresearch.com", "quantserve.com",
        "amazon-adsystem.com", "ads.twitter.com", "static.ads-twitter.com",
        "pixel.facebook.com", "an.facebook.com", "stats.g.doubleclick.net",
        "pagead2.googlesyndication.com"
    )

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        Uri.parse(url).host?.let { host ->
            DohManager.preResolveDns(host, prefs)
        }
        if (isActive()) (view.context as? MainActivity)?.onPageStarted(url)
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        (view.context as? MainActivity)?.onTabUrlUpdated(view, url)
        if (isActive()) (view.context as? MainActivity)?.onPageFinished(url)
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
        super.doUpdateVisitedHistory(view, url, isReload)
        (view.context as? MainActivity)?.onTabUrlUpdated(view, url)
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url
        val scheme = url.scheme ?: return true

        if (scheme == "intent") {
            try {
                val intent = Intent.parseUri(url.toString(), Intent.URI_INTENT_SCHEME)
                val context = view.context
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return true
                }
                val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                if (!fallbackUrl.isNullOrEmpty()) {
                    view.loadUrl(fallbackUrl)
                    return true
                }
            } catch (_: Exception) {}
            return true
        }

        if (scheme != "http" && scheme != "https") {
            try {
                view.context.startActivity(Intent(Intent.ACTION_VIEW, url))
            } catch (_: ActivityNotFoundException) {}
            return true
        }

        val host = url.host ?: return false
        if (prefs.getBoolean("block_trackers", true)) {
            if (trackerHosts.any { host.contains(it) }) return true
        }
        DohManager.preResolveDns(host, prefs)

        if (!request.isForMainFrame) return false
        val context = view.context
        val intent = Intent(Intent.ACTION_VIEW, url).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val pm = context.packageManager
        val matches = pm.queryIntentActivities(intent, 0)
        val nonBrowserMatch = matches.any { info ->
            val pkg = info.activityInfo.packageName
            pkg != context.packageName && !isBrowserPackage(pkg)
        }
        if (nonBrowserMatch) {
            try {
                val appIntent = Intent(Intent.ACTION_VIEW, url).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(appIntent)
                return true
            } catch (_: ActivityNotFoundException) {}
        }

        return false
    }

    private fun isBrowserPackage(pkg: String): Boolean {
        return pkg.contains("browser") || pkg.contains("chrome") ||
               pkg.contains("firefox") || pkg.contains("opera") ||
               pkg.contains("samsung.android.app.internet") ||
               pkg.contains("microsoft.bing") || pkg.contains("brave")
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val host = request.url.host ?: return super.shouldInterceptRequest(view, request)
        if (prefs.getBoolean("block_trackers", true)) {
            if (trackerHosts.any { host.contains(it) }) {
                return WebResourceResponse("text/plain", "UTF-8", null)
            }
        }
        DohManager.preResolveDns(host, prefs)
        return super.shouldInterceptRequest(view, request)
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        handler.cancel()
    }
}
