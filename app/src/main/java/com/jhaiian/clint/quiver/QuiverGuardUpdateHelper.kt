package com.jhaiian.clint.quiver

import android.widget.Toast

import com.jhaiian.clint.R
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import com.jhaiian.clint.util.formatFileSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// Shows a confirmation dialog before starting an update check so the user is aware that
// the operation will make network requests. Skips immediately (info-only) if no lists
// have been downloaded yet, since there is nothing to update.
internal fun QuiverGuardActivity.showFilterListUpdateConfirmation() {
    if (uiState.isUpdateRunning || uiState.isCompileRunning) return
    val downloadedCount = effectiveFilterLists().count { it.isDownloaded && !it.isLocal }
    if (downloadedCount == 0) {
        uiState.confirmDialog = ConfirmDialogConfig(
            title = getString(R.string.filter_list_update_check_title),
            message = getString(R.string.filter_list_update_no_lists_message),
            positiveLabel = getString(R.string.action_ok)
        )
        return
    }
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.filter_list_update_check_title),
        message = getString(R.string.filter_list_update_check_message, downloadedCount),
        negativeLabel = getString(R.string.action_cancel),
        positiveLabel = getString(R.string.filter_list_update_check_action),
        onPositive = { startFilterListUpdateCheck(forceUpdate = false) }
    )
}

// Iterates over filter lists using FilterListUpdateChecker, emitting progress events for
// each one. Results are accumulated and handled by onUpdateCheckComplete. listsOverride
// lets callers supply a pre-filtered subset (e.g. only enabled lists, or a single list
// from the per-row overflow menu). When null, all downloaded lists are used. When
// forceUpdate is true, the checker skips conditional HTTP headers so the server always
// responds with fresh content. progressTitleOverride lets callers (e.g. single-item
// operations) show a more specific title than the generic "Checking for Updates" /
// "Force Updating Filter Lists" defaults.
internal fun QuiverGuardActivity.startFilterListUpdateCheck(
    forceUpdate: Boolean = false,
    listsOverride: List<FilterList>? = null,
    progressTitleOverride: String? = null
) {
    if (uiState.isUpdateRunning || uiState.isCompileRunning) return
    val filterLists = listsOverride ?: effectiveFilterLists().filter { it.isDownloaded && !it.isLocal }
    if (filterLists.isEmpty()) return

    uiState.isUpdateRunning = true

    // Use a different title when force-updating so the user knows all lists are being
    // re-downloaded rather than conditionally checked. A caller-supplied override takes
    // precedence (used for single-item operations to name the list).
    val dialogTitle = progressTitleOverride ?: if (forceUpdate) {
        getString(R.string.filter_list_force_update_progress_title)
    } else {
        getString(R.string.filter_list_update_progress_title)
    }
    uiState.updateProgress = UpdateProgressUi(
        title = dialogTitle,
        totalCount = filterLists.size,
        statusText = if (forceUpdate) getString(R.string.filter_list_force_update_progress_preparing) else getString(R.string.filter_list_update_progress_checking)
    )

    val updatedResults = mutableListOf<FilterListUpdateItemResult.Updated>()
    val failedResults = mutableListOf<FilterListUpdateItemResult.Failed>()
    var upToDateCount = 0
    var processedCount = 0

    activeUpdateJob = activityScope.launch {
        try {
            FilterListUpdateChecker.checkAndUpdateAll(
                applicationContext,
                filterLists,
                forceUpdate = forceUpdate
            ).collect { event ->
                when (event) {
                    is FilterListUpdateEvent.CheckingList -> {
                        processedCount = event.index
                        uiState.updateProgress = uiState.updateProgress?.copy(
                            processedCount = event.index,
                            statusText = if (forceUpdate) getString(R.string.filter_list_force_update_progress_preparing) else getString(R.string.filter_list_update_progress_checking),
                            currentListName = event.filterList.name
                        )
                    }
                    is FilterListUpdateEvent.DownloadingList -> {
                        // Show determinate progress once the content-length is known, or a
                        // size-only indicator when it is not available.
                        uiState.updateProgress = uiState.updateProgress?.copy(
                            statusText = if (event.totalBytes > 0L) {
                                getString(R.string.filter_list_update_progress_downloading_known, formatFileSize(event.bytesRead), formatFileSize(event.totalBytes))
                            } else {
                                getString(R.string.filter_list_update_progress_downloading_unknown, formatFileSize(event.bytesRead))
                            }
                        )
                    }
                    is FilterListUpdateEvent.ItemComplete -> {
                        processedCount++
                        uiState.updateProgress = uiState.updateProgress?.copy(processedCount = processedCount)
                        when (val result = event.result) {
                            is FilterListUpdateItemResult.Updated -> {
                                // Persist the new download metadata so future update checks can
                                // use the new ETag or Last-Modified headers.
                                val downloadedAt = System.currentTimeMillis()
                                database().updateDownloadResult(
                                    result.filterList.id,
                                    FilterListDownloader.localFileFor(applicationContext, result.filterList.id).absolutePath,
                                    result.newFileSizeBytes, downloadedAt, result.newRuleCount,
                                    result.newEtag, result.newLastModified
                                )
                                updatedResults.add(result)
                            }
                            is FilterListUpdateItemResult.Failed -> failedResults.add(result)
                            is FilterListUpdateItemResult.UpToDate -> upToDateCount++
                            is FilterListUpdateItemResult.Skipped -> {}
                        }
                    }
                }
            }
            uiState.updateProgress = null
            onUpdateCheckComplete(updatedResults, failedResults, upToDateCount)
        } catch (e: CancellationException) {
            uiState.updateProgress = null
            Toast.makeText(this@startFilterListUpdateCheck, getString(R.string.filter_list_update_cancelled), Toast.LENGTH_SHORT).show()
            throw e
        } catch (_: Exception) {
            uiState.updateProgress = null
            Toast.makeText(this@startFilterListUpdateCheck, getString(R.string.filter_list_update_error_generic), Toast.LENGTH_SHORT).show()
        } finally {
            uiState.isUpdateRunning = false
        }
    }
}

