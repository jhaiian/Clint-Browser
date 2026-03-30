package com.jhaiian.clint.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.jhaiian.clint.R

class GeneralSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.general_preferences, rootKey)
    }
}
