package com.jhaiian.clint.quiver.engine

import android.webkit.WebResourceRequest

object ResourceTypeDetector {

    private val SCRIPT_EXTENSIONS = setOf("js", "mjs", "jsx", "ts")
    private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "webp", "svg", "ico", "bmp", "avif")
    private val STYLESHEET_EXTENSIONS = setOf("css", "less", "scss")
    private val FONT_EXTENSIONS = setOf("woff", "woff2", "ttf", "otf", "eot")
    private val MEDIA_EXTENSIONS = setOf("mp4", "webm", "mp3", "ogg", "flac", "wav", "avi", "mov", "m4v", "m3u8", "mkv", "opus")

    fun detect(request: WebResourceRequest): String {
        val scheme = request.url.scheme?.lowercase() ?: ""

        if (scheme == "ws" || scheme == "wss") return "websocket"

        val accept = request.requestHeaders?.get("Accept") ?: ""
        val destination = request.requestHeaders?.get("Sec-Fetch-Dest") ?: ""
        val mode = request.requestHeaders?.get("Sec-Fetch-Mode") ?: ""

        when (destination.lowercase()) {
            "script" -> return "script"
            "style" -> return "stylesheet"
            "image", "img" -> return "image"
            "font" -> return "font"
            "media", "video", "audio" -> return "media"
            "worker", "sharedworker", "serviceworker" -> return "script"
            "iframe", "frame" -> return "sub_frame"
            "document" -> return if (request.isForMainFrame) "document" else "sub_frame"
            "object", "embed" -> return "object"
            "track" -> return "media"
            "manifest" -> return "other"
        }

        when (mode.lowercase()) {
            "cors", "no-cors" -> {
                if (accept.contains("application/json") || accept.contains("text/plain")) return "xhr"
            }
            "websocket" -> return "websocket"
            "navigate" -> return if (request.isForMainFrame) "document" else "sub_frame"
        }

        if (accept.isNotEmpty()) {
            return when {
                accept.contains("text/html") && request.isForMainFrame -> "document"
                accept.contains("text/html") -> "sub_frame"
                accept.contains("text/css") -> "stylesheet"
                accept.contains("image/") -> "image"
                accept.contains("application/javascript") || accept.contains("text/javascript") -> "script"
                accept.contains("font/") || accept.contains("application/font") -> "font"
                accept.contains("audio/") || accept.contains("video/") -> "media"
                else -> "other"
            }
        }

        val path = request.url.path?.lowercase() ?: ""
        val ext = path.substringAfterLast('.', "").substringBefore('?').substringBefore('#')
        return when {
            ext in SCRIPT_EXTENSIONS -> "script"
            ext in IMAGE_EXTENSIONS -> "image"
            ext in STYLESHEET_EXTENSIONS -> "stylesheet"
            ext in FONT_EXTENSIONS -> "font"
            ext in MEDIA_EXTENSIONS -> "media"

            else -> "xhr"
        }
    }
}
