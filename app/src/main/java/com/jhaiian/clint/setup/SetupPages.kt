package com.jhaiian.clint.setup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Shield

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import com.jhaiian.clint.ui.ClintRadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.ThemeSwatchUtils
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun SetupWelcomePage(
    consentChecked: Boolean,
    onConsentCheckedChange: (Boolean) -> Unit,
    onPrivacyClick: () -> Unit,
    onTermsClick: () -> Unit,
    onContinue: () -> Unit
) {
    val colors = LocalClintColors.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.ic_clint_logo),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.padding(top = 32.dp).size(100.dp)
        )
        Text(
            stringResource(R.string.setup_welcome_title),
            color = colors.onSurface,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            stringResource(R.string.setup_welcome_subtitle),
            color = colors.secondaryText,
            fontSize = 14.sp,
            lineHeight = 19.6.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )
        Card(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Shield, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                    Text(
                        stringResource(R.string.document_viewer_privacy_policy_title),
                        color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f).padding(start = 12.dp)
                    )
                    Text(
                        stringResource(R.string.setup_terms_read),
                        color = colors.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onPrivacyClick)
                            .padding(4.dp)
                    )
                }
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(1.dp).background(colors.surfaceVariant))
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Info, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                    Text(
                        stringResource(R.string.document_viewer_terms_title),
                        color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f).padding(start = 12.dp)
                    )
                    Text(
                        stringResource(R.string.setup_terms_read),
                        color = colors.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onTermsClick)
                            .padding(4.dp)
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = consentChecked,
                onCheckedChange = onConsentCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = colors.primary)
            )
            Text(
                stringResource(R.string.setup_terms_agree),
                color = colors.secondaryText, fontSize = 13.sp, lineHeight = 18.2.sp,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
        }
        SetupPrimaryButton(
            text = stringResource(R.string.setup_terms_continue),
            onClick = onContinue,
            backgroundColor = colors.buttonBackground,
            enabled = consentChecked,
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}

data class IntensitySwatchColors(
    val softBg: Color, val softSurface: Color,
    val strongBg: Color, val strongSurface: Color,
    val pureBg: Color, val pureSurface: Color,
    val accent: Color
)

@Composable
fun rememberIntensitySwatchColors(theme: String, accent: String): IntensitySwatchColors {
    val context = LocalContext.current
    return remember(theme, accent) {
        val isLight = theme == "light"
        val accentColorInt = when (accent) {
            "purple" -> ThemeSwatchUtils.resolvePurpleSwatchColors(context, theme).accent
            "blue" -> ThemeSwatchUtils.resolveBlueSwatchColors(context, theme).accent
            "yellow" -> ThemeSwatchUtils.resolveYellowSwatchColors(context, theme).accent
            "red" -> ThemeSwatchUtils.resolveRedSwatchColors(context, theme).accent
            "green" -> ThemeSwatchUtils.resolveGreenSwatchColors(context, theme).accent
            "orange" -> ThemeSwatchUtils.resolveOrangeSwatchColors(context, theme).accent
            else -> MaterialColors.getColor(context, androidx.appcompat.R.attr.colorPrimary, 0xFFBB86FC.toInt())
        }
        val (softBg, softSurface) = ThemeSwatchUtils.resolveSoftTintSwatchBgSurface(context, theme, accent)
        val (strongBgRes, strongSurfaceRes) = when (accent) {
            "blue" -> (if (isLight) R.color.blue_accent_light_bg else R.color.blue_accent_dark_bg) to (if (isLight) R.color.blue_accent_light_surface else R.color.blue_accent_dark_surface)
            "yellow" -> (if (isLight) R.color.yellow_accent_light_bg else R.color.yellow_accent_dark_bg) to (if (isLight) R.color.yellow_accent_light_surface else R.color.yellow_accent_dark_surface)
            "red" -> (if (isLight) R.color.red_accent_light_bg else R.color.red_accent_dark_bg) to (if (isLight) R.color.red_accent_light_surface else R.color.red_accent_dark_surface)
            "green" -> (if (isLight) R.color.green_accent_light_bg else R.color.green_accent_dark_bg) to (if (isLight) R.color.green_accent_light_surface else R.color.green_accent_dark_surface)
            "orange" -> (if (isLight) R.color.orange_accent_light_bg else R.color.orange_accent_dark_bg) to (if (isLight) R.color.orange_accent_light_surface else R.color.orange_accent_dark_surface)
            else -> (if (isLight) R.color.purple_accent_light_bg else R.color.purple_accent_dark_bg) to (if (isLight) R.color.purple_accent_light_surface else R.color.purple_accent_dark_surface)
        }
        val pure = if (isLight) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        IntensitySwatchColors(
            softBg = Color(softBg), softSurface = Color(softSurface),
            strongBg = Color(ContextCompat.getColor(context, strongBgRes)),
            strongSurface = Color(ContextCompat.getColor(context, strongSurfaceRes)),
            pureBg = Color(pure), pureSurface = Color(pure),
            accent = Color(accentColorInt)
        )
    }
}

