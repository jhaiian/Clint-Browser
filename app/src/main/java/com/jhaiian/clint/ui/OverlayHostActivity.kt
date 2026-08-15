package com.jhaiian.clint.ui

import androidx.compose.runtime.Composable

/**
 * Implemented by every Activity that can host a full-window Compose dialog/overlay as part of
 * its own [androidx.activity.compose.setContent] composition. Setting [overlayContent] renders
 * it inline wherever that Activity's root composable collects it (see each implementer's
 * `setContent` block); setting it back to null tears it down.
 *
 * This replaces the old pattern of mounting a second, standalone `ComposeView` directly onto
 * the window's `decorView` for one-off dialogs (document viewer, download request, update flow)
 * — that approach worked but lived outside the Activity's real UI tree as an invisible sibling
 * child of the DecorView. Routing it through the Activity's own composition instead keeps
 * everything in one tree.
 */
interface OverlayHostActivity {
    var overlayContent: (@Composable () -> Unit)?
}
