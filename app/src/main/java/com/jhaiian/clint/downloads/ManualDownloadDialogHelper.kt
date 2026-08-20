package com.jhaiian.clint.downloads

import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.delegates.PREF_BATTERY_OPT_ASKED
import com.jhaiian.clint.settings.downloads.DownloadSettingsKeys
import java.io.File

internal fun resolveFilename(url: String, contentDisposition: String, contentType: String): String {
    if (contentDisposition.isNotBlank()) {
        val cdFilename = DownloadFileHelper.extractFilenameFromContentDisposition(contentDisposition)
        if (!cdFilename.isNullOrBlank()) return cdFilename
    }
    val guessed = URLUtil.guessFileName(url, contentDisposition, contentType)
    if (!guessed.isNullOrBlank() && guessed != "downloadfile") return guessed
    val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType)
    val nameFromUrl = url.substringAfterLast('/').substringBefore('?').substringBefore('#')
        .ifBlank { "download" }
    return if (ext != null && !nameFromUrl.contains('.')) "$nameFromUrl.$ext" else nameFromUrl
}

internal fun DownloadsActivity.performManualDownload(
    url: String,
    filename: String,
    userAgent: String,
    retryEnabled: Boolean,
    unmeteredOnly: Boolean,
    splitParts: Int,
    multithreadingParts: Int,
    speedLimitBytesPerSec: Long,
    locationMode: String,
    customLocationUri: String?,
    scheduledStartAtMillis: Long,
    onDismiss: () -> Unit,
    onRename: () -> Unit
) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    val pm = getSystemService(android.os.PowerManager::class.java)
    if (!prefs.getBoolean(PREF_BATTERY_OPT_ASKED, false) && !pm.isIgnoringBatteryOptimizations(packageName)) {
        prefs.edit().putBoolean(PREF_BATTERY_OPT_ASKED, true).apply()
        uiState.confirmDialogConfig = com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig(
            title = getString(R.string.battery_opt_rationale_title),
            message = getString(R.string.battery_opt_rationale_message),
            cancelable = false,
            positiveLabel = getString(R.string.action_allow),
            onPositive = {
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
                continueManualDownload(url, filename, userAgent, retryEnabled, unmeteredOnly, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri, scheduledStartAtMillis, onDismiss, onRename)
            },
            negativeLabel = getString(R.string.action_not_now),
            onNegative = {
                continueManualDownload(url, filename, userAgent, retryEnabled, unmeteredOnly, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri, scheduledStartAtMillis, onDismiss, onRename)
            }
        )
        return
    }
    continueManualDownload(url, filename, userAgent, retryEnabled, unmeteredOnly, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri, scheduledStartAtMillis, onDismiss, onRename)
}

private fun DownloadsActivity.continueManualDownload(
    url: String,
    filename: String,
    userAgent: String,
    retryEnabled: Boolean,
    unmeteredOnly: Boolean,
    splitParts: Int,
    multithreadingParts: Int,
    speedLimitBytesPerSec: Long,
    locationMode: String,
    customLocationUri: String?,
    scheduledStartAtMillis: Long,
    onDismiss: () -> Unit,
    onRename: () -> Unit
) {
    val cm = getSystemService(android.net.ConnectivityManager::class.java)
    val isMetered = cm?.isActiveNetworkMetered ?: false
    if (unmeteredOnly && isMetered) {
        uiState.confirmDialogConfig = com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig(
            title = getString(R.string.download_metered_warning_title),
            message = getString(R.string.download_metered_warning_message),
            positiveLabel = getString(R.string.action_yes),
            onPositive = {
                checkConflictAndEnqueueManual(url, filename, userAgent, retryEnabled, false, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri, scheduledStartAtMillis, onDismiss, onRename)
            },
            negativeLabel = getString(R.string.action_no),
            onNegative = {
                checkConflictAndEnqueueManual(url, filename, userAgent, retryEnabled, true, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri, scheduledStartAtMillis, onDismiss, onRename)
            },
            neutralLabel = getString(R.string.action_cancel)
        )
        return
    }
    checkConflictAndEnqueueManual(url, filename, userAgent, retryEnabled, unmeteredOnly, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri, scheduledStartAtMillis, onDismiss, onRename)
}