// Handles the four possible outcome combinations after all lists are processed: all
// up-to-date (toast), all updated successfully (toast + recompile), some updated and
// some failed (result dialog with recompile option), all failed (info result dialog).
private fun QuiverGuardActivity.onUpdateCheckComplete(
    updatedResults: List<FilterListUpdateItemResult.Updated>,
    failedResults: List<FilterListUpdateItemResult.Failed>,
    upToDateCount: Int
) {
    refreshFilterListDisplay()

    when {
        updatedResults.isEmpty() && failedResults.isEmpty() -> {
            Toast.makeText(this, getString(R.string.filter_list_update_all_up_to_date), Toast.LENGTH_SHORT).show()
        }
        updatedResults.isNotEmpty() && failedResults.isEmpty() -> {
            Toast.makeText(this, getString(R.string.filter_list_update_success_recompiling, updatedResults.size), Toast.LENGTH_SHORT).show()
            triggerRecompilationAfterUpdate()
        }
        updatedResults.isNotEmpty() && failedResults.isNotEmpty() -> {
            showPartialUpdateResultDialog(updatedResults.size, failedResults)
        }
        else -> {
            val failedNames = failedResults.joinToString(separator = "\n") { "• ${it.filterList.name}" }
            uiState.updateResult = UpdateResultUi(
                title = getString(R.string.filter_list_update_result_title),
                message = getString(R.string.filter_list_update_all_failed, failedResults.size, failedNames)
            )
        }
    }
}

// Shown when at least one list was updated and at least one failed, giving the user the
// choice to compile with the partial update or cancel.
private fun QuiverGuardActivity.showPartialUpdateResultDialog(
    updatedCount: Int,
    failedResults: List<FilterListUpdateItemResult.Failed>
) {
    val failedNames = failedResults.joinToString(separator = "\n") { "• ${it.filterList.name}" }
    uiState.updateResult = UpdateResultUi(
        title = getString(R.string.filter_list_update_result_title),
        message = getString(R.string.filter_list_update_partial_result, updatedCount, failedResults.size, failedNames),
        onCompile = { triggerRecompilationAfterUpdate() }
    )
}

