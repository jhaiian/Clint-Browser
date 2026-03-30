package com.jhaiian.clint.bookmarks

data class Bookmark(
    val url: String,
    val title: String,
    val faviconUrl: String = ""
)
