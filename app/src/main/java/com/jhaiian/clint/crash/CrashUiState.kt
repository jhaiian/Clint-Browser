package com.jhaiian.clint.crash

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

/** On-screen and clipboard content is truncated past this length — very large logs are
 *  rare but shouldn't be able to freeze the UI or blow past clipboard limits. */
const val MAX_CRASH_CLIP_CHARS = 450_000

class CrashReportItem(val file: File, val title: String, val content: String)

class CrashUiState(val hideStatusBar: Boolean) {
    var isLoading by mutableStateOf(true)
    val reports = mutableStateListOf<CrashReportItem>()
    var clearAllConfirmOpen by mutableStateOf(false)
    var detailReport by mutableStateOf<CrashReportItem?>(null)
    var reportTemplate by mutableStateOf("")
}
