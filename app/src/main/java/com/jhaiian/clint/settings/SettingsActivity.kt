package com.jhaiian.clint.settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.automirrored.filled.ArrowBack

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceManager
import com.jhaiian.clint.BuildConfig
import com.jhaiian.clint.R
import com.jhaiian.clint.base.ClintActivity
import com.jhaiian.clint.quiver.QuiverGuardActivity
import com.jhaiian.clint.settings.main.MainSettingsScreen
import com.jhaiian.clint.ui.DocumentViewer
import com.jhaiian.clint.ui.OverlayHostActivity
import com.jhaiian.clint.ui.theme.ClintComposeTheme
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.launch

private const val DEST_LOOK_AND_FEEL = "look_and_feel"
private const val DEST_BROWSER = "browser"
private const val DEST_PRIVACY = "privacy"
private const val DEST_SITE_SETTINGS = "site_settings"
private const val DEST_DATA_SAVER = "data_saver"
private const val DEST_DOWNLOADS = "downloads"
private const val DEST_UPDATES = "updates"
private const val DEST_MISC = "misc"
private const val DEST_DEBUG = "debug"
private const val DEST_ABOUT = "about"

/**
 * Hosts every settings screen as Compose content behind a single adaptive list-detail scaffold:
 * on a phone-width window only one pane shows at a time (list, then the chosen detail screen,
 * matching the old Fragment back-stack UX); once the window is Medium/Expanded (tablet, unfolded
 * foldable, split-screen, desktop freeform window) both panes show side by side, per Android 17's
 * adaptive-app guidance. [NavigableListDetailPaneScaffold] owns the pane transitions and back
 * navigation; each individual settings screen's logic lives in the `*Pane` composables in
 * SettingsPanes.kt, ported 1:1 from the old fragments (same SharedPreferences keys/behavior).
 */
class SettingsActivity : ClintActivity(), OverlayHostActivity {

    /** Full-window Compose overlay (document viewer, update flow) rendered inline in this
     *  activity's own composition; see [OverlayHostActivity]. */
    override var overlayContent by mutableStateOf<(@Composable () -> Unit)?>(null)

    /** Set true by a Look & Feel change that needs a restart to fully apply; if still true when
     *  this Activity stops (i.e. the user backed out instead of tapping "Restart Now"), the
     *  restart is applied silently on the way out rather than lost. */
    var pendingRestart = false

    /** hide_status_bar is applied to the UI state immediately for a responsive toggle, but the
     *  actual SharedPreferences write is deferred until the user commits to a restart (now or
     *  on exit), matching the address-bar-position flow. Null once written/discarded. */
    var pendingHideStatusBar: Boolean? = null

    private var hideStatusBarAtLaunch = false
    private var addressBarPositionAtLaunch = "top"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        hideStatusBarAtLaunch = prefs.getBoolean("hide_status_bar", false)
        addressBarPositionAtLaunch = prefs.getString("address_bar_position", "top") ?: "top"
        val theme = prefs.getString("app_theme", "dark") ?: "dark"

        val initialDestination = when (intent.getStringExtra(EXTRA_OPEN_FRAGMENT)) {
            "data_saver" -> DEST_DATA_SAVER
            "download_settings" -> DEST_DOWNLOADS
            else -> null
        }

        setContent {
            ClintComposeTheme(theme = theme) {
                SettingsNavHost(activity = this, initialDestination = initialDestination)
                overlayContent?.invoke()
            }
        }
    }

    /** Called after a Look & Feel change that was deferred via "Later" so the pending restart
     *  is applied automatically once the user leaves Settings, instead of only on explicit
     *  confirmation. */
    fun scheduleRestartIfChanged() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val effectiveHideStatusBar = pendingHideStatusBar ?: prefs.getBoolean("hide_status_bar", false)
        val statusBarChanged = effectiveHideStatusBar != hideStatusBarAtLaunch
        val positionChanged = (prefs.getString("address_bar_position", "top") ?: "top") != addressBarPositionAtLaunch
        pendingRestart = statusBarChanged || positionChanged
    }

    fun restartApp() {
        pendingRestart = false
        pendingHideStatusBar?.let { pending ->
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("hide_status_bar", pending).apply()
            pendingHideStatusBar = null
        }
        val restartIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        restartIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(restartIntent)
    }

    override fun onStop() {
        super.onStop()
        if (pendingRestart) restartApp()
    }

    companion object {
        const val EXTRA_OPEN_FRAGMENT = "extra_open_fragment"
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun SettingsNavHost(activity: SettingsActivity, initialDestination: String?) {
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (initialDestination != null) {
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, initialDestination)
        }
    }

    NavigableListDetailPaneScaffold(
        navigator = navigator,
        listPane = {
            AnimatedPane {
                SettingsListPane(activity = activity) { destination ->
                    scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, destination) }
                }
            }
        },
        detailPane = {
            AnimatedPane {
                SettingsDetailPane(
                    activity = activity,
                    destination = navigator.currentDestination?.contentKey,
                    onBack = {
                        if (navigator.canNavigateBack(BackNavigationBehavior.PopUntilContentChange)) {
                            scope.launch { navigator.navigateBack(BackNavigationBehavior.PopUntilContentChange) }
                        }
                    }
                )
            }
        }
    )
}

