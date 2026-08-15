package com.jhaiian.clint.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.preference.PreferenceManager

/**
 * Loads a favicon for [pageUrl] (or [storedFaviconUrl] if the caller already has one on hand,
 * e.g. a saved bookmark), honoring the data-saver "disable images" preference the same way the
 * old View-based BookmarksAdapter/TabAdapter did — a cache-only lookup instead of a network
 * fetch whenever data saver is on and set to block images.
 */
@Composable
fun rememberClintFavicon(pageUrl: String, storedFaviconUrl: String = ""): Bitmap? {
    val context = LocalContext.current
    var bitmap by remember(pageUrl, storedFaviconUrl) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(pageUrl, storedFaviconUrl) {
        val faviconUrl = storedFaviconUrl.ifBlank { FaviconCache.faviconUrlFor(pageUrl) }
        if (faviconUrl.isNotEmpty()) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val cacheOnly = prefs.getBoolean("data_saver_enabled", false) &&
                prefs.getBoolean("data_saver_disable_images", true)
            FaviconCache.load(context, faviconUrl, cacheOnly) { bmp -> bitmap = bmp }
        }
    }
    return bitmap
}
