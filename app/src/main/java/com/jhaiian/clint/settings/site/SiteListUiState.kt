package com.jhaiian.clint.settings.site

import com.jhaiian.clint.ui.listscreen.ListSortKey
import com.jhaiian.clint.ui.listscreen.ListSortOrder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SiteListUiState {
    var allItems by mutableStateOf<List<SiteEntry>>(emptyList())
    var searchQuery by mutableStateOf("")
    var isSearchMode by mutableStateOf(false)
    var sortKey by mutableStateOf(ListSortKey.DATE_ADDED)
    var sortOrder by mutableStateOf(ListSortOrder.DESCENDING)
    var selectedOrigins by mutableStateOf<Set<String>>(emptySet())
    var isInSelectionMode by mutableStateOf(false)
    var sortMenuOpen by mutableStateOf(false)
    var moreOptionsMenuOpen by mutableStateOf(false)
    var addDialogOpen by mutableStateOf(false)
    var deleteConfirmOpen by mutableStateOf(false)

    /** Tap in selection mode: toggles one row without touching selection-mode itself,
     *  which persists even once the count reaches zero (matches the original adapter). */
    fun toggleSelection(origin: String) {
        selectedOrigins = if (origin in selectedOrigins) selectedOrigins - origin else selectedOrigins + origin
    }

    /** Long-press on a row when not yet selecting: enters selection mode with that row selected. */
    fun enterSelectionWith(origin: String) {
        isInSelectionMode = true
        selectedOrigins = selectedOrigins + origin
    }

    fun selectAll(displayed: List<SiteEntry>) {
        selectedOrigins = selectedOrigins + displayed.map { it.origin }
    }

    /** Inverts selection only within the currently filtered/displayed rows, preserving
     *  the selection state of any row a search filter is currently hiding. */
    fun invertSelection(displayed: List<SiteEntry>) {
        val displayedOrigins = displayed.map { it.origin }.toSet()
        val keptOutsideView = selectedOrigins - displayedOrigins
        val invertedWithinView = displayedOrigins - selectedOrigins
        selectedOrigins = keptOutsideView + invertedWithinView
    }

    fun deselectAll() {
        selectedOrigins = emptySet()
    }

    fun exitSelectionMode() {
        isInSelectionMode = false
        selectedOrigins = emptySet()
    }

    fun removeSelectedItems() {
        allItems = allItems.filterNot { it.origin in selectedOrigins }
        isInSelectionMode = false
        selectedOrigins = emptySet()
    }
}
