package com.jhaiian.clint.quiver

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import com.jhaiian.clint.ui.listscreen.ListSortKey
import com.jhaiian.clint.ui.listscreen.ListSortOrder

/** Rule count and enabled state pushed into the pinned Manual Filter row. */
data class ManualFilterSummary(val ruleCount: Int, val isEnabled: Boolean)

data class DownloadProgressUi(
    val filterListName: String,
    val bytesRead: Long = 0L,
    val totalBytes: Long = 0L,
    val indeterminate: Boolean = true
)

data class UpdateProgressUi(
    val title: String,
    val totalCount: Int,
    val processedCount: Int = 0,
    val statusText: String = "",
    val currentListName: String = ""
)

/** Shown after an update check completes with at least one failure, or with none
 *  updated and none failed except via a toast (see QuiverGuardUpdateHelper). When
 *  onCompile is null this is an info-only OK dialog; otherwise Cancel/Compile. */
data class UpdateResultUi(
    val title: String,
    val message: String,
    val onCompile: (() -> Unit)? = null
)

data class CompileProgressUi(
    val stageText: String,
    val listCounterText: String,
    val rulesText: String,
    val elapsedText: String
)

data class CompileResultRow(val label: String, val value: String)

data class CompileResultUi(
    val isSuccess: Boolean,
    val title: String,
    val rows: List<CompileResultRow>,
    val failureDetail: String? = null,
    val onRetry: (() -> Unit)? = null
)

/** Tracks the two-stage add-from-link flow: idle -> fetching -> fetched (ready to
 *  confirm the title) or error (shown under the URL field, resets to idle on edit). */
sealed class AddLinkFetchStatus {
    object Idle : AddLinkFetchStatus()
    data class Fetching(val bytesRead: Long, val totalBytes: Long) : AddLinkFetchStatus()
    data class Fetched(val file: java.io.File, val sizeBytes: Long, val ruleCount: Long, val metadataTitle: String?) : AddLinkFetchStatus()
    data class Error(val message: String) : AddLinkFetchStatus()
}

class QuiverGuardUiState {
    var filterLists by mutableStateOf<List<FilterList>>(emptyList())
    var manualFilterSummary by mutableStateOf(ManualFilterSummary(ruleCount = 0, isEnabled = false))
    var masterEnabled by mutableStateOf(false)
    var bannerText by mutableStateOf<String?>(null)

    var searchQuery by mutableStateOf("")
    var isSearchMode by mutableStateOf(false)
    var sortKey by mutableStateOf(ListSortKey.DATE_ADDED)
    var sortOrder by mutableStateOf(ListSortOrder.DESCENDING)
    var selectedIds by mutableStateOf<Set<Long>>(emptySet())
    var isInSelectionMode by mutableStateOf(false)

    var isFabMenuOpen by mutableStateOf(false)
    var sortMenuOpen by mutableStateOf(false)
    var filterListActionsMenuOpen by mutableStateOf(false)
    var selectionOptionsMenuOpen by mutableStateOf(false)

    // In-memory overrides for enabled states / staged removals not yet compiled.
    var pendingEnabledOverrides by mutableStateOf<Map<Long, Boolean>>(emptyMap())
    var pendingRemovedIds by mutableStateOf<Set<Long>>(emptySet())
    var isStartupDirty by mutableStateOf(false)
    var isCompileRunning by mutableStateOf(false)
    var isUpdateRunning by mutableStateOf(false)
    var downloadingIds by mutableStateOf<Set<Long>>(emptySet())

    var confirmDialog by mutableStateOf<ConfirmDialogConfig?>(null)
    var downloadProgress by mutableStateOf<DownloadProgressUi?>(null)
    var updateProgress by mutableStateOf<UpdateProgressUi?>(null)
    var updateResult by mutableStateOf<UpdateResultUi?>(null)
    var compileProgress by mutableStateOf<CompileProgressUi?>(null)
    var compileResult by mutableStateOf<CompileResultUi?>(null)
    var addFromLinkDialogOpen by mutableStateOf(false)
    var addLinkFetchStatus by mutableStateOf<AddLinkFetchStatus>(AddLinkFetchStatus.Idle)
    internal var addFromFileImport by mutableStateOf<LocalFilterListImportResult.Success?>(null)
    var experimentalDialogOpen by mutableStateOf(false)
    var setupGuideDialogOpen by mutableStateOf(false)

    fun isConfigurationDirty(): Boolean =
        pendingEnabledOverrides.isNotEmpty() || pendingRemovedIds.isNotEmpty() || isStartupDirty

    fun toggleSelection(id: Long) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    fun enterSelectionWith(id: Long) {
        isInSelectionMode = true
        selectedIds = selectedIds + id
    }

    fun selectAll(displayed: List<FilterList>) {
        selectedIds = selectedIds + displayed.map { it.id }
    }

    /** Inverts selection only within the currently filtered/displayed rows, preserving
     *  the selection state of any row a search filter is currently hiding. */
    fun invertSelection(displayed: List<FilterList>) {
        val displayedIds = displayed.map { it.id }.toSet()
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
