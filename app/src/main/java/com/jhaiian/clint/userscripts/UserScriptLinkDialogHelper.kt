package com.jhaiian.clint.userscripts

import android.widget.Toast

import com.jhaiian.clint.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun UserScriptsActivity.showAddUserScriptFromLinkDialog() {
    uiState.addLinkFetchStatus = AddUserScriptLinkFetchStatus.Idle
    uiState.addFromLinkDialogOpen = true
}

internal fun UserScriptsActivity.fetchUserScriptFromUrl(url: String) {
    uiState.addLinkFetchStatus = AddUserScriptLinkFetchStatus.Fetching(0L, 0L)
    activityScope.launch {
        try {
            UserScriptFetcher.fetch(applicationContext, url).collect { progress ->
                when (progress) {
                    is UserScriptFetchProgress.Progress ->
                        uiState.addLinkFetchStatus = AddUserScriptLinkFetchStatus.Fetching(progress.bytesRead, progress.totalBytes)
                    is UserScriptFetchProgress.Success ->
                        uiState.addLinkFetchStatus = AddUserScriptLinkFetchStatus.Fetched(progress.code, progress.metadata.name)
                }
            }
        } catch (e: Exception) {
            uiState.addLinkFetchStatus = AddUserScriptLinkFetchStatus.Error(e.message ?: getString(R.string.filter_list_add_error_invalid_url))
        }
    }
}

internal fun UserScriptsActivity.resetUserScriptLinkFetch() {
    uiState.addLinkFetchStatus = AddUserScriptLinkFetchStatus.Idle
}

internal fun UserScriptsActivity.confirmAddUserScriptFromLink(url: String) {
    val fetched = uiState.addLinkFetchStatus as? AddUserScriptLinkFetchStatus.Fetched ?: return
    val activity = this
    activityScope.launch {
        val requiresCache = withContext(Dispatchers.IO) {
            val meta = UserScriptMetadataParser.parse(fetched.code, "")
            if (meta.requires.isEmpty() && meta.resources.isEmpty()) "" else UserScriptRequireFetcher.fetchAssets(meta)
        }
        withContext(Dispatchers.IO) {
            db.insert(fetched.code, requiresCache, true, url)
        }
        UserScriptState.bumpDataVersion(activity)
        reload()
        uiState.addFromLinkDialogOpen = false
        uiState.addLinkFetchStatus = AddUserScriptLinkFetchStatus.Idle
        Toast.makeText(activity, getString(R.string.quiver_guard_download_success_toast, fetched.metadataName), Toast.LENGTH_SHORT).show()
    }
}
