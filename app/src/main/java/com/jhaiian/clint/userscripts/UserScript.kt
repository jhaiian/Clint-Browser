package com.jhaiian.clint.userscripts

data class UserScript(
    val id: Long = 0,
    val code: String,
    val requiresCache: String = "",
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val sourceUrl: String? = null,
    val etag: String? = null,
    val lastModified: String? = null,
    val updatedAt: Long = 0L
) {
    val isLocal: Boolean
        get() = sourceUrl.isNullOrBlank()
}
