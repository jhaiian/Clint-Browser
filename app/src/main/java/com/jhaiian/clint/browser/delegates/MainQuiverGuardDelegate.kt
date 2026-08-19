package com.jhaiian.clint.browser.delegates

import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.quiver.QuiverGuardActivity
import com.jhaiian.clint.quiver.engine.BlockedRequestCounter
import com.jhaiian.clint.quiver.engine.QuiverGuardEngine
import com.jhaiian.clint.quiver.engine.QuiverGuardWebIntegration
import com.jhaiian.clint.settings.sitepermissions.SitePermissionDatabase
import com.jhaiian.clint.settings.sitepermissions.SitePermissionManager
import com.jhaiian.clint.tabs.BrowserTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.withContext

internal fun MainActivity.observeQuiverGuardCounter() {
    BlockedRequestCounter.activeTabCount
        .launchIn(lifecycleScope)
}

internal fun MainActivity.initializeQuiverGuardEngine() {
    lifecycleScope.launch(Dispatchers.IO) {
        val result = QuiverGuardWebIntegration.initialize(this@initializeQuiverGuardEngine)
        if (result.requiresRecompile) {
            withContext(Dispatchers.Main) {
                showQuiverGuardRecompileRequiredDialog(result)
            }
        }
    }
}

internal fun MainActivity.showQuiverGuardRecompileRequiredDialog(reason: QuiverGuardEngine.PreloadResult) {
    val messageRes = when (reason) {
        QuiverGuardEngine.PreloadResult.VERSION_MISMATCH -> R.string.quiver_guard_recompile_reason_version_mismatch
        QuiverGuardEngine.PreloadResult.BAD_HEADER -> R.string.quiver_guard_recompile_reason_bad_header
        QuiverGuardEngine.PreloadResult.BAD_CHECKSUM -> R.string.quiver_guard_recompile_reason_bad_checksum
        QuiverGuardEngine.PreloadResult.FLATBUFFER_PARSING_ERROR -> R.string.quiver_guard_recompile_reason_flatbuffer_error
        else -> R.string.quiver_guard_recompile_reason_unknown
    }
    uiState.confirmDialogConfig = com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig(
        title = getString(R.string.quiver_guard_recompile_required_title),
        message = getString(messageRes),
        cancelable = false,
        positiveLabel = getString(R.string.quiver_guard_recompile_required_button),
        onPositive = {
            startActivity(
                Intent(this, QuiverGuardActivity::class.java)
                    .putExtra(QuiverGuardActivity.EXTRA_AUTO_RECOMPILE, true)
            )
        }
    )
}

internal fun MainActivity.onQuiverGuardPageStarted(tab: BrowserTab, url: String) {
    val isEnabled = prefs.getBoolean("quiver_guard_enabled", false)
    if (!isEnabled) return
    if (!url.startsWith("http://") && !url.startsWith("https://")) return

    quiverGuardJobs.remove(tab.id)?.cancel()
    quiverGuardJobs[tab.id] = lifecycleScope.launch {
        val host = runCatching { android.net.Uri.parse(url).host }.getOrNull()

        val isExcepted = withContext(Dispatchers.IO) {
            host != null && SitePermissionManager.getState(
                this@onQuiverGuardPageStarted, host, SitePermissionDatabase.TYPE_QUIVER_GUARD_EXCEPTION
            ) != null
        }

        BlockedRequestCounter.resetTab(tab.id)

        if (isExcepted) {

            quiverGuardScriptHandlers.remove(tab.id)
            return@launch
        }

        val script = withContext(Dispatchers.IO) {
            QuiverGuardWebIntegration.buildDocumentStartScript(this@onQuiverGuardPageStarted, url, true)
        }

        if (script != null) {
            QuiverGuardWebIntegration.applyDocumentStartScript(
                tab.webView, quiverGuardScriptHandlers, tab.id, script
            )
            QuiverGuardWebIntegration.applyCosmeticFilterScript(tab.webView, script)
        }
    }
}

internal fun MainActivity.onQuiverGuardPageFinished(tab: BrowserTab, url: String) {
    val isEnabled = prefs.getBoolean("quiver_guard_enabled", false)
    if (!isEnabled) return
    if (!url.startsWith("http://") && !url.startsWith("https://")) return

    quiverGuardJobs.remove(tab.id)?.cancel()
    quiverGuardJobs[tab.id] = lifecycleScope.launch {
        val host = runCatching { android.net.Uri.parse(url).host }.getOrNull()
        val isExcepted = withContext(Dispatchers.IO) {
            host != null && SitePermissionManager.getState(
                this@onQuiverGuardPageFinished, host, SitePermissionDatabase.TYPE_QUIVER_GUARD_EXCEPTION
            ) != null
        }
        if (isExcepted) return@launch

        val script = withContext(Dispatchers.IO) {
            QuiverGuardWebIntegration.buildCosmeticFilterScript(this@onQuiverGuardPageFinished, url, true)
        } ?: return@launch

        QuiverGuardWebIntegration.applyCosmeticFilterScript(tab.webView, script)
    }
}

internal fun MainActivity.onQuiverGuardTabClosed(tab: BrowserTab) {
    quiverGuardJobs.remove(tab.id)?.cancel()
    quiverGuardScriptHandlers.remove(tab.id)
    BlockedRequestCounter.removeTab(tab.id)
}

internal fun MainActivity.onQuiverGuardEnabled(enabled: Boolean) {
    if (!enabled) {
        quiverGuardScriptHandlers.clear()
        reloadActiveTabIfWeb()
        return
    }
    lifecycleScope.launch {
        val result = withContext(Dispatchers.IO) {
            QuiverGuardWebIntegration.initialize(this@onQuiverGuardEnabled)
        }
        reloadActiveTabIfWeb()
        if (result.requiresRecompile) {
            showQuiverGuardRecompileRequiredDialog(result)
        }
    }
}

private fun MainActivity.reloadActiveTabIfWeb() {
    tabManager.activeTab?.let { tab ->
        val url = tab.webView.url ?: return@let
        if (url.startsWith("http://") || url.startsWith("https://")) {
            tab.webView.reload()
        }
    }
}
