package com.jhaiian.clint.settings.lookandfeel

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import com.jhaiian.clint.R
import com.jhaiian.clint.util.LocaleHelper
import java.util.Locale

/** A language shipped in the app. */
data class LanguageOption(val tag: String, val locale: Locale)

/**
 * Discovers every language the APK ships, derived at runtime from the compiled values-*
 * qualifiers rather than a hardcoded list. A shipped translation is only detected by comparing
 * its strings against the base English values, since an untranslated string silently falls back
 * to English.
 *
 * [Context.getAssets] locales include qualifiers contributed only by dependencies (AndroidX,
 * Compose, pseudo-locales used for testing) that translate their own strings but none of this
 * app's, so a locale is only kept when at least one of this app's own strings is translated;
 * otherwise it is indistinguishable from the base language and just adds noise to the picker.
 * Region variants such as "fil-PH" that have no values-fil-rPH override resolve to the exact same
 * strings as their base "fil" tag, so entries are also deduplicated by their resolved content and
 * the shortest matching tag is kept.
 *
 * This walks every string resource for every shipped locale, so it is deliberately cached for the
 * lifetime of the process: the app's own translations never change while it's running, and callers
 * are expected to invoke this off the main thread.
 */
fun collectLanguageOptions(context: Context): List<LanguageOption> {
    cachedLanguageOptions?.let { return it }

    val stringIds = R.string::class.java.fields.mapNotNull { field ->
        runCatching { field.getInt(null) }.getOrNull()
    }
    val baseResources = resourcesFor(context, Locale.forLanguageTag(LocaleHelper.BASE_LANGUAGE_TAG))
    val baseValues = stringIds.map { id -> runCatching { baseResources.getString(id) }.getOrNull() }

    val shippedTags = context.assets.locales
        .filter { it.isNotBlank() && !it.equals(LocaleHelper.BASE_LANGUAGE_TAG, ignoreCase = true) }
        .distinct()

    val translated = shippedTags
        .mapNotNull { tag ->
            val locale = Locale.forLanguageTag(tag)
            val resources = resourcesFor(context, locale)
            val values = stringIds.map { id -> runCatching { resources.getString(id) }.getOrNull() }
            val hasOwnTranslation = values.indices.any { i -> values[i] != null && values[i] != baseValues[i] }
            if (!hasOwnTranslation) null else Triple(tag, locale, values)
        }
        .groupBy { it.third }
        .values
        .map { group -> group.minBy { it.first.length } }
        .map { (tag, locale, _) -> LanguageOption(tag, locale) }

    val base = LanguageOption(LocaleHelper.BASE_LANGUAGE_TAG, Locale.forLanguageTag(LocaleHelper.BASE_LANGUAGE_TAG))
    return (listOf(base) + translated)
        .sortedBy { it.locale.getDisplayName(it.locale).lowercase(it.locale) }
        .also { cachedLanguageOptions = it }
}

private var cachedLanguageOptions: List<LanguageOption>? = null

private fun resourcesFor(context: Context, locale: Locale): Resources {
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    return context.createConfigurationContext(config).resources
}
