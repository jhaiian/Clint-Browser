package com.jhaiian.clint.settings.datasaver

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class DataSaverUiState(
    initialEnabled: Boolean,
    initialDisableImages: Boolean,
    initialDisableAutoplay: Boolean
) {
    var enabled by mutableStateOf(initialEnabled)
    var disableImages by mutableStateOf(initialDisableImages)
    var disableAutoplay by mutableStateOf(initialDisableAutoplay)
}
