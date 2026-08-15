package com.jhaiian.clint.quiver

import com.jhaiian.clint.quiver.engine.CompiledManifestData

// Tracks in-memory edits (enabled toggles, staged removals) that haven't been compiled
// yet, and recomputes the displayed list from the database plus those overrides. Unlike
// the View-based original, there is no imperative "refresh the FAB drawable" step here:
// QuiverGuardScreen reads isConfigurationDirty()/isCompileRunning reactively on every
// recomposition, so the FAB icon and banner simply follow the state automatically.

internal fun QuiverGuardActivity.isConfigurationDirty(): Boolean = uiState.isConfigurationDirty()

internal fun QuiverGuardActivity.effectiveFilterLists(): List<FilterList> {
    val all = database().getAllFilterLists()
    return all.filterNot { it.id in uiState.pendingRemovedIds }
        .map { row -> uiState.pendingEnabledOverrides[row.id]?.let { row.copy(isEnabled = it) } ?: row }
}

internal fun QuiverGuardActivity.refreshFilterListDisplay() {
    uiState.filterLists = effectiveFilterLists()
}

internal fun QuiverGuardActivity.setPendingEnabled(id: Long, enabled: Boolean) {
    uiState.pendingEnabledOverrides = uiState.pendingEnabledOverrides + (id to enabled)
    refreshFilterListDisplay()
}

internal fun QuiverGuardActivity.stagePendingRemoval(id: Long) {
    uiState.pendingRemovedIds = uiState.pendingRemovedIds + id
    uiState.pendingEnabledOverrides = uiState.pendingEnabledOverrides - id
    refreshFilterListDisplay()
}

internal fun QuiverGuardActivity.stagePendingRemovals(ids: Set<Long>) {
    uiState.pendingRemovedIds = uiState.pendingRemovedIds + ids
    uiState.pendingEnabledOverrides = uiState.pendingEnabledOverrides - ids
    refreshFilterListDisplay()
}

internal fun QuiverGuardActivity.discardPendingChanges() {
    uiState.pendingEnabledOverrides = emptyMap()
    uiState.pendingRemovedIds = emptySet()
    uiState.isStartupDirty = false
    refreshFilterListDisplay()
}

internal fun QuiverGuardActivity.isDownloadInProgress(id: Long): Boolean = id in uiState.downloadingIds

internal fun QuiverGuardActivity.markDownloading(id: Long, active: Boolean) {
    uiState.downloadingIds = if (active) uiState.downloadingIds + id else uiState.downloadingIds - id
}

// Called after a new list finishes downloading or a custom/local list is added: both
// cases mark the list enabled going forward without requiring a separate toggle tap.
internal fun QuiverGuardActivity.onFilterListDownloaded(id: Long) {
    setPendingEnabled(id, true)
}

internal fun QuiverGuardActivity.onFilterListAdded(filterList: FilterList) {
    refreshFilterListDisplay()
    setPendingEnabled(filterList.id, true)
}

// Compares the compiled manifest's manual-filter entry (if any) against the current
// rule set's content fingerprint to decide whether a recompile is needed even though
// FilterListDatabase has no row for the manual filter to diff against directly.
internal fun QuiverGuardActivity.isManualFilterDirty(manifest: CompiledManifestData): Boolean {
    val entry = manifest.entries.firstOrNull { it.id == ManualFilterState.COMPILE_ID }
    val rules = manualFilterDb().getAllRules()
    val contributes = ManualFilterState.isEnabled(this) && rules.isNotEmpty()
    if (entry == null) return contributes
    if (!contributes) return true
    return entry.contentFingerprint != ManualFilterState.contentFingerprint(rules)
}
