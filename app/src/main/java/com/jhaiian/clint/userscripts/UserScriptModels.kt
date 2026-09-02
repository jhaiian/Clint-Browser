package com.jhaiian.clint.userscripts

import com.jhaiian.clint.ui.listscreen.ListSortKey
import com.jhaiian.clint.ui.listscreen.ListSortOrder

fun filterAndSortUserScripts(
    items: List<UserScriptListItem>,
    query: String,
    sortKey: ListSortKey,
    sortOrder: ListSortOrder
): List<UserScriptListItem> {
    val filtered = if (query.isBlank()) {
        items
    } else {
        val lower = query.trim().lowercase()
        items.filter {
            it.metadata.name.lowercase().contains(lower) || it.metadata.description.lowercase().contains(lower)
        }
    }
    return when (sortKey) {
        ListSortKey.TITLE -> if (sortOrder == ListSortOrder.ASCENDING)
            filtered.sortedBy { it.metadata.name.lowercase() }
        else
            filtered.sortedByDescending { it.metadata.name.lowercase() }

        ListSortKey.DATE_ADDED -> if (sortOrder == ListSortOrder.ASCENDING)
            filtered.sortedBy { it.script.createdAt }
        else
            filtered.sortedByDescending { it.script.createdAt }
    }
}

fun sectionLetterForUserScript(entry: UserScriptListItem, sortKey: ListSortKey): String = when (sortKey) {
    ListSortKey.TITLE -> entry.metadata.name.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
    ListSortKey.DATE_ADDED -> "#"
}
