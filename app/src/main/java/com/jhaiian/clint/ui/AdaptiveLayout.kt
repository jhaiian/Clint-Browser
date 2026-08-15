package com.jhaiian.clint.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Walks a Context's wrapper chain to find the hosting Activity. Compose content mounted
 *  inside a Fragment's ComposeView only has a Context (not a direct Activity reference), so
 *  callers like [SettingsScreenScaffold] need this to reach [rememberMaxContentWidth]. */
fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * The max width a screen's content column should grow to once the window is wider than a
 * phone. `null` means no constraint (compact width: phone portrait, or a narrow split-screen
 * pane). Used to keep list/settings rows from stretching edge-to-edge on a tablet, unfolded
 * foldable, or desktop freeform window, matching Material's large-screen guidance for
 * single-pane content. Toolbars are expected to stay full-bleed and only the scrollable
 * content itself should be wrapped in [AdaptiveWidthContainer].
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun rememberMaxContentWidth(activity: Activity): Dp? {
    val windowSizeClass = calculateWindowSizeClass(activity)
    return when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Medium -> 700.dp
        WindowWidthSizeClass.Expanded -> 960.dp
        else -> null
    }
}

/** Convenience overload for composables that only have a [Context] (e.g. content hosted in a
 *  Fragment's ComposeView) rather than a direct Activity reference. Returns `null` if no
 *  hosting Activity can be found, same as the compact-width case. */
@Composable
fun rememberMaxContentWidth(context: Context): Dp? {
    val activity = context.findActivity() ?: return null
    return rememberMaxContentWidth(activity)
}

/**
 * Centers [content] and caps it at [maxContentWidth] (pass the result of
 * [rememberMaxContentWidth]); on a compact/phone width this is a no-op full-bleed Box.
 */
@Composable
fun AdaptiveWidthContainer(
    maxContentWidth: Dp?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.fillMaxWidth().fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .then(if (maxContentWidth != null) Modifier.widthIn(max = maxContentWidth) else Modifier),
            content = content
        )
    }
}
