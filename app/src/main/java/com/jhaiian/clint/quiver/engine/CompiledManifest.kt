package com.jhaiian.clint.quiver.engine

import java.io.File
import org.json.JSONArray
import org.json.JSONObject

data class CompiledManifestEntry(
    val id: Long,
    val name: String,
    val downloadUrl: String,
    val isCustom: Boolean,
    val isEnabled: Boolean,
    val contentFingerprint: String,
)

data class CompiledManifestData(
    val entries: List<CompiledManifestEntry>,
    val totalRuleLines: Long,
    val outputFileSizeBytes: Long,
    val durationMs: Long,
    val compiledAtMillis: Long = System.currentTimeMillis(),
)

object CompiledManifest {

    fun write(file: File, data: CompiledManifestData) {
        val entriesJson = JSONArray()
        for (entry in data.entries) {
            entriesJson.put(
                JSONObject().apply {
                    put("id", entry.id)
                    put("name", entry.name)
                    put("downloadUrl", entry.downloadUrl)
                    put("isCustom", entry.isCustom)
                    put("isEnabled", entry.isEnabled)
                    put("contentFingerprint", entry.contentFingerprint)
                }
            )
        }
        val root = JSONObject().apply {
            put("entries", entriesJson)
            put("totalRuleLines", data.totalRuleLines)
            put("outputFileSizeBytes", data.outputFileSizeBytes)
            put("durationMs", data.durationMs)
            put("compiledAtMillis", data.compiledAtMillis)
        }
        file.writeText(root.toString())
    }

    fun read(file: File): CompiledManifestData? {
        if (!file.exists()) return null
        return try {
            val root = JSONObject(file.readText())
            val entriesJson = root.getJSONArray("entries")
            val entries = (0 until entriesJson.length()).map { i ->
                val e = entriesJson.getJSONObject(i)
                CompiledManifestEntry(
                    id = e.getLong("id"),
                    name = e.getString("name"),
                    downloadUrl = e.getString("downloadUrl"),
                    isCustom = e.getBoolean("isCustom"),
                    isEnabled = e.getBoolean("isEnabled"),
                    contentFingerprint = e.getString("contentFingerprint"),
                )
            }
            CompiledManifestData(
                entries = entries,
                totalRuleLines = root.optLong("totalRuleLines", 0L),
                outputFileSizeBytes = root.optLong("outputFileSizeBytes", 0L),
                durationMs = root.optLong("durationMs", 0L),
                compiledAtMillis = root.optLong("compiledAtMillis", 0L),
            )
        } catch (e: org.json.JSONException) {
            null
        }
    }
}
