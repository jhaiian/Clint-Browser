package com.jhaiian.clint.quiver
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward

import com.jhaiian.clint.R

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import com.jhaiian.clint.ui.ClintSwitch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.text.format.DateFormat
import com.jhaiian.clint.settings.common.SettingsRow
import com.jhaiian.clint.ui.AdaptiveWidthContainer
import com.jhaiian.clint.ui.rememberClintFavicon
import com.jhaiian.clint.ui.listscreen.ClintSearchField
import com.jhaiian.clint.ui.listscreen.ListFastScroller
import com.jhaiian.clint.ui.listscreen.SelectionOptionsMenu
import com.jhaiian.clint.ui.listscreen.ListSortKey
import com.jhaiian.clint.ui.listscreen.SortMenu
import com.jhaiian.clint.ui.listscreen.ListSortOrder
import com.jhaiian.clint.ui.theme.LocalClintColors
import com.jhaiian.clint.util.formatFileSize
import java.text.NumberFormat
import java.util.Date

@Composable
fun QuiverGuardScreen(
    state: QuiverGuardUiState,
    maxContentWidth: Dp?,
    onExit: () -> Unit,
    onMasterToggle: (Boolean) -> Unit,
    onManualFilterClick: () -> Unit,
    onItemClick: (FilterList) -> Unit,
    onAddFromFileClick: () -> Unit,
    onAddFromLinkClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onFabPrimaryClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onFilterListActionsClick: () -> Unit,
    onItemCheckUpdate: (FilterList) -> Unit,
    onItemForceUpdate: (FilterList) -> Unit,
    onItemRemove: (FilterList) -> Unit,
    onItemCopyName: (FilterList) -> Unit,
    onItemCopyLink: (FilterList) -> Unit,
    onItemShareLink: (FilterList) -> Unit,
    onSelectionCheckUpdate: () -> Unit,
    onSelectionForceUpdate: () -> Unit,
    onSelectionRemove: () -> Unit,
    onSelectionCopyName: () -> Unit,
    onSelectionCopyLink: () -> Unit,
    onSelectionShareLink: () -> Unit,
    onCheckUpdateActive: () -> Unit,
    onCheckUpdateAll: () -> Unit,
    onForceUpdateActive: () -> Unit,
    onForceUpdateAll: () -> Unit,
    onRecompile: () -> Unit
) {
    val colors = LocalClintColors.current
    val displayed = remember(state.filterLists, state.searchQuery, state.sortKey, state.sortOrder) {
        filterAndSortFilterLists(state.filterLists, state.searchQuery, state.sortKey, state.sortOrder)
    }
    val listState = rememberLazyListState()
    val dirty = state.isConfigurationDirty()
    val locked = state.isCompileRunning || state.isUpdateRunning
    val fastScrollerInteractive = !state.isSearchMode && state.sortKey == ListSortKey.TITLE
    val showDeleteFab = state.isInSelectionMode && state.selectedIds.isNotEmpty()
    val showPrimaryFab = !state.isInSelectionMode

    fun handleBack() {
        when {
            state.isFabMenuOpen -> state.isFabMenuOpen = false
            state.isSearchMode -> { state.isSearchMode = false; state.searchQuery = "" }
            state.isInSelectionMode -> state.exitSelectionMode()
            else -> onExit()
        }
    }

    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            QuiverGuardToolbar(
                state = state,
                onBack = ::handleBack,
                onSelectAll = { state.selectAll(displayed) },
                onInvertSelection = { state.invertSelection(displayed) },
                onRefreshClick = onRefreshClick,
                onFilterListActionsClick = onFilterListActionsClick,
                onSelectionCheckUpdate = onSelectionCheckUpdate,
                onSelectionForceUpdate = onSelectionForceUpdate,
                onSelectionRemove = onSelectionRemove,
                onSelectionCopyName = onSelectionCopyName,
                onSelectionCopyLink = onSelectionCopyLink,
                onSelectionShareLink = onSelectionShareLink,
                onCheckUpdateActive = onCheckUpdateActive,
                onCheckUpdateAll = onCheckUpdateAll,
                onForceUpdateActive = onForceUpdateActive,
                onForceUpdateAll = onForceUpdateAll,
                onRecompile = onRecompile
            )
            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            state.bannerText?.let { message ->
                Row(
                    Modifier.fillMaxWidth().background(colors.colorErrorContainer).padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Warning, contentDescription = null, tint = colors.colorError, modifier = Modifier.size(20.dp))
                    Text(message, color = colors.colorOnErrorContainer, fontSize = 13.sp, modifier = Modifier.padding(start = 12.dp))
                }
            }

            MasterSwitchRow(masterEnabled = state.masterEnabled, onToggle = onMasterToggle)

            Text(
                stringResource(R.string.quiver_guard_section_filter_lists),
                color = colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp).alpha(if (state.masterEnabled) 1f else 0.38f)
            )

            Box(Modifier.weight(1f).fillMaxWidth()) {
                AdaptiveWidthContainer(maxContentWidth) {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 2.dp, bottom = 88.dp)) {
                        item(key = "manual_filter") {
                            ManualFilterRow(
                                summary = state.manualFilterSummary,
                                masterEnabled = state.masterEnabled,
                                interactionLocked = locked,
                                onClick = onManualFilterClick
                            )
                        }
                        items(displayed, key = { it.id }) { filterList ->
                            FilterListRow(
                                filterList = filterList,
                                isSelected = filterList.id in state.selectedIds,
                                isDownloading = filterList.id in state.downloadingIds,
                                masterEnabled = state.masterEnabled,
                                interactionLocked = locked,
                                isInSelectionMode = state.isInSelectionMode,
                                onClick = {
                                    if (state.isInSelectionMode) state.toggleSelection(filterList.id) else onItemClick(filterList)
                                },
                                onLongClick = {
                                    if (!state.isInSelectionMode) {
                                        state.enterSelectionWith(filterList.id)
                                    } else if (filterList.id !in state.selectedIds) {
                                        state.selectedIds = state.selectedIds + filterList.id
                                    }
                                },
                                onCheckUpdate = { onItemCheckUpdate(filterList) },
                                onForceUpdate = { onItemForceUpdate(filterList) },
                                onRemove = { onItemRemove(filterList) },
                                onCopyName = { onItemCopyName(filterList) },
                                onCopyLink = { onItemCopyLink(filterList) },
                                onShareLink = { onItemShareLink(filterList) }
                            )
                        }
                    }
                    ListFastScroller(
                        listState = listState,
                        itemCount = displayed.size + 1,
                        isInteractive = fastScrollerInteractive,
                        sectionLetterAt = { index ->
                            if (index == 0) "#" else sectionLetterForFilterList(displayed[index - 1], state.sortKey)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                QuiverGuardFabMenu(
                    isOpen = state.isFabMenuOpen,
                    dirty = dirty,
                    enabled = state.masterEnabled && !locked,
                    showDeleteFab = showDeleteFab,
                    showPrimaryFab = showPrimaryFab,
                    onScrimClick = { state.isFabMenuOpen = false },
                    onToggleMenu = { state.isFabMenuOpen = !state.isFabMenuOpen },
                    onPrimaryClick = onFabPrimaryClick,
                    onDeleteClick = onDeleteClick,
                    onFileClick = { state.isFabMenuOpen = false; onAddFromFileClick() },
                    onLinkClick = { state.isFabMenuOpen = false; onAddFromLinkClick() }
                )
            }
        }
    }
}

