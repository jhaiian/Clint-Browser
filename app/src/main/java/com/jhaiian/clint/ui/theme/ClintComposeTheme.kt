package com.jhaiian.clint.ui.theme

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.android.material.color.MaterialColors
import com.jhaiian.clint.R

data class ClintColors(
    val background: Color,
    val onSurface: Color,
    val secondaryText: Color,
    val cardBackground: Color,
    val buttonBackground: Color,
    val surfaceVariant: Color,
    val popupBackground: Color,
    val primary: Color,
    val iconTint: Color,
    val divider: Color,
    val popupText: Color,
    val surface: Color,
    val buttonIconTint: Color,
    val popupStroke: Color,
    val popupCheck: Color,
    val onPrimary: Color,
    val colorError: Color,
    val colorErrorContainer: Color,
    val colorOnErrorContainer: Color,
    val buttonTextColor: Color,
    val addressBarColor: Color,
    val isLight: Boolean
)

val LocalClintColors = compositionLocalOf<ClintColors> {
    error("ClintComposeTheme not applied")
}

private fun resolveClintColors(context: android.content.Context, isLight: Boolean): ClintColors {
    fun attr(attrId: Int, fallback: Int) = Color(MaterialColors.getColor(context, attrId, fallback))
    return ClintColors(
        background = attr(android.R.attr.colorBackground, 0xFF121212.toInt()),
        onSurface = attr(com.google.android.material.R.attr.colorOnSurface, 0xFFFFFFFF.toInt()),
        secondaryText = attr(R.attr.clintSecondaryTextColor, 0xFFAAAAAA.toInt()),
        cardBackground = attr(R.attr.clintCardBackground, 0xFF1E1E1E.toInt()),
        buttonBackground = attr(R.attr.clintButtonBackground, 0xFFBB86FC.toInt()),
        surfaceVariant = attr(R.attr.clintSurfaceVariant, 0xFF2A2A2A.toInt()),
        popupBackground = attr(R.attr.clintPopupBackground, 0xFF1E1E1E.toInt()),
        primary = attr(androidx.appcompat.R.attr.colorPrimary, 0xFFBB86FC.toInt()),
        iconTint = attr(R.attr.clintIconTint, 0xFFAAAAAA.toInt()),
        divider = attr(R.attr.clintDividerColor, 0x1FFFFFFF),
        popupText = attr(R.attr.clintPopupTextColor, 0xFFFFFFFF.toInt()),
        surface = attr(com.google.android.material.R.attr.colorSurface, 0xFF1E1E1E.toInt()),
        buttonIconTint = attr(R.attr.clintButtonIconTint, 0xFF000000.toInt()),
        popupStroke = attr(R.attr.clintPopupStrokeColor, 0x33FFFFFF),
        popupCheck = attr(R.attr.clintPopupCheckColor, 0xFFBB86FC.toInt()),
        onPrimary = attr(com.google.android.material.R.attr.colorOnPrimary, 0xFF000000.toInt()),
        colorError = attr(android.R.attr.colorError, 0xFFCF6679.toInt()),
        colorErrorContainer = attr(com.google.android.material.R.attr.colorErrorContainer, 0xFF4E0002.toInt()),
        colorOnErrorContainer = attr(com.google.android.material.R.attr.colorOnErrorContainer, 0xFFFFDAD6.toInt()),
        buttonTextColor = attr(R.attr.clintButtonTextColor, 0xFF000000.toInt()),
        addressBarColor = attr(R.attr.clintAddressBarColor, 0xFF2A2A2A.toInt()),
        isLight = isLight
    )
}

@Composable
fun ClintComposeTheme(theme: String, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val isLight = when (theme) {
        "light" -> true
        "dark" -> false
        else -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) !=
            Configuration.UI_MODE_NIGHT_YES
    }

    val clintColors = remember(context.theme, theme) { resolveClintColors(context, isLight) }

    val base = if (isLight) lightColorScheme() else darkColorScheme()
    val colorScheme = base.copy(
        primary = clintColors.primary,
        onPrimary = if (isLight) Color.White else Color.Black,
        background = clintColors.background,
        onBackground = clintColors.onSurface,
        surface = clintColors.cardBackground,
        onSurface = clintColors.onSurface,
        surfaceVariant = clintColors.surfaceVariant,
        onSurfaceVariant = clintColors.secondaryText
    )

    CompositionLocalProvider(LocalClintColors provides clintColors) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
