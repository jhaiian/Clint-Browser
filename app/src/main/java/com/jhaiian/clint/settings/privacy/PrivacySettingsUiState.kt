package com.jhaiian.clint.settings.privacy

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class PrivacySettingsUiState(
    initialBlockThirdPartyCookies: Boolean,
    initialCustomUserAgent: Boolean,
    initialHttpsOnly: Boolean
) {
    var blockThirdPartyCookies by mutableStateOf(initialBlockThirdPartyCookies)
    var customUserAgent by mutableStateOf(initialCustomUserAgent)
    var httpsOnly by mutableStateOf(initialHttpsOnly)
}
