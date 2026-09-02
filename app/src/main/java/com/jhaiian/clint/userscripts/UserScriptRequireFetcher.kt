package com.jhaiian.clint.userscripts

import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

object UserScriptRequireFetcher {
    private val client = OkHttpClient()

    fun fetchAssets(metadata: UserScriptMetadata): String {
        val root = JSONObject()
        val requiresArray = JSONArray()
        for (url in metadata.requires) {
            val source = runCatching {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
                    response.body.string()
                }
            }.getOrNull()
            if (source != null) {
                requiresArray.put(JSONObject().put("url", url).put("source", source))
            }
        }
        root.put("requires", requiresArray)

        val resourcesArray = JSONArray()
        for ((name, url) in metadata.resources) {
            val fetched = runCatching {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
                    val mime = response.header("Content-Type")?.substringBefore(";") ?: "application/octet-stream"
                    val bytes = response.body.bytes()
                    Pair(mime, Base64.encodeToString(bytes, Base64.NO_WRAP))
                }
            }.getOrNull()
            if (fetched != null) {
                resourcesArray.put(
                    JSONObject()
                        .put("name", name)
                        .put("url", url)
                        .put("mime", fetched.first)
                        .put("base64", fetched.second)
                )
            }
        }
        root.put("resources", resourcesArray)
        return root.toString()
    }

    fun fetchAll(urls: List<String>): String {
        val array = JSONArray()
        for (url in urls) {
            val source = runCatching {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
                    response.body.string()
                }
            }.getOrNull()
            if (source != null) {
                array.put(JSONObject().put("url", url).put("source", source))
            }
        }
        return JSONObject().put("requires", array).put("resources", JSONArray()).toString()
    }

    fun parseSources(cache: String): List<String> {
        if (cache.isBlank()) return emptyList()
        return runCatching {
            val requiresArray = if (cache.trimStart().startsWith("{")) {
                JSONObject(cache).optJSONArray("requires") ?: JSONArray()
            } else {
                JSONArray(cache)
            }
            (0 until requiresArray.length()).map { requiresArray.getJSONObject(it).getString("source") }
        }.getOrElse { emptyList() }
    }

    data class ResourceAsset(val name: String, val url: String, val mime: String, val base64: String)

    fun parseResources(cache: String): List<ResourceAsset> {
        if (cache.isBlank() || !cache.trimStart().startsWith("{")) return emptyList()
        return runCatching {
            val resourcesArray = JSONObject(cache).optJSONArray("resources") ?: JSONArray()
            (0 until resourcesArray.length()).map {
                val entry = resourcesArray.getJSONObject(it)
                ResourceAsset(
                    name = entry.getString("name"),
                    url = entry.getString("url"),
                    mime = entry.optString("mime", "application/octet-stream"),
                    base64 = entry.getString("base64")
                )
            }
        }.getOrElse { emptyList() }
    }
}
