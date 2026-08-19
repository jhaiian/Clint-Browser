package com.jhaiian.clint.quiver
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ArrowBack

import com.jhaiian.clint.R

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.jhaiian.clint.ui.ClintOutlinedTextField
import androidx.compose.material3.Surface
import com.jhaiian.clint.ui.ClintSwitch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.ui.AdaptiveWidthContainer
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.listscreen.ClintSearchField
import com.jhaiian.clint.ui.listscreen.ListFastScroller
import com.jhaiian.clint.ui.listscreen.ListSortKey
import com.jhaiian.clint.ui.listscreen.ListSortOrder
import com.jhaiian.clint.ui.listscreen.SelectionOptionsMenu
import com.jhaiian.clint.ui.listscreen.SortMenu
import com.jhaiian.clint.ui.theme.LocalClintColors

sealed class ManualFilterRuleDialogMode {
    object Add : ManualFilterRuleDialogMode()
    data class Edit(val rule: ManualFilterRule) : ManualFilterRuleDialogMode()
}

class ManualFilterUiState {
    var rules by mutableStateOf<List<ManualFilterRule>>(emptyList())
    var isEnabled by mutableStateOf(false)
    var ruleDialogMode by mutableStateOf<ManualFilterRuleDialogMode?>(null)

    var searchQuery by mutableStateOf("")
    var isSearchMode by mutableStateOf(false)
    var sortKey by mutableStateOf(ListSortKey.DATE_ADDED)
    var sortOrder by mutableStateOf(ListSortOrder.ASCENDING)

    var selectedIds by mutableStateOf<Set<Long>>(emptySet())
    var isInSelectionMode by mutableStateOf(false)

    var sortMenuOpen by mutableStateOf(false)
    var selectionOptionsMenuOpen by mutableStateOf(false)

    val selectedCount get() = selectedIds.size

    fun toggleSelection(id: Long) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    fun enterSelectionWith(id: Long) {
        isInSelectionMode = true
        selectedIds = selectedIds + id
    }

    fun selectAll(displayed: List<ManualFilterRule>) {
        selectedIds = selectedIds + displayed.map { it.id }
    }

