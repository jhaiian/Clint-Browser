package com.jhaiian.clint.userscripts

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.amrdeveloper.codeview.Code
import com.amrdeveloper.codeview.CodeView
import com.amrdeveloper.codeview.CodeViewAdapter
import com.amrdeveloper.codeview.Keyword
import com.amrdeveloper.codeview.Snippet
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.theme.ClintColors
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.abs

private val PATTERN_KEYWORDS = Pattern.compile(
    "\\b(var|let|const|function|return|if|else|for|while|do|break|continue|switch|case|default|new|delete|typeof|instanceof|in|of|try|catch|finally|throw|class|extends|super|this|null|undefined|true|false|void|yield|async|await|static|get|set|import|export|from|as)\\b"
)
private val PATTERN_BUILTINS = Pattern.compile(
    "\\b(console|window|document|Math|JSON|Array|Object|String|Number|Boolean|Promise|Map|Set|RegExp|Date|Error|fetch|localStorage|sessionStorage|unsafeWindow|GM|GM_setValue|GM_getValue|GM_deleteValue|GM_listValues|GM_addValueChangeListener|GM_removeValueChangeListener|GM_addStyle|GM_addElement|GM_xmlhttpRequest|GM_openInTab|GM_setClipboard|GM_notification|GM_download|GM_getResourceText|GM_getResourceURL|GM_log|GM_info|GM_registerMenuCommand|GM_unregisterMenuCommand)\\b"
)
private val PATTERN_METADATA = Pattern.compile("^//\\s*@\\S+.*$|^//\\s*==/?UserScript==\\s*$", Pattern.MULTILINE)
private val PATTERN_SINGLE_LINE_COMMENT = Pattern.compile("//[^\\n]*")
private val PATTERN_MULTI_LINE_COMMENT = Pattern.compile("/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/")
private val PATTERN_STRING = Pattern.compile("\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'|`(?:[^`\\\\]|\\\\.)*`")
private val PATTERN_NUMBERS = Pattern.compile("\\b(0x[0-9a-fA-F]+|\\d*\\.?\\d+)\\b")

private fun buildEditorSuggestions(): List<Code> {
    val keywordNames = listOf(
        "var", "let", "const", "function", "return", "if", "else", "for", "while", "do",
        "break", "continue", "switch", "case", "default", "new", "delete", "typeof",
        "instanceof", "try", "catch", "finally", "throw", "class", "extends", "super",
        "this", "null", "undefined", "true", "false", "async", "await"
    )
    val builtinNames = listOf(
        "console.log", "document.querySelector", "document.querySelectorAll",
        "document.createElement", "addEventListener", "removeEventListener",
        "JSON.stringify", "JSON.parse", "setTimeout", "setInterval", "fetch",
        "localStorage.getItem", "localStorage.setItem", "unsafeWindow",
        "GM", "GM_setValue", "GM_getValue", "GM_deleteValue", "GM_listValues",
        "GM_addValueChangeListener", "GM_removeValueChangeListener",
        "GM_addStyle", "GM_addElement", "GM_xmlhttpRequest", "GM_openInTab", "GM_setClipboard",
        "GM_notification", "GM_download", "GM_getResourceText", "GM_getResourceURL",
        "GM_log", "GM_info", "GM_registerMenuCommand", "GM_unregisterMenuCommand"
    )
    val snippets = listOf(
        Snippet("fn", "function () {\n  \n}"),
        Snippet("afn", "() => {\n  \n}"),
        Snippet("iife", "(function() {\n  'use strict';\n  \n})();"),
        Snippet("log", "console.log();"),
        Snippet("qs", "document.querySelector('');"),
        Snippet("qsa", "document.querySelectorAll('');"),
        Snippet("try", "try {\n  \n} catch (e) {\n  console.error(e);\n}"),
        Snippet("timeout", "setTimeout(() => {\n  \n}, 1000);"),
        Snippet("interval", "setInterval(() => {\n  \n}, 1000);"),
        Snippet("gmxhr", "GM_xmlhttpRequest({\n  method: 'GET',\n  url: '',\n  onload: function(response) {\n    \n  }\n});"),
        Snippet("gmset", "GM_setValue('', '');"),
        Snippet("gmget", "GM_getValue('', '');")
    )
    val keywords = (keywordNames + builtinNames).map { Keyword(it) }
    return keywords + snippets
}

