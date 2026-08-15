package com.jhaiian.clint.update

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job

sealed interface UpdateFlowStep {
    data object None : UpdateFlowStep
    data object NoUpdate : UpdateFlowStep
    data object CheckFailed : UpdateFlowStep
    data class Available(
        val version: String,
        val versionCode: Long,
        val changelog: String,
        val downloadUrl: String?,
        val isBeta: Boolean
    ) : UpdateFlowStep
    data object Downloading : UpdateFlowStep
}

class DownloadProgressState {
    var statusText by mutableStateOf("")
    var isIndeterminate by mutableStateOf(true)
    var progressFraction by mutableStateOf(0f)
    var sizeText by mutableStateOf("")
    var speedText by mutableStateOf("")
}

/** Backs one "showing" of the update flow: a single overlay can move between NoUpdate,
 *  CheckFailed, Available, and Downloading as the user interacts with it. */
class UpdateFlowState(val hideStatusBar: Boolean) {
    var step by mutableStateOf<UpdateFlowStep>(UpdateFlowStep.None)
    val download = DownloadProgressState()
    var downloadJob: Job? = null
}
