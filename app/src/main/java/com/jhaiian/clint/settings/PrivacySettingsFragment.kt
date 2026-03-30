package com.jhaiian.clint.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.jhaiian.clint.R

class PrivacySettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.privacy_preferences, rootKey)
    }
}
