package com.jhaiian.clint.settings.lookandfeel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.jhaiian.clint.ui.ClintRadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.settings.common.SettingsPickerOptionBottomSpacing
import com.jhaiian.clint.settings.common.SettingsPickerOptionContentPadding
import com.jhaiian.clint.setup.AccentSwatch
import com.jhaiian.clint.setup.AddressBarPreview
import com.jhaiian.clint.setup.CheckSlot
import com.jhaiian.clint.setup.DefaultChip
import com.jhaiian.clint.setup.DrawableImage
import com.jhaiian.clint.setup.MenuStylePreview
import com.jhaiian.clint.setup.ScrollHidePreview
import com.jhaiian.clint.setup.SelectableCard
import com.jhaiian.clint.setup.navBarSlotDescRes
import com.jhaiian.clint.setup.navBarSlotTitleRes
import com.jhaiian.clint.setup.rememberIntensitySwatchColors
import com.jhaiian.clint.setup.scrollCardVisible
import com.jhaiian.clint.ui.ThemeSwatchUtils
import com.jhaiian.clint.ui.theme.LocalClintColors
import com.jhaiian.clint.util.LocaleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val OptionContentPadding = SettingsPickerOptionContentPadding
private val OptionBottomSpacing = SettingsPickerOptionBottomSpacing

/** Resolves the background/surface pair used by the layout previews for the given theme + accent. */
@Composable
private fun rememberBgSurface(theme: String, accent: String): Pair<Color, Color> {
    val context = LocalContext.current
    return remember(theme, accent) {
        val swatch = when (accent) {
            "material_you" -> ThemeSwatchUtils.resolveMaterialYouSwatchColors(context, theme)
            "purple" -> ThemeSwatchUtils.resolvePurpleSwatchColors(context, theme)
            "blue" -> ThemeSwatchUtils.resolveBlueSwatchColors(context, theme)
            "yellow" -> ThemeSwatchUtils.resolveYellowSwatchColors(context, theme)
            "red" -> ThemeSwatchUtils.resolveRedSwatchColors(context, theme)
            "green" -> ThemeSwatchUtils.resolveGreenSwatchColors(context, theme)
            "orange" -> ThemeSwatchUtils.resolveOrangeSwatchColors(context, theme)
            else -> ThemeSwatchUtils.resolveDefaultSwatchColors(context, theme)
        }
        Color(swatch.bg) to Color(swatch.surface)
    }
}

