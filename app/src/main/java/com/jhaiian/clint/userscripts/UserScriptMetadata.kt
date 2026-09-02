package com.jhaiian.clint.userscripts

data class UserScriptMetadata(
    val name: String,
    val namespace: String,
    val version: String,
    val description: String,
    val author: String,
    val icon: String,
    val matches: List<String>,
    val includes: List<String>,
    val excludes: List<String>,
    val excludeMatches: List<String>,
    val requires: List<String>,
    val resources: List<Pair<String, String>>,
    val connects: List<String>,
    val grants: List<String>,
    val runAt: String,
    val injectInto: String,
    val noframes: Boolean,
    val unwrap: Boolean,
    val hasMetadataBlock: Boolean,
    val updateUrl: String = "",
    val downloadUrl: String = ""
) {
    val grantsNone: Boolean get() = grants.size == 1 && grants[0].equals("none", ignoreCase = true)
}

object UserScriptMetadataParser {

    private val HEADER_REGEX = Regex("//\\s*==UserScript==([\\s\\S]*?)//\\s*==/UserScript==")
    private val TAG_REGEX = Regex("^//\\s*@(\\S+)\\s*(.*)$")

    fun parse(code: String, fallbackName: String): UserScriptMetadata {
        val headerMatch = HEADER_REGEX.find(code)
        val values = mutableMapOf<String, MutableList<String>>()
        if (headerMatch != null) {
            headerMatch.groupValues[1].lineSequence().forEach { rawLine ->
                val tagMatch = TAG_REGEX.find(rawLine.trim()) ?: return@forEach
                val key = tagMatch.groupValues[1].lowercase()
                val value = tagMatch.groupValues[2].trim()
                values.getOrPut(key) { mutableListOf() }.add(value)
            }
        }

        fun single(key: String, default: String = ""): String =
            values[key]?.firstOrNull()?.takeIf { it.isNotBlank() } ?: default

        fun list(key: String): List<String> = values[key]?.filter { it.isNotBlank() } ?: emptyList()

        val runAt = when (single("run-at")) {
            "document-start" -> "document-start"
            "document-end", "document-body" -> "document-end"
            "context-menu" -> "context-menu"
            else -> "document-idle"
        }

        val resources = list("resource").mapNotNull { entry ->
            val parts = entry.trim().split(Regex("\\s+"), limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }

        val injectInto = when (single("inject-into").lowercase()) {
            "content" -> "content"
            "auto" -> "auto"
            else -> "page"
        }

        return UserScriptMetadata(
            name = single("name", fallbackName),
            namespace = single("namespace"),
            version = single("version"),
            description = single("description"),
            author = single("author"),
            icon = single("icon").ifBlank { single("iconurl") },
            matches = list("match"),
            includes = list("include"),
            excludes = list("exclude"),
            excludeMatches = list("exclude-match"),
            requires = list("require"),
            resources = resources,
            connects = list("connect"),
            grants = list("grant"),
            runAt = runAt,
            injectInto = injectInto,
            noframes = values.containsKey("noframes"),
            unwrap = values.containsKey("unwrap"),
            hasMetadataBlock = headerMatch != null,
            updateUrl = single("updateurl"),
            downloadUrl = single("downloadurl")
        )
    }
}
