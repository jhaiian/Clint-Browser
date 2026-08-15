package com.jhaiian.clint.settings.downloads

/**
 * SharedPreferences keys, mode values, and defaults for download settings. Referenced from many
 * download-related files (DownloadSizeUtils, DownloadWorker, ClintDownloadManager, the manual/
 * redownload dialogs, DownloadNotificationHelper, MainDownloadDialogDelegate, etc.) in addition
 * to [DownloadSettingsScreen] itself, so it lives here as a plain object rather than inside any
 * single screen or Activity. Kept exactly as they were on the old DownloadSettingsFragment.
 */
object DownloadSettingsKeys {
    const val PREF_DOWNLOAD_LOCATION_MODE  = "download_location_mode"
    const val PREF_DOWNLOAD_CUSTOM_URI     = "download_custom_uri"
    const val MODE_DEFAULT                 = "default"
    const val MODE_CUSTOM                  = "custom"
    const val PREF_UNMETERED_ONLY          = "download_unmetered_only"
    const val DEFAULT_UNMETERED_ONLY       = false
    const val PREF_SCHEDULE_ENABLED        = "download_schedule_enabled"
    const val DEFAULT_SCHEDULE_ENABLED     = false
    const val PREF_SCHEDULE_START_MINUTES  = "download_schedule_start_minutes"
    const val DEFAULT_SCHEDULE_START_MINUTES = 23 * 60
    const val PREF_SCHEDULE_END_MINUTES    = "download_schedule_end_minutes"
    const val DEFAULT_SCHEDULE_END_MINUTES = 7 * 60
    const val PREF_RETRY_ENABLED           = "download_retry_enabled"
    const val PREF_RETRY_UNRECOVERABLE     = "download_retry_unrecoverable"
    const val PREF_RETRY_COUNT             = "download_retry_count"
    const val PREF_RETRY_INTERVAL          = "download_retry_interval"
    const val DEFAULT_RETRY_ENABLED        = true
    const val DEFAULT_RETRY_UNRECOVERABLE  = false
    const val DEFAULT_RETRY_COUNT          = 0
    const val DEFAULT_RETRY_INTERVAL       = 5
    const val PREF_CONCURRENT_DOWNLOADS    = "download_concurrent_limit"
    const val DEFAULT_CONCURRENT_DOWNLOADS = 1
    const val PREF_SPLIT_PARTS             = "download_split_parts"
    const val DEFAULT_SPLIT_PARTS          = 32
    const val PREF_MULTITHREADING_PARTS    = "download_multithreading_parts"
    const val DEFAULT_MULTITHREADING_PARTS = 4
    const val PREF_SPEED_LIMIT_AMOUNT      = "download_speed_limit_amount"
    const val DEFAULT_SPEED_LIMIT_AMOUNT   = 0
    const val PREF_SPEED_LIMIT_UNIT        = "download_speed_limit_unit"
    const val PREF_PUSH_NOTIFICATIONS      = "download_push_notifications"
    const val DEFAULT_PUSH_NOTIFICATIONS   = true
}