/**
 * CodeViewAdapter's stock getView() inflates an XML layout resource. This app builds all
 * UI in Compose, so the suggestion row is instead built as a single plain TextView in code.
 */
private class IdeAutoCompleteAdapter(
    context: Context,
    private val colors: ClintColors,
    codes: List<Code>
) : CodeViewAdapter(context, 0, 0, codes) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val code = getItem(position) as Code
        val row = (convertView as? TextView) ?: TextView(parent.context)
        row.text = code.getCodeTitle()
        row.setTextColor(colors.popupText.toArgb())
        row.setBackgroundColor(colors.popupBackground.toArgb())
        row.typeface = Typeface.MONOSPACE
        row.textSize = 14f
        row.setPadding(28, 20, 28, 20)
        return row
    }
}

/**
 * A single IDE-style editor panel: an optional find/replace bar, the code surface
 * itself, a status bar (language + cursor position), and a quick-insert accessory
 * bar that appears above the keyboard while the editor is focused.
 */
@Composable
fun JsCodeEditor(
    code: String,
    onCodeChange: (String) -> Unit,
    colors: ClintColors,
    modifier: Modifier = Modifier,
    findBarVisible: Boolean,
    onCloseFindBar: () -> Unit
) {
    val latestOnCodeChange by rememberUpdatedState(onCodeChange)
    val codeViewRef = remember { mutableStateOf<IdeCodeView?>(null) }

    var line by remember { mutableStateOf(1) }
    var column by remember { mutableStateOf(1) }
    var isFocused by remember { mutableStateOf(false) }
    var canUndo by remember { mutableStateOf(false) }
    var canRedo by remember { mutableStateOf(false) }

    Column(modifier) {
        AnimatedVisibility(visible = findBarVisible, enter = fadeIn(), exit = fadeOut()) {
            Column {
                FindReplaceBar(
                    codeView = codeViewRef.value,
                    colors = colors,
                    onCodeChanged = { latestOnCodeChange(it) },
                    onClose = onCloseFindBar
                )
                HorizontalDivider(color = colors.divider, thickness = 1.dp)
            }
        }

        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { context ->
                IdeCodeView(context).apply {
                    setEnableLineNumber(true)
                    setEnableHighlightCurrentLine(true)
                    setTabLength(2)
                    setEnableAutoIndentation(false)
                    setIndentationStarts(setOf('{', '(', '['))
                    setIndentationEnds(setOf('}', ')', ']'))
                    setThreshold(2)
                    setMaxSuggestionsSize(6)
                    setAutoCompleteItemHeightInDp(40)
                    setAdapter(IdeAutoCompleteAdapter(context, colors, buildEditorSuggestions()))
                    typeface = Typeface.MONOSPACE
                    textSize = 13f
                    setPadding(24, 16, 24, 16)
                    onSelectionChange = { l, c -> line = l; column = c }
                    onFocusChange = { isFocused = it }
                    onHistoryChange = { undo, redo -> canUndo = undo; canRedo = redo }
                    addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                        override fun afterTextChanged(s: Editable?) {
                            latestOnCodeChange(s?.toString().orEmpty())
                        }
                    })
                    codeViewRef.value = this
                }
            },
            update = { view ->
                view.setBackgroundColor(colors.cardBackground.toArgb())
                view.setTextColor(colors.onSurface.toArgb())
                view.setLineNumberTextColor(colors.secondaryText.toArgb())
                view.setHighlightCurrentLineColor(colors.surfaceVariant.copy(alpha = 0.4f).toArgb())
                view.setMatchingHighlightColor(colors.primary.copy(alpha = 0.45f).toArgb())
                view.resetSyntaxPatternList()
                view.resetHighlighter()
                view.addSyntaxPattern(PATTERN_METADATA, colors.primary.copy(alpha = 0.75f).toArgb())
                view.addSyntaxPattern(PATTERN_KEYWORDS, colors.primary.toArgb())
                view.addSyntaxPattern(PATTERN_BUILTINS, colors.colorError.toArgb())
                view.addSyntaxPattern(PATTERN_STRING, AndroidColor.parseColor("#98C379"))
                view.addSyntaxPattern(PATTERN_NUMBERS, AndroidColor.parseColor("#D19A66"))
                view.addSyntaxPattern(PATTERN_SINGLE_LINE_COMMENT, colors.secondaryText.toArgb())
                view.addSyntaxPattern(PATTERN_MULTI_LINE_COMMENT, colors.secondaryText.toArgb())
                if (view.text?.toString() != code) {
                    view.setTextHighlighted(code)
                    view.resetHistory()
                } else {
                    view.reHighlightSyntax()
                }
            }
        )

        Column(Modifier.navigationBarsPadding().imePadding()) {
            HorizontalDivider(color = colors.divider, thickness = 1.dp)
            EditorStatusBar(colors = colors, line = line, column = column)
            AnimatedVisibility(visible = isFocused, enter = fadeIn(), exit = fadeOut()) {
                AccessoryBar(
                    colors = colors,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    onInsert = { text ->
                        codeViewRef.value?.let { it.insertAtCursor(text); it.requestFocus() }
                    },
                    onMoveCursor = { delta ->
                        codeViewRef.value?.let { it.moveCursor(delta); it.requestFocus() }
                    },
                    onUndo = { codeViewRef.value?.let { it.undo(); it.requestFocus() } },
                    onRedo = { codeViewRef.value?.let { it.redo(); it.requestFocus() } }
                )
            }
        }
    }
}