// Starts a compile run after a successful update so the engine immediately benefits
// from the freshly downloaded rule content.
private fun QuiverGuardActivity.triggerRecompilationAfterUpdate() {
    if (!uiState.isCompileRunning) {
        startCompilation()
    }
}

// Returns all filter lists that are both enabled and downloaded. These are the "active"
// lists — the ones currently contributing to request filtering. Used by the overflow
// menu operations that target the active subset only.
internal fun QuiverGuardActivity.getActiveFilterLists(): List<FilterList> =
    effectiveFilterLists().filter { it.isEnabled && it.isDownloaded }

// Confirms before checking for updates on (or force-downloading) only the
// enabled+downloaded filter lists. If no lists are enabled, informs the user so they
// know to enable at least one list first.
internal fun QuiverGuardActivity.showActiveFilterListUpdateConfirmation(forceUpdate: Boolean) {
    if (uiState.isUpdateRunning || uiState.isCompileRunning) {
        Toast.makeText(this, getString(R.string.filter_list_operation_in_progress), Toast.LENGTH_SHORT).show()
        return
    }
    val activeLists = getActiveFilterLists().filter { !it.isLocal }
    if (activeLists.isEmpty()) {
        uiState.confirmDialog = ConfirmDialogConfig(
            title = if (forceUpdate) getString(R.string.filter_list_force_update_active_confirm_title) else getString(R.string.filter_list_update_check_title),
            message = getString(R.string.filter_list_no_active_selected),
            positiveLabel = getString(R.string.action_ok)
        )
        return
    }
    val (title, message, action) = if (forceUpdate) {
        Triple(
            getString(R.string.filter_list_force_update_active_confirm_title),
            getString(R.string.filter_list_force_update_active_confirm_message, activeLists.size),
            getString(R.string.filter_list_force_update_action)
        )
    } else {
        Triple(
            getString(R.string.filter_list_update_check_title),
            getString(R.string.filter_list_check_update_active_message, activeLists.size),
            getString(R.string.filter_list_update_check_action)
        )
    }
    uiState.confirmDialog = ConfirmDialogConfig(
        title = title, message = message,
        negativeLabel = getString(R.string.action_cancel),
        positiveLabel = action,
        onPositive = { startFilterListUpdateCheck(forceUpdate = forceUpdate, listsOverride = activeLists) }
    )
}

// Shows a confirmation dialog before force-updating all downloaded filter lists.
// Reports the count so the user knows the scope of the network operation.
internal fun QuiverGuardActivity.showForceUpdateAllConfirmation() {
    if (uiState.isUpdateRunning || uiState.isCompileRunning) {
        Toast.makeText(this, getString(R.string.filter_list_operation_in_progress), Toast.LENGTH_SHORT).show()
        return
    }
    val downloadedCount = effectiveFilterLists().count { it.isDownloaded && !it.isLocal }
    if (downloadedCount == 0) {
        uiState.confirmDialog = ConfirmDialogConfig(
            title = getString(R.string.filter_list_force_update_all_confirm_title),
            message = getString(R.string.filter_list_force_update_no_lists_message),
            positiveLabel = getString(R.string.action_ok)
        )
        return
    }
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.filter_list_force_update_all_confirm_title),
        message = getString(R.string.filter_list_force_update_all_confirm_message, downloadedCount),
        negativeLabel = getString(R.string.action_cancel),
        positiveLabel = getString(R.string.filter_list_force_update_action),
        onPositive = { startFilterListUpdateCheck(forceUpdate = true) }
    )
}

// Shows a confirmation dialog before triggering a full recompile from the overflow
// menu. Unlike the FAB compile path (which only activates when there are unsaved
// changes), this can be triggered at any time.
internal fun QuiverGuardActivity.showRecompileConfirmation() {
    if (uiState.isCompileRunning) return
    if (uiState.isUpdateRunning) {
        Toast.makeText(this, getString(R.string.filter_list_operation_in_progress), Toast.LENGTH_SHORT).show()
        return
    }
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.filter_list_recompile_confirm_title),
        message = getString(R.string.filter_list_recompile_confirm_message),
        negativeLabel = getString(R.string.action_cancel),
        positiveLabel = getString(R.string.quiver_guard_back_dialog_compile),
        onPositive = { startCompilation() }
    )
}