private fun DownloadsActivity.checkConflictAndEnqueueManual(
    url: String,
    filename: String,
    userAgent: String,
    retryEnabled: Boolean,
    unmeteredOnly: Boolean,
    splitParts: Int,
    multithreadingParts: Int,
    speedLimitBytesPerSec: Long,
    locationMode: String,
    customLocationUri: String?,
    scheduledStartAtMillis: Long,
    onDismiss: () -> Unit,
    onRename: () -> Unit
) {
    val existing = ClintDownloadManager.findActiveDownloadForUrl(url)
    if (existing != null) {
        uiState.confirmDialogConfig = com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig(
            title = getString(R.string.download_already_active_title),
            message = getString(R.string.download_already_active_message, existing.filename),
            positiveLabel = getString(R.string.action_download_anyway),
            onPositive = {
                checkFilenameConflictAndEnqueueManual(url, filename, userAgent, retryEnabled, unmeteredOnly, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri, scheduledStartAtMillis, onDismiss, onRename)
            },
            negativeLabel = getString(R.string.action_cancel)
        )
        return
    }
    checkFilenameConflictAndEnqueueManual(url, filename, userAgent, retryEnabled, unmeteredOnly, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri, scheduledStartAtMillis, onDismiss, onRename)
}

private fun DownloadsActivity.enqueueManualDownload(
    url: String,
    filename: String,
    userAgent: String,
    retryEnabled: Boolean,
    unmeteredOnly: Boolean,
    splitParts: Int,
    multithreadingParts: Int,
    speedLimitBytesPerSec: Long,
    locationMode: String,
    customLocationUri: String?,
    scheduledStartAtMillis: Long,
    onDismiss: () -> Unit
) {
    if (DownloadFileHelper.isCustomLocationAccessible(this, locationMode, customLocationUri)) onDismiss()
    ClintDownloadManager.enqueue(this, url, filename, userAgent, "", "", retryEnabled, unmeteredOnly, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri, scheduledStartAtMillis)
}

private fun DownloadsActivity.checkFilenameConflictAndEnqueueManual(
    url: String,
    filename: String,
    userAgent: String,
    retryEnabled: Boolean,
    unmeteredOnly: Boolean,
    splitParts: Int,
    multithreadingParts: Int,
    speedLimitBytesPerSec: Long,
    locationMode: String,
    customLocationUri: String?,
    scheduledStartAtMillis: Long,
    onDismiss: () -> Unit,
    onRename: () -> Unit
) {
    val isSaf = locationMode == DownloadSettingsKeys.MODE_CUSTOM
    val fileExists = if (isSaf) {
        val treeUri = customLocationUri?.let { Uri.parse(it) } ?: DownloadFileHelper.getSafTreeUri(this)
        treeUri?.let { DocumentFile.fromTreeUri(this, it)?.findFile(filename) } != null
    } else {
        File(DownloadFileHelper.resolveDownloadDir(), filename).exists()
    }
    if (!fileExists) {
        enqueueManualDownload(url, filename, userAgent, retryEnabled, unmeteredOnly, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri, scheduledStartAtMillis, onDismiss)
        return
    }
    uiState.conflictDialogRequest = DownloadConflictDialogRequest(
        onAddDuplicate = {
            enqueueManualDownload(url, filename, userAgent, retryEnabled, unmeteredOnly, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri, scheduledStartAtMillis, onDismiss)
        },
        onOverride = {
            deleteExistingManual(filename, locationMode, customLocationUri)
            enqueueManualDownload(url, filename, userAgent, retryEnabled, unmeteredOnly, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri, scheduledStartAtMillis, onDismiss)
        },
        onRename = onRename
    )
}

private fun DownloadsActivity.deleteExistingManual(
    filename: String,
    locationMode: String,
    customLocationUri: String?
) {
    val matchingIds = ClintDownloadManager.downloadsFlow.value.filter { it.filename == filename }.map { it.id }
    matchingIds.forEach { ClintDownloadManager.remove(this, it, deleteFile = true) }
    val isSaf = locationMode == DownloadSettingsKeys.MODE_CUSTOM
    if (isSaf) {
        val treeUri = customLocationUri?.let { Uri.parse(it) } ?: DownloadFileHelper.getSafTreeUri(this)
        treeUri?.let { DocumentFile.fromTreeUri(this, it)?.findFile(filename)?.delete() }
    } else {
        File(DownloadFileHelper.resolveDownloadDir(), filename).delete()
    }
}
