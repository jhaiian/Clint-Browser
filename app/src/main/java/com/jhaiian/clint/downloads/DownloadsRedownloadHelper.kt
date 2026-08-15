package com.jhaiian.clint.downloads

import android.net.Uri
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.downloads.DownloadSettingsKeys
import com.jhaiian.clint.ui.theme.ClintComposeTheme

internal fun DownloadsActivity.showRedownloadDialog(item: DownloadItem) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    val (initialSpeedLimitAmount, initialSpeedLimitUnit) = speedLimitBytesToAmountAndUnit(this, item.speedLimitBytesPerSec)

    val theme = prefs.getString("app_theme", "dark") ?: "dark"
    val hideStatusBar = prefs.getBoolean("hide_status_bar", false)

    val dismiss: () -> Unit = { overlayContent = null }

    overlayContent = {
        ClintComposeTheme(theme = theme) {
            DownloadRequestDialog(
                hideStatusBar = hideStatusBar,
                url = item.url,
                onCopyLink = { copyRedownloadLink(item.url) },
                initialFilename = item.filename,
                contentLengthBytes = item.totalBytes,
                checkStorage = false,
                showOptions = true,
                showSchedule = false,
                showStorageInfo = false,
                initialLocationMode = item.locationMode.ifBlank {
                    prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_LOCATION_MODE, DownloadSettingsKeys.MODE_DEFAULT) ?: DownloadSettingsKeys.MODE_DEFAULT
                },
                initialCustomUri = (item.customLocationUri ?: prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_CUSTOM_URI, null))?.let { Uri.parse(it) },
                initialRetryEnabled = item.retryEnabled,
                initialUnmeteredOnly = item.unmeteredOnly,
                initialSplitParts = item.splitParts,
                initialMultithreadingParts = item.multithreadingParts,
                initialSpeedLimitAmount = initialSpeedLimitAmount,
                initialSpeedLimitUnit = initialSpeedLimitUnit,
                fragmentManager = supportFragmentManager,
                onLaunchFolderPicker = { onPicked -> launchManualFolderPicker(onPicked) },
                onDismiss = dismiss,
                onSubmit = { submission, _, _ ->
                    performRedownload(
                        item = item,
                        filename = submission.filename,
                        retryEnabled = submission.retryEnabled,
                        unmeteredOnly = submission.unmeteredOnly,
                        splitParts = submission.splitParts,
                        multithreadingParts = submission.multithreadingParts,
                        speedLimitBytesPerSec = submission.speedLimitBytesPerSec,
                        locationMode = submission.locationMode,
                        customLocationUri = submission.customLocationUri,
                        onDismiss = {
                            Toast.makeText(this@showRedownloadDialog, getString(R.string.toast_downloading, submission.filename), Toast.LENGTH_SHORT).show()
                            dismiss()
                            ClintDownloadManager.remove(this@showRedownloadDialog, item.id, true)
                            lastRefreshMs = 0L
                        }
                    )
                }
            )
        }
    }
}

private fun DownloadsActivity.copyRedownloadLink(url: String) {
    val clipboard = getSystemService(android.content.ClipboardManager::class.java)
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText(getString(R.string.download_dialog_link_clip_label), url))
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(this, getString(R.string.download_dialog_link_copied), Toast.LENGTH_SHORT).show()
    }
}

private fun DownloadsActivity.performRedownload(
    item: DownloadItem,
    filename: String,
    retryEnabled: Boolean,
    unmeteredOnly: Boolean,
    splitParts: Int,
    multithreadingParts: Int,
    speedLimitBytesPerSec: Long,
    locationMode: String,
    customLocationUri: String?,
    onDismiss: () -> Unit
) {
    val cm = getSystemService(android.net.ConnectivityManager::class.java)
    val isMetered = cm?.isActiveNetworkMetered ?: false
    if (unmeteredOnly && isMetered) {
        uiState.confirmDialogConfig = com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig(
            title = getString(R.string.download_metered_warning_title),
            message = getString(R.string.download_metered_warning_message),
            positiveLabel = getString(R.string.action_yes),
            onPositive = {
                onDismiss()
                ClintDownloadManager.enqueue(this, item.url, filename, item.userAgent, item.referer, item.cookies, retryEnabled, false, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri)
            },
            negativeLabel = getString(R.string.action_no),
            onNegative = {
                onDismiss()
                ClintDownloadManager.enqueue(this, item.url, filename, item.userAgent, item.referer, item.cookies, retryEnabled, true, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri)
            },
            neutralLabel = getString(R.string.action_cancel)
        )
        return
    }
    onDismiss()
    ClintDownloadManager.enqueue(this, item.url, filename, item.userAgent, item.referer, item.cookies, retryEnabled, unmeteredOnly, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri)
}