@Composable
private fun EditorStatusBar(colors: ClintColors, line: Int, column: Int) {
    Surface(color = colors.surface) {
        Row(
            Modifier.fillMaxWidth().height(30.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusBarLabel(stringResource(R.string.user_scripts_editor_status_language), colors)
            Spacer(Modifier.width(10.dp))
            StatusBarLabel(stringResource(R.string.user_scripts_editor_status_spaces), colors)
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.user_scripts_editor_status_position, line, column),
                color = colors.secondaryText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun StatusBarLabel(text: String, colors: ClintColors) {
    Text(text, color = colors.secondaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
}

@Composable
private fun AccessoryBar(
    colors: ClintColors,
    canUndo: Boolean,
    canRedo: Boolean,
    onInsert: (String) -> Unit,
    onMoveCursor: (Int) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    Surface(color = colors.surfaceVariant) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeyCapButton("\u25c2", colors, description = stringResource(R.string.user_scripts_editor_cursor_left_desc)) { onMoveCursor(-1) }
            KeyCapButton("\u25b8", colors, description = stringResource(R.string.user_scripts_editor_cursor_right_desc)) { onMoveCursor(1) }
            AccessoryDivider(colors)
            KeyCapButton("Tab", colors) { onInsert("  ") }
            KeyCapButton("{", colors) { onInsert("{") }
            KeyCapButton("}", colors) { onInsert("}") }
            KeyCapButton("(", colors) { onInsert("(") }
            KeyCapButton(")", colors) { onInsert(")") }
            KeyCapButton(";", colors) { onInsert(";") }
            KeyCapButton("\"", colors) { onInsert("\"") }
            KeyCapButton("'", colors) { onInsert("'") }
            AccessoryDivider(colors)
            KeyCapButton(
                "\u21b6", colors, enabled = canUndo,
                description = stringResource(R.string.user_scripts_editor_undo_desc)
            ) { onUndo() }
            KeyCapButton(
                "\u21b7", colors, enabled = canRedo,
                description = stringResource(R.string.user_scripts_editor_redo_desc)
            ) { onRedo() }
        }
    }
}

@Composable
private fun AccessoryDivider(colors: ClintColors) {
    Box(
        Modifier
            .padding(horizontal = 4.dp)
            .width(1.dp)
            .height(22.dp)
            .background(colors.divider)
    )
}

@Composable
private fun KeyCapButton(
    label: String,
    colors: ClintColors,
    enabled: Boolean = true,
    description: String? = null,
    onClick: () -> Unit
) {
    var boxModifier = Modifier
        .padding(end = 6.dp)
        .size(width = 38.dp, height = 34.dp)
        .background(colors.cardBackground, RoundedCornerShape(6.dp))
        .border(1.dp, colors.divider, RoundedCornerShape(6.dp))
        .clickable(enabled = enabled, onClick = onClick)
    if (description != null) {
        boxModifier = boxModifier.semantics { contentDescription = description }
    }
    Box(modifier = boxModifier, contentAlignment = Alignment.Center) {
        Text(
            label,
            color = if (enabled) colors.onSurface else colors.secondaryText.copy(alpha = 0.4f),
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FindReplaceBar(
    codeView: IdeCodeView?,
    colors: ClintColors,
    onCodeChanged: (String) -> Unit,
    onClose: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf("") }
    var matchCount by remember { mutableStateOf(0) }
    var matchIndex by remember { mutableStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    fun runSearch(newQuery: String) {
        query = newQuery
        if (newQuery.isEmpty()) {
            codeView?.clearMatches()
            matchCount = 0
            matchIndex = 0
            return
        }
        val matches = codeView?.findMatches(Pattern.quote(newQuery)) ?: emptyList()
        matchCount = matches.size
        if (matches.isNotEmpty()) {
            codeView?.findNextMatch()
            matchIndex = 1
        } else {
            matchIndex = 0
        }
    }

    fun refreshAfterEdit() {
        onCodeChanged(codeView?.text?.toString().orEmpty())
        runSearch(query)
    }

    Surface(color = colors.surface) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { codeView?.clearMatches(); onClose() }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close_search), tint = colors.iconTint)
                }
                BasicTextField(
                    value = query,
                    onValueChange = { runSearch(it) },
                    singleLine = true,
                    textStyle = TextStyle(color = colors.onSurface, fontSize = 15.sp),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { codeView?.findNextMatch() }),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(stringResource(R.string.user_scripts_editor_find_hint), color = colors.secondaryText, fontSize = 15.sp)
                        }
                        inner()
                    },
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp).focusRequester(focusRequester)
                )
                Text(
                    if (query.isEmpty()) "" else if (matchCount == 0) stringResource(R.string.user_scripts_editor_find_no_matches)
                    else stringResource(R.string.user_scripts_editor_find_count, matchIndex, matchCount),
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    modifier = Modifier.wrapContentWidth().padding(end = 4.dp)
                )
                IconButton(onClick = {
                    if (matchCount > 0) {
                        codeView?.findPrevMatch()
                        matchIndex = if (matchIndex <= 1) matchCount else matchIndex - 1
                    }
                }) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = stringResource(R.string.user_scripts_editor_find_prev_desc), tint = colors.iconTint)
                }
                IconButton(onClick = {
                    if (matchCount > 0) {
                        codeView?.findNextMatch()
                        matchIndex = if (matchIndex >= matchCount) 1 else matchIndex + 1
                    }
                }) {
                    Icon(Icons.Filled.ArrowDownward, contentDescription = stringResource(R.string.user_scripts_editor_find_next_desc), tint = colors.iconTint)
                }
            }
            Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(48.dp))
                BasicTextField(
                    value = replacement,
                    onValueChange = { replacement = it },
                    singleLine = true,
                    textStyle = TextStyle(color = colors.onSurface, fontSize = 15.sp),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    decorationBox = { inner ->
                        if (replacement.isEmpty()) {
                            Text(stringResource(R.string.user_scripts_editor_replace_hint), color = colors.secondaryText, fontSize = 15.sp)
                        }
                        inner()
                    },
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                )
                TextButton(onClick = {
                    if (query.isNotEmpty() && matchCount > 0) {
                        codeView?.replaceFirstMatch(Pattern.quote(query), Matcher.quoteReplacement(replacement))
                        refreshAfterEdit()
                    }
                }, enabled = query.isNotEmpty() && matchCount > 0) {
                    Text(stringResource(R.string.user_scripts_editor_replace_action), color = colors.primary, fontSize = 13.sp)
                }
                TextButton(onClick = {
                    if (query.isNotEmpty() && matchCount > 0) {
                        codeView?.replaceAllMatches(Pattern.quote(query), Matcher.quoteReplacement(replacement))
                        refreshAfterEdit()
                    }
                }, enabled = query.isNotEmpty() && matchCount > 0) {
                    Text(stringResource(R.string.user_scripts_editor_replace_all_action), color = colors.primary, fontSize = 13.sp)
                }
            }
        }
    }
}

