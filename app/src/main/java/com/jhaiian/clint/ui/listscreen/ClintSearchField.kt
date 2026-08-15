package com.jhaiian.clint.ui.listscreen
import androidx.compose.material.icons.filled.Close

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.theme.LocalClintColors

/**
 * The shared toolbar search field for list screens: a single-line [BasicTextField] with a
 * hint, a search IME action, and a trailing close button — used whenever a list-screen
 * toolbar's normal title/icons swap out for a live search box (History, QuiverGuard,
 * SiteList). Grabs focus and shows the keyboard as soon as it enters composition, so callers
 * just need to gate it behind their own `isSearchMode` flag rather than managing a
 * [FocusRequester] themselves.
 *
 * Must be called directly inside a `Row { ... }`, since it lays out as the flexible field
 * followed by the close button, both sized against the row via [RowScope.weight].
 */
@Composable
fun RowScope.ClintSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    hint: String,
    onClose: () -> Unit
) {
    val colors = LocalClintColors.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(color = colors.onSurface, fontSize = 16.sp),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
        decorationBox = { inner ->
            if (query.isEmpty()) {
                Text(hint, color = colors.secondaryText, fontSize = 16.sp)
            }
            inner()
        },
        modifier = Modifier.weight(1f).padding(start = 4.dp, end = 4.dp).focusRequester(focusRequester)
    )
    IconButton(onClick = onClose) {
        Icon(
            androidx.compose.material.icons.Icons.Filled.Close,
            contentDescription = stringResource(R.string.action_close_search),
            tint = colors.iconTint
        )
    }
}
