package com.jhaiian.clint.userscripts
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Update

import com.jhaiian.clint.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.listscreen.ListMenuItem
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun UserScriptActionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onCheckUpdateActive: () -> Unit,
    onCheckUpdateAll: () -> Unit,
    onForceUpdateActive: () -> Unit,
    onForceUpdateAll: () -> Unit
) {
    val colors = LocalClintColors.current
    DropdownMenu(
        expanded = expanded, onDismissRequest = onDismiss,
        shape = PopupShape,
        containerColor = colors.popupBackground,
        border = BorderStroke(1.dp, colors.popupStroke)
    ) {
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.Update, stringResource(R.string.user_scripts_check_update_active), false) { onDismiss(); onCheckUpdateActive() }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.Update, stringResource(R.string.user_scripts_check_update_all), false) { onDismiss(); onCheckUpdateAll() }
        HorizontalDivider(color = colors.divider)
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.RestartAlt, stringResource(R.string.user_scripts_force_update_active), false) { onDismiss(); onForceUpdateActive() }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.RestartAlt, stringResource(R.string.user_scripts_force_update_all), false) { onDismiss(); onForceUpdateAll() }
    }
}

@Composable
fun UserScriptItemOptionsMenu(
    expanded: Boolean,
    isLocal: Boolean,
    onDismiss: () -> Unit,
    onCheckUpdate: () -> Unit,
    onForceUpdate: () -> Unit,
    onRemove: () -> Unit,
    onCopyName: () -> Unit,
    onCopyLink: () -> Unit,
    onShareLink: () -> Unit
) {
    val colors = LocalClintColors.current
    DropdownMenu(
        expanded = expanded, onDismissRequest = onDismiss,
        shape = PopupShape,
        containerColor = colors.popupBackground,
        border = BorderStroke(1.dp, colors.popupStroke)
    ) {
        if (!isLocal) {
            ListMenuItem(androidx.compose.material.icons.Icons.Filled.Update, stringResource(R.string.filter_list_menu_check_update), false) { onDismiss(); onCheckUpdate() }
            ListMenuItem(androidx.compose.material.icons.Icons.Filled.RestartAlt, stringResource(R.string.filter_list_menu_force_update), false) { onDismiss(); onForceUpdate() }
            HorizontalDivider(color = colors.divider)
        }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.Delete, stringResource(R.string.filter_list_menu_remove), false) { onDismiss(); onRemove() }
        HorizontalDivider(color = colors.divider)
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.ContentCopy, stringResource(R.string.filter_list_menu_copy_name), false) { onDismiss(); onCopyName() }
        if (!isLocal) {
            ListMenuItem(androidx.compose.material.icons.Icons.Filled.ContentCopy, stringResource(R.string.user_scripts_menu_copy_link), false) { onDismiss(); onCopyLink() }
            ListMenuItem(androidx.compose.material.icons.Icons.Filled.Share, stringResource(R.string.user_scripts_menu_share_link), false) { onDismiss(); onShareLink() }
        }
    }
}