/**
 * CodeView extended with the small pieces of imperative behavior a real editor needs:
 * cursor position reporting, focus reporting, a lightweight undo/redo history and
 * cursor-relative text insertion for the accessory bar.
 */
private class IdeCodeView(context: Context) : CodeView(context) {

    var onSelectionChange: ((line: Int, column: Int) -> Unit)? = null
    var onFocusChange: ((Boolean) -> Unit)? = null
    var onHistoryChange: ((canUndo: Boolean, canRedo: Boolean) -> Unit)? = null

    private val history = EditHistoryTracker()
    private var isApplyingHistory = false

    private val panTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var lastTouchX = 0f
    private var isPanning = false
    private var panMaxScrollX = 0

    private val indentStarts = setOf('{', '(', '[')
    private val indentEnds = setOf('}', ')', ']')
    private val indentTabLength = 2
    private var pendingCursorOffset: Int? = null

    private val autoIndentFilter = InputFilter { source, start, end, dest, dStart, dEnd ->
        if (start >= end || source[start] != '\n' || end - start != 1) return@InputFilter null

        var lineStart = dStart
        while (lineStart > 0 && dest[lineStart - 1] != '\n') lineStart--
        val beforeCursor = dest.subSequence(lineStart, dStart)

        var baseIndent = 0
        while (baseIndent < beforeCursor.length && beforeCursor[baseIndent] == ' ') baseIndent++

        var lastNonWs = -1
        for (i in beforeCursor.length - 1 downTo 0) {
            if (beforeCursor[i] != ' ') {
                lastNonWs = i
                break
            }
        }
        val lastChar = if (lastNonWs >= 0) beforeCursor[lastNonWs] else null
        val nextChar = if (dEnd < dest.length) dest[dEnd] else null

        val opensHere = lastChar != null && indentStarts.contains(lastChar)
        val closesNext = nextChar != null && indentEnds.contains(nextChar)

        val result: String
        val cursorOffset: Int
        if (opensHere && closesNext) {
            val innerIndent = baseIndent + indentTabLength
            result = "\n" + " ".repeat(innerIndent) + "\n" + " ".repeat(baseIndent)
            cursorOffset = dStart + 1 + innerIndent
        } else if (opensHere) {
            val newIndent = baseIndent + indentTabLength
            result = "\n" + " ".repeat(newIndent)
            cursorOffset = dStart + result.length
        } else if (closesNext) {
            val newIndent = (baseIndent - indentTabLength).coerceAtLeast(0)
            result = "\n" + " ".repeat(newIndent)
            cursorOffset = dStart + result.length
        } else {
            result = "\n" + " ".repeat(baseIndent)
            cursorOffset = dStart + result.length
        }

        pendingCursorOffset = cursorOffset
        result
    }

