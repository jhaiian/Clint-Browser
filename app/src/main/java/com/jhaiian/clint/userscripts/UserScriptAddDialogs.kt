package com.jhaiian.clint.userscripts

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

@Composable
fun AddUserScriptFromLinkDialog(
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    fetchStatus: AddUserScriptLinkFetchStatus,
    onFetch: (url: String) -> Unit,
    onUrlChanged: () -> Unit,
    onConfirm: (url: String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var url by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf<String?>(null) }
    val invalidUrlMessage = stringResource(R.string.filter_list_add_error_invalid_url)

    val isFetched = fetchStatus is AddUserScriptLinkFetchStatus.Fetched
    val isFetching = fetchStatus is AddUserScriptLinkFetchStatus.Fetching

    LaunchedEffect(fetchStatus) {
        when (fetchStatus) {
            is AddUserScriptLinkFetchStatus.Fetched -> urlError = null
            is AddUserScriptLinkFetchStatus.Error -> urlError = fetchStatus.message
            else -> {}
        }
    }

    ClintDialog(
        title = stringResource(R.string.user_scripts_add_link_dialog_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
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
                            onConfirm(url.trim())
                        } else if (UserScriptFetcher.isValidUrl(url.trim())) {
                            onFetch(url.trim())
                        } else {
                            urlError = invalidUrlMessage
                        }
                    },
                    enabled = !isFetching && url.isNotBlank()
                ) {
                    Text(
                        stringResource(if (isFetched) R.string.action_add else R.string.filter_list_add_action_fetch),
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
            if (fetchStatus is AddUserScriptLinkFetchStatus.Fetched) {
                Text(
                    stringResource(R.string.user_scripts_add_link_detected, fetchStatus.metadataName),
                    color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp)
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
