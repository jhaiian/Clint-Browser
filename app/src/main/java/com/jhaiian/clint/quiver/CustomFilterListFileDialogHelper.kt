package com.jhaiian.clint.quiver

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.jhaiian.clint.R
import kotlinx.coroutines.launch

// Launches the system file picker for adding a filter list from a local file.
internal fun QuiverGuardActivity.launchAddFilterListFromFile() {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "*/*"
    }
    filePickerLauncher.launch(intent)
}

// Called with the picked file's Uri once the file picker returns. Validates and copies
// the file's content into the app's storage; on success shows the title-confirmation
// dialog, on failure shows a toast (there is nothing to confirm, so no dialog is shown).
internal fun QuiverGuardActivity.importFilterListFromFile(uri: Uri) {
    activityScope.launch {
        when (val result = LocalFilterListImporter.import(applicationContext, uri)) {
            is LocalFilterListImportResult.Success -> uiState.addFromFileImport = result
            is LocalFilterListImportResult.Error -> Toast.makeText(this@importFilterListFromFile, getString(result.messageResId), Toast.LENGTH_SHORT).show()
        }
    }
}

// Persists the already-imported local file as a new custom (non-downloadable) filter
// list using the (possibly edited) title, and marks it enabled so it is included in the
// next compile without an extra tap.
internal fun QuiverGuardActivity.confirmAddFilterListFromFile(title: String) {
    val imported = uiState.addFromFileImport ?: return
    val id = database().addCustomFilterList(title, "")
    database().updateDownloadResult(id, imported.file.absolutePath, imported.sizeBytes, System.currentTimeMillis(), imported.ruleCount, null, null)
    onFilterListAdded(FilterList(id = id, name = title, downloadUrl = "", isEnabled = true, localPath = imported.file.absolutePath, fileSizeBytes = imported.sizeBytes, downloadedAt = System.currentTimeMillis(), ruleCount = imported.ruleCount, isCustom = true))
    uiState.addFromFileImport = null
    Toast.makeText(this, getString(R.string.quiver_guard_download_success_toast, title), Toast.LENGTH_SHORT).show()
}
