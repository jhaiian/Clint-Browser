package com.jhaiian.clint.settings

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R

class MainSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()
        updateSearchEngineSummary()
        updateVersionSummary()
    }

    private fun updateSearchEngineSummary() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val engine = prefs.getString("search_engine", "duckduckgo")
        val label = when (engine) {
            "brave" -> getString(R.string.engine_brave)
            "google" -> getString(R.string.engine_google)
            else -> getString(R.string.engine_duckduckgo)
        }
        findPreference<Preference>("pref_search_engine")?.summary = label
    }

    private fun updateVersionSummary() {
        val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
        findPreference<Preference>("pref_about")?.summary =
            getString(R.string.about_summary, pInfo.versionName)
    }
}