    init {
        setOnFocusChangeListener { _, hasFocus -> onFocusChange?.invoke(hasFocus) }
        filters = arrayOf(autoIndentFilter)
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val offset = pendingCursorOffset ?: return
                pendingCursorOffset = null
                val length = s?.length ?: return
                setSelection(offset.coerceIn(0, length))
            }
        })
        addTextChangedListener(object : TextWatcher {
            private var beforeText: CharSequence? = null

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (isApplyingHistory || s == null) return
                beforeText = s.subSequence(start, start + count)
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isApplyingHistory || s == null) return
                val afterText = s.subSequence(start, start + count)
                history.record(start, beforeText, afterText)
                onHistoryChange?.invoke(history.canUndo, history.canRedo)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        val layout = layout ?: return
        val length = text?.length ?: return
        if (selStart < 0 || selStart > length) return
        val currentLine = layout.getLineForOffset(selStart)
        val currentColumn = selStart - layout.getLineStart(currentLine)
        onSelectionChange?.invoke(currentLine + 1, currentColumn + 1)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = event.x
                touchDownY = event.y
                lastTouchX = event.x
                isPanning = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isPanning) {
                    val dx = event.x - touchDownX
                    val dy = event.y - touchDownY
                    if (abs(dx) > panTouchSlop && abs(dx) > abs(dy)) {
                        isPanning = true
                        lastTouchX = event.x
                        panMaxScrollX = computeMaxScrollX()
                        val cancelEvent = MotionEvent.obtain(event)
                        cancelEvent.action = MotionEvent.ACTION_CANCEL
                        super.onTouchEvent(cancelEvent)
                        cancelEvent.recycle()
                        parent?.requestDisallowInterceptTouchEvent(true)
                    }
                }
                if (isPanning) {
                    val deltaX = (lastTouchX - event.x).toInt()
                    lastTouchX = event.x
                    scrollTo((scrollX + deltaX).coerceIn(0, panMaxScrollX), scrollY)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isPanning) {
                    isPanning = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun computeMaxScrollX(): Int {
        val currentLayout = layout ?: return 0
        var maxLineWidth = 0f
        for (i in 0 until currentLayout.lineCount) {
            val lineWidth = currentLayout.getLineWidth(i)
            if (lineWidth > maxLineWidth) maxLineWidth = lineWidth
        }
        val visibleWidth = width - totalPaddingLeft - totalPaddingRight
        return (maxLineWidth - visibleWidth).toInt().coerceAtLeast(0)
    }

    fun insertAtCursor(insert: String) {
        val length = text?.length ?: return
        val start = selectionStart.coerceIn(0, length)
        val end = selectionEnd.coerceIn(0, length)
        text?.replace(minOf(start, end), maxOf(start, end), insert)
    }

    fun moveCursor(delta: Int) {
        val length = text?.length ?: return
        setSelection((selectionStart + delta).coerceIn(0, length))
    }

    fun undo() {
        val edit = history.undo() ?: return
        val length = text?.length ?: return
        val end = (edit.start + (edit.after?.length ?: 0)).coerceIn(0, length)
        val start = edit.start.coerceIn(0, length)
        isApplyingHistory = true
        text?.replace(start, end, edit.before ?: "")
        isApplyingHistory = false
        setSelection((start + (edit.before?.length ?: 0)).coerceIn(0, text?.length ?: 0))
        onHistoryChange?.invoke(history.canUndo, history.canRedo)
    }

    fun redo() {
        val edit = history.redo() ?: return
        val length = text?.length ?: return
        val end = (edit.start + (edit.before?.length ?: 0)).coerceIn(0, length)
        val start = edit.start.coerceIn(0, length)
        isApplyingHistory = true
        text?.replace(start, end, edit.after ?: "")
        isApplyingHistory = false
        setSelection((start + (edit.after?.length ?: 0)).coerceIn(0, text?.length ?: 0))
        onHistoryChange?.invoke(history.canUndo, history.canRedo)
    }

    fun resetHistory() {
        history.clear()
        onHistoryChange?.invoke(false, false)
    }
}

private class EditHistoryTracker {
    data class Edit(val start: Int, val before: CharSequence?, val after: CharSequence?)

    private val entries = ArrayDeque<Edit>()
    private var position = 0
    private val maxSize = 300

    val canUndo: Boolean get() = position > 0
    val canRedo: Boolean get() = position < entries.size

    fun clear() {
        entries.clear()
        position = 0
    }

    fun record(start: Int, before: CharSequence?, after: CharSequence?) {
        while (entries.size > position) entries.removeLast()
        entries.addLast(Edit(start, before, after))
        position++
        while (entries.size > maxSize) {
            entries.removeFirst()
            position--
        }
    }

    fun undo(): Edit? {
        if (position == 0) return null
        position--
        return entries[position]
    }

    fun redo(): Edit? {
        if (position >= entries.size) return null
        val edit = entries[position]
        position++
        return edit
    }
}
