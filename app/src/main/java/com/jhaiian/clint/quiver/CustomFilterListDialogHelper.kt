package com.jhaiian.clint.quiver

import android.widget.Toast

import com.jhaiian.clint.R
import kotlinx.coroutines.launch

// Opens the two-stage add-from-link dialog (see AddFilterListFromLinkDialog in
// QuiverGuardAddDialogs.kt). Resets any leftover status from a previous attempt so the
// dialog always starts at Stage 1.
internal fun QuiverGuardActivity.showAddFilterListDialog() {
    uiState.addLinkFetchStatus = AddLinkFetchStatus.Idle
    uiState.addFromLinkDialogOpen = true
}

// Stage 1: downloads and validates the URL's content into a temp file, reflecting
// progress through uiState.addLinkFetchStatus. On success the dialog reveals the title
// field, pre-filled from a "! Title:" metadata comment if the list declares one.
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

// Called when the user edits the URL after a fetch completed or failed: discards the
// temp file (if any) and resets back to Stage 1 so a stale fetch result can't be
// confirmed against a since-edited URL.
internal fun QuiverGuardActivity.resetFilterListLinkFetch() {
    (uiState.addLinkFetchStatus as? AddLinkFetchStatus.Fetched)?.file?.let { if (it.exists()) it.delete() }
    uiState.addLinkFetchStatus = AddLinkFetchStatus.Idle
}

// Stage 2: persists the already-fetched temp file as a new custom filter list, using
// the (possibly edited) title. Moves the temp file into permanent storage, records the
// download result so it behaves exactly like a normally-downloaded list, and marks it
// enabled so it is included in the next compile without an extra tap.
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
