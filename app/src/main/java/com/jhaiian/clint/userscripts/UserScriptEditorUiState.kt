package com.jhaiian.clint.userscripts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class UserScriptEditorUiState {
    var code by mutableStateOf("")
    var initialCode by mutableStateOf("")
    var scriptId by mutableStateOf(-1L)
    var isSaving by mutableStateOf(false)

    val isNew: Boolean get() = scriptId < 0
    val isDirty: Boolean get() = code != initialCode
}

fun defaultUserScriptTemplate(): String = """
// ==UserScript==
// @name         New Script
// @namespace    https://clintbrowser/
// @version      1.0
// @description  
// @author       
// @match        *://*/*
// @grant        none
// @run-at       document-idle
// ==/UserScript==

(function() {
    'use strict';

})();
""".trimIndent()
