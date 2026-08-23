package com.jhaiian.clint.settings.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class BrowserSettingsUiState(
    initialSearchEngine: String,
    initialSearchSuggestionsApi: String,
    initialJavascriptEnabled: Boolean,
    initialHideStatusBar: Boolean
) {
    var searchEngine by mutableStateOf(initialSearchEngine)
    var searchSuggestionsApi by mutableStateOf(initialSearchSuggestionsApi)
    var javascriptEnabled by mutableStateOf(initialJavascriptEnabled)
    var hideStatusBar by mutableStateOf(initialHideStatusBar)
    var searchEngineDialogOpen by mutableStateOf(false)
    var searchSuggestionsApiDialogOpen by mutableStateOf(false)
}
