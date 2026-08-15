package com.jhaiian.clint.settings.downloads

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Identifies which picker dialog, if any, is currently shown over the screen. */
enum class DownloadSettingsDialog {
    MEASUREMENT_SYSTEM, RETRY_COUNT, RETRY_INTERVAL, SPEED_LIMIT
}

class DownloadSettingsUiState(
    initialLocationMode: String,
    initialCustomUri: Uri?,
    initialMeasurementSystemDecimal: Boolean,
    initialUnmeteredOnly: Boolean,
    initialScheduleEnabled: Boolean,
    initialScheduleStartMinutes: Int,
    initialScheduleEndMinutes: Int,
    initialConcurrentDownloads: Int,
    initialSplitParts: Int,
    initialMultithreadingParts: Int,
    initialSpeedLimitAmount: Int,
    initialSpeedLimitUnit: String,
    initialRetryEnabled: Boolean,
    initialRetryUnrecoverable: Boolean,
    initialRetryCount: Int,
    initialRetryInterval: Int,
    initialIgnoringBatteryOptimizations: Boolean,
    initialShowGrantAllFilesAccessRow: Boolean,
    initialAllFilesAccessGranted: Boolean,
    initialPushNotifications: Boolean,
    initialHideStatusBar: Boolean
) {
    var locationMode by mutableStateOf(initialLocationMode)
    var customUri by mutableStateOf(initialCustomUri)
    var measurementSystemDecimal by mutableStateOf(initialMeasurementSystemDecimal)
    var unmeteredOnly by mutableStateOf(initialUnmeteredOnly)

    var scheduleEnabled by mutableStateOf(initialScheduleEnabled)
    var scheduleStartMinutes by mutableStateOf(initialScheduleStartMinutes)
    var scheduleEndMinutes by mutableStateOf(initialScheduleEndMinutes)

    var concurrentDownloads by mutableStateOf(initialConcurrentDownloads)
    var splitParts by mutableStateOf(initialSplitParts)
    var multithreadingParts by mutableStateOf(initialMultithreadingParts)

    var speedLimitAmount by mutableStateOf(initialSpeedLimitAmount)
    var speedLimitUnit by mutableStateOf(initialSpeedLimitUnit)

    var retryEnabled by mutableStateOf(initialRetryEnabled)
    var retryUnrecoverable by mutableStateOf(initialRetryUnrecoverable)
    var retryCount by mutableStateOf(initialRetryCount)
    var retryInterval by mutableStateOf(initialRetryInterval)

    /** Kept separate from [initialIgnoringBatteryOptimizations] so onResume() can refresh it after
     *  the user comes back from the system settings screen without recreating the whole UiState. */
    var ignoringBatteryOptimizations by mutableStateOf(initialIgnoringBatteryOptimizations)

    /** GitHub flavor only, Android 11+ (see [com.jhaiian.clint.settings.downloads.DownloadSettingsKeys]). */
    var showGrantAllFilesAccessRow by mutableStateOf(initialShowGrantAllFilesAccessRow)
    var allFilesAccessGranted by mutableStateOf(initialAllFilesAccessGranted)

    var pushNotifications by mutableStateOf(initialPushNotifications)
    var hideStatusBar by mutableStateOf(initialHideStatusBar)

    var openDialog by mutableStateOf<DownloadSettingsDialog?>(null)
}
