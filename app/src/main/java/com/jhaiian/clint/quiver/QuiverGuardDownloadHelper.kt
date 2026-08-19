package com.jhaiian.clint.quiver

import android.widget.Toast

import com.jhaiian.clint.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal fun QuiverGuardActivity.startFilterListDownload(filterList: FilterList) {

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

                        database().updateDownloadResult(
                            filterList.id,
                            progress.file.absolutePath,
                            progress.bytesTotal,
                            downloadedAt,
                            progress.ruleCount,
                            progress.etag,
                            progress.lastModified
                        )

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