@Composable
fun SetupThemePage(
    scrollState: androidx.compose.foundation.ScrollState,
    theme: String,
    accent: String,
    intensity: String,
    onThemeSelected: (String) -> Unit,
    onAccentSelected: (String) -> Unit,
    onIntensitySelected: (String) -> Unit,
    onNext: () -> Unit
) {
    val colors = LocalClintColors.current
    val context = LocalContext.current

    Column(
        Modifier.fillMaxSize().verticalScroll(scrollState).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.setup_theme_title), color = colors.onSurface, fontSize = 26.sp, fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
        Text(stringResource(R.string.setup_theme_subtitle), color = colors.secondaryText, fontSize = 13.sp, lineHeight = 19.5.sp, modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 28.dp))

        data class ThemeOption(val key: String, val titleRes: Int, val descRes: Int, val drawableRes: Int)
        listOf(
            ThemeOption("dark", R.string.theme_dark, R.string.theme_dark_desc, R.drawable.theme_swatch_dark),
            ThemeOption("light", R.string.theme_light, R.string.theme_light_desc, R.drawable.theme_swatch_light),
            ThemeOption("default", R.string.theme_default, R.string.theme_default_desc, R.drawable.theme_swatch_default)
        ).forEach { option ->
            SelectableCard(selected = theme == option.key, onClick = { onThemeSelected(option.key) }, cardBackground = colors.cardBackground, primary = colors.primary) {
                DrawableImage(option.drawableRes, modifier = Modifier.size(44.dp))
                Column(Modifier.weight(1f).padding(start = 16.dp, end = 8.dp)) {
                    Text(stringResource(option.titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(option.descRes), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                CheckSlot(theme == option.key, colors.primary)
            }
        }

        SectionLabel(stringResource(R.string.setup_accent_section_label), colors.primary, Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp))

        data class AccentOption(val key: String, val titleRes: Int, val descRes: Int)
        listOf(
            AccentOption("purple", R.string.accent_purple, R.string.accent_purple_desc),
            AccentOption("material_you", R.string.accent_material_you, R.string.accent_material_you_desc),
            AccentOption("default", R.string.accent_default, R.string.accent_default_desc),
            AccentOption("blue", R.string.accent_blue, R.string.accent_blue_desc),
            AccentOption("yellow", R.string.accent_yellow, R.string.accent_yellow_desc),
            AccentOption("red", R.string.accent_red, R.string.accent_red_desc),
            AccentOption("green", R.string.accent_green, R.string.accent_green_desc),
            AccentOption("orange", R.string.accent_orange, R.string.accent_orange_desc)
        ).forEach { option ->
            val swatch = remember(theme, option.key) {
                when (option.key) {
                    "material_you" -> ThemeSwatchUtils.resolveMaterialYouSwatchColors(context, theme)
                    "purple" -> ThemeSwatchUtils.resolvePurpleSwatchColors(context, theme)
                    "blue" -> ThemeSwatchUtils.resolveBlueSwatchColors(context, theme)
                    "yellow" -> ThemeSwatchUtils.resolveYellowSwatchColors(context, theme)
                    "red" -> ThemeSwatchUtils.resolveRedSwatchColors(context, theme)
                    "green" -> ThemeSwatchUtils.resolveGreenSwatchColors(context, theme)
                    "orange" -> ThemeSwatchUtils.resolveOrangeSwatchColors(context, theme)
                    else -> ThemeSwatchUtils.resolveDefaultSwatchColors(context, theme)
                }
            }
            SelectableCard(selected = accent == option.key, onClick = { onAccentSelected(option.key) }, cardBackground = colors.cardBackground, primary = colors.primary) {
                AccentSwatch(Color(swatch.bg), Color(swatch.surface), Color(swatch.accent))
                Column(Modifier.weight(1f).padding(start = 16.dp, end = 8.dp)) {
                    Text(stringResource(option.titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(option.descRes), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                CheckSlot(accent == option.key, colors.primary)
            }
        }

        val intensityEnabled = theme != "default"
        if (intensityEnabled) {
            val strongVisible = accent in setOf("purple", "blue", "yellow", "red", "green", "orange")
            val swatches = rememberIntensitySwatchColors(theme, accent)
            SectionLabel(stringResource(R.string.setup_intensity_section_label), colors.primary, Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp))

            SelectableCard(selected = intensity == "soft_tint", onClick = { onIntensitySelected("soft_tint") }, cardBackground = colors.cardBackground, primary = colors.primary) {
                AccentSwatch(swatches.softBg, swatches.softSurface, swatches.accent)
                Column(Modifier.weight(1f).padding(start = 16.dp, end = 8.dp)) {
                    Text(stringResource(R.string.surface_intensity_soft), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.surface_intensity_soft_desc), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                CheckSlot(intensity == "soft_tint", colors.primary)
            }
            if (strongVisible) {
                SelectableCard(selected = intensity == "strong_tint", onClick = { onIntensitySelected("strong_tint") }, cardBackground = colors.cardBackground, primary = colors.primary) {
                    AccentSwatch(swatches.strongBg, swatches.strongSurface, swatches.accent)
                    Column(Modifier.weight(1f).padding(start = 16.dp, end = 8.dp)) {
                        Text(stringResource(R.string.surface_intensity_strong), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.surface_intensity_strong_desc), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                    CheckSlot(intensity == "strong_tint", colors.primary)
                }
            }
            SelectableCard(selected = intensity == "pure_mode", onClick = { onIntensitySelected("pure_mode") }, cardBackground = colors.cardBackground, primary = colors.primary) {
                AccentSwatch(swatches.pureBg, swatches.pureSurface, swatches.accent)
                Column(Modifier.weight(1f).padding(start = 16.dp, end = 8.dp)) {
                    Text(stringResource(R.string.surface_intensity_pure), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(if (theme == "light") R.string.surface_intensity_pure_light_desc else R.string.surface_intensity_pure_dark_desc),
                        color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp)
                    )
                }
                CheckSlot(intensity == "pure_mode", colors.primary)
            }
        }

        SetupPrimaryButton(stringResource(R.string.next), onNext, colors.buttonBackground, Modifier.padding(top = 24.dp, bottom = 24.dp))
    }
}

@Composable
fun SetupEnginePage(engine: String, onEngineSelected: (String) -> Unit, onNext: () -> Unit) {
    val colors = LocalClintColors.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painterResource(R.drawable.ic_clint_logo), stringResource(R.string.app_name), modifier = Modifier.padding(top = 24.dp).size(140.dp))
        Text(stringResource(R.string.app_name), color = colors.onSurface, fontSize = 32.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 20.dp))
        Text(stringResource(R.string.setup_subtitle), color = colors.secondaryText, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 19.6.sp, modifier = Modifier.padding(top = 8.dp))
        Text(stringResource(R.string.choose_search_engine), color = colors.primary, fontSize = 12.sp, letterSpacing = 0.1.sp, fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth().padding(top = 36.dp, bottom = 12.dp))

        data class EngineOption(val key: String, val titleRes: Int, val descRes: Int, val showDefault: Boolean)
        listOf(
            EngineOption("duckduckgo", R.string.engine_duckduckgo, R.string.engine_duckduckgo_desc, true),
            EngineOption("brave", R.string.engine_brave, R.string.engine_brave_desc, false),
            EngineOption("google", R.string.engine_google, R.string.engine_google_desc, false)
        ).forEach { option ->
            val sel = engine == option.key
            SelectableCard(selected = sel, onClick = { onEngineSelected(option.key) }, cardBackground = colors.cardBackground, primary = colors.primary) {
                ClintRadioButton(selected = sel)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(stringResource(option.titleRes), color = colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(option.descRes), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                if (option.showDefault) DefaultChip(stringResource(R.string.default_label), colors.primary)
            }
        }

        SetupPrimaryButton(stringResource(R.string.next), onNext, colors.buttonBackground, Modifier.padding(top = 24.dp, bottom = 24.dp))
    }
}

@Composable
fun SetupDefaultBrowserPage(
    isDefaultBrowser: Boolean,
    onSetDefault: () -> Unit,
    onSkip: () -> Unit
) {
    val colors = LocalClintColors.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.setup_default_browser_title), color = colors.onSurface, fontSize = 26.sp, fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
        Text(stringResource(R.string.setup_default_browser_description), color = colors.secondaryText, fontSize = 13.sp, lineHeight = 19.5.sp, modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 28.dp))

        Card(Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = colors.cardBackground)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(androidx.compose.material.icons.Icons.Filled.Language, null, tint = colors.primary, modifier = Modifier.size(36.dp))
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(stringResource(R.string.setup_default_browser_card_title), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(R.string.setup_default_browser_card_desc), color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
                if (isDefaultBrowser) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Check, null, tint = colors.primary, modifier = Modifier.padding(start = 8.dp).size(22.dp))
                }
            }
        }

        SetupPrimaryButton(
            text = stringResource(if (isDefaultBrowser) R.string.get_started else R.string.setup_default_browser_set_button),
            onClick = onSetDefault,
            backgroundColor = colors.buttonBackground
        )
        SetupPrimaryButton(
            text = stringResource(R.string.setup_default_browser_skip),
            onClick = onSkip,
            backgroundColor = colors.cardBackground,
            modifier = Modifier.padding(top = 10.dp, bottom = 24.dp)
        )
    }
}
