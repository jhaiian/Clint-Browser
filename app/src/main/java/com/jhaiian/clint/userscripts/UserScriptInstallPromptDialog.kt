package com.jhaiian.clint.userscripts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.theme.LocalClintColors

data class UserScriptInstallPromptRequest(
    val url: String,
    val scriptName: String? = null,
    val isInstalling: Boolean = false,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit
)

@Composable
internal fun UserScriptInstallPromptDialog(
    request: UserScriptInstallPromptRequest,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current

    ClintDialog(
        title = stringResource(R.string.user_script_install_prompt_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        cancelable = !request.isInstalling,
        onDismiss = { if (!request.isInstalling) { onDismiss(); request.onCancel() } },
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onDismiss(); request.onCancel() }, enabled = !request.isInstalling) {
                    Text(stringResource(R.string.user_script_install_prompt_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                if (request.isInstalling) {
                    CircularProgressIndicator(modifier = Modifier.padding(horizontal = 16.dp).size(18.dp), color = colors.primary, strokeWidth = 2.dp)
                } else {
                    TextButton(onClick = { request.onConfirm() }) {
                        Text(stringResource(R.string.user_script_install_prompt_install), color = colors.primary, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    ) {
        Text(
            stringResource(R.string.user_script_install_prompt_message, request.scriptName ?: request.url),
            color = colors.onSurface,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
