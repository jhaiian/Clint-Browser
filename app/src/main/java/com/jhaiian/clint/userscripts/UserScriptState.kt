package com.jhaiian.clint.userscripts

import android.content.Context
import androidx.preference.PreferenceManager

object UserScriptState {
    private const val PREF_ENABLED = "user_scripts_enabled"
    private const val PREF_DATA_VERSION = "user_scripts_data_version"

    fun isEnabled(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putBoolean(PREF_ENABLED, enabled)
            .apply()
    }

    fun getDataVersion(context: Context): Long =
        PreferenceManager.getDefaultSharedPreferences(context).getLong(PREF_DATA_VERSION, 0L)

    fun bumpDataVersion(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putLong(PREF_DATA_VERSION, System.currentTimeMillis())
            .apply()
    }
}
