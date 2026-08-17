package com.jhaiian.clint.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.launch

/**
 * Implemented by every Activity that can show a themed Snackbar as part of its own Compose
 * composition. This is the lightweight sibling of [OverlayHostActivity]: that one hosts a single
 * full-window dialog/overlay, this one hosts a single bottom Snackbar, driven the same way (a
 * state object the Activity's own composition collects, set from anywhere via [showClintSnackbar]).
 */
interface SnackbarHostActivity {
    val snackbarHostState: SnackbarHostState
}

/**
 * Shows [message] in the Activity's Snackbar, replacing any Snackbar already showing. If
 * [actionLabel] is non-null, tapping it runs [onAction].
 */
fun <T> T.showClintSnackbar(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) where T : SnackbarHostActivity, T : LifecycleOwner {
    lifecycleScope.launch {
        snackbarHostState.currentSnackbarData?.dismiss()
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed) onAction?.invoke()
    }
}

/**
 * Renders [hostState]'s queued Snackbar with the app's own colors instead of Material's stock
 * ones, so it reads as part of Clint rather than a plain default Android bar. Pin this as a
 * top-level sibling in each Activity's `setContent`, the same way [OverlayHostActivity.overlayContent] is.
 */
@Composable
fun ClintSnackbarHost(hostState: SnackbarHostState) {
    val colors = LocalClintColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        SnackbarHost(hostState) { data ->
            Snackbar(
                snackbarData = data,
                shape = RoundedCornerShape(12.dp),
                containerColor = colors.popupBackground,
                contentColor = colors.popupText,
                actionColor = colors.primary
            )
        }
    }
}
