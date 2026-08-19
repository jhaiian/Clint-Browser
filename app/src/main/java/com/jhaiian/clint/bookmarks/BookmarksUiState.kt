package com.jhaiian.clint.bookmarks

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import com.jhaiian.clint.ui.listscreen.ListSortOrder

enum class BookmarksSortKey { TITLE, LAST_VISIT, DATE_ADDED }

class BookmarksUiState {
    var items by mutableStateOf<List<Bookmark>>(emptyList())
    var isLoading by mutableStateOf(true)

    var searchQuery by mutableStateOf("")
    var isSearchMode by mutableStateOf(false)
    var sortKey by mutableStateOf(BookmarksSortKey.LAST_VISIT)
    var sortOrder by mutableStateOf(ListSortOrder.DESCENDING)

    var selectedKeys by mutableStateOf<Set<String>>(emptySet())
    var isInSelectionMode by mutableStateOf(false)

    var sortMenuOpen by mutableStateOf(false)
    var selectionOptionsMenuOpen by mutableStateOf(false)

    var deleteConfirm by mutableStateOf<ConfirmDialogConfig?>(null)

    val selectedCount get() = selectedKeys.size

    fun toggleSelection(url: String) {
        selectedKeys = if (url in selectedKeys) selectedKeys - url else selectedKeys + url
    }

    fun enterSelectionWith(url: String) {
        isInSelectionMode = true
        selectedKeys = selectedKeys + url
    }

    fun selectAll(displayed: List<Bookmark>) {
        selectedKeys = selectedKeys + displayed.map { it.url }
    }

    fun invertSelection(displayed: List<Bookmark>) {
        val displayedKeys = displayed.map { it.url }.toSet()
        val keptOutsideView = selectedKeys - displayedKeys
        val invertedWithinView = displayedKeys - selectedKeys
        selectedKeys = keptOutsideView + invertedWithinView
    }

    fun deselectAll() {
        selectedKeys = emptySet()
    }

    fun exitSelectionMode() {
        isInSelectionMode = false
        selectedKeys = emptySet()
    }
}
