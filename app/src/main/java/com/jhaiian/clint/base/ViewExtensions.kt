package com.jhaiian.clint.base

import android.view.View
import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Requests the soft keyboard be shown for this view, using the modern
 * WindowInsetsController API (via the AndroidX compat shim) instead of the
 * deprecated [android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT] flag.
 * [window] must be the Window this view is actually attached to (the host
 * Activity's window, or a Dialog's window when the view lives in a dialog) -
 * ViewCompat.getWindowInsetsController(View) is itself deprecated in favor of
 * this explicit-Window form. Safe to call across the app's full supported API
 * range (26+).
 */
fun View.showSoftKeyboard(window: Window) {
    requestFocus()
    WindowCompat.getInsetsController(window, this).show(WindowInsetsCompat.Type.ime())
}
