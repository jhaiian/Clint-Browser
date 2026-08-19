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
