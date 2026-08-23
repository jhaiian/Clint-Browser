package com.jhaiian.clint.quiver

import com.jhaiian.clint.quiver.engine.CompiledManifestData

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
    val all = database().getAllFilterLists()
    val baseline = all.firstOrNull { it.id == id }?.isEnabled
    uiState.pendingEnabledOverrides = if (enabled == baseline) {
        uiState.pendingEnabledOverrides - id
    } else {
        uiState.pendingEnabledOverrides + (id to enabled)
    }
    uiState.filterLists = all.filterNot { it.id in uiState.pendingRemovedIds }
        .map { row -> uiState.pendingEnabledOverrides[row.id]?.let { row.copy(isEnabled = it) } ?: row }
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

internal fun QuiverGuardActivity.onFilterListDownloaded(id: Long) {
    setPendingEnabled(id, true)
}

internal fun QuiverGuardActivity.onFilterListAdded(filterList: FilterList) {
    refreshFilterListDisplay()
    setPendingEnabled(filterList.id, true)
}

internal fun QuiverGuardActivity.isManualFilterDirty(manifest: CompiledManifestData): Boolean {
    val entry = manifest.entries.firstOrNull { it.id == ManualFilterState.COMPILE_ID }
    val rules = manualFilterDb().getAllRules()
    val contributes = ManualFilterState.isEnabled(this) && rules.isNotEmpty()
    if (entry == null) return contributes
    if (!contributes) return true
    return entry.contentFingerprint != ManualFilterState.contentFingerprint(rules)
}
