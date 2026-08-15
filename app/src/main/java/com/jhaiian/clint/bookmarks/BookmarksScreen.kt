package com.jhaiian.clint.bookmarks
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ArrowBack

import com.jhaiian.clint.R

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.ui.AdaptiveWidthContainer
import com.jhaiian.clint.ui.listscreen.ListFastScroller
import com.jhaiian.clint.ui.listscreen.ListSortOrder
import com.jhaiian.clint.ui.listscreen.SelectionOptionsMenu
import com.jhaiian.clint.ui.rememberClintFavicon
import com.jhaiian.clint.ui.listscreen.ClintSearchField
import com.jhaiian.clint.ui.theme.LocalClintColors
import com.jhaiian.clint.util.formatRelativeTimestamp

/** See [com.jhaiian.clint.history.HistoryScreen] for the adaptive-width rationale this mirrors. */
@Composable
fun BookmarksScreen(
    state: BookmarksUiState,
    maxContentWidth: Dp?,
    onExit: () -> Unit,
    onOpenItem: (Bookmark) -> Unit,
    onDeleteSelectedClick: () -> Unit
) {
    val colors = LocalClintColors.current
    val displayed = remember(state.items, state.searchQuery, state.sortKey, state.sortOrder) {
        filterAndSortBookmarks(state.items, state.searchQuery, state.sortKey, state.sortOrder)
    }
    val listState = rememberLazyListState()
    val fastScrollerInteractive = !state.isSearchMode && state.sortKey == BookmarksSortKey.TITLE
    val showDeleteFab = state.isInSelectionMode && state.selectedKeys.isNotEmpty()

    fun handleBack() {
        when {
            state.isSearchMode -> { state.isSearchMode = false; state.searchQuery = "" }
            state.isInSelectionMode -> state.exitSelectionMode()
            else -> onExit()
        }
    }

    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BookmarksToolbar(
                state = state,
                onBack = ::handleBack,
                onSelectAll = { state.selectAll(displayed) },
                onInvertSelection = { state.invertSelection(displayed) }
            )
            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.isLoading -> Unit
                    displayed.isEmpty() -> Text(
                        stringResource(R.string.bookmarks_empty),
                        color = colors.secondaryText, fontSize = 15.sp,
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp)
                    )
                    else -> {
                        AdaptiveWidthContainer(maxContentWidth) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp)
                            ) {
                                items(displayed, key = { it.url }) { item ->
                                    BookmarkRow(
                                        item = item,
                                        isSelected = item.url in state.selectedKeys,
                                        isInSelectionMode = state.isInSelectionMode,
                                        onClick = {
                                            if (state.isInSelectionMode) state.toggleSelection(item.url) else onOpenItem(item)
                                        },
                                        onLongClick = {
                                            if (!state.isInSelectionMode) {
                                                state.enterSelectionWith(item.url)
                                            } else if (item.url !in state.selectedKeys) {
                                                state.selectedKeys = state.selectedKeys + item.url
                                            }
                                        }
                                    )
                                }
                            }
                            ListFastScroller(
                                listState = listState,
                                itemCount = displayed.size,
                                isInteractive = fastScrollerInteractive,
                                sectionLetterAt = { index -> sectionLetterForBookmark(displayed[index]) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                if (showDeleteFab) {
                    FloatingActionButton(
                        onClick = onDeleteSelectedClick,
                        containerColor = colors.buttonBackground, contentColor = colors.buttonIconTint,
                        modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 24.dp, end = 20.dp)
                    ) {
                        Icon(
                            androidx.compose.material.icons.Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.bookmarks_delete_selected_desc)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarksToolbar(
    state: BookmarksUiState,
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
                    hint = stringResource(R.string.bookmarks_search_hint),
                    onClose = { state.isSearchMode = false; state.searchQuery = "" }
                )
            } else {
                Text(
                    text = if (state.isInSelectionMode) stringResource(R.string.bookmarks_selected_count, state.selectedCount) else stringResource(R.string.bookmarks_title),
                    color = colors.onSurface, fontSize = 19.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
            }

            if (showToolbarIcons) {
                IconButton(onClick = { state.isSearchMode = true }) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Search, contentDescription = stringResource(R.string.bookmarks_search), tint = colors.iconTint)
                }
                Box {
                    IconButton(onClick = { state.sortMenuOpen = true }) {
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.history_sort), tint = colors.primary)
                    }
                    BookmarksSortMenu(
                        expanded = state.sortMenuOpen,
                        onDismiss = { state.sortMenuOpen = false },
                        sortKey = state.sortKey, sortOrder = state.sortOrder,
                        onSortByTitle = { state.sortKey = BookmarksSortKey.TITLE; state.sortOrder = ListSortOrder.ASCENDING },
                        onSortByLastVisit = { state.sortKey = BookmarksSortKey.LAST_VISIT; state.sortOrder = ListSortOrder.DESCENDING },
                        onSortByDateAdded = { state.sortKey = BookmarksSortKey.DATE_ADDED; state.sortOrder = ListSortOrder.DESCENDING },
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
private fun BookmarkRow(
    item: Bookmark,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = LocalClintColors.current
    val cardColor = if (isSelected) lerp(colors.cardBackground, colors.primary, 0.22f) else colors.cardBackground

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cardColor)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val favicon = rememberClintFavicon(item.url, item.faviconUrl)
        Box(Modifier.size(40.dp).clip(CircleShape).background(colors.surfaceVariant), contentAlignment = Alignment.Center) {
            if (favicon != null) {
                Image(bitmap = favicon.asImageBitmap(), contentDescription = null, modifier = Modifier.size(22.dp))
            } else {
                Icon(androidx.compose.material.icons.Icons.Filled.Public, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(20.dp))
            }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                item.title.ifBlank { item.url },
                color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                item.url.removePrefix("https://").removePrefix("http://"),
                color = colors.secondaryText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
            if (item.lastVisit > 0L) {
                Text(
                    formatRelativeTimestamp(item.lastVisit),
                    color = colors.secondaryText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

private fun filterAndSortBookmarks(
    items: List<Bookmark>,
    query: String,
    sortKey: BookmarksSortKey,
    sortOrder: ListSortOrder
): List<Bookmark> {
    val filtered = if (query.isBlank()) items else {
        val q = query.trim().lowercase()
        items.filter { it.title.lowercase().contains(q) || it.url.lowercase().contains(q) }
    }
    val sorted = when (sortKey) {
        BookmarksSortKey.TITLE -> filtered.sortedBy { it.title.ifBlank { it.url }.lowercase() }
        BookmarksSortKey.LAST_VISIT -> filtered.sortedBy { it.lastVisit }
        BookmarksSortKey.DATE_ADDED -> filtered.sortedBy { it.addedAt }
    }
    return if (sortOrder == ListSortOrder.DESCENDING) sorted.reversed() else sorted
}

private fun sectionLetterForBookmark(item: Bookmark): String {
    val display = item.title.ifBlank { item.url }.trimStart()
    val first = display.firstOrNull() ?: return "#"
    return if (first.isLetter()) first.uppercaseChar().toString() else if (first.isDigit()) first.toString() else "#"
}
