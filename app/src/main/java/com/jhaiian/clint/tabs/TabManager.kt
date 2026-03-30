package com.jhaiian.clint.tabs

class TabManager {

    val tabs = mutableListOf<BrowserTab>()
    var activeIndex = 0

    val activeTab: BrowserTab? get() = tabs.getOrNull(activeIndex)
    val count: Int get() = tabs.size

    fun add(tab: BrowserTab): Int {
        tabs.add(tab)
        activeIndex = tabs.lastIndex
        return activeIndex
    }

    fun closeTab(index: Int) {
        if (index !in tabs.indices) return
        val tab = tabs[index]
        if (tab.isIncognito) {
            tab.webView.clearCache(true)
            tab.webView.clearHistory()
        }
        tab.webView.destroy()
        tabs.removeAt(index)
        if (activeIndex >= tabs.size) {
            activeIndex = (tabs.size - 1).coerceAtLeast(0)
        }
    }

    fun switchTo(index: Int) {
        if (index in tabs.indices) activeIndex = index
    }

    fun previews(): List<TabPreview> = tabs.map {
        TabPreview(it.id, it.title.ifBlank { "New Tab" }, it.url, it.isIncognito)
    }

    fun destroyAll() {
        tabs.forEach {
            if (it.isIncognito) {
                it.webView.clearCache(true)
                it.webView.clearHistory()
            }
            it.webView.destroy()
        }
        tabs.clear()
        activeIndex = 0
    }
}