@Composable
fun ThemeSelectorDialog(current: String, hideStatusBar: Boolean, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    ClintDialog(title = stringResource(R.string.pref_app_theme_title), hideStatusBar = hideStatusBar, onDismiss = onDismiss) {
        data class ThemeOption(val key: String, val titleRes: Int, val descRes: Int, val drawableRes: Int)
        listOf(
            ThemeOption("dark", R.string.theme_dark, R.string.theme_dark_desc, R.drawable.theme_swatch_dark),
            ThemeOption("light", R.string.theme_light, R.string.theme_light_desc, R.drawable.theme_swatch_light),
            ThemeOption("default", R.string.theme_default, R.string.theme_default_desc, R.drawable.theme_swatch_default)
        ).forEach { option ->
            SelectableCard(
                selected = current == option.key, onClick = { onSelect(option.key) },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing
            ) {
                DrawableImage(option.drawableRes, modifier = Modifier.size(44.dp))
                Column(Modifier.weight(1f).padding(start = 16.dp, end = 8.dp)) {
                    Text(stringResource(option.titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(option.descRes), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                CheckSlot(current == option.key, colors.primary)
            }
        }
    }
}

@Composable
fun AccentColorDialog(current: String, theme: String, hideStatusBar: Boolean, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    ClintDialog(title = stringResource(R.string.pref_accent_color_title), hideStatusBar = hideStatusBar, onDismiss = onDismiss) {
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
            SelectableCard(
                selected = current == option.key, onClick = { onSelect(option.key) },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing
            ) {
                AccentSwatch(Color(swatch.bg), Color(swatch.surface), Color(swatch.accent))
                Column(Modifier.weight(1f).padding(start = 16.dp, end = 8.dp)) {
                    Text(stringResource(option.titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(option.descRes), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                CheckSlot(current == option.key, colors.primary)
            }
        }
    }
}

@Composable
fun SurfaceIntensityDialog(
    current: String,
    theme: String,
    accent: String,
    hideStatusBar: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    val strongVisible = accent in setOf("purple", "blue", "yellow", "red", "green", "orange")
    val swatches = rememberIntensitySwatchColors(theme, accent)

    ClintDialog(title = stringResource(R.string.pref_surface_intensity_title), hideStatusBar = hideStatusBar, onDismiss = onDismiss) {
        SelectableCard(
            selected = current == "soft_tint", onClick = { onSelect("soft_tint") },
            cardBackground = colors.surfaceVariant, primary = colors.primary,
            contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing
        ) {
            AccentSwatch(swatches.softBg, swatches.softSurface, swatches.accent)
            Column(Modifier.weight(1f).padding(start = 16.dp, end = 8.dp)) {
                Text(stringResource(R.string.surface_intensity_soft), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.surface_intensity_soft_desc), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            }
            CheckSlot(current == "soft_tint", colors.primary)
        }
        if (strongVisible) {
            SelectableCard(
                selected = current == "strong_tint", onClick = { onSelect("strong_tint") },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing
            ) {
                AccentSwatch(swatches.strongBg, swatches.strongSurface, swatches.accent)
                Column(Modifier.weight(1f).padding(start = 16.dp, end = 8.dp)) {
                    Text(stringResource(R.string.surface_intensity_strong), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.surface_intensity_strong_desc), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                CheckSlot(current == "strong_tint", colors.primary)
            }
        }
        SelectableCard(
            selected = current == "pure_mode", onClick = { onSelect("pure_mode") },
            cardBackground = colors.surfaceVariant, primary = colors.primary,
            contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing
        ) {
            AccentSwatch(swatches.pureBg, swatches.pureSurface, swatches.accent)
            Column(Modifier.weight(1f).padding(start = 16.dp, end = 8.dp)) {
                Text(stringResource(R.string.surface_intensity_pure), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(if (theme == "light") R.string.surface_intensity_pure_light_desc else R.string.surface_intensity_pure_dark_desc),
                    color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp)
                )
            }
            CheckSlot(current == "pure_mode", colors.primary)
        }
    }
}

@Composable
fun AddressBarPositionDialog(
    current: String,
    theme: String,
    accent: String,
    hideStatusBar: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    val (bg, surface) = rememberBgSurface(theme, accent)
    val onSurface = colors.onSurface

    ClintDialog(title = stringResource(R.string.pref_address_bar_position_title), hideStatusBar = hideStatusBar, onDismiss = onDismiss) {
        data class AddrOption(val key: String, val titleRes: Int, val descRes: Int)
        listOf(
            AddrOption("top", R.string.address_bar_position_top, R.string.address_bar_position_top_desc),
            AddrOption("bottom", R.string.address_bar_position_bottom, R.string.address_bar_position_bottom_desc),
            AddrOption("split", R.string.address_bar_position_split, R.string.address_bar_position_split_desc)
        ).forEach { option ->
            SelectableCard(
                selected = current == option.key, onClick = { onSelect(option.key) },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing
            ) {
                AddressBarPreview(option.key, bg, surface, onSurface)
                Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                    Text(stringResource(option.titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(option.descRes), color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
                CheckSlot(current == option.key, colors.primary)
            }
        }
    }
}

@Composable
fun MenuStyleDialog(
    current: String,
    addressBarPosition: String,
    theme: String,
    accent: String,
    hideStatusBar: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    val (bg, surface) = rememberBgSurface(theme, accent)
    val onSurface = colors.onSurface
    val panelBg = colors.popupBackground

    ClintDialog(title = stringResource(R.string.pref_menu_style_title), hideStatusBar = hideStatusBar, onDismiss = onDismiss) {
        data class MenuOption(val key: String, val variant: String, val titleRes: Int, val descRes: Int)
        listOf(
            MenuOption("popup", "popup", R.string.menu_style_popup, R.string.menu_style_popup_desc),
            MenuOption("bottom_sheet", "sheet", R.string.menu_style_bottom_sheet, R.string.menu_style_bottom_sheet_desc)
        ).forEach { option ->
            SelectableCard(
                selected = current == option.key, onClick = { onSelect(option.key) },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing
            ) {
                MenuStylePreview(option.variant, addressBarPosition, bg, surface, onSurface, panelBg)
                Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                    Text(stringResource(option.titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(option.descRes), color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
                CheckSlot(current == option.key, colors.primary)
            }
        }
    }
}

/**
 * onSelect reports the tapped slot key ("off"/"search_bar"/"navigation_bar"/"both"), not the final
 * preference value; when the address bar sits at the bottom the navigation_bar slot actually hides
 * the search bar, so the caller is responsible for translating it before persisting.
 */
@Composable
fun ScrollHideModeDialog(
    current: String,
    addressBarPosition: String,
    theme: String,
    accent: String,
    hideStatusBar: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    val (bg, surface) = rememberBgSurface(theme, accent)
    val onSurface = colors.onSurface

    ClintDialog(title = stringResource(R.string.pref_nested_scroll_title), hideStatusBar = hideStatusBar, onDismiss = onDismiss) {
        listOf("off", "search_bar", "navigation_bar", "both").forEach { kind ->
            if (scrollCardVisible(kind, addressBarPosition)) {
                val (titleRes, descRes) = when (kind) {
                    "off" -> R.string.nested_scroll_off to R.string.nested_scroll_off_desc
                    "search_bar" -> R.string.nested_scroll_search_bar to R.string.nested_scroll_search_bar_desc
                    "navigation_bar" -> navBarSlotTitleRes(addressBarPosition) to navBarSlotDescRes(addressBarPosition)
                    else -> R.string.nested_scroll_both to R.string.nested_scroll_both_desc
                }
                val selected = when (kind) {
                    "search_bar" -> current == "search_bar" && addressBarPosition != "bottom"
                    "navigation_bar" -> (current == "search_bar" && addressBarPosition == "bottom") ||
                        (current == "navigation_bar" && addressBarPosition == "split")
                    else -> current == kind
                }
                SelectableCard(
                    selected = selected, onClick = { onSelect(kind) },
                    cardBackground = colors.surfaceVariant, primary = colors.primary,
                    contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing
                ) {
                    ScrollHidePreview(kind, addressBarPosition, bg, surface, onSurface, animate = kind != "off")
                    Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                        Text(stringResource(titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(stringResource(descRes), color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                    CheckSlot(selected, colors.primary)
                }
            }
        }
    }
}

@Composable
fun ExitConfirmationDialog(
    current: String,
    hideStatusBar: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var selected by remember(current) { mutableStateOf(current) }

    ClintDialog(
        title = stringResource(R.string.exit_confirmation_title),
        hideStatusBar = hideStatusBar,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { onConfirm(selected) }) {
                    Text(stringResource(android.R.string.ok), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        data class ExitOption(val key: String, val titleRes: Int, val descRes: Int, val showDefault: Boolean)
        listOf(
            ExitOption("off", R.string.exit_confirmation_off, R.string.exit_confirmation_off_desc, false),
            ExitOption("toast", R.string.exit_confirmation_toast, R.string.exit_confirmation_toast_desc, true),
            ExitOption("dialog", R.string.exit_confirmation_dialog, R.string.exit_confirmation_dialog_desc, false)
        ).forEach { option ->
            val sel = selected == option.key
            SelectableCard(
                selected = sel, onClick = { selected = option.key },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing
            ) {
                ClintRadioButton(selected = sel)
                Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                    Text(stringResource(option.titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(option.descRes), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                if (option.showDefault) DefaultChip(stringResource(R.string.default_label), colors.primary)
            }
        }
    }
}

@Composable
fun LanguageSelectorDialog(current: String, hideStatusBar: Boolean, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    var options by remember { mutableStateOf(emptyList<LanguageOption>()) }

    // Scanning every string resource across every shipped locale is too slow for the main thread,
    // so the dialog opens immediately with just System and the rest populate once ready.
    LaunchedEffect(Unit) {
        options = withContext(Dispatchers.Default) { collectLanguageOptions(context) }
    }

    ClintDialog(title = stringResource(R.string.pref_language_title), hideStatusBar = hideStatusBar, onDismiss = onDismiss) {
        val systemSelected = current == LocaleHelper.LANGUAGE_SYSTEM
        SelectableCard(
            selected = systemSelected, onClick = { onSelect(LocaleHelper.LANGUAGE_SYSTEM) },
            cardBackground = colors.surfaceVariant, primary = colors.primary,
            contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing
        ) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(stringResource(R.string.language_system), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(stringResource(R.string.language_system_desc), color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
            CheckSlot(systemSelected, colors.primary)
        }
        options.forEach { option ->
            val selected = current == option.tag
            SelectableCard(
                selected = selected, onClick = { onSelect(option.tag) },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing
            ) {
                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        option.locale.getDisplayName(option.locale).replaceFirstChar { it.titlecase(option.locale) },
                        color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium
                    )
                    if (option.tag == LocaleHelper.BASE_LANGUAGE_TAG) {
                        Text(
                            stringResource(R.string.language_base_desc),
                            color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                CheckSlot(selected, colors.primary)
            }
        }
    }
}
