package com.jhaiian.clint.quiver

import android.widget.Toast

import com.jhaiian.clint.R
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import com.jhaiian.clint.util.formatFileSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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

internal fun QuiverGuardActivity.startFilterListUpdateCheck(
    forceUpdate: Boolean = false,
    listsOverride: List<FilterList>? = null,
    progressTitleOverride: String? = null
) {
    if (uiState.isUpdateRunning || uiState.isCompileRunning) return
    val filterLists = listsOverride ?: effectiveFilterLists().filter { it.isDownloaded && !it.isLocal }
    if (filterLists.isEmpty()) return

    uiState.isUpdateRunning = true

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

private fun QuiverGuardActivity.triggerRecompilationAfterUpdate() {
    if (!uiState.isCompileRunning) {
        startCompilation()
    }
}

internal fun QuiverGuardActivity.getActiveFilterLists(): List<FilterList> =
    effectiveFilterLists().filter { it.isEnabled && it.isDownloaded }

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
