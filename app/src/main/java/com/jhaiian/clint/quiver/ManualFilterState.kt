package com.jhaiian.clint.quiver

import android.content.Context
import androidx.preference.PreferenceManager

internal object ManualFilterState {

    const val COMPILE_ID = -1L

    private const val PREF_ENABLED = "quiver_guard_manual_filter_enabled"

    fun isEnabled(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putBoolean(PREF_ENABLED, enabled)
            .apply()
    }

    fun contentFingerprint(rules: List<ManualFilterRule>): String {
        val joined = rules.joinToString("\n") { it.ruleText }
        return "${rules.size}:${joined.hashCode()}"
    }
}
