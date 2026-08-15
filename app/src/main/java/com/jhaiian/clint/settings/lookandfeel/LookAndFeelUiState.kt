package com.jhaiian.clint.settings.lookandfeel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Identifies which picker dialog, if any, is currently shown over the screen. */
enum class LookAndFeelDialog {
    THEME, ACCENT, SURFACE_INTENSITY, ADDRESS_BAR_POSITION, MENU_STYLE, SCROLL_HIDE_MODE, EXIT_CONFIRMATION
}

/**
 * Hoisted state for the Look & Feel screen. Owned by the fragment so it survives
 * recomposition but not process death; the fragment re-reads SharedPreferences into
 * a fresh instance whenever its view is recreated.
 */
class LookAndFeelUiState(
    initialTheme: String,
    initialAccent: String,
    initialIntensity: String,
    initialForceDarkWeb: Boolean,
    initialScrollHideMode: String,
    initialAddressBarPosition: String,
    initialMenuStyle: String,
    initialHideStatusBar: Boolean,
    initialExitConfirmation: String
) {
    var theme by mutableStateOf(initialTheme)
    var accent by mutableStateOf(initialAccent)
    var intensity by mutableStateOf(initialIntensity)
    var forceDarkWeb by mutableStateOf(initialForceDarkWeb)

    var scrollHideMode by mutableStateOf(initialScrollHideMode)
    var addressBarPosition by mutableStateOf(initialAddressBarPosition)
    var menuStyle by mutableStateOf(initialMenuStyle)
    var hideStatusBar by mutableStateOf(initialHideStatusBar)

    var exitConfirmation by mutableStateOf(initialExitConfirmation)

    var openDialog by mutableStateOf<LookAndFeelDialog?>(null)
}
