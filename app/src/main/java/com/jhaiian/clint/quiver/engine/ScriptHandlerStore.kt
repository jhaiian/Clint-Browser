package com.jhaiian.clint.quiver.engine

import androidx.webkit.ScriptHandler
import java.util.concurrent.ConcurrentHashMap

class ScriptHandlerStore {
    private val handlers = ConcurrentHashMap<String, ScriptHandler>()

    fun put(tabId: String, handler: ScriptHandler) {
        remove(tabId)
        handlers[tabId] = handler
    }

    fun remove(tabId: String) {
        handlers.remove(tabId)?.remove()
    }

    fun clear() {
        val all = handlers.keys().toList()
        for (key in all) remove(key)
    }
}
