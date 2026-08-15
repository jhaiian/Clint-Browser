package com.jhaiian.clint.quiver

import android.widget.Toast

import com.jhaiian.clint.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// Launches a download coroutine for the given filter list, reflects byte-level progress
// through uiState.downloadProgress, and persists the result to the database on success.
// Cancelling the dialog cancels the coroutine, which in turn cancels the OkHttp call and
// deletes the partial download file.
internal fun QuiverGuardActivity.startFilterListDownload(filterList: FilterList) {
    // Prevent duplicate downloads if the user taps the download button twice.
    if (isDownloadInProgress(filterList.id)) return
    val activity = this
    markDownloading(filterList.id, true)

    uiState.downloadProgress = DownloadProgressUi(filterListName = filterList.name, indeterminate = true)

    activeDownloadJob = activityScope.launch {
        var didSucceed = false
        try {
            FilterListDownloader.download(applicationContext, filterList).collect { progress ->
                when (progress) {
                    is FilterListDownloadProgress.Progress -> {
                        uiState.downloadProgress = DownloadProgressUi(
                            filterListName = filterList.name,
                            bytesRead = progress.bytesRead,
                            totalBytes = progress.totalBytes,
                            indeterminate = progress.totalBytes <= 0
                        )
                    }
                    is FilterListDownloadProgress.Success -> {
                        didSucceed = true
                        val downloadedAt = System.currentTimeMillis()
                        // Persist all download metadata including ETag and Last-Modified for future
                        // conditional HTTP requests during update checks.
                        database().updateDownloadResult(
                            filterList.id,
                            progress.file.absolutePath,
                            progress.bytesTotal,
                            downloadedAt,
                            progress.ruleCount,
                            progress.etag,
                            progress.lastModified
                        )
                        // Auto-enable the list after a successful download so it is immediately
                        // available for the user to include in the next compile.
                        onFilterListDownloaded(filterList.id)
                        refreshFilterListDisplay()
                    }
                }
            }
            uiState.downloadProgress = null
            if (didSucceed) {
                Toast.makeText(activity, getString(R.string.quiver_guard_download_success_toast, filterList.name), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, getString(R.string.quiver_guard_download_error_toast, filterList.name), Toast.LENGTH_SHORT).show()
            }
        } catch (e: CancellationException) {
            uiState.downloadProgress = null
            Toast.makeText(activity, getString(R.string.quiver_guard_download_cancelled_toast), Toast.LENGTH_SHORT).show()
            throw e
        } catch (e: FilterListDownloadException) {
            uiState.downloadProgress = null
            Toast.makeText(activity, e.message ?: getString(R.string.quiver_guard_download_error_toast, filterList.name), Toast.LENGTH_SHORT).show()
        } finally {
            markDownloading(filterList.id, false)
        }
    }
}
