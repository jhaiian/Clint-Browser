package com.jhaiian.clint.quiver

import com.jhaiian.clint.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import com.jhaiian.clint.ui.ClintOutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.theme.LocalClintColors
import com.jhaiian.clint.util.formatFileSize

/**
 * Stage 1 (Fetch): user enters a URL; onFetch triggers the caller's download+validate
 * coroutine, reflected back through [fetchStatus]. Stage 2 (Add): the title field appears,
 * pre-filled from the list's metadata if it had a Title: comment, and confirming calls
 * onConfirm. Editing the URL after a fetch calls onUrlChanged so the caller can reset
 * fetchStatus back to Idle and discard the temp file, mirroring the original's Stage-1 reset.
 */
@Composable
fun AddFilterListFromLinkDialog(
    hideStatusBar: Boolean,
    fetchStatus: AddLinkFetchStatus,
    onFetch: (url: String) -> Unit,
    onUrlChanged: () -> Unit,
    onConfirm: (url: String, title: String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var url by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf<String?>(null) }
    val invalidUrlMessage = stringResource(R.string.filter_list_add_error_invalid_url)

    val isFetched = fetchStatus is AddLinkFetchStatus.Fetched
    val isFetching = fetchStatus is AddLinkFetchStatus.Fetching

    LaunchedEffect(fetchStatus) {
        when (fetchStatus) {
            is AddLinkFetchStatus.Fetched -> {
                urlError = null
                if (!fetchStatus.metadataTitle.isNullOrBlank()) title = fetchStatus.metadataTitle
            }
            is AddLinkFetchStatus.Error -> urlError = fetchStatus.message
            else -> {}
        }
    }

    ClintDialog(
        title = stringResource(R.string.filter_list_add_dialog_title),
        hideStatusBar = hideStatusBar,
        onDismiss = onDismiss,
        cancelable = !isFetching,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss, enabled = !isFetching) {
                    Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(
                    onClick = {
                        if (isFetched) {
                            if (title.isNotBlank()) onConfirm(url.trim(), title.trim())
                        } else if (CustomFilterListFetcher.isValidUrl(url.trim())) {
                            onFetch(url.trim())
                        } else {
                            urlError = invalidUrlMessage
                        }
                    },
                    enabled = !isFetching && (if (isFetched) title.isNotBlank() else url.isNotBlank())
                ) {
                    Text(
                        stringResource(if (isFetched) R.string.filter_list_add_action_add else R.string.filter_list_add_action_fetch),
                        color = colors.primary, fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            ClintOutlinedTextField(
                value = url,
                onValueChange = {
                    url = it
                    if (isFetched || urlError != null) { urlError = null; onUrlChanged() }
                },
                label = { Text(stringResource(R.string.filter_list_add_url_hint)) },
                singleLine = true,
                enabled = !isFetching,
                isError = urlError != null,
                supportingText = urlError?.let { msg -> { Text(msg, color = colors.colorError, fontSize = 12.sp) } },
                modifier = Modifier.fillMaxWidth()
            )
            if (isFetched) {
                ClintOutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.filter_list_add_title_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            }
            if (isFetching) {
                val fetching = fetchStatus
                LinearProgressIndicator(
                    progress = { if (fetching.totalBytes > 0L) fetching.bytesRead.toFloat() / fetching.totalBytes.toFloat() else 0f },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    color = colors.primary, trackColor = colors.surfaceVariant
                )
                Text(
                    if (fetching.totalBytes > 0L) {
                        val percent = ((fetching.bytesRead * 100) / fetching.totalBytes).toInt()
                        stringResource(R.string.quiver_guard_download_progress_known, formatFileSize(fetching.bytesRead), formatFileSize(fetching.totalBytes), percent)
                    } else {
                        stringResource(R.string.quiver_guard_download_progress_unknown, formatFileSize(fetching.bytesRead))
                    },
                    color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

/** Single-stage counterpart for a file already imported and validated: the user only
 *  confirms or edits the title, pre-filled from the picked file's name. */
@Composable
internal fun AddFilterListFromFileDialog(
    imported: LocalFilterListImportResult.Success,
    hideStatusBar: Boolean,
    onConfirm: (title: String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var title by remember { mutableStateOf(imported.suggestedTitle) }
    var isSaving by remember { mutableStateOf(false) }

    ClintDialog(
        title = stringResource(R.string.filter_list_add_file_dialog_title),
        hideStatusBar = hideStatusBar,
        onDismiss = onDismiss,
        cancelable = !isSaving,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss, enabled = !isSaving) {
                    Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(
                    onClick = { if (title.isNotBlank()) { isSaving = true; onConfirm(title.trim()) } },
                    enabled = !isSaving && title.isNotBlank()
                ) {
                    Text(stringResource(R.string.filter_list_add_action_add), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                stringResource(R.string.filter_list_add_file_summary, imported.ruleCount, formatFileSize(imported.sizeBytes)),
                color = colors.secondaryText, fontSize = 13.sp
            )
            ClintOutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.filter_list_add_title_hint)) },
                singleLine = true,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )
        }
    }
}
