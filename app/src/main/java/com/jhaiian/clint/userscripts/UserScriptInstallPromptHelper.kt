package com.jhaiian.clint.userscripts

import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.delegates.addUserScripts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val USER_SCRIPT_INSTALL_PROMPT_PREFIX = "https://update.greasyfork.org/scripts/"

internal fun MainActivity.maybeShowUserScriptInstallPrompt(url: String): Boolean {
    if (!url.startsWith(USER_SCRIPT_INSTALL_PROMPT_PREFIX, ignoreCase = true)) return false
    if (uiState.userScriptInstallPromptRequest?.url == url) return true

    val webView = tabManager.activeTab?.webView
    webView?.stopLoading()

    uiState.userScriptInstallPromptRequest = UserScriptInstallPromptRequest(
        url = url,
        onConfirm = { installUserScriptFromPrompt(url) },
        onCancel = {
            uiState.userScriptInstallPromptRequest = null
            if (webView?.canGoBack() == true) webView.goBack()
        }
    )
    return true
}

private fun MainActivity.installUserScriptFromPrompt(url: String) {
    uiState.userScriptInstallPromptRequest = uiState.userScriptInstallPromptRequest?.copy(isInstalling = true)
    val webView = tabManager.activeTab?.webView
    lifecycleScope.launch {
        try {
            var fetchedCode: String? = null
            var fetchedName: String? = null
            UserScriptFetcher.fetch(applicationContext, url).collect { progress ->
                if (progress is UserScriptFetchProgress.Success) {
                    fetchedCode = progress.code
                    fetchedName = progress.metadata.name
                    uiState.userScriptInstallPromptRequest = uiState.userScriptInstallPromptRequest?.copy(scriptName = progress.metadata.name)
                }
            }
            val code = fetchedCode ?: throw UserScriptFetchException(getString(R.string.user_scripts_add_error_invalid_format))
            val requiresCache = withContext(Dispatchers.IO) {
                val meta = UserScriptMetadataParser.parse(code, "")
                if (meta.requires.isEmpty() && meta.resources.isEmpty()) "" else UserScriptRequireFetcher.fetchAssets(meta)
            }
            withContext(Dispatchers.IO) {
                UserScriptDatabase(applicationContext).insert(code, requiresCache, true, url)
            }
            UserScriptState.bumpDataVersion(this@installUserScriptFromPrompt)
            tabManager.tabs.forEach { addUserScripts(it) }
            uiState.userScriptInstallPromptRequest = null
            Toast.makeText(this@installUserScriptFromPrompt, getString(R.string.quiver_guard_download_success_toast, fetchedName ?: url), Toast.LENGTH_SHORT).show()
            if (webView?.canGoBack() == true) webView.goBack()
        } catch (e: Exception) {
            uiState.userScriptInstallPromptRequest = null
            Toast.makeText(this@installUserScriptFromPrompt, e.message ?: getString(R.string.filter_list_add_error_invalid_url), Toast.LENGTH_SHORT).show()
            if (webView?.canGoBack() == true) webView.goBack()
        }
    }
}