@Composable
private fun MasterSwitchRow(masterEnabled: Boolean, onToggle: (Boolean) -> Unit) {
    val colors = LocalClintColors.current
    SettingsRow(
        icon = androidx.compose.material.icons.Icons.Filled.Security,
        title = stringResource(R.string.quiver_guard_master_switch_title),
        summary = stringResource(R.string.quiver_guard_description),
        colors = colors,
        onClick = { onToggle(!masterEnabled) },
        trailing = {
            ClintSwitch(checked = masterEnabled)
        }
    )
}

@Composable
private fun QuiverGuardToolbar(
    state: QuiverGuardUiState,
    onBack: () -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onRefreshClick: () -> Unit,
    onFilterListActionsClick: () -> Unit,
    onSelectionCheckUpdate: () -> Unit,
    onSelectionForceUpdate: () -> Unit,
    onSelectionRemove: () -> Unit,
    onSelectionCopyName: () -> Unit,
    onSelectionCopyLink: () -> Unit,
    onSelectionShareLink: () -> Unit,
    onCheckUpdateActive: () -> Unit,
    onCheckUpdateAll: () -> Unit,
    onForceUpdateActive: () -> Unit,
    onForceUpdateAll: () -> Unit,
    onRecompile: () -> Unit
) {
    val colors = LocalClintColors.current
    val showToolbarIcons = !state.isInSelectionMode && !state.isSearchMode
    var selectionItemOptionsMenuOpen by remember { mutableStateOf(false) }

    Surface(color = colors.surface, shadowElevation = 4.dp, modifier = Modifier.statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    if (state.isInSelectionMode) androidx.compose.material.icons.Icons.Filled.Close else androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null, tint = colors.onSurface
                )
            }

            if (state.isSearchMode) {
                ClintSearchField(
                    query = state.searchQuery,
                    onQueryChange = { state.searchQuery = it },
                    hint = stringResource(R.string.filter_list_search_hint),
                    onClose = { state.isSearchMode = false; state.searchQuery = "" }
                )
            } else {
                Text(
                    text = if (state.isInSelectionMode) stringResource(R.string.history_selected_count, state.selectedIds.size) else stringResource(R.string.quiver_guard),
                    color = colors.onSurface, fontSize = 19.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
            }

            if (showToolbarIcons) {
                IconButton(onClick = { state.isSearchMode = true }) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Search, contentDescription = null, tint = colors.iconTint)
                }
                IconButton(onClick = onRefreshClick) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Update, contentDescription = null, tint = colors.iconTint)
                }
                Box {
                    IconButton(onClick = { state.sortMenuOpen = true }) {
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.Sort, contentDescription = null, tint = colors.iconTint)
                    }
                    SortMenu(
                        expanded = state.sortMenuOpen,
                        onDismiss = { state.sortMenuOpen = false },
                        sortKey = state.sortKey, sortOrder = state.sortOrder,
                        onSortByTitle = { state.sortKey = ListSortKey.TITLE; state.sortOrder = ListSortOrder.ASCENDING },
                        onSortByDateAdded = { state.sortKey = ListSortKey.DATE_ADDED; state.sortOrder = ListSortOrder.DESCENDING },
                        onSortAscending = { state.sortOrder = ListSortOrder.ASCENDING },
                        onSortDescending = { state.sortOrder = ListSortOrder.DESCENDING }
                    )
                }
                Box {
                    IconButton(onClick = onFilterListActionsClick) {
                        Icon(androidx.compose.material.icons.Icons.Filled.MoreVert, contentDescription = null, tint = colors.iconTint)
                    }
                    FilterListActionsMenu(
                        expanded = state.filterListActionsMenuOpen,
                        onDismiss = { state.filterListActionsMenuOpen = false },
                        onCheckUpdateActive = onCheckUpdateActive,
                        onCheckUpdateAll = onCheckUpdateAll,
                        onForceUpdateActive = onForceUpdateActive,
                        onForceUpdateAll = onForceUpdateAll,
                        onRecompile = onRecompile
                    )
                }
            }
            if (state.isInSelectionMode) {
                Box {
                    IconButton(onClick = { state.selectionOptionsMenuOpen = true }) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Checklist, contentDescription = null, tint = colors.iconTint)
                    }
                    SelectionOptionsMenu(
                        expanded = state.selectionOptionsMenuOpen,
                        onDismiss = { state.selectionOptionsMenuOpen = false },
                        onSelectAll = onSelectAll, onInvertSelection = onInvertSelection,
                        onDeselectAll = { state.deselectAll() }
                    )
                }
                if (state.selectedIds.isNotEmpty()) {
                    Box {
                        IconButton(onClick = { selectionItemOptionsMenuOpen = true }) {
                            Icon(androidx.compose.material.icons.Icons.Filled.MoreVert, contentDescription = null, tint = colors.iconTint)
                        }
                        FilterListItemOptionsMenu(
                            expanded = selectionItemOptionsMenuOpen,
                            isLocal = false,
                            onDismiss = { selectionItemOptionsMenuOpen = false },
                            onCheckUpdate = onSelectionCheckUpdate,
                            onForceUpdate = onSelectionForceUpdate,
                            onRemove = onSelectionRemove,
                            onCopyName = onSelectionCopyName,
                            onCopyLink = onSelectionCopyLink,
                            onShareLink = onSelectionShareLink
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualFilterRow(
    summary: ManualFilterSummary,
    masterEnabled: Boolean,
    interactionLocked: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalClintColors.current
    val statusText = when {
        summary.ruleCount == 0 -> stringResource(R.string.quiver_guard_manual_filter_status_empty)
        summary.isEnabled -> stringResource(R.string.quiver_guard_manual_filter_status_enabled, NumberFormat.getNumberInstance().format(summary.ruleCount))
        else -> stringResource(R.string.quiver_guard_manual_filter_status_disabled, NumberFormat.getNumberInstance().format(summary.ruleCount))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .alpha(if (masterEnabled) 1f else 0.38f)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardBackground)
            .clickable(enabled = masterEnabled && !interactionLocked, onClick = onClick)
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(colors.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(androidx.compose.material.icons.Icons.Filled.Tune, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(stringResource(R.string.quiver_guard_manual_filter_title), color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(statusText, color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun FilterListRow(
    filterList: FilterList,
    isSelected: Boolean,
    isDownloading: Boolean,
    masterEnabled: Boolean,
    interactionLocked: Boolean,
    isInSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCheckUpdate: () -> Unit,
    onForceUpdate: () -> Unit,
    onRemove: () -> Unit,
    onCopyName: () -> Unit,
    onCopyLink: () -> Unit,
    onShareLink: () -> Unit
) {
    var optionsMenuOpen by remember { mutableStateOf(false) }
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val cardColor = if (isSelected) lerp(colors.cardBackground, colors.primary, 0.22f) else colors.cardBackground
    val rowAlpha = when {
        !masterEnabled -> 0.38f
        isDownloading -> 0.6f
        else -> 1f
    }
    val statusText = when {
        isDownloading -> stringResource(R.string.filter_list_status_downloading)
        filterList.isDownloaded -> stringResource(
            R.string.filter_list_status_downloaded_with_rules,
            NumberFormat.getNumberInstance().format(filterList.ruleCount),
            formatFileSize(filterList.fileSizeBytes),
            DateFormat.getMediumDateFormat(context).format(Date(filterList.downloadedAt))
        )
        else -> stringResource(R.string.filter_list_status_not_downloaded)
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .alpha(rowAlpha)
            .clip(RoundedCornerShape(14.dp))
            .background(cardColor)
            .combinedClickable(enabled = masterEnabled && !interactionLocked, onClick = onClick, onLongClick = onLongClick)
            .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val favicon = rememberClintFavicon(filterList.downloadUrl)
        Box(Modifier.size(40.dp).clip(CircleShape).background(colors.surfaceVariant), contentAlignment = Alignment.Center) {
            if (favicon != null) {
                Image(bitmap = favicon.asImageBitmap(), contentDescription = null, modifier = Modifier.size(22.dp))
            } else {
                Icon(androidx.compose.material.icons.Icons.Filled.Security, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(20.dp))
            }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 4.dp)) {
            Text(filterList.name, color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(statusText, color = colors.secondaryText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        }
        if (!isInSelectionMode) {
            ClintSwitch(checked = filterList.isEnabled)
        }
        Box {
            IconButton(onClick = { optionsMenuOpen = true }, enabled = masterEnabled && !interactionLocked) {
                Icon(androidx.compose.material.icons.Icons.Filled.MoreVert, contentDescription = null, tint = colors.iconTint)
            }
            FilterListItemOptionsMenu(
                expanded = optionsMenuOpen,
                isLocal = filterList.isLocal,
                onDismiss = { optionsMenuOpen = false },
                onCheckUpdate = onCheckUpdate,
                onForceUpdate = onForceUpdate,
                onRemove = onRemove,
                onCopyName = onCopyName,
                onCopyLink = onCopyLink,
                onShareLink = onShareLink
            )
        }
    }
}

@Composable
private fun BoxScope.QuiverGuardFabMenu(
    isOpen: Boolean,
    dirty: Boolean,
    enabled: Boolean,
    showDeleteFab: Boolean,
    showPrimaryFab: Boolean,
    onScrimClick: () -> Unit,
    onToggleMenu: () -> Unit,
    onPrimaryClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onFileClick: () -> Unit,
    onLinkClick: () -> Unit
) {
    val colors = LocalClintColors.current
    val rotation by animateFloatAsState(if (isOpen) 45f else 0f, label = "fabMenuRotation")

    if (isOpen) {
        Box(Modifier.fillMaxSize().background(colors.background.copy(alpha = 0.6f)).clickable(onClick = onScrimClick))
    }

    if (showDeleteFab) {
        FloatingActionButton(
            onClick = onDeleteClick,
            containerColor = colors.buttonBackground, contentColor = colors.buttonIconTint,
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 24.dp, end = 20.dp)
        ) {
            Icon(androidx.compose.material.icons.Icons.Filled.Delete, contentDescription = null)
        }
    }

    if (showPrimaryFab) {
        AnimatedVisibility(
            visible = isOpen,
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 96.dp, end = 20.dp),
            enter = fadeIn(), exit = fadeOut()
        ) {
            Column(horizontalAlignment = Alignment.End) {
                FabMenuPill(text = stringResource(R.string.filter_list_add_menu_from_file), icon = androidx.compose.material.icons.Icons.Filled.Description, onClick = onFileClick)
                Spacer(Modifier.height(10.dp))
                FabMenuPill(text = stringResource(R.string.filter_list_add_menu_from_link), icon = androidx.compose.material.icons.Icons.Filled.Link, onClick = onLinkClick)
            }
        }

        FloatingActionButton(
            onClick = { if (dirty) onPrimaryClick() else onToggleMenu() },
            containerColor = colors.buttonBackground, contentColor = colors.buttonIconTint,
            modifier = Modifier
                .align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 24.dp, end = 20.dp)
                .alpha(if (!enabled) 0.38f else 1f)
        ) {
            Icon(
                if (dirty) androidx.compose.material.icons.Icons.Filled.Save else androidx.compose.material.icons.Icons.Filled.Add,
                contentDescription = stringResource(if (dirty) R.string.quiver_guard_compile_fab_desc else R.string.filter_list_add_fab_desc),
                modifier = if (dirty) Modifier else Modifier.rotate(rotation)
            )
        }
    }
}

@Composable
private fun FabMenuPill(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.buttonBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = colors.buttonTextColor, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(end = 10.dp))
        Icon(icon, contentDescription = null, tint = colors.buttonIconTint, modifier = Modifier.size(18.dp))
    }
}
