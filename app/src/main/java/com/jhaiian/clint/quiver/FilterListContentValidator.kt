package com.jhaiian.clint.quiver

import java.io.File

internal object FilterListContentValidator {

    private const val METADATA_SCAN_LINE_LIMIT = 200

    private const val HTML_SNIFF_CHARS = 512

    data class FilterListAnalysis(val ruleCount: Long, val metadata: Map<String, String>)

    fun looksLikeHtml(file: File): Boolean {
        val sample = try {
            file.bufferedReader().use { reader ->
                val buffer = CharArray(HTML_SNIFF_CHARS)
                val read = reader.read(buffer)
                if (read <= 0) "" else String(buffer, 0, read)
            }
        } catch (_: Exception) {
            ""
        }
        val lowered = sample.trimStart().lowercase()
        return lowered.startsWith("<!doctype html") || lowered.startsWith("<html")
    }

    fun analyzeFile(file: File): FilterListAnalysis {
        var ruleCount = 0L
        val metadata = mutableMapOf<String, String>()
        var headerEnded = false
        var scannedHeaderLines = 0
        file.bufferedReader().useLines { lines ->
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) continue
                if (trimmed.startsWith("!")) {
                    if (!headerEnded && scannedHeaderLines < METADATA_SCAN_LINE_LIMIT) {
                        val content = trimmed.removePrefix("!").trim()
                        val colonIndex = content.indexOf(':')
                        if (colonIndex > 0) {
                            val key = content.substring(0, colonIndex).trim()
                            val value = content.substring(colonIndex + 1).trim()
                            if (key.isNotEmpty() && value.isNotEmpty()) {
                                metadata[key] = value
                            }
                        }
                        scannedHeaderLines++
                    }
                    continue
                }
                headerEnded = true
                ruleCount++
            }
        }
        return FilterListAnalysis(ruleCount, metadata)
    }
}
