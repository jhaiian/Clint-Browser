package com.jhaiian.clint.quiver.engine

import android.webkit.JavascriptInterface
import org.json.JSONArray
import org.json.JSONException

internal object QuiverGuardJsBridge {

    @JavascriptInterface
    fun urlCosmeticResources(url: String): String = QuiverGuardEngine.urlCosmeticResourcesJson(url)

    @JavascriptInterface
    fun rewrittenUrl(url: String): String = QuiverGuardEngine.checkNetworkRequestJson(url, url, "document", "GET")

    @JavascriptInterface
    fun hiddenClassIdSelectors(classesJson: String, idsJson: String, exceptionsJson: String): String {
        val classes = parseStringArray(classesJson)
        val ids = parseStringArray(idsJson)
        val exceptions = parseStringArray(exceptionsJson)
        val selectors = QuiverGuardEngine.hiddenClassIdSelectors(classes, ids, exceptions) ?: emptyList()
        return JSONArray(selectors).toString()
    }

    private fun parseStringArray(json: String): List<String> = try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { arr.getString(it) }
    } catch (e: JSONException) {
        emptyList()
    }
}
