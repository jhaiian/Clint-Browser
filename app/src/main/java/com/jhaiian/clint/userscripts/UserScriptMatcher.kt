package com.jhaiian.clint.userscripts

object UserScriptMatcher {

    private val MATCH_PATTERN_REGEX = Regex("^(\\*|https?|ftp|file)://(\\*|(?:\\*\\.)?[^/*]+|)(/.*)?$")

    private fun escapeJsRegex(s: String): String {
        val builder = StringBuilder()
        for (c in s) {
            if (c == '.' || c == '+' || c == '?' || c == '^' || c == '$' ||
                c == '{' || c == '}' || c == '(' || c == ')' || c == '|' ||
                c == '[' || c == ']' || c == '\\' || c == '/'
            ) {
                builder.append('\\')
            }
            builder.append(c)
        }
        return builder.toString()
    }

    fun matchPatternToRegexSource(pattern: String): String? {
        val trimmed = pattern.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed == "<all_urls>") return "^https?://.*$"
        val found = MATCH_PATTERN_REGEX.find(trimmed) ?: return null
        val (scheme, host, pathRaw) = found.destructured
        val schemePattern = if (scheme == "*") "https?" else escapeJsRegex(scheme)
        val hostPattern = when {
            host == "*" -> "[^/]*"
            host.startsWith("*.") -> "(?:[^/]*\\.)?" + escapeJsRegex(host.removePrefix("*."))
            else -> escapeJsRegex(host)
        }
        val pathPattern = if (pathRaw.isEmpty()) "/.*" else pathRaw.split("*").joinToString(".*") { escapeJsRegex(it) }
        return "^$schemePattern://$hostPattern(?::\\d+)?$pathPattern$"
    }

    fun globToRegexSource(pattern: String): String {
        val trimmed = pattern.trim()
        if (trimmed.length >= 2 && trimmed.startsWith("/") && trimmed.endsWith("/")) {
            return trimmed.substring(1, trimmed.length - 1)
        }
        return "^" + trimmed.split("*").joinToString(".*") { escapeJsRegex(it) } + "$"
    }
}
