package com.jhaiian.clint.downloads
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Folder

import com.jhaiian.clint.R

import android.net.Uri
import android.webkit.URLUtil
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.settings.common.dialogSectionBackground
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.settings.downloads.DownloadSettingsKeys
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintOutlinedTextField
import com.jhaiian.clint.ui.ClintSlider
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import com.jhaiian.clint.ui.listscreen.ConfirmDialogHost
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.theme.LocalClintColors
import com.jhaiian.clint.util.formatFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

data class ManualDownloadSubmission(
    val url: String,
    val filename: String,
    val retryEnabled: Boolean,
    val unmeteredOnly: Boolean,
    val splitParts: Int,
    val multithreadingParts: Int,
    val speedLimitBytesPerSec: Long,
    val locationMode: String,
    val customLocationUri: String?,
    val scheduledStartAtMillis: Long
)

@Composable
fun DownloadManualDialog(
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    prefillUrl: String? = null,
    onDismiss: () -> Unit,
    onSubmit: (ManualDownloadSubmission, onDismiss: () -> Unit, onRename: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val activity = context as DownloadsActivity
    val colors = LocalClintColors.current
    val scope = rememberCoroutineScope()
    val prefs = remember { androidx.preference.PreferenceManager.getDefaultSharedPreferences(context) }

    var url by remember { mutableStateOf(prefillUrl ?: "") }
    var isFetching by remember { mutableStateOf(false) }
    var isFetched by remember { mutableStateOf(false) }
    var urlError by remember { mutableStateOf<String?>(null) }
    var filename by remember { mutableStateOf("") }
    var extension by remember { mutableStateOf("") }
    var fileSizeText by remember { mutableStateOf<String?>(null) }
    var fetchedContentLength by remember { mutableStateOf(-1L) }

    var locationMode by remember {
        mutableStateOf(prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_LOCATION_MODE, DownloadSettingsKeys.MODE_DEFAULT) ?: DownloadSettingsKeys.MODE_DEFAULT)
    }
    var customUri by remember {
        mutableStateOf(prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_CUSTOM_URI, null)?.let { Uri.parse(it) })
    }
    var locationMenuOpen by remember { mutableStateOf(false) }

    var retryEnabled by remember { mutableStateOf(prefs.getBoolean(DownloadSettingsKeys.PREF_RETRY_ENABLED, DownloadSettingsKeys.DEFAULT_RETRY_ENABLED)) }
    var unmeteredOnly by remember { mutableStateOf(prefs.getBoolean(DownloadSettingsKeys.PREF_UNMETERED_ONLY, DownloadSettingsKeys.DEFAULT_UNMETERED_ONLY)) }
    var splitParts by remember { mutableStateOf(prefs.getInt(DownloadSettingsKeys.PREF_SPLIT_PARTS, DownloadSettingsKeys.DEFAULT_SPLIT_PARTS).coerceIn(1, 32)) }
    var multithreadingParts by remember { mutableStateOf(prefs.getInt(DownloadSettingsKeys.PREF_MULTITHREADING_PARTS, DownloadSettingsKeys.DEFAULT_MULTITHREADING_PARTS).coerceIn(1, 8)) }

    val initSpeedLimitAmount = remember { prefs.getInt(DownloadSettingsKeys.PREF_SPEED_LIMIT_AMOUNT, DownloadSettingsKeys.DEFAULT_SPEED_LIMIT_AMOUNT) }
    val initSpeedLimitUnit = remember { prefs.getString(DownloadSettingsKeys.PREF_SPEED_LIMIT_UNIT, DEFAULT_SPEED_LIMIT_UNIT) ?: DEFAULT_SPEED_LIMIT_UNIT }
    var speedLimitText by remember { mutableStateOf(if (initSpeedLimitAmount > 0) initSpeedLimitAmount.toString() else "") }
    val kbLabel = stringResource(R.string.speed_limit_unit_kb)
    val mbLabel = stringResource(R.string.speed_limit_unit_mb)
    var speedUnitLabel by remember { mutableStateOf(if (initSpeedLimitUnit == SPEED_LIMIT_UNIT_MB) mbLabel else kbLabel) }
    var speedUnitMenuOpen by remember { mutableStateOf(false) }

    var scheduleEnabled by remember { mutableStateOf(false) }
    var scheduledMillis by remember { mutableStateOf(0L) }
    var blockingError by remember { mutableStateOf<ConfirmDialogConfig?>(null) }

    fun doFetch() {
        val typed = url.trim()
        if (!URLUtil.isValidUrl(typed)) {
            urlError = context.getString(R.string.download_manual_error_invalid_url)
            return
        }
        urlError = null
        isFetching = true
        scope.launch {
            val ua = android.webkit.WebSettings.getDefaultUserAgent(context)
            val result = withContext(Dispatchers.IO) {
                try {
                    val headRequest = okhttp3.Request.Builder().url(typed).head().header("User-Agent", ua).build()
                    val headResponse = ClintDownloadManager.httpClient.newCall(headRequest).execute()
                    headResponse.use { resp ->
                        if (!resp.isSuccessful && resp.code != 405) return@withContext null
                        val contentDisposition = resp.header("Content-Disposition") ?: ""
                        val contentType = resp.header("Content-Type")?.substringBefore(";")?.trim() ?: ""
                        var contentLength = resp.header("Content-Length")?.toLongOrNull() ?: -1L
                        if (contentLength <= 0L) {
                            try {
                                val rangeRequest = okhttp3.Request.Builder().url(typed).get()
                                    .header("User-Agent", ua).header("Range", "bytes=0-0").build()
                                val rangeResponse = ClintDownloadManager.httpClient.newCall(rangeRequest).execute()
                                contentLength = rangeResponse.header("Content-Range")?.substringAfterLast('/')?.toLongOrNull()
                                    ?: rangeResponse.header("Content-Length")?.toLongOrNull() ?: -1L
                                rangeResponse.close()
                            } catch (_: Exception) { }
                        }
                        resolveFilename(typed, contentDisposition, contentType) to contentLength
                    }
                } catch (_: Exception) { null }
            }
            isFetching = false
            if (result == null) {
                urlError = context.getString(R.string.download_manual_error_network)
                return@launch
            }
            val (resolvedName, contentLength) = result
            isFetched = true
            fetchedContentLength = contentLength
            val dot = resolvedName.lastIndexOf('.')
            filename = if (dot > 0) resolvedName.substring(0, dot) else resolvedName
            extension = if (dot > 0) resolvedName.substring(dot + 1) else ""
            fileSizeText = if (contentLength > 0L) context.getString(R.string.download_dialog_file_size_value, formatFileSize(contentLength))
                else context.getString(R.string.download_dialog_file_size_unknown)
        }
    }

    fun resetFetchState() {
        if (isFetched) {
            isFetched = false
            filename = ""
            extension = ""
            fileSizeText = null
            fetchedContentLength = -1L
        }
    }

    LaunchedEffect(Unit) {
        if (!prefillUrl.isNullOrBlank()) doFetch()
    }

    ClintDialog(
        title = stringResource(R.string.download_dialog_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(
                    onClick = {
                        if (!isFetched) {
                            doFetch()
                            return@TextButton
                        }
                        val resolvedFilename = if (extension.isNotBlank()) "$filename.$extension" else filename
                        val storageError = checkStorageAvailable(context, fetchedContentLength, locationMode, customUri)
                        if (storageError != null) {
                            blockingError = ConfirmDialogConfig(
                                title = context.getString(R.string.download_error_storage_title),
                                message = storageError,
                                positiveLabel = context.getString(R.string.action_ok)
                            )
                            return@TextButton
                        }
                        val fat32Error = checkFat32FileSizeLimit(context, fetchedContentLength, locationMode, customUri)
                        if (fat32Error != null) {
                            blockingError = ConfirmDialogConfig(
                                title = context.getString(R.string.download_error_fat32_title),
                                message = fat32Error,
                                positiveLabel = context.getString(R.string.action_ok)
                            )
                            return@TextButton
                        }
                        val effectiveScheduledMillis = if (scheduleEnabled) scheduledMillis else 0L
                        if (effectiveScheduledMillis > 0L && effectiveScheduledMillis <= System.currentTimeMillis()) {
                            blockingError = ConfirmDialogConfig(
                                title = context.getString(R.string.download_schedule_invalid_title),
                                message = context.getString(R.string.download_schedule_past_time_error),
                                positiveLabel = context.getString(R.string.action_ok)
                            )
                            return@TextButton
                        }
                        val speedAmount = speedLimitText.toIntOrNull()?.coerceAtLeast(0) ?: 0
                        val speedUnit = if (speedUnitLabel == mbLabel) SPEED_LIMIT_UNIT_MB else SPEED_LIMIT_UNIT_KB
                        val speedLimitBytesPerSec = resolveSpeedLimitBytesPerSec(context, speedAmount, speedUnit)
                        val submission = ManualDownloadSubmission(
                            url = url.trim(),
                            filename = resolvedFilename,
                            retryEnabled = retryEnabled,
                            unmeteredOnly = unmeteredOnly,
                            splitParts = splitParts,
                            multithreadingParts = multithreadingParts,
                            speedLimitBytesPerSec = speedLimitBytesPerSec,
                            locationMode = locationMode,
                            customLocationUri = customUri?.toString(),
                            scheduledStartAtMillis = effectiveScheduledMillis
                        )
                        onSubmit(submission, onDismiss) {

                        }
                    },
                    enabled = url.isNotBlank() && !isFetching
                ) {
                    Text(
                        if (isFetching) stringResource(R.string.download_manual_fetching)
                        else if (isFetched) stringResource(R.string.action_download)
                        else stringResource(R.string.download_manual_fetch),
                        color = colors.primary, fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            DialogSectionLabel(stringResource(R.string.download_dialog_section_link))
            SettingsSection(colors.dialogSectionBackground) {
                Column(Modifier.padding(16.dp)) {
                    ClintOutlinedTextField(
                        value = url,
                        onValueChange = { url = it; urlError = null; resetFetchState() },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.download_manual_url_hint)) },
                        singleLine = true,
                        isError = urlError != null,
                        trailingIcon = if (isFetching) { { CircularProgressIndicator(modifier = Modifier.size(20.dp), color = colors.primary, strokeWidth = 2.dp) } } else null,
                        supportingText = urlError?.let { err -> { Text(err, color = colors.colorError) } }
                    )
                }
            }

            DialogSectionLabel(stringResource(R.string.download_dialog_section_file))
            SettingsSection(colors.dialogSectionBackground) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        ClintOutlinedTextField(
                            value = filename, onValueChange = { filename = it },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(R.string.download_dialog_filename_hint)) }, singleLine = true
                        )
                        ClintOutlinedTextField(
                            value = extension, onValueChange = { extension = it },
                            modifier = Modifier.width(96.dp).padding(start = 8.dp),
                            label = { Text(stringResource(R.string.download_dialog_extension_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) }, singleLine = true
                        )
                    }
                    fileSizeText?.let { Text(it, color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
                }
            }

            DialogSectionLabel(stringResource(R.string.download_dialog_section_location))
            SettingsSection(colors.dialogSectionBackground) {
                Column(Modifier.padding(16.dp)) {
                    var locationAnchorWidthPx by remember { mutableStateOf(0) }
                    val density = LocalDensity.current
                    Box(Modifier.onGloballyPositioned { coordinates -> locationAnchorWidthPx = coordinates.size.width }) {
                        Row(
                            Modifier.fillMaxWidth().clickable { locationMenuOpen = true }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.download_location_label), color = colors.secondaryText, fontSize = 11.sp)
                                Text(
                                    if (locationMode == DownloadSettingsKeys.MODE_CUSTOM) stringResource(R.string.download_location_option_custom) else stringResource(R.string.download_location_option_default),
                                    color = colors.onSurface, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Icon(androidx.compose.material.icons.Icons.Filled.ArrowDownward, contentDescription = null, tint = colors.iconTint)
                        }
                        DropdownMenu(
                            expanded = locationMenuOpen, onDismissRequest = { locationMenuOpen = false },
                            modifier = Modifier.width(with(density) { locationAnchorWidthPx.toDp() }),
                            shape = PopupShape, containerColor = colors.popupBackground, border = BorderStroke(1.dp, colors.popupStroke)
                        ) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.download_location_option_default), color = colors.onSurface) }, onClick = {
                                locationMode = DownloadSettingsKeys.MODE_DEFAULT; locationMenuOpen = false
                            })
                            DropdownMenuItem(text = { Text(stringResource(R.string.download_location_option_custom), color = colors.onSurface) }, onClick = {
                                locationMode = DownloadSettingsKeys.MODE_CUSTOM; locationMenuOpen = false
                                activity.launchManualFolderPicker { uri ->
                                    prefs.edit().putString(DownloadSettingsKeys.PREF_DOWNLOAD_CUSTOM_URI, uri.toString()).apply()
                                    customUri = uri
                                }
                            })
                        }
                    }
                    if (locationMode == DownloadSettingsKeys.MODE_CUSTOM) {
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                activity.launchManualFolderPicker { uri ->
                                    prefs.edit().putString(DownloadSettingsKeys.PREF_DOWNLOAD_CUSTOM_URI, uri.toString()).apply()
                                    customUri = uri
                                }
                            }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(androidx.compose.material.icons.Icons.Filled.Folder, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(20.dp))
                            Text(
                                customUri?.let { uriToDisplayPath(it) } ?: stringResource(R.string.download_location_tap_to_choose),
                                color = colors.onSurface, fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                    }
                }
            }

            DialogSectionLabel(stringResource(R.string.download_dialog_section_options))
            SettingsSection(colors.dialogSectionBackground) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth().clickable { retryEnabled = !retryEnabled }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.download_retry_enabled_title), color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(stringResource(R.string.download_retry_enabled_summary), color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        ClintSwitch(checked = retryEnabled)
                    }
                    Row(
                        Modifier.fillMaxWidth().clickable { unmeteredOnly = !unmeteredOnly }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.download_unmetered_only_title), color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(stringResource(R.string.download_dialog_unmetered_summary), color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        ClintSwitch(checked = unmeteredOnly)
                    }

                    Text(
                        stringResource(R.string.download_split_parts_title), color = colors.onSurface,
                        fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        pluralStringResource(R.plurals.download_split_parts_value, splitParts, splitParts),
                        color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp)
                    )
                    ClintSlider(
                        value = splitParts.toFloat(),
                        onValueChange = { splitParts = it.toInt() },
                        valueRange = 1f..32f, steps = 30
                    )

                    Text(
                        stringResource(R.string.download_multithreading_title), color = colors.onSurface,
                        fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        pluralStringResource(R.plurals.download_multithreading_value, multithreadingParts, multithreadingParts),
                        color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp)
                    )
                    ClintSlider(
                        value = multithreadingParts.toFloat(),
                        onValueChange = { multithreadingParts = it.toInt() },
                        valueRange = 1f..8f, steps = 6
                    )

                    Text(
                        stringResource(R.string.download_dialog_speed_limit_title), color = colors.onSurface,
                        fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        stringResource(R.string.download_dialog_speed_limit_desc), color = colors.secondaryText,
                        fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ClintOutlinedTextField(
                            value = speedLimitText,
                            onValueChange = { speedLimitText = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(R.string.download_dialog_speed_limit_hint)) },
                            singleLine = true
                        )
                        Box(Modifier.padding(start = 8.dp)) {
                            Row(
                                Modifier.clickable { speedUnitMenuOpen = true }.padding(horizontal = 12.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(speedUnitLabel, color = colors.onSurface, fontSize = 14.sp)
                                Icon(androidx.compose.material.icons.Icons.Filled.ArrowDownward, contentDescription = null, tint = colors.iconTint, modifier = Modifier.padding(start = 2.dp))
                            }
                            DropdownMenu(
                                expanded = speedUnitMenuOpen, onDismissRequest = { speedUnitMenuOpen = false },
                                shape = PopupShape, containerColor = colors.popupBackground, border = BorderStroke(1.dp, colors.popupStroke),
                                modifier = Modifier.width(120.dp)
                            ) {
                                DropdownMenuItem(text = { Text(kbLabel, color = colors.onSurface) }, onClick = { speedUnitLabel = kbLabel; speedUnitMenuOpen = false })
                                DropdownMenuItem(text = { Text(mbLabel, color = colors.onSurface) }, onClick = { speedUnitLabel = mbLabel; speedUnitMenuOpen = false })
                            }
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth().clickable {
                            scheduleEnabled = !scheduleEnabled
                            if (scheduleEnabled) {
                                scheduledMillis = Calendar.getInstance().apply { add(Calendar.MINUTE, 1) }.timeInMillis
                                if (needsExactAlarmPermissionRationale(context)) blockingError = exactAlarmPermissionDialogConfig(context)
                            }
                        }.padding(vertical = 10.dp).padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.download_schedule_this_title), color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(stringResource(R.string.download_schedule_this_summary), color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        ClintSwitch(checked = scheduleEnabled)
                    }
                    if (scheduleEnabled) {
                        val dateFmt = remember { android.text.format.DateFormat.getMediumDateFormat(context) }
                        val timeFmt = remember { android.text.format.DateFormat.getTimeFormat(context) }
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                showScheduleDatePicker(activity.supportFragmentManager, scheduledMillis) { year, month, day ->
                                    scheduledMillis = Calendar.getInstance().apply {
                                        timeInMillis = scheduledMillis
                                        set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, day)
                                    }.timeInMillis
                                }
                            }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.download_schedule_date_title), color = colors.onSurface, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Text(dateFmt.format(java.util.Date(scheduledMillis)), color = colors.secondaryText, fontSize = 13.sp)
                        }
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                showScheduleTimePicker(context, activity.supportFragmentManager, scheduledMillis) { hour, minute ->
                                    scheduledMillis = Calendar.getInstance().apply {
                                        timeInMillis = scheduledMillis
                                        set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0)
                                    }.timeInMillis
                                }
                            }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.download_schedule_time_title), color = colors.onSurface, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Text(timeFmt.format(java.util.Date(scheduledMillis)), color = colors.secondaryText, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    ConfirmDialogHost(blockingError, hideStatusBar, hideSystemNavigation) { blockingError = null }
}

@Composable
private fun DialogSectionLabel(text: String) {
    val colors = LocalClintColors.current
    Text(text, color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
}
