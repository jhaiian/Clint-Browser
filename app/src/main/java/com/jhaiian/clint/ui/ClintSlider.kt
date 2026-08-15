package com.jhaiian.clint.ui

import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jhaiian.clint.ui.theme.LocalClintColors

/**
 * The app's themed replacement for Material3's bare [Slider]. A plain
 * `Slider(colors = SliderDefaults.colors(thumbColor = ..., activeTrackColor = ...))` call only
 * covers the thumb and the filled (active) side of the track — the unfilled (inactive) side and
 * every tick color are left on Material3's stock default color scheme, which is why the
 * unselected portion of the track rendered as a purple-tinted default instead of matching this
 * app's theme. This maps every role instead, so new sliders (e.g. ones migrated from XML) get
 * the correct theme for free.
 */
@Composable
fun ClintSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val colors = LocalClintColors.current
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = SliderDefaults.colors(
            thumbColor = colors.primary,
            activeTrackColor = colors.primary,
            activeTickColor = colors.background,
            inactiveTrackColor = colors.surfaceVariant,
            inactiveTickColor = colors.secondaryText,
            disabledThumbColor = colors.primary.copy(alpha = 0.38f),
            disabledActiveTrackColor = colors.primary.copy(alpha = 0.38f),
            disabledActiveTickColor = colors.background.copy(alpha = 0.38f),
            disabledInactiveTrackColor = colors.surfaceVariant.copy(alpha = 0.38f),
            disabledInactiveTickColor = colors.secondaryText.copy(alpha = 0.38f)
        )
    )
}
