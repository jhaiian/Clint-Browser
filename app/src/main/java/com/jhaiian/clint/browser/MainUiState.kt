package com.jhaiian.clint.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Mirrors the "address_bar_position" preference's three layouts. */
internal enum class AddressBarPosition { TOP, BOTTOM, SPLIT }

internal enum class SuggestionType { BOOKMARK, HISTORY, SUGGESTION }

internal data class SuggestionItem(
    val query: String,
    val displayText: String,
    val type: SuggestionType
)

/**
 * Observable Compose state for MainActivity's browser chrome (toolbars, address bar, search
 * overlay, progress, fullscreen). This is what `ActivityMainBinding` plus the Main*Delegate
 * files used to push straight into Views; the delegates now write into this instead of
 * `binding.*`, and [MainScreen] just renders whatever it currently holds. The WebView itself
 * and its SwipeRefreshLayout host stay real Android Views (there's no meaningful Compose gain
 * there) — [MainActivity] holds those directly as properties and hosts them via a single
 * `AndroidView`; everything else here is genuine Compose.
 */
internal class MainUiState {
    // Which toolbars are visible, per the address_bar_position preference.
    var addressBarPosition by mutableStateOf(AddressBarPosition.TOP)

    // Address bar text + lock icon. Kept separate per top/bottom instance so switching
    // address_bar_position mid-session never shows stale text in the newly-shown bar.
    var addressBarTextTop by mutableStateOf("")
    var addressBarTextBottom by mutableStateOf("")
    var addressBarSecureTop by mutableStateOf(true)
    var addressBarSecureBottom by mutableStateOf(true)

    // Full-screen search overlay (replaces the old Material SearchView).
    var searchOverlayOpen by mutableStateOf(false)
    var searchOverlayIsBottom by mutableStateOf(false)
    var searchQuery by mutableStateOf("")
    var suggestions by mutableStateOf<List<SuggestionItem>>(emptyList())
    /** Set by the voice-search activity-result callback; [SearchOverlay] consumes and clears it. */
    var voiceResult by mutableStateOf<String?>(null)

    // Page load progress, shared by both toolbars' progress indicators.
    var pageLoadProgress by mutableIntStateOf(0)
    var isPageLoading by mutableStateOf(false)

    // Bottom-nav-bar button state.
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)
    var isBookmarked by mutableStateOf(false)
    var hasActiveUrl by mutableStateOf(false)

    // Tabs / incognito.
    var tabCountText by mutableStateOf("1")
    var isIncognito by mutableStateOf(false)

    // Scroll-hide animation fractions: 0f fully shown, 1f fully hidden.
    var topBarFraction by mutableFloatStateOf(0f)
    var bottomBarFraction by mutableFloatStateOf(0f)

    // Measured bar heights and window insets, all in raw pixels (matches the pre-Compose math
    // in MainScrollDelegate, which this state directly feeds).
    var topBarFullHeightPx by mutableIntStateOf(0)
    var bottomBarFullHeightPx by mutableIntStateOf(0)
    var statusBarInsetPx by mutableIntStateOf(0)
    var cachedStatusBarInsetPx by mutableIntStateOf(0)
    var navBarInsetPx by mutableIntStateOf(0)
    var hideStatusBar by mutableStateOf(false)

    // Web content padding, derived from the fractions/heights above; consumed by the
    // AndroidView hosting the WebView/SwipeRefreshLayout.
    var contentPaddingTopPx by mutableIntStateOf(0)
    var contentPaddingBottomPx by mutableIntStateOf(0)

    // Full-screen (e.g. video) mode.
    var isFullscreen by mutableStateOf(false)

    // Image/link long-press action sheets and the content-preview sheet (page/image/reader-mode
    // preview). Non-null while the corresponding ModalBottomSheet should be shown; MainScreen
    // renders each conditionally and the sheet's own onDismiss sets it back to null.
    var imageLongPressRequest by mutableStateOf<com.jhaiian.clint.browser.sheets.ImageLongPressRequest?>(null)
    var linkLongPressRequest by mutableStateOf<com.jhaiian.clint.browser.sheets.LinkLongPressRequest?>(null)
    var contentPreviewRequest by mutableStateOf<com.jhaiian.clint.browser.sheets.ContentPreviewRequest?>(null)

    // Generic 2-3 button confirm dialog, shared by every simple system-permission-rationale
    // prompt (camera/mic/location) across the delegates — see ui/listscreen/ConfirmDialog.kt.
    var confirmDialogConfig by mutableStateOf<com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig?>(null)
    var conflictDialogRequest by mutableStateOf<com.jhaiian.clint.downloads.DownloadConflictDialogRequest?>(null)
    var webPermissionDialogRequest by mutableStateOf<com.jhaiian.clint.ui.WebPermissionDialogRequest?>(null)
    var popupAlertRequest by mutableStateOf<com.jhaiian.clint.browser.dialogs.PopupAlertRequest?>(null)
    var refreshLinkDialogRequest by mutableStateOf<com.jhaiian.clint.browser.dialogs.RefreshLinkDialogRequest?>(null)
    var openInAppRequest by mutableStateOf<com.jhaiian.clint.browser.webview.OpenInAppRequest?>(null)
}
