package com.jhaiian.clint.settings.lookandfeel

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import com.jhaiian.clint.R
import com.jhaiian.clint.util.LocaleHelper
import java.util.Locale

data class LanguageOption(val tag: String, val locale: Locale)

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
