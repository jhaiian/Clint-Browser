package com.jhaiian.clint.settings.datasaver
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.PausePresentation

import androidx.compose.foundation.layout.padding
import com.jhaiian.clint.ui.ClintSwitch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.common.RowDivider
import com.jhaiian.clint.settings.common.SettingsRow
import com.jhaiian.clint.settings.common.SettingsScreenScaffold
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.setup.SectionLabel
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun DataSaverScreen(
    state: DataSaverUiState,
    onEnabledClick: () -> Unit,
    onDisableImagesClick: () -> Unit,
    onDisableAutoplayClick: () -> Unit
) {
    val colors = LocalClintColors.current

    SettingsScreenScaffold {
        SectionLabel(stringResource(R.string.data_saver_section), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.DataSaverOn,
                title = stringResource(R.string.data_saver_title),
                summary = stringResource(R.string.data_saver_summary),
                colors = colors,
                onClick = onEnabledClick,
                trailing = {
                    ClintSwitch(checked = state.enabled)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.HideImage,
                title = stringResource(R.string.data_saver_disable_images_title),
                summary = stringResource(R.string.data_saver_disable_images_summary),
                colors = colors,
                onClick = onDisableImagesClick,
                enabled = state.enabled,
                trailing = {
                    ClintSwitch(checked = state.disableImages)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.PausePresentation,
                title = stringResource(R.string.data_saver_disable_autoplay_title),
                summary = stringResource(R.string.data_saver_disable_autoplay_summary),
                colors = colors,
                onClick = onDisableAutoplayClick,
                enabled = state.enabled,
                trailing = {
                    ClintSwitch(checked = state.disableAutoplay)
                }
            )
        }
    }
}
