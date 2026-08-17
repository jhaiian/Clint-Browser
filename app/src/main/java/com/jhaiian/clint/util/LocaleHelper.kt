package com.jhaiian.clint.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.preference.PreferenceManager
import java.util.Locale

/**
 * Resolves the app's per-app language preference and wraps a [Context] with it, independent of
 * the device's system language. Used from both [android.app.Application] and every
 * [com.jhaiian.clint.base.ClintActivity] so Compose screens, dialogs, and notifications all
 * resolve strings against the same locale.
 */
object LocaleHelper {
    const val PREF_APP_LANGUAGE = "app_language"
    const val LANGUAGE_SYSTEM = "system"
    const val BASE_LANGUAGE_TAG = "en"

    fun wrapContext(context: Context): Context {
        val locale = resolveEffectiveLocale(context)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun resolveEffectiveLocale(context: Context): Locale {
        val stored = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(PREF_APP_LANGUAGE, LANGUAGE_SYSTEM) ?: LANGUAGE_SYSTEM
        if (stored == LANGUAGE_SYSTEM) {
            val systemLocale = systemLocale()
            return if (isSupported(context, systemLocale.language)) systemLocale else Locale.forLanguageTag(BASE_LANGUAGE_TAG)
        }
        return Locale.forLanguageTag(stored)
    }

    private fun systemLocale(): Locale = Resources.getSystem().configuration.locales[0]

    /** A language is supported if it's the base English fallback or ships a values-* qualifier in the APK. */
    private fun isSupported(context: Context, languageTag: String): Boolean {
        if (languageTag.equals(BASE_LANGUAGE_TAG, ignoreCase = true)) return true
        return context.assets.locales.any { it.equals(languageTag, ignoreCase = true) }
    }
}
