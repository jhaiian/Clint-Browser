package com.jhaiian.clint.settings.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class BrowserSettingsUiState(
    initialSearchEngine: String,
    initialCustomSearchEngineName: String,
    initialCustomSearchEngineUrl: String,
    initialSearchSuggestionsApi: String,
    initialCustomSearchSuggestionsApiName: String,
    initialCustomSearchSuggestionsApiUrl: String,
    initialJavascriptEnabled: Boolean,
    initialFramelessShortcut: Boolean,
    initialHideStatusBar: Boolean,
    initialHideSystemNavigation: Boolean,
    initialIncognitoSearchHistory: Boolean
) {
    var searchEngine by mutableStateOf(initialSearchEngine)
    var customSearchEngineName by mutableStateOf(initialCustomSearchEngineName)
    var customSearchEngineUrl by mutableStateOf(initialCustomSearchEngineUrl)
    var searchSuggestionsApi by mutableStateOf(initialSearchSuggestionsApi)
    var customSearchSuggestionsApiName by mutableStateOf(initialCustomSearchSuggestionsApiName)
    var customSearchSuggestionsApiUrl by mutableStateOf(initialCustomSearchSuggestionsApiUrl)
    var javascriptEnabled by mutableStateOf(initialJavascriptEnabled)
    var framelessShortcut by mutableStateOf(initialFramelessShortcut)
    var hideStatusBar by mutableStateOf(initialHideStatusBar)
    var hideSystemNavigation by mutableStateOf(initialHideSystemNavigation)
    var incognitoSearchHistory by mutableStateOf(initialIncognitoSearchHistory)
    var searchEngineDialogOpen by mutableStateOf(false)
    var searchSuggestionsApiDialogOpen by mutableStateOf(false)
}
