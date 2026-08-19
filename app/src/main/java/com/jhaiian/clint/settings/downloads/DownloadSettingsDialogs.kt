package com.jhaiian.clint.settings.downloads
import androidx.compose.material.icons.filled.ArrowDownward

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import com.jhaiian.clint.ui.ClintOutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.setup.CheckSlot
import com.jhaiian.clint.setup.SelectableCard
import com.jhaiian.clint.settings.common.dialogSectionBackground
import com.jhaiian.clint.settings.common.SettingsPickerOptionBottomSpacing
import com.jhaiian.clint.settings.common.SettingsPickerOptionContentPadding
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.downloads.SPEED_LIMIT_UNIT_KB
import com.jhaiian.clint.downloads.SPEED_LIMIT_UNIT_MB
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.theme.LocalClintColors

private val OptionContentPadding = SettingsPickerOptionContentPadding
private val OptionBottomSpacing = SettingsPickerOptionBottomSpacing

@Composable
fun MeasurementSystemDialog(
    current: Boolean,
    hideStatusBar: Boolean,
    onSelect: (decimal: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    ClintDialog(title = stringResource(R.string.measurement_system_dialog_title), hideStatusBar = hideStatusBar, onDismiss = onDismiss) {
        data class Option(val decimal: Boolean, val titleRes: Int, val descRes: Int)
        listOf(
            Option(false, R.string.measurement_system_binary, R.string.measurement_system_binary_desc),
            Option(true, R.string.measurement_system_decimal, R.string.measurement_system_decimal_desc)
        ).forEach { option ->
            SelectableCard(
                selected = current == option.decimal, onClick = { onSelect(option.decimal) },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing
            ) {
                Column(Modifier.weight(1f).padding(start = 4.dp, end = 8.dp)) {
                    Text(stringResource(option.titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(option.descRes), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                CheckSlot(current == option.decimal, colors.primary)
            }
        }
    }
}

@Composable
private fun NumberEntryDialog(
    title: String,
    message: String,
    hint: String,
    initialValue: Int,
    minValue: Int,
    hideStatusBar: Boolean,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var text by remember { mutableStateOf(initialValue.toString()) }

    ClintDialog(
        title = title,
        hideStatusBar = hideStatusBar,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { onConfirm(text.toIntOrNull()?.coerceAtLeast(minValue) ?: initialValue) }) {
                    Text(stringResource(R.string.action_ok), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Text(message, color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
            SettingsSection(colors.dialogSectionBackground) {
                Column(Modifier.padding(16.dp)) {
                    ClintOutlinedTextField(
                        value = text,
                        onValueChange = { new -> if (new.all { it.isDigit() }) text = new },
                        label = { Text(hint) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
fun RetryCountDialog(current: Int, hideStatusBar: Boolean, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    NumberEntryDialog(
        title = stringResource(R.string.download_retry_count_dialog_title),
        message = stringResource(R.string.download_retry_count_dialog_message),
        hint = stringResource(R.string.download_retry_count_dialog_hint),
        initialValue = current,
        minValue = 0,
        hideStatusBar = hideStatusBar,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun RetryIntervalDialog(current: Int, hideStatusBar: Boolean, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    NumberEntryDialog(
        title = stringResource(R.string.download_retry_interval_dialog_title),
        message = stringResource(R.string.download_retry_interval_dialog_message),
        hint = stringResource(R.string.download_retry_interval_dialog_hint),
        initialValue = current,
        minValue = 1,
        hideStatusBar = hideStatusBar,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun SpeedLimitDialog(
    currentAmount: Int,
    currentUnit: String,
    hideStatusBar: Boolean,
    onConfirm: (amount: Int, unit: String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var amountText by remember { mutableStateOf(if (currentAmount > 0) currentAmount.toString() else "") }
    var unit by remember { mutableStateOf(currentUnit) }
    var unitMenuExpanded by remember { mutableStateOf(false) }

    ClintDialog(
        title = stringResource(R.string.download_speed_limit_dialog_title),
        hideStatusBar = hideStatusBar,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { onConfirm(amountText.toIntOrNull()?.coerceAtLeast(0) ?: 0, unit) }) {
                    Text(stringResource(R.string.action_ok), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Text(
                stringResource(R.string.download_speed_limit_dialog_message),
                color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp)
            )
            SettingsSection(colors.dialogSectionBackground) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    ClintOutlinedTextField(
                        value = amountText,
                        onValueChange = { new -> if (new.all { it.isDigit() }) amountText = new },
                        label = { Text(stringResource(R.string.download_speed_limit_dialog_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    Box(Modifier.padding(start = 8.dp)) {
                        val unitLabel = stringResource(if (unit == SPEED_LIMIT_UNIT_MB) R.string.speed_limit_unit_mb else R.string.speed_limit_unit_kb)
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { unitMenuExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(unitLabel, color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Icon(
                                androidx.compose.material.icons.Icons.Filled.ArrowDownward, contentDescription = null,
                                tint = colors.iconTint, modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = unitMenuExpanded,
                            onDismissRequest = { unitMenuExpanded = false },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = colors.popupBackground
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.speed_limit_unit_kb)) },
                                onClick = { unit = SPEED_LIMIT_UNIT_KB; unitMenuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.speed_limit_unit_mb)) },
                                onClick = { unit = SPEED_LIMIT_UNIT_MB; unitMenuExpanded = false }
                            )
                        }
                    }
                }
            }
        }
    }
}
