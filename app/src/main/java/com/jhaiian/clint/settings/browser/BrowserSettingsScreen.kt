package com.jhaiian.clint.settings.browser
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff

import androidx.compose.foundation.layout.padding
import com.jhaiian.clint.ui.ClintSwitch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.common.SettingsRow
import com.jhaiian.clint.settings.common.SettingsScreenScaffold
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.setup.SectionLabel
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun BrowserSettingsScreen(
    state: BrowserSettingsUiState,
    onSearchEngineConfirmed: (String) -> Unit,
    onSearchSuggestionsApiConfirmed: (String) -> Unit,
    onJavascriptRowClicked: () -> Unit,
    onFramelessShortcutRowClicked: () -> Unit,
    onWebsiteBlockerRowClicked: () -> Unit,
    onQuiverGuardRowClicked: () -> Unit,
    onIncognitoSearchHistoryRowClicked: () -> Unit
) {
    val colors = LocalClintColors.current

    SettingsScreenScaffold(
        overlay = {
            if (state.searchEngineDialogOpen) {
                SearchEngineDialog(
                    current = state.searchEngine,
                    hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onConfirm = onSearchEngineConfirmed,
                    onDismiss = { state.searchEngineDialogOpen = false }
                )
            }
            if (state.searchSuggestionsApiDialogOpen) {
                SearchSuggestionsApiDialog(
                    current = state.searchSuggestionsApi,
                    hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onConfirm = onSearchSuggestionsApiConfirmed,
                    onDismiss = { state.searchSuggestionsApiDialogOpen = false }
                )
            }
        }
    ) {
        SectionLabel(stringResource(R.string.pref_category_search).uppercase(), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Search,
                title = stringResource(R.string.search_engine),
                summary = stringResource(engineSummaryRes(state.searchEngine)),
                colors = colors,
                onClick = { state.searchEngineDialogOpen = true }
            )
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ManageSearch,
                title = stringResource(R.string.search_suggestions_api),
                summary = stringResource(engineSummaryRes(state.searchSuggestionsApi)),
                colors = colors,
                onClick = { state.searchSuggestionsApiDialogOpen = true }
            )
        }

        SectionLabel(stringResource(R.string.browser_settings).uppercase(), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.DesktopWindows,
                title = stringResource(R.string.javascript_enabled),
                summary = stringResource(R.string.javascript_enabled_summary),
                colors = colors,
                onClick = onJavascriptRowClicked,
                trailing = {
                    ClintSwitch(checked = state.javascriptEnabled)
                }
            )
        }

        SectionLabel(stringResource(R.string.pref_category_shortcuts).uppercase(), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.OpenInNew,
                title = stringResource(R.string.frameless_shortcut_title),
                summary = stringResource(R.string.frameless_shortcut_summary),
                colors = colors,
                onClick = onFramelessShortcutRowClicked,
                trailing = {
                    ClintSwitch(checked = state.framelessShortcut)
                }
            )
        }

        SectionLabel(stringResource(R.string.pref_category_protection).uppercase(), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Shield,
                title = stringResource(R.string.website_blocker_title),
                summary = stringResource(R.string.website_blocker_settings_summary),
                colors = colors,
                onClick = onWebsiteBlockerRowClicked
            )
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Security,
                title = stringResource(R.string.quiver_guard),
                summary = stringResource(R.string.quiver_guard_description),
                colors = colors,
                onClick = onQuiverGuardRowClicked
            )
        }

        SectionLabel(stringResource(R.string.pref_category_incognito).uppercase(), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.VisibilityOff,
                title = stringResource(R.string.incognito_search_history_title),
                summary = stringResource(R.string.incognito_search_history_summary),
                colors = colors,
                onClick = onIncognitoSearchHistoryRowClicked,
                trailing = {
                    ClintSwitch(checked = state.incognitoSearchHistory)
                }
            )
        }
    }
}

private fun engineSummaryRes(engine: String): Int = when (engine) {
    "brave" -> R.string.engine_brave
    "google" -> R.string.engine_google
    else -> R.string.engine_duckduckgo
}
