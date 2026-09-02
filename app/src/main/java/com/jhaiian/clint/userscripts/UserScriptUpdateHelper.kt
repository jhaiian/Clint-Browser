package com.jhaiian.clint.userscripts

import android.widget.Toast

import com.jhaiian.clint.R
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import com.jhaiian.clint.quiver.UpdateProgressUi
import com.jhaiian.clint.quiver.UpdateResultUi
import com.jhaiian.clint.util.formatFileSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal fun UserScriptsActivity.showUserScriptUpdateConfirmation() {
    if (uiState.isUpdateRunning) return
    val updatableCount = uiState.scripts.count { !it.isLocal }
    if (updatableCount == 0) {
        uiState.confirmDialog = ConfirmDialogConfig(
            title = getString(R.string.filter_list_update_check_title),
            message = getString(R.string.user_scripts_update_no_scripts_message),
            positiveLabel = getString(R.string.action_ok)
        )
        return
    }
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.filter_list_update_check_title),
        message = getString(R.string.user_scripts_update_check_message, updatableCount),
        negativeLabel = getString(R.string.action_cancel),
        positiveLabel = getString(R.string.filter_list_update_check_action),
        onPositive = { startUserScriptUpdateCheck(forceUpdate = false) }
    )
}

internal fun UserScriptsActivity.startUserScriptUpdateCheck(
    forceUpdate: Boolean = false,
    scriptsOverride: List<UserScript>? = null,
    progressTitleOverride: String? = null
) {
    if (uiState.isUpdateRunning) return
    val scripts = scriptsOverride ?: uiState.scripts.filter { !it.isLocal }
    if (scripts.isEmpty()) return

    uiState.isUpdateRunning = true
    uiState.updatingIds = scripts.map { it.id }.toSet()

    val dialogTitle = progressTitleOverride ?: if (forceUpdate) {
        getString(R.string.user_scripts_force_update_progress_title)
    } else {
        getString(R.string.filter_list_update_progress_title)
    }
    uiState.updateProgress = UpdateProgressUi(
        title = dialogTitle,
        totalCount = scripts.size,
        statusText = if (forceUpdate) getString(R.string.filter_list_force_update_progress_preparing) else getString(R.string.filter_list_update_progress_checking)
    )

    val updatedResults = mutableListOf<UserScriptUpdateItemResult.Updated>()
    val failedResults = mutableListOf<UserScriptUpdateItemResult.Failed>()
    var upToDateCount = 0
    var processedCount = 0

    activeUpdateJob = activityScope.launch {
        try {
            UserScriptUpdateChecker.checkAndUpdateAll(
                applicationContext,
                scripts,
                forceUpdate = forceUpdate
            ).collect { event ->
                when (event) {
                    is UserScriptUpdateEvent.CheckingScript -> {
                        processedCount = event.index
                        uiState.updateProgress = uiState.updateProgress?.copy(
                            processedCount = event.index,
                            statusText = if (forceUpdate) getString(R.string.filter_list_force_update_progress_preparing) else getString(R.string.filter_list_update_progress_checking),
                            currentListName = UserScriptMetadataParser.parse(event.script.code, "Untitled Script").name
                        )
                    }
                    is UserScriptUpdateEvent.DownloadingScript -> {
                        uiState.updateProgress = uiState.updateProgress?.copy(
                            statusText = if (event.totalBytes > 0L) {
                                getString(R.string.filter_list_update_progress_downloading_known, formatFileSize(event.bytesRead), formatFileSize(event.totalBytes))
                            } else {
                                getString(R.string.filter_list_update_progress_downloading_unknown, formatFileSize(event.bytesRead))
                            }
                        )
                    }
                    is UserScriptUpdateEvent.ItemComplete -> {
                        processedCount++
                        uiState.updateProgress = uiState.updateProgress?.copy(processedCount = processedCount)
                        when (val result = event.result) {
                            is UserScriptUpdateItemResult.Updated -> {
                                val updatedAt = System.currentTimeMillis()
                                db.applyUpdateResult(
                                    result.script.id, result.newCode, result.newRequiresCache,
                                    result.newEtag, result.newLastModified, updatedAt
                                )
                                updatedResults.add(result)
                            }
                            is UserScriptUpdateItemResult.Failed -> failedResults.add(result)
                            is UserScriptUpdateItemResult.UpToDate -> upToDateCount++
                        }
                    }
                }
            }
            uiState.updateProgress = null
            onUserScriptUpdateCheckComplete(updatedResults, failedResults, upToDateCount)
        } catch (e: CancellationException) {
            uiState.updateProgress = null
            Toast.makeText(this@startUserScriptUpdateCheck, getString(R.string.filter_list_update_cancelled), Toast.LENGTH_SHORT).show()
            throw e
        } catch (_: Exception) {
            uiState.updateProgress = null
            Toast.makeText(this@startUserScriptUpdateCheck, getString(R.string.filter_list_update_error_generic), Toast.LENGTH_SHORT).show()
        } finally {
            uiState.isUpdateRunning = false
            uiState.updatingIds = emptySet()
        }
    }
}

