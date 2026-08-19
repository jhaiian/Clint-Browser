package com.jhaiian.clint.tabs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Hoisted state for the Tab Grid menu. Lives for as long as the menu is open; selection and
 * drag state are intentionally not persisted beyond that, matching the existing Tab Sheet's
 * "local mirror, synced back to TabManager on each action" approach.
 */
class TabMenuUiState {
    var selectionMode by mutableStateOf(false)
    val selectedIds = mutableStateListOf<String>()

    fun toggleSelected(tabId: String) {
        if (selectedIds.contains(tabId)) selectedIds.remove(tabId) else selectedIds.add(tabId)
        if (selectedIds.isEmpty()) selectionMode = false
    }

    fun enterSelectionMode(tabId: String) {
        selectionMode = true
        if (!selectedIds.contains(tabId)) selectedIds.add(tabId)
    }

    fun selectAll(tabIds: List<String>) {
        selectedIds.clear()
        selectedIds.addAll(tabIds)
    }

    fun invertSelection(tabIds: List<String>) {
        val inverted = tabIds.filterNot { selectedIds.contains(it) }
        selectedIds.clear()
        selectedIds.addAll(inverted)
        if (selectedIds.isEmpty()) selectionMode = false
    }

    fun exitSelectionMode() {
        selectionMode = false
        selectedIds.clear()
    }
}
