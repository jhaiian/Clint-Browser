package com.jhaiian.clint.userscripts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig

private fun UserScriptsActivity.selectedUserScripts(): List<UserScript> =
    uiState.scripts.filter { it.id in uiState.selectedIds }

private fun UserScriptsActivity.scriptDisplayName(script: UserScript): String =
    UserScriptMetadataParser.parse(script.code, "Untitled Script").name

internal fun UserScriptsActivity.confirmCheckUpdateForItem(script: UserScript) {
    if (uiState.isUpdateRunning) {
        Toast.makeText(this, getString(R.string.filter_list_operation_in_progress), Toast.LENGTH_SHORT).show()
        return
    }
    if (script.isLocal) {
        Toast.makeText(this, getString(R.string.user_scripts_item_local_no_update), Toast.LENGTH_SHORT).show()
        return
    }
    val name = scriptDisplayName(script)
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.filter_list_update_check_title),
        message = getString(R.string.filter_list_item_check_update_confirm_message, name),
        negativeLabel = getString(R.string.action_cancel),
        positiveLabel = getString(R.string.filter_list_update_check_action),
        onPositive = {
            startUserScriptUpdateCheck(
                forceUpdate = false,
                scriptsOverride = listOf(script),
                progressTitleOverride = getString(R.string.filter_list_item_check_update_progress_title, name)
            )
        }
    )
}

internal fun UserScriptsActivity.confirmForceUpdateForItem(script: UserScript) {
    if (uiState.isUpdateRunning) {
        Toast.makeText(this, getString(R.string.filter_list_operation_in_progress), Toast.LENGTH_SHORT).show()
        return
    }
    if (script.isLocal) {
        Toast.makeText(this, getString(R.string.user_scripts_item_local_no_update), Toast.LENGTH_SHORT).show()
        return
    }
    val name = scriptDisplayName(script)
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.user_scripts_force_update_selected_confirm_title),
        message = getString(R.string.filter_list_item_force_update_confirm_message, name),
        negativeLabel = getString(R.string.action_cancel),
        positiveLabel = getString(R.string.filter_list_force_update_action),
        onPositive = {
            startUserScriptUpdateCheck(
                forceUpdate = true,
                scriptsOverride = listOf(script),
                progressTitleOverride = getString(R.string.filter_list_item_force_update_progress_title, name)
            )
        }
    )
}

internal fun UserScriptsActivity.confirmRemoveUserScriptItem(script: UserScript) {
    if (uiState.isUpdateRunning) {
        Toast.makeText(this, getString(R.string.filter_list_operation_in_progress), Toast.LENGTH_SHORT).show()
        return
    }
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.user_scripts_delete_confirm_title),
        message = getString(R.string.user_scripts_delete_confirm_message, 1),
        negativeLabel = getString(R.string.action_cancel),
        positiveLabel = getString(R.string.history_delete_selected),
        onPositive = {
            db.removeAll(setOf(script.id))
            UserScriptState.bumpDataVersion(this)
            reload()
        }
    )
}

internal fun UserScriptsActivity.copyUserScriptName(script: UserScript) {
    val name = scriptDisplayName(script)
    val clipboard = getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.user_scripts_name_clip_label), name))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(this, getString(R.string.filter_list_item_name_copied), Toast.LENGTH_SHORT).show()
    }
}

internal fun UserScriptsActivity.copyUserScriptSourceLink(script: UserScript) {
    val link = script.sourceUrl ?: return
    val clipboard = getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.user_scripts_link_clip_label), link))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(this, getString(R.string.filter_list_item_link_copied), Toast.LENGTH_SHORT).show()
    }
}

internal fun UserScriptsActivity.shareUserScriptSourceLink(script: UserScript) {
    val link = script.sourceUrl ?: return
    try {
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, link) },
                getString(R.string.user_scripts_share_chooser_title)
            )
        )
    } catch (_: Exception) {
    }
}

internal fun UserScriptsActivity.confirmCheckUpdateForSelection() {
    val selection = selectedUserScripts()
    if (selection.isEmpty()) return
    if (uiState.isUpdateRunning) {
        Toast.makeText(this, getString(R.string.filter_list_operation_in_progress), Toast.LENGTH_SHORT).show()
        return
    }
    val updatable = selection.filter { !it.isLocal }
    if (updatable.isEmpty()) {
        uiState.confirmDialog = ConfirmDialogConfig(
            title = getString(R.string.filter_list_update_check_title),
            message = getString(R.string.user_scripts_selection_not_updatable),
            positiveLabel = getString(R.string.action_ok)
        )
        return
    }
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.filter_list_update_check_title),
        message = getString(R.string.user_scripts_check_update_selected_message, updatable.size),
        negativeLabel = getString(R.string.action_cancel),
        positiveLabel = getString(R.string.filter_list_update_check_action),
        onPositive = { startUserScriptUpdateCheck(forceUpdate = false, scriptsOverride = updatable) }
    )
}

internal fun UserScriptsActivity.confirmForceUpdateForSelection() {
    val selection = selectedUserScripts()
    if (selection.isEmpty()) return
    if (uiState.isUpdateRunning) {
        Toast.makeText(this, getString(R.string.filter_list_operation_in_progress), Toast.LENGTH_SHORT).show()
        return
    }
    val updatable = selection.filter { !it.isLocal }
    if (updatable.isEmpty()) {
        uiState.confirmDialog = ConfirmDialogConfig(
            title = getString(R.string.user_scripts_force_update_selected_confirm_title),
            message = getString(R.string.user_scripts_selection_not_updatable),
            positiveLabel = getString(R.string.action_ok)
        )
        return
    }
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.user_scripts_force_update_selected_confirm_title),
        message = getString(R.string.user_scripts_force_update_selected_confirm_message, updatable.size),
        negativeLabel = getString(R.string.action_cancel),
        positiveLabel = getString(R.string.filter_list_force_update_action),
        onPositive = { startUserScriptUpdateCheck(forceUpdate = true, scriptsOverride = updatable) }
    )
}

internal fun UserScriptsActivity.copySelectedUserScriptNames() {
    val selection = selectedUserScripts()
    if (selection.isEmpty()) return
    val clipboard = getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.user_scripts_name_clip_label), selection.joinToString("\n") { scriptDisplayName(it) }))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(this, getString(R.string.filter_list_selection_names_copied), Toast.LENGTH_SHORT).show()
    }
}

internal fun UserScriptsActivity.copySelectedUserScriptSourceLinks() {
    val selection = selectedUserScripts()
    if (selection.isEmpty()) return
    val clipboard = getSystemService(ClipboardManager::class.java)
    val combined = selection.mapNotNull { it.sourceUrl?.takeIf { url -> url.isNotBlank() } }.joinToString("\n")
    clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.user_scripts_link_clip_label), combined))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(this, getString(R.string.filter_list_selection_links_copied), Toast.LENGTH_SHORT).show()
    }
}

internal fun UserScriptsActivity.shareSelectedUserScriptSourceLinks() {
    val selection = selectedUserScripts()
    if (selection.isEmpty()) return
    try {
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, selection.mapNotNull { it.sourceUrl?.takeIf { url -> url.isNotBlank() } }.joinToString("\n"))
                },
                getString(R.string.user_scripts_share_chooser_title)
            )
        )
    } catch (_: Exception) {
    }
}