    fun invertSelection(displayed: List<ManualFilterRule>) {
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

@Composable
fun ManualFilterScreen(
    state: ManualFilterUiState,
    maxContentWidth: Dp?,
    onExit: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (ManualFilterRule) -> Unit,
    onDeleteClick: (ManualFilterRule) -> Unit,
    onDeleteSelectedClick: () -> Unit
) {
    val colors = LocalClintColors.current
    val displayed = remember(state.rules, state.searchQuery, state.sortKey, state.sortOrder) {
        filterAndSortManualFilterRules(state.rules, state.searchQuery, state.sortKey, state.sortOrder)
    }
    val listState = rememberLazyListState()
    val fastScrollerInteractive = !state.isSearchMode && state.sortKey == ListSortKey.TITLE
    val showDeleteFab = state.isInSelectionMode && state.selectedIds.isNotEmpty()
    val showAddFab = !state.isInSelectionMode

    fun handleBack() {
        when {
            state.isSearchMode -> { state.isSearchMode = false; state.searchQuery = "" }
            state.isInSelectionMode -> state.exitSelectionMode()
            else -> onExit()
        }
    }

    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ManualFilterToolbar(
                state = state,
                onBack = ::handleBack,
                onSelectAll = { state.selectAll(displayed) },
                onInvertSelection = { state.invertSelection(displayed) }
            )
            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            Row(
                Modifier.fillMaxWidth().clickable { onToggleEnabled(!state.isEnabled) }.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.quiver_guard_manual_filter_master_switch_title), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(R.string.quiver_guard_manual_filter_master_switch_summary), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                ClintSwitch(checked = state.isEnabled)
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (displayed.isEmpty()) {
                    Text(
                        stringResource(
                            if (state.searchQuery.isBlank()) R.string.quiver_guard_manual_filter_empty_state
                            else R.string.quiver_guard_manual_filter_no_results
                        ),
                        color = colors.secondaryText, fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.Center).padding(32.dp)
                    )
                } else {
                    AdaptiveWidthContainer(maxContentWidth) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp)
                        ) {
                            items(displayed, key = { it.id }) { rule ->
                                ManualFilterRuleRow(
                                    rule = rule,
                                    masterEnabled = state.isEnabled,
                                    isSelected = rule.id in state.selectedIds,
                                    isInSelectionMode = state.isInSelectionMode,
                                    onClick = {
                                        if (state.isInSelectionMode) state.toggleSelection(rule.id)
                                    },
                                    onLongClick = {
                                        if (!state.isInSelectionMode) {
                                            state.enterSelectionWith(rule.id)
                                        } else if (rule.id !in state.selectedIds) {
                                            state.selectedIds = state.selectedIds + rule.id
                                        }
                                    },
                                    onEditClick = { onEditClick(rule) },
                                    onDeleteClick = { onDeleteClick(rule) }
                                )
                            }
                        }
                        ListFastScroller(
                            listState = listState,
                            itemCount = displayed.size,
                            isInteractive = fastScrollerInteractive,
                            sectionLetterAt = { index -> sectionLetterForManualFilterRule(displayed[index]) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                if (showDeleteFab) {
                    FloatingActionButton(
                        onClick = onDeleteSelectedClick,
                        containerColor = colors.buttonBackground, contentColor = colors.buttonIconTint,
                        modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 24.dp, end = 20.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Delete, contentDescription = stringResource(R.string.history_delete_selected_desc))
                    }
                }
                if (showAddFab) {
                    FloatingActionButton(
                        onClick = onAddClick,
                        containerColor = colors.buttonBackground, contentColor = colors.buttonIconTint,
                        modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 24.dp, end = 20.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Add, contentDescription = stringResource(R.string.quiver_guard_manual_filter_add_fab_desc))
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualFilterToolbar(
    state: ManualFilterUiState,
    onBack: () -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit
) {
    val colors = LocalClintColors.current
    val showToolbarIcons = !state.isInSelectionMode && !state.isSearchMode

    Surface(color = colors.surface, shadowElevation = 4.dp, modifier = Modifier.statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    if (state.isInSelectionMode) androidx.compose.material.icons.Icons.Filled.Close else androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(if (state.isInSelectionMode) R.string.history_cancel_selection_desc else R.string.back),
                    tint = colors.onSurface
                )
            }

            if (state.isSearchMode) {
                ClintSearchField(
                    query = state.searchQuery,
                    onQueryChange = { state.searchQuery = it },
                    hint = stringResource(R.string.quiver_guard_manual_filter_search_hint),
                    onClose = { state.isSearchMode = false; state.searchQuery = "" }
                )
            } else {
                Text(
                    text = if (state.isInSelectionMode) stringResource(R.string.history_selected_count, state.selectedCount) else stringResource(R.string.quiver_guard_manual_filter_title),
                    color = colors.onSurface, fontSize = 19.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
            }

            if (showToolbarIcons) {
                IconButton(onClick = { state.isSearchMode = true }) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Search, contentDescription = stringResource(R.string.history_search), tint = colors.iconTint)
                }
                Box {
                    IconButton(onClick = { state.sortMenuOpen = true }) {
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.history_sort), tint = colors.primary)
                    }
                    SortMenu(
                        expanded = state.sortMenuOpen,
                        onDismiss = { state.sortMenuOpen = false },
                        sortKey = state.sortKey, sortOrder = state.sortOrder,
                        onSortByTitle = { state.sortKey = ListSortKey.TITLE; state.sortOrder = ListSortOrder.ASCENDING },
                        onSortByDateAdded = { state.sortKey = ListSortKey.DATE_ADDED; state.sortOrder = ListSortOrder.ASCENDING },
                        onSortAscending = { state.sortOrder = ListSortOrder.ASCENDING },
                        onSortDescending = { state.sortOrder = ListSortOrder.DESCENDING }
                    )
                }
            }
            if (state.isInSelectionMode) {
                Box {
                    IconButton(onClick = { state.selectionOptionsMenuOpen = true }) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Checklist, contentDescription = stringResource(R.string.history_more_options), tint = colors.primary)
                    }
                    SelectionOptionsMenu(
                        expanded = state.selectionOptionsMenuOpen,
                        onDismiss = { state.selectionOptionsMenuOpen = false },
                        onSelectAll = onSelectAll, onInvertSelection = onInvertSelection,
                        onDeselectAll = { state.deselectAll() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualFilterRuleRow(
    rule: ManualFilterRule,
    masterEnabled: Boolean,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val colors = LocalClintColors.current
    val cardColor = if (isSelected) lerp(colors.cardBackground, colors.primary, 0.22f) else colors.cardBackground
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cardColor)
            .alpha(if (masterEnabled) 1f else 0.6f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            rule.ruleText, color = colors.onSurface, fontSize = 14.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (isInSelectionMode) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (isSelected) {
                    Icon(
                        androidx.compose.material.icons.Icons.Filled.Check, contentDescription = null,
                        tint = colors.primary, modifier = Modifier.size(24.dp)
                    )
                }
            }
        } else {
            IconButton(onClick = onEditClick) {
                Icon(androidx.compose.material.icons.Icons.Filled.Tune, contentDescription = null, tint = colors.iconTint)
            }
            IconButton(onClick = onDeleteClick) {
                Icon(androidx.compose.material.icons.Icons.Filled.Delete, contentDescription = null, tint = colors.iconTint)
            }
        }
    }
}