private fun UserScriptsActivity.onUserScriptUpdateCheckComplete(
    updatedResults: List<UserScriptUpdateItemResult.Updated>,
    failedResults: List<UserScriptUpdateItemResult.Failed>,
    upToDateCount: Int
) {
    UserScriptState.bumpDataVersion(this)
    reload()

    when {
        updatedResults.isEmpty() && failedResults.isEmpty() -> {
            Toast.makeText(this, getString(R.string.user_scripts_update_all_up_to_date), Toast.LENGTH_SHORT).show()
        }
        updatedResults.isNotEmpty() && failedResults.isEmpty() -> {
            Toast.makeText(this, getString(R.string.user_scripts_update_success, updatedResults.size), Toast.LENGTH_SHORT).show()
        }
        updatedResults.isNotEmpty() && failedResults.isNotEmpty() -> {
            val failedNames = failedResults.joinToString(separator = "\n") { "• ${UserScriptMetadataParser.parse(it.script.code, "Untitled Script").name}" }
            uiState.updateResult = UpdateResultUi(
                title = getString(R.string.filter_list_update_result_title),
                message = getString(R.string.user_scripts_update_partial_result, updatedResults.size, failedResults.size, failedNames)
            )
        }
        else -> {
            val failedNames = failedResults.joinToString(separator = "\n") { "• ${UserScriptMetadataParser.parse(it.script.code, "Untitled Script").name}" }
            uiState.updateResult = UpdateResultUi(
                title = getString(R.string.filter_list_update_result_title),
                message = getString(R.string.user_scripts_update_all_failed, failedResults.size, failedNames)
            )
        }
    }
}

internal fun UserScriptsActivity.getActiveUserScripts(): List<UserScript> =
    uiState.scripts.filter { it.enabled }

internal fun UserScriptsActivity.showActiveUserScriptUpdateConfirmation(forceUpdate: Boolean) {
    if (uiState.isUpdateRunning) {
        Toast.makeText(this, getString(R.string.filter_list_operation_in_progress), Toast.LENGTH_SHORT).show()
        return
    }
    val activeScripts = getActiveUserScripts().filter { !it.isLocal }
    if (activeScripts.isEmpty()) {
        uiState.confirmDialog = ConfirmDialogConfig(
            title = if (forceUpdate) getString(R.string.user_scripts_force_update_active_confirm_title) else getString(R.string.filter_list_update_check_title),
            message = getString(R.string.user_scripts_no_active_selected),
            positiveLabel = getString(R.string.action_ok)
        )
        return
    }
    val (title, message, action) = if (forceUpdate) {
        Triple(
            getString(R.string.user_scripts_force_update_active_confirm_title),
            getString(R.string.user_scripts_force_update_active_confirm_message, activeScripts.size),
            getString(R.string.filter_list_force_update_action)
        )
    } else {
        Triple(
            getString(R.string.filter_list_update_check_title),
            getString(R.string.user_scripts_check_update_active_message, activeScripts.size),
            getString(R.string.filter_list_update_check_action)
        )
    }
    uiState.confirmDialog = ConfirmDialogConfig(
        title = title, message = message,
        negativeLabel = getString(R.string.action_cancel),
        positiveLabel = action,
        onPositive = { startUserScriptUpdateCheck(forceUpdate = forceUpdate, scriptsOverride = activeScripts) }
    )
}

internal fun UserScriptsActivity.showForceUpdateAllUserScriptsConfirmation() {
    if (uiState.isUpdateRunning) {
        Toast.makeText(this, getString(R.string.filter_list_operation_in_progress), Toast.LENGTH_SHORT).show()
        return
    }
    val updatableCount = uiState.scripts.count { !it.isLocal }
    if (updatableCount == 0) {
        uiState.confirmDialog = ConfirmDialogConfig(
            title = getString(R.string.user_scripts_force_update_all_confirm_title),
            message = getString(R.string.user_scripts_force_update_no_scripts_message),
            positiveLabel = getString(R.string.action_ok)
        )
        return
    }
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.user_scripts_force_update_all_confirm_title),
        message = getString(R.string.user_scripts_force_update_all_confirm_message, updatableCount),
        negativeLabel = getString(R.string.action_cancel),
        positiveLabel = getString(R.string.filter_list_force_update_action),
        onPositive = { startUserScriptUpdateCheck(forceUpdate = true) }
    )
}
