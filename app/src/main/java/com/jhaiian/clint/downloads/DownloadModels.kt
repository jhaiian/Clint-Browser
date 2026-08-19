package com.jhaiian.clint.downloads

import java.io.File

enum class DownloadStatus {
    QUEUED, CONNECTING, ALLOCATING, DOWNLOADING, RETRYING, COPYING_TEMP, DELETING_TEMP, PAUSED, FAILED, COMPLETE;

    companion object {

        val ACTIVELY_WORKING: Set<DownloadStatus> =
            setOf(CONNECTING, DOWNLOADING, ALLOCATING, COPYING_TEMP, DELETING_TEMP, RETRYING)

        val NOT_FINISHED: Set<DownloadStatus> =
            setOf(QUEUED, CONNECTING, ALLOCATING, DOWNLOADING, RETRYING, COPYING_TEMP, DELETING_TEMP, PAUSED)
    }
}

data class DownloadItem(
    val id: Int,
    var url: String,
    var filename: String,
    val userAgent: String,
    val referer: String = "",
    val cookies: String = "",
    var bytesDownloaded: Long = 0L,
    var totalBytes: Long = -1L,
    var status: DownloadStatus = DownloadStatus.DOWNLOADING,
    var file: File? = null,
    var errorMessage: String? = null,
    var startedAt: Long = 0L,
    var speedBytesPerSec: Long = 0L,
    var resumable: Boolean = false,
    var copyProgress: Int = 0,
    var contentUri: String? = null,
    var retryAttempt: Int = 0,
    var retryDelaySec: Int = 0,
    var allocationProgress: Int = 0,
    var waitingForUnmetered: Boolean = false,
    var waitingForNetwork: Boolean = false,
    var waitingForSchedule: Boolean = false,

    var scheduledStartAtMillis: Long = 0L,

    var waitingForCustomSchedule: Boolean = false,
    var activeElapsedMs: Long = 0L,
    @Transient var activeStartedAt: Long = 0L,
    @Transient var parallelRateLimited: Boolean = false,
    var completedAt: Long = 0L,
    var retryEnabled: Boolean = true,
    var lastErrorWasServerError: Boolean = false,
    var unmeteredOnly: Boolean = false,
    var splitParts: Int = 32,
    var multithreadingParts: Int = 4,

    var speedLimitBytesPerSec: Long = 0L,
    var locationMode: String = "default",
    var customLocationUri: String? = null,

    var completedPartsMask: Long = 0L,

    var partOffsets: String = ""
) {
    val progressPercent: Int
        get() = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else -1

    fun averageSpeedBytesPerSec(): Long {
        val activeMs = activeElapsedMs + (if (activeStartedAt > 0L) System.currentTimeMillis() - activeStartedAt else 0L)
        return if (activeMs > 0L && bytesDownloaded > 0L) bytesDownloaded * 1000L / activeMs else 0L
    }
}
