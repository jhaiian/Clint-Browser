package com.jhaiian.clint.quiver.engine

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object BlockedRequestCounter {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val tabCounters = ConcurrentHashMap<String, AtomicLong>()

    private val _globalCount = MutableStateFlow(0L)
    val globalCount: StateFlow<Long> = _globalCount.asStateFlow()

    private val _activeTabCount = MutableStateFlow(0L)
    val activeTabCount: StateFlow<Long> = _activeTabCount.asStateFlow()

    private var activeTabId: String? = null

    fun increment(tabId: String) {
        val counter = tabCounters.getOrPut(tabId) { AtomicLong(0L) }
        counter.incrementAndGet()
        val newGlobal = _globalCount.value + 1L
        mainHandler.post {
            _globalCount.value = newGlobal
            if (tabId == activeTabId) {
                _activeTabCount.value = counter.get()
            }
        }
    }

    fun setActiveTab(tabId: String?) {
        activeTabId = tabId
        val count = if (tabId != null) tabCounters[tabId]?.get() ?: 0L else 0L
        mainHandler.post { _activeTabCount.value = count }
    }

    fun resetTab(tabId: String) {
        tabCounters[tabId]?.set(0L)
        if (tabId == activeTabId) {
            mainHandler.post { _activeTabCount.value = 0L }
        }
    }

    fun removeTab(tabId: String) {
        tabCounters.remove(tabId)
        if (tabId == activeTabId) {
            mainHandler.post { _activeTabCount.value = 0L }
        }
    }

    fun getTabCount(tabId: String): Long = tabCounters[tabId]?.get() ?: 0L

    fun formatCount(count: Long): String = when {
        count >= 1_000_000L -> "${count / 1_000_000L}M"
        count >= 1_000L -> "${count / 1_000L}k"
        else -> count.toString()
    }
}
