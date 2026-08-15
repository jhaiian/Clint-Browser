package com.jhaiian.clint.ui
import androidx.compose.material.icons.filled.Check

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.theme.LocalClintColors

/**
 * The app's themed replacement for Material3's bare [Switch]. Every switch in the app is a
 * read-only visual indicator driven by its parent row's click (hence the hardcoded
 * `onCheckedChange = null`), so callers only ever need [checked].
 *
 * Two things a plain `Switch(colors = SwitchDefaults.colors(checkedTrackColor = ...))` call
 * gets wrong, which is why this exists instead of fixing each call site individually:
 *  - Only `checkedTrackColor` was ever overridden. Every other color role — including the
 *    thumb itself and the entire unchecked state — was left on Material3's stock default
 *    color scheme, which is why the thumb rendered as a flat near-black circle and the
 *    unchecked track/border showed the default palette's tint instead of this app's theme.
 *  - No `thumbContent`, so there was never a checkmark in the thumb.
 */
@Composable
fun ClintSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    Switch(
        checked = checked,
        onCheckedChange = null,
        modifier = modifier,
        thumbContent = if (checked) {
            {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize)
                )
            }
        } else null,
        colors = SwitchDefaults.colors(
            checkedThumbColor = colors.onSurface,
            checkedTrackColor = colors.primary,
            checkedBorderColor = Color.Transparent,
            checkedIconColor = colors.primary,
            uncheckedThumbColor = colors.secondaryText,
            uncheckedTrackColor = colors.surfaceVariant,
            uncheckedBorderColor = colors.divider,
            uncheckedIconColor = colors.surfaceVariant,
            disabledCheckedThumbColor = colors.onSurface.copy(alpha = 0.38f),
            disabledCheckedTrackColor = colors.primary.copy(alpha = 0.38f),
            disabledCheckedBorderColor = Color.Transparent,
            disabledCheckedIconColor = colors.primary.copy(alpha = 0.38f),
            disabledUncheckedThumbColor = colors.secondaryText.copy(alpha = 0.38f),
            disabledUncheckedTrackColor = colors.surfaceVariant.copy(alpha = 0.38f),
            disabledUncheckedBorderColor = colors.divider.copy(alpha = 0.38f),
            disabledUncheckedIconColor = colors.surfaceVariant.copy(alpha = 0.38f)
        )
    )
}
