package com.jhaiian.clint.settings.lookandfeel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class LookAndFeelDialog {
    THEME, ACCENT, SURFACE_INTENSITY, ADDRESS_BAR_POSITION, MENU_STYLE, TAB_MENU_STYLE, SCROLL_HIDE_MODE, EXIT_CONFIRMATION, LANGUAGE
}

class LookAndFeelUiState(
    initialTheme: String,
    initialAccent: String,
    initialIntensity: String,
    initialForceDarkWeb: Boolean,
    initialLanguage: String,
    initialScrollHideMode: String,
    initialAddressBarPosition: String,
    initialMenuStyle: String,
    initialTabMenuStyle: String,
    initialHideStatusBar: Boolean,
    initialExitConfirmation: String
) {
    var theme by mutableStateOf(initialTheme)
    var accent by mutableStateOf(initialAccent)
    var intensity by mutableStateOf(initialIntensity)
    var forceDarkWeb by mutableStateOf(initialForceDarkWeb)
    var language by mutableStateOf(initialLanguage)

    var scrollHideMode by mutableStateOf(initialScrollHideMode)
    var addressBarPosition by mutableStateOf(initialAddressBarPosition)
    var menuStyle by mutableStateOf(initialMenuStyle)
    var tabMenuStyle by mutableStateOf(initialTabMenuStyle)
    var hideStatusBar by mutableStateOf(initialHideStatusBar)

    var exitConfirmation by mutableStateOf(initialExitConfirmation)

    var openDialog by mutableStateOf<LookAndFeelDialog?>(null)
}
