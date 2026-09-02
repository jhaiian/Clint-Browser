package com.jhaiian.clint.userscripts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jhaiian.clint.quiver.UpdateProgressUi
import com.jhaiian.clint.quiver.UpdateResultUi
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import com.jhaiian.clint.ui.listscreen.ListSortKey
import com.jhaiian.clint.ui.listscreen.ListSortOrder

sealed class AddUserScriptLinkFetchStatus {
    object Idle : AddUserScriptLinkFetchStatus()
    data class Fetching(val bytesRead: Long, val totalBytes: Long) : AddUserScriptLinkFetchStatus()
    data class Fetched(val code: String, val metadataName: String) : AddUserScriptLinkFetchStatus()
    data class Error(val message: String) : AddUserScriptLinkFetchStatus()
}

class UserScriptsUiState {
    var scripts by mutableStateOf(emptyList<UserScript>())
    var isEnabled by mutableStateOf(true)
    var bannerText by mutableStateOf<String?>(null)

    var isSearchMode by mutableStateOf(false)
    var searchQuery by mutableStateOf("")
    var sortKey by mutableStateOf(ListSortKey.TITLE)
    var sortOrder by mutableStateOf(ListSortOrder.ASCENDING)
    var isInSelectionMode by mutableStateOf(false)
    var selectedIds by mutableStateOf(emptySet<Long>())

    var isFabMenuOpen by mutableStateOf(false)
    var sortMenuOpen by mutableStateOf(false)
    var scriptActionsMenuOpen by mutableStateOf(false)
    var selectionOptionsMenuOpen by mutableStateOf(false)

    var isUpdateRunning by mutableStateOf(false)
    var updatingIds by mutableStateOf<Set<Long>>(emptySet())

    var confirmDialog by mutableStateOf<ConfirmDialogConfig?>(null)
    var updateProgress by mutableStateOf<UpdateProgressUi?>(null)
    var updateResult by mutableStateOf<UpdateResultUi?>(null)

    var addFromLinkDialogOpen by mutableStateOf(false)
    var addLinkFetchStatus by mutableStateOf<AddUserScriptLinkFetchStatus>(AddUserScriptLinkFetchStatus.Idle)

    fun toggleSelection(id: Long) {
        selectedIds = if (selectedIds.contains(id)) selectedIds - id else selectedIds + id
    }

    fun enterSelectionMode(id: Long) {
        isInSelectionMode = true
        selectedIds = setOf(id)
    }

    fun selectAll(displayed: List<UserScriptListItem>) {
        selectedIds = selectedIds + displayed.map { it.script.id }
    }

    fun invertSelection(displayed: List<UserScriptListItem>) {
        val displayedIds = displayed.map { it.script.id }.toSet()
        val keptOutsideView = selectedIds - displayedIds
        val invertedWithinView = displayedIds - selectedIds
        selectedIds = keptOutsideView + invertedWithinView
    }

    fun deselectAll() {
        selectedIds = emptySet()
    }

    fun exitSelectionMode() {
        isInSelectionMode = false
        selectedIds = emptySet()
    }
}

data class UserScriptListItem(val script: UserScript, val metadata: UserScriptMetadata)

fun buildListItems(scripts: List<UserScript>): List<UserScriptListItem> =
    scripts.map { UserScriptListItem(it, UserScriptMetadataParser.parse(it.code, "Untitled Script")) }

fun matchPatternCount(metadata: UserScriptMetadata): Int = metadata.matches.size + metadata.includes.size