private fun filterAndSortManualFilterRules(
    rules: List<ManualFilterRule>,
    query: String,
    sortKey: ListSortKey,
    sortOrder: ListSortOrder
): List<ManualFilterRule> {
    val filtered = if (query.isBlank()) rules else {
        val q = query.trim().lowercase()
        rules.filter { it.ruleText.lowercase().contains(q) }
    }
    val sorted = when (sortKey) {
        ListSortKey.TITLE -> filtered.sortedBy { it.ruleText.lowercase() }
        ListSortKey.DATE_ADDED -> filtered.sortedBy { it.createdAt }
    }
    return if (sortOrder == ListSortOrder.DESCENDING) sorted.reversed() else sorted
}

private fun sectionLetterForManualFilterRule(rule: ManualFilterRule): String {
    val first = rule.ruleText.trimStart().firstOrNull() ?: return "#"
    return if (first.isLetter()) first.uppercaseChar().toString() else if (first.isDigit()) first.toString() else "#"
}

@Composable
fun ManualFilterRuleDialog(
    mode: ManualFilterRuleDialogMode,
    hideStatusBar: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var text by remember(mode) { mutableStateOf((mode as? ManualFilterRuleDialogMode.Edit)?.rule?.ruleText ?: "") }
    val isEdit = mode is ManualFilterRuleDialogMode.Edit

    ClintDialog(
        title = stringResource(if (isEdit) R.string.quiver_guard_manual_filter_edit_dialog_title else R.string.quiver_guard_manual_filter_add_dialog_title),
        hideStatusBar = hideStatusBar,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { if (text.isNotBlank()) onConfirm(text) }, enabled = text.isNotBlank()) {
                    Text(stringResource(if (isEdit) R.string.quiver_guard_manual_filter_edit_action_save else R.string.filter_list_add_action_add), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        ClintOutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(if (isEdit) R.string.quiver_guard_manual_filter_rule_hint else R.string.quiver_guard_manual_filter_rule_hint)) },
            singleLine = isEdit,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
