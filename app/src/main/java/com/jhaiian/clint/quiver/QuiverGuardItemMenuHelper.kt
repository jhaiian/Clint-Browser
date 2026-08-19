package com.jhaiian.clint.quiver

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig

private fun QuiverGuardActivity.selectedFilterLists(): List<FilterList> =
    uiState.filterLists.filter { it.id in uiState.selectedIds }

internal fun QuiverGuardActivity.confirmCheckUpdateForItem(filterList: FilterList) {
    if (uiState.isUpdateRunning || uiState.isCompileRunning || isDownloadInProgress(filterList.id)) {
        Toast.makeText(this, getString(R.string.filter_list_operation_in_progress), Toast.LENGTH_SHORT).show()
        return
    }
    if (filterList.isLocal) {
        Toast.makeText(this, getString(R.string.filter_list_item_local_no_update), Toast.LENGTH_SHORT).show()
        return
    }
    if (!filterList.isDownloaded) {
        Toast.makeText(this, getString(R.string.filter_list_item_not_downloaded, filterList.name), Toast.LENGTH_SHORT).show()
        return
    }
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.filter_list_update_check_title),
        message = getString(R.string.filter_list_item_check_update_confirm_message, filterList.name),
        negativeLabel = getString(R.string.action_cancel),
        positiveLabel = getString(R.string.filter_list_update_check_action),
        onPositive = {
            startFilterListUpdateCheck(
                forceUpdate = false,
                listsOverride = listOf(filterList),
                progressTitleOverride = getString(R.string.filter_list_item_check_update_progress_title, filterList.name)
            )
        }
    )
}

internal fun QuiverGuardActivity.confirmForceUpdateForItem(filterList: FilterList) {
    if (uiState.isUpdateRunning || uiState.isCompileRunning || isDownloadInProgress(filterList.id)) {
        Toast.makeText(this, getString(R.string.filter_list_operation_in_progress), Toast.LENGTH_SHORT).show()
        return
    }
    if (filterList.isLocal) {
        Toast.makeText(this, getString(R.string.filter_list_item_local_no_update), Toast.LENGTH_SHORT).show()
        return
    }
    if (!filterList.isDownloaded) {
        Toast.makeText(this, getString(R.string.filter_list_item_not_downloaded, filterList.name), Toast.LENGTH_SHORT).show()
        return
    }
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.filter_list_force_update_selected_confirm_title),
        message = getString(R.string.filter_list_item_force_update_confirm_message, filterList.name),
        negativeLabel = getString(R.string.action_cancel),
        positiveLabel = getString(R.string.filter_list_force_update_action),
        onPositive = {
            startFilterListUpdateCheck(
                forceUpdate = true,
                listsOverride = listOf(filterList),
                progressTitleOverride = getString(R.string.filter_list_item_force_update_progress_title, filterList.name)
            )
        }
    )
}

internal fun QuiverGuardActivity.confirmRemoveFilterListItem(filterList: FilterList) {
    if (uiState.isUpdateRunning || uiState.isCompileRunning || isDownloadInProgress(filterList.id)) {
        Toast.makeText(this, getString(R.string.filter_list_operation_in_progress), Toast.LENGTH_SHORT).show()
        return
    }
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.filter_list_delete_confirm_title),
        message = getString(R.string.filter_list_delete_confirm_message, 1),
        negativeLabel = getString(R.string.action_cancel),
        positiveLabel = getString(R.string.history_delete_selected),
        onPositive = { stagePendingRemoval(filterList.id) }
    )
}

internal fun QuiverGuardActivity.copyFilterListName(filterList: FilterList) {
    val clipboard = getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.filter_list_name_clip_label), filterList.name))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(this, getString(R.string.filter_list_item_name_copied), Toast.LENGTH_SHORT).show()
    }
}

internal fun QuiverGuardActivity.copyFilterListDownloadLink(filterList: FilterList) {
    val clipboard = getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.filter_list_link_clip_label), filterList.downloadUrl))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(this, getString(R.string.filter_list_item_link_copied), Toast.LENGTH_SHORT).show()
    }
}

