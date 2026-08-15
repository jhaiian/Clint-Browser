package com.jhaiian.clint.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jhaiian.clint.ui.theme.LocalClintColors

/**
 * The app's themed replacement for Material3's bare [OutlinedTextField]. Four separate call
 * sites across the app each hand-rolled the same `OutlinedTextFieldDefaults.colors(...)` block
 * (focused text/border/label/cursor only) — none of them set `unfocusedBorderColor`, so the
 * outline showed Material3's stock default (unmapped, purple-tinted) color the moment the field
 * lost focus. This maps every role, including disabled and error states (using the app's
 * existing `colorError` role), so new fields — including ones migrated from XML — get correct
 * theming without repeating the color block.
 */
@Composable
fun ClintOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null
) {
    val colors = LocalClintColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        enabled = enabled,
        readOnly = readOnly,
        isError = isError,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        trailingIcon = trailingIcon,
        supportingText = supportingText,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.onSurface,
            unfocusedTextColor = colors.onSurface,
            disabledTextColor = colors.onSurface,
            errorTextColor = colors.onSurface,
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.divider,
            disabledBorderColor = colors.divider,
            errorBorderColor = colors.colorError,
            cursorColor = colors.primary,
            errorCursorColor = colors.colorError,
            focusedLabelColor = colors.primary,
            unfocusedLabelColor = colors.secondaryText,
            disabledLabelColor = colors.secondaryText,
            errorLabelColor = colors.colorError,
            focusedTrailingIconColor = colors.iconTint,
            unfocusedTrailingIconColor = colors.iconTint,
            disabledTrailingIconColor = colors.iconTint,
            errorTrailingIconColor = colors.colorError
        )
    )
}
