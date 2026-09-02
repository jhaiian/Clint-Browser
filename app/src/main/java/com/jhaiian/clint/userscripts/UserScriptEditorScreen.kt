package com.jhaiian.clint.userscripts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.ClintDialogStatusBarEffect
import com.jhaiian.clint.ui.listscreen.ListMenuItem
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.theme.ClintColors
import com.jhaiian.clint.ui.theme.LocalClintColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScriptEditorScreen(
    state: UserScriptEditorUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onDeleteClick: () -> Unit,
    hideStatusBar: Boolean = false,
    hideSystemNavigation: Boolean = false
) {
    val colors = LocalClintColors.current
    val fallbackName = stringResource(
        if (state.isNew) R.string.user_scripts_editor_new_title else R.string.user_scripts_editor_edit_title
    )
    val metadata = remember(state.code, fallbackName) { UserScriptMetadataParser.parse(state.code, fallbackName) }
    val title = metadata.name.ifBlank { fallbackName }

    var findBarVisible by remember { mutableStateOf(false) }
    var infoSheetVisible by remember { mutableStateOf(false) }
    var overflowExpanded by remember { mutableStateOf(false) }
    val unsavedDesc = stringResource(R.string.user_scripts_editor_unsaved_desc)

    Column(Modifier.fillMaxSize().background(colors.background)) {
        Surface(color = colors.surface, shadowElevation = 3.dp, modifier = Modifier.statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = colors.iconTint)
                }
                Row(Modifier.weight(1f).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        color = colors.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (state.isDirty) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier
                                .size(6.dp)
                                .background(colors.primary, CircleShape)
                                .semantics { contentDescription = unsavedDesc }
                        )
                    }
                }
                IconButton(onClick = { findBarVisible = !findBarVisible }) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = stringResource(R.string.user_scripts_editor_search_desc),
                        tint = if (findBarVisible) colors.primary else colors.iconTint
                    )
                }
                IconButton(onClick = { infoSheetVisible = true }) {
                    Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.user_scripts_editor_info_desc), tint = colors.iconTint)
                }
                if (state.isSaving) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = colors.primary, strokeWidth = 2.dp)
                    }
                } else {
                    IconButton(onClick = onSave) {
                        Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.user_scripts_save_desc), tint = colors.primary)
                    }
                }
                Box {
                    IconButton(onClick = { overflowExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.user_scripts_editor_more_desc), tint = colors.iconTint)
                    }
                    DropdownMenu(
                        expanded = overflowExpanded,
                        onDismissRequest = { overflowExpanded = false },
                        shape = PopupShape,
                        containerColor = colors.popupBackground,
                        border = BorderStroke(1.dp, colors.popupStroke)
                    ) {
                        if (!state.isNew) {
                            ListMenuItem(Icons.Filled.Delete, stringResource(R.string.action_delete), checked = false) {
                                overflowExpanded = false
                                onDeleteClick()
                            }
                        }
                    }
                }
            }
        }

        JsCodeEditor(
            code = state.code,
            onCodeChange = { state.code = it },
            colors = colors,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            findBarVisible = findBarVisible,
            onCloseFindBar = { findBarVisible = false }
        )
    }

    if (infoSheetVisible) {
        ScriptInfoSheet(
            metadata = metadata,
            colors = colors,
            hideStatusBar = hideStatusBar,
            hideSystemNavigation = hideSystemNavigation,
            onDismiss = { infoSheetVisible = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScriptInfoSheet(
    metadata: UserScriptMetadata,
    colors: ClintColors,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val runAtLabel = when (metadata.runAt) {
        "document-start" -> stringResource(R.string.user_scripts_run_at_start)
        "document-end" -> stringResource(R.string.user_scripts_run_at_end)
        else -> stringResource(R.string.user_scripts_run_at_idle)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.popupBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.divider) }
    ) {
        ClintDialogStatusBarEffect(hideStatusBar, hideSystemNavigation)
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                stringResource(R.string.user_scripts_editor_info_title),
                color = colors.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                metadata.description.ifBlank { stringResource(R.string.user_scripts_editor_info_no_description) },
                color = colors.secondaryText,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 14.dp)
            )
            HorizontalDivider(color = colors.divider)
            Column(Modifier.padding(vertical = 10.dp)) {
                if (metadata.version.isNotBlank()) {
                    InfoRow(stringResource(R.string.user_scripts_editor_info_version_label), metadata.version, colors)
                }
                if (metadata.author.isNotBlank()) {
                    InfoRow(stringResource(R.string.user_scripts_editor_info_author_label), metadata.author, colors)
                }
                InfoRow(stringResource(R.string.user_scripts_editor_info_runat_label), runAtLabel, colors)
            }
            InfoListSection(stringResource(R.string.user_scripts_editor_info_matches), metadata.matches, colors)
            InfoListSection(stringResource(R.string.user_scripts_editor_info_includes), metadata.includes, colors)
            InfoListSection(stringResource(R.string.user_scripts_editor_info_excludes), metadata.excludes, colors)
            InfoListSection(stringResource(R.string.user_scripts_editor_info_grants), metadata.grants, colors)
            InfoListSection(stringResource(R.string.user_scripts_editor_info_requires), metadata.requires, colors)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, colors: ClintColors) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.width(96.dp))
        Text(value, color = colors.onSurface, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun InfoListSection(label: String, items: List<String>, colors: ClintColors) {
    if (items.isEmpty()) return
    Text(
        "$label (${items.size})",
        color = colors.secondaryText,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
    )
    Column {
        items.forEach { item ->
            Text(
                item,
                color = colors.onSurface,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 3.dp)
            )
        }
    }
}
