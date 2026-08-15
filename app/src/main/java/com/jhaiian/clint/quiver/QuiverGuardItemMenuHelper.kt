package com.jhaiian.clint.quiver

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig

// The per-row overflow menu is rendered in Compose (FilterListItemOptionsMenu) locally
// inside FilterListRow, anchored to its own more-button so it opens next to the row the
// user tapped rather than at some detached anchor. The selection-mode overflow (shown
// when items are checked) is a separate instance anchored the same way inside the
// toolbar, next to its own more-button. No PopupWindow-building functions are needed
// here; this file keeps only the six action handlers both menus delegate to.

private fun QuiverGuardActivity.selectedFilterLists(): List<FilterList> =
    uiState.filterLists.filter { it.id in uiState.selectedIds }

// Confirms then performs a conditional update check scoped to a single list. Blocked
// while another compile/update is running, and requires the list to already be
// downloaded since there is nothing to conditionally check against otherwise (no prior
// ETag/Last-Modified to send, no local file to compare).
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

// Confirms then performs an unconditional re-download scoped to a single list. "Force"
// means no conditional headers are sent regardless of any stored ETag or Last-Modified,
// so the list is always redownloaded and always recompiled on success — never
// short-circuited to an UpToDate result.
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

// Confirms then stages a single list for removal, reusing the same staged-removal
// pattern as the multi-select delete flow: the database row and local file are only
// actually deleted once the next compile completes successfully, so the removal can
// still be discarded via the existing "discard changes" path.
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

// Copies the filter list's display name to the clipboard. A toast confirmation is only
// shown pre-Android-13, since the system clipboard overlay already confirms the copy
// on Tiramisu and above.
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

// Confirms then checks the downloaded subset of the current selection for available
// updates. Lists never downloaded are silently excluded rather than blocking the whole
// batch, mirroring how the toolbar's "active lists" action only ever targets lists that
// can meaningfully be checked.
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

// Confirms then force-updates the downloaded subset of the selection, the same way
// confirmForceUpdateForItem does for a single list.
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

// Copies every selected list's name to the clipboard as one block of text with one name
// per line, so the result can be pasted as a ready-made list.
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
