package com.jhaiian.clint.quiver

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceManager
import com.jhaiian.clint.base.ClintActivity
import com.jhaiian.clint.ui.rememberMaxContentWidth
import com.jhaiian.clint.ui.theme.ClintComposeTheme

class ManualFilterActivity : ClintActivity() {

    private lateinit var db: ManualFilterDatabase
    private lateinit var uiState: ManualFilterUiState

    private fun reload() {
        uiState.rules = db.getAllRules()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        db = ManualFilterDatabase(this)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = prefs.getString("app_theme", "dark") ?: "dark"
        val hideStatusBar = prefs.getBoolean("hide_status_bar", false)

        uiState = ManualFilterUiState()
        uiState.isEnabled = ManualFilterState.isEnabled(this)
        reload()

        setContent {
            ClintComposeTheme(theme = theme) {
                val maxContentWidth = rememberMaxContentWidth(this)
                Box {
                    ManualFilterScreen(
                        state = uiState,
                        maxContentWidth = maxContentWidth,
                        onExit = { finish() },
                        onToggleEnabled = { enabled ->
                            uiState.isEnabled = enabled
                            ManualFilterState.setEnabled(this@ManualFilterActivity, enabled)
                        },
                        onAddClick = { uiState.ruleDialogMode = ManualFilterRuleDialogMode.Add },
                        onEditClick = { rule -> uiState.ruleDialogMode = ManualFilterRuleDialogMode.Edit(rule) },
                        onDeleteClick = { rule -> uiState.deleteTarget = rule }
                    )

                    uiState.ruleDialogMode?.let { mode ->
                        ManualFilterRuleDialog(
                            mode = mode,
                            hideStatusBar = hideStatusBar,
                            onConfirm = { text ->
                                when (mode) {
                                    is ManualFilterRuleDialogMode.Add -> {
                                        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                                        if (lines.isNotEmpty()) db.addRules(lines)
                                    }
                                    is ManualFilterRuleDialogMode.Edit -> db.updateRuleText(mode.rule.id, text.trim())
                                }
                                reload()
                                uiState.ruleDialogMode = null
                            },
                            onDismiss = { uiState.ruleDialogMode = null }
                        )
                    }

                    ManualFilterDeleteConfirmDialog(
                        rule = uiState.deleteTarget,
                        hideStatusBar = hideStatusBar,
                        onConfirm = {
                            uiState.deleteTarget?.let { db.deleteRule(it.id) }
                            reload()
                            uiState.deleteTarget = null
                        },
                        onDismiss = { uiState.deleteTarget = null }
                    )
                }
            }
        }
    }
}
