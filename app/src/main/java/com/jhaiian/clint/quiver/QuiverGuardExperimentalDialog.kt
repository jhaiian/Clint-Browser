package com.jhaiian.clint.quiver

import com.jhaiian.clint.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.delay

private const val EXPERIMENTAL_COUNTDOWN_SECONDS = 3

@Composable
fun ExperimentalDialog(open: Boolean, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onDismiss: () -> Unit) {
    if (!open) return
    val colors = LocalClintColors.current
    var secondsRemaining by remember { mutableStateOf(EXPERIMENTAL_COUNTDOWN_SECONDS) }

    LaunchedEffect(Unit) {
        while (secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining--
        }
    }

    ClintDialog(
        title = stringResource(R.string.quiver_guard_experimental_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = {},
        cancelable = false,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss, enabled = secondsRemaining <= 0) {
                    Text(
                        if (secondsRemaining > 0) "${stringResource(R.string.action_ok)} ($secondsRemaining)" else stringResource(R.string.action_ok),
                        color = if (secondsRemaining <= 0) colors.primary else colors.secondaryText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    ) {
        Text(
            stringResource(R.string.quiver_guard_experimental_message),
            color = colors.onSurface, fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
