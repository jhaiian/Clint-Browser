package com.jhaiian.clint.quiver

data class FilterList(
    val id: Long,
    val name: String,
    val downloadUrl: String,
    val isEnabled: Boolean,

    val localPath: String?,
    val fileSizeBytes: Long,

    val downloadedAt: Long,
    val ruleCount: Long,
    val isCustom: Boolean,

    val compiledAt: Long = 0L,

    val etag: String? = null,
    val lastModified: String? = null
) {

    val isDownloaded: Boolean
        get() = downloadedAt > 0L && !localPath.isNullOrBlank()

    val isNeverCompiled: Boolean
        get() = compiledAt <= 0L

    val isLocal: Boolean
        get() = downloadUrl.isBlank()
}