@Composable
private fun SettingsListPane(activity: SettingsActivity, onNavigate: (String) -> Unit) {
    val versionName = remember {
        activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: ""
    }
    Column(Modifier.fillMaxSize()) {
        SettingsToolbar(title = stringResource(R.string.settings), onBack = { activity.finish() })
        Box(Modifier.weight(1f)) {
            MainSettingsScreen(
                versionName = versionName,
                onLookAndFeelClick = { onNavigate(DEST_LOOK_AND_FEEL) },
                onBrowserClick = { onNavigate(DEST_BROWSER) },
                onPrivacyClick = { onNavigate(DEST_PRIVACY) },
                onQuiverGuardClick = { activity.startActivity(Intent(activity, QuiverGuardActivity::class.java)) },
                onSiteSettingsClick = { onNavigate(DEST_SITE_SETTINGS) },
                onDataSaverClick = { onNavigate(DEST_DATA_SAVER) },
                onDownloadsClick = { onNavigate(DEST_DOWNLOADS) },
                onUpdatesClick = {
                    if (BuildConfig.IS_FDROID) {
                        DocumentViewer.show(
                            activity,
                            activity.getString(R.string.document_viewer_changelog_title),
                            DocumentViewer.CHANGELOG_URL
                        )
                    } else {
                        onNavigate(DEST_UPDATES)
                    }
                },
                onMiscClick = { onNavigate(DEST_MISC) },
                onDebugClick = { onNavigate(DEST_DEBUG) },
                onAboutClick = { onNavigate(DEST_ABOUT) }
            )
        }
    }
}

@Composable
private fun SettingsDetailPane(activity: SettingsActivity, destination: String?, onBack: () -> Unit) {
    if (destination == null) {
        SettingsEmptyDetailPane()
        return
    }
    Column(Modifier.fillMaxSize()) {
        SettingsToolbar(title = stringResource(destinationTitleRes(destination)), onBack = onBack)
        Box(Modifier.weight(1f)) {
            when (destination) {
                DEST_LOOK_AND_FEEL -> LookAndFeelPane(activity)
                DEST_BROWSER -> BrowserSettingsPane(activity)
                DEST_PRIVACY -> PrivacySettingsPane(activity)
                DEST_SITE_SETTINGS -> SiteSettingsPane(activity)
                DEST_DATA_SAVER -> DataSaverPane(activity)
                DEST_DOWNLOADS -> DownloadSettingsPane(activity)
                DEST_UPDATES -> UpdateSettingsPane(activity)
                DEST_MISC -> MiscPane(activity)
                DEST_DEBUG -> DebugPane(activity)
                DEST_ABOUT -> AboutPane(activity)
            }
        }
    }
}

private fun destinationTitleRes(destination: String): Int = when (destination) {
    DEST_LOOK_AND_FEEL -> R.string.look_and_feel
    DEST_BROWSER -> R.string.browser_settings
    DEST_PRIVACY -> R.string.privacy_settings
    DEST_SITE_SETTINGS -> R.string.site_settings
    DEST_DATA_SAVER -> R.string.data_saver_title
    DEST_DOWNLOADS -> R.string.download_settings_title
    DEST_UPDATES -> R.string.pref_updates_title
    DEST_MISC -> R.string.pref_misc_title
    DEST_DEBUG -> R.string.debug_title
    DEST_ABOUT -> R.string.about
    else -> R.string.settings
}

/** The same toolbar shape every other Compose screen in the app uses (a status-bar-padded,
 *  elevated Surface with a 56dp Row), rather than Material3's TopAppBar, to stay visually
 *  consistent with HistoryScreen/DownloadsScreen/BookmarksScreen/etc. */
@Composable
private fun SettingsToolbar(title: String, onBack: () -> Unit) {
    val colors = LocalClintColors.current
    Surface(color = colors.surface, shadowElevation = 4.dp, modifier = Modifier.statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = colors.onSurface)
            }
            Text(
                title,
                color = colors.onSurface,
                fontSize = 19.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

/** Shown in the detail pane on a wide/dual-pane window before any category has been picked. */
@Composable
private fun SettingsEmptyDetailPane() {
    val colors = LocalClintColors.current
    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    androidx.compose.material.icons.Icons.Filled.Tune,
                    contentDescription = null,
                    tint = colors.secondaryText,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    stringResource(R.string.settings_select_category),
                    color = colors.secondaryText,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}