internal fun QuiverGuardActivity.shareFilterListDownloadLink(filterList: FilterList) {
    try {
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, filterList.downloadUrl) },
                getString(R.string.filter_list_share_chooser_title)
            )
        )
    } catch (_: Exception) {
    }
}

internal fun QuiverGuardActivity.confirmCheckUpdateForSelection() {
    val selection = selectedFilterLists()
    if (selection.isEmpty()) return
    if (uiState.isUpdateRunning || uiState.isCompileRunning || selection.any { isDownloadInProgress(it.id) }) {
        Toast.makeText(this, getString(R.string.filter_list_operation_in_progress), Toast.LENGTH_SHORT).show()
        return
    }
    val downloaded = selection.filter { it.isDownloaded && !it.isLocal }
    if (downloaded.isEmpty()) {
        uiState.confirmDialog = ConfirmDialogConfig(
            title = getString(R.string.filter_list_update_check_title),
            message = getString(R.string.filter_list_selection_not_downloaded),
            positiveLabel = getString(R.string.action_ok)
        )
        return
    }
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.filter_list_update_check_title),
        message = getString(R.string.filter_list_check_update_selected_message, downloaded.size),
        negativeLabel = getString(R.string.action_cancel),
        positiveLabel = getString(R.string.filter_list_update_check_action),
        onPositive = { startFilterListUpdateCheck(forceUpdate = false, listsOverride = downloaded) }
    )
}

internal fun QuiverGuardActivity.confirmForceUpdateForSelection() {
    val selection = selectedFilterLists()
    if (selection.isEmpty()) return
    if (uiState.isUpdateRunning || uiState.isCompileRunning || selection.any { isDownloadInProgress(it.id) }) {
        Toast.makeText(this, getString(R.string.filter_list_operation_in_progress), Toast.LENGTH_SHORT).show()
        return
    }
    val downloaded = selection.filter { it.isDownloaded && !it.isLocal }
    if (downloaded.isEmpty()) {
        uiState.confirmDialog = ConfirmDialogConfig(
            title = getString(R.string.filter_list_force_update_selected_confirm_title),
            message = getString(R.string.filter_list_selection_not_downloaded),
            positiveLabel = getString(R.string.action_ok)
        )
        return
    }
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.filter_list_force_update_selected_confirm_title),
        message = getString(R.string.filter_list_force_update_selected_confirm_message, downloaded.size),
        negativeLabel = getString(R.string.action_cancel),
        positiveLabel = getString(R.string.filter_list_force_update_action),
        onPositive = { startFilterListUpdateCheck(forceUpdate = true, listsOverride = downloaded) }
    )
}

internal fun QuiverGuardActivity.copySelectedFilterListNames() {
    val selection = selectedFilterLists()
    if (selection.isEmpty()) return
    val clipboard = getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.filter_list_name_clip_label), selection.joinToString("\n") { it.name }))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(this, getString(R.string.filter_list_selection_names_copied), Toast.LENGTH_SHORT).show()
    }
}

internal fun QuiverGuardActivity.copySelectedFilterListDownloadLinks() {
    val selection = selectedFilterLists()
    if (selection.isEmpty()) return
    val clipboard = getSystemService(ClipboardManager::class.java)
    val combined = selection.filterNot { it.isLocal }.joinToString("\n") { it.downloadUrl }
    clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.filter_list_link_clip_label), combined))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(this, getString(R.string.filter_list_selection_links_copied), Toast.LENGTH_SHORT).show()
    }
}

internal fun QuiverGuardActivity.shareSelectedFilterListDownloadLinks() {
    val selection = selectedFilterLists()
    if (selection.isEmpty()) return
    try {
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, selection.filterNot { it.isLocal }.joinToString("\n") { it.downloadUrl })
                },
                getString(R.string.filter_list_share_chooser_title)
            )
        )
    } catch (_: Exception) {
    }
}
