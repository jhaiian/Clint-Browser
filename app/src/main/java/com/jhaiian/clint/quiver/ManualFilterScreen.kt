package com.jhaiian.clint.quiver
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.automirrored.filled.ArrowBack

import com.jhaiian.clint.R

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.jhaiian.clint.ui.ClintOutlinedTextField
import androidx.compose.material3.Surface
import com.jhaiian.clint.ui.ClintSwitch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.ui.AdaptiveWidthContainer
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.theme.LocalClintColors

sealed class ManualFilterRuleDialogMode {
    object Add : ManualFilterRuleDialogMode()
    data class Edit(val rule: ManualFilterRule) : ManualFilterRuleDialogMode()
}

class ManualFilterUiState {
    var rules by mutableStateOf<List<ManualFilterRule>>(emptyList())
    var isEnabled by mutableStateOf(false)
    var ruleDialogMode by mutableStateOf<ManualFilterRuleDialogMode?>(null)
    var deleteTarget by mutableStateOf<ManualFilterRule?>(null)
}

@Composable
fun ManualFilterScreen(
    state: ManualFilterUiState,
    maxContentWidth: Dp?,
    onExit: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (ManualFilterRule) -> Unit,
    onDeleteClick: (ManualFilterRule) -> Unit
) {
    val colors = LocalClintColors.current
    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Surface(color = colors.surface, shadowElevation = 4.dp, modifier = Modifier.statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onExit) {
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = colors.onSurface)
                    }
                    Text(
                        stringResource(R.string.quiver_guard_manual_filter_title),
                        color = colors.onSurface, fontSize = 19.sp, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 4.dp)
                    )
                }
            }
            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            Row(
                Modifier.fillMaxWidth().clickable { onToggleEnabled(!state.isEnabled) }.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.quiver_guard_manual_filter_master_switch_title), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(R.string.quiver_guard_manual_filter_master_switch_summary), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                ClintSwitch(checked = state.isEnabled)
            }
            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (state.rules.isEmpty()) {
                    Text(
                        stringResource(R.string.quiver_guard_manual_filter_empty_state),
                        color = colors.secondaryText, fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.Center).padding(32.dp)
                    )
                } else {
                    AdaptiveWidthContainer(maxContentWidth) {
                        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp)) {
                            items(state.rules, key = { it.id }) { rule ->
                                ManualFilterRuleRow(
                                    rule = rule,
                                    masterEnabled = state.isEnabled,
                                    onEditClick = { onEditClick(rule) },
                                    onDeleteClick = { onDeleteClick(rule) }
                                )
                            }
                        }
                    }
                }

                FloatingActionButton(
                    onClick = onAddClick,
                    containerColor = colors.buttonBackground, contentColor = colors.buttonIconTint,
                    modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 24.dp, end = 20.dp)
                ) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Add, contentDescription = stringResource(R.string.quiver_guard_manual_filter_add_fab_desc))
                }
            }
        }
    }
}

@Composable
private fun ManualFilterRuleRow(
    rule: ManualFilterRule,
    masterEnabled: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val colors = LocalClintColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardBackground)
            .alpha(if (masterEnabled) 1f else 0.6f)
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            rule.ruleText, color = colors.onSurface, fontSize = 14.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onEditClick) {
            Icon(androidx.compose.material.icons.Icons.Filled.Tune, contentDescription = null, tint = colors.iconTint)
        }
        IconButton(onClick = onDeleteClick) {
            Icon(androidx.compose.material.icons.Icons.Filled.Delete, contentDescription = null, tint = colors.iconTint)
        }
    }
}

/** Shared add/edit dialog: a single multi-line text field for the rule pattern(s). When
 *  adding, the user can paste multiple lines at once (one rule per line); when editing,
 *  the field holds just the one rule being changed. */
@Composable
fun ManualFilterRuleDialog(
    mode: ManualFilterRuleDialogMode,
    hideStatusBar: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var text by remember(mode) { mutableStateOf((mode as? ManualFilterRuleDialogMode.Edit)?.rule?.ruleText ?: "") }
    val isEdit = mode is ManualFilterRuleDialogMode.Edit

    ClintDialog(
        title = stringResource(if (isEdit) R.string.quiver_guard_manual_filter_edit_dialog_title else R.string.quiver_guard_manual_filter_add_dialog_title),
        hideStatusBar = hideStatusBar,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { if (text.isNotBlank()) onConfirm(text) }, enabled = text.isNotBlank()) {
                    Text(stringResource(if (isEdit) R.string.quiver_guard_manual_filter_edit_action_save else R.string.filter_list_add_action_add), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        ClintOutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(if (isEdit) R.string.quiver_guard_manual_filter_rule_hint else R.string.quiver_guard_manual_filter_rule_hint)) },
            singleLine = isEdit,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun ManualFilterDeleteConfirmDialog(rule: ManualFilterRule?, hideStatusBar: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    if (rule == null) return
    val colors = LocalClintColors.current
    ClintDialog(
        title = stringResource(R.string.quiver_guard_manual_filter_delete_confirm_title),
        hideStatusBar = hideStatusBar,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { onDismiss(); onConfirm() }) {
                    Text(stringResource(R.string.history_delete_selected), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Text(
            stringResource(R.string.quiver_guard_manual_filter_delete_confirm_message, rule.ruleText),
            color = colors.onSurface, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
