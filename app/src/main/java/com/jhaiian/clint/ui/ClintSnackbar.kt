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

interface SnackbarHostActivity {
    val snackbarHostState: SnackbarHostState
}

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
