package com.jhaiian.clint.quiver

import android.widget.Toast

import com.jhaiian.clint.R
import kotlinx.coroutines.launch

internal fun QuiverGuardActivity.showAddFilterListDialog() {
    uiState.addLinkFetchStatus = AddLinkFetchStatus.Idle
    uiState.addFromLinkDialogOpen = true
}

internal fun QuiverGuardActivity.fetchFilterListFromUrl(url: String) {
    uiState.addLinkFetchStatus = AddLinkFetchStatus.Fetching(0L, 0L)
    activityScope.launch {
        try {
            CustomFilterListFetcher.fetch(applicationContext, url).collect { progress ->
                when (progress) {
                    is CustomFilterListFetchProgress.Progress ->
                        uiState.addLinkFetchStatus = AddLinkFetchStatus.Fetching(progress.bytesRead, progress.totalBytes)
                    is CustomFilterListFetchProgress.Success ->
                        uiState.addLinkFetchStatus = AddLinkFetchStatus.Fetched(progress.file, progress.bytesTotal, progress.ruleCount, progress.metadata["Title"])
                }
            }
        } catch (e: Exception) {
            uiState.addLinkFetchStatus = AddLinkFetchStatus.Error(e.message ?: getString(R.string.filter_list_add_error_invalid_url))
        }
    }
}

internal fun QuiverGuardActivity.resetFilterListLinkFetch() {
    (uiState.addLinkFetchStatus as? AddLinkFetchStatus.Fetched)?.file?.let { if (it.exists()) it.delete() }
    uiState.addLinkFetchStatus = AddLinkFetchStatus.Idle
}

internal fun QuiverGuardActivity.confirmAddFilterListFromLink(url: String, title: String) {
    val fetched = uiState.addLinkFetchStatus as? AddLinkFetchStatus.Fetched ?: return
    val id = database().addCustomFilterList(title, url)
    val destFile = FilterListDownloader.localFileFor(applicationContext, id)
    try {
        fetched.file.copyTo(destFile, overwrite = true)
        fetched.file.delete()
    } catch (_: Exception) {
    }
    database().updateDownloadResult(id, destFile.absolutePath, fetched.sizeBytes, System.currentTimeMillis(), fetched.ruleCount, null, null)
    onFilterListAdded(FilterList(id = id, name = title, downloadUrl = url, isEnabled = true, localPath = destFile.absolutePath, fileSizeBytes = fetched.sizeBytes, downloadedAt = System.currentTimeMillis(), ruleCount = fetched.ruleCount, isCustom = true))
    uiState.addFromLinkDialogOpen = false
    uiState.addLinkFetchStatus = AddLinkFetchStatus.Idle
    Toast.makeText(this, getString(R.string.quiver_guard_download_success_toast, title), Toast.LENGTH_SHORT).show()
}
