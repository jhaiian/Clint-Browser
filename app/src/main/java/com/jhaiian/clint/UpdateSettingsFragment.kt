package com.jhaiian.clint

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class UpdateSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.update_preferences, rootKey)

        findPreference<Preference>("check_for_updates")?.setOnPreferenceClickListener {
            val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val isBeta = prefs.getBoolean("beta_channel", false)
            UpdateChecker.check(requireActivity(), isBeta, silent = false)
            true
        }

        findPreference<SwitchPreferenceCompat>("beta_channel")?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean) {
                MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_ClintBrowser_Dialog)
                    .setTitle("Enrol in Beta")
                    .setMessage(
                        "Beta releases give you early access to new features before they reach the stable channel.\n\n" +
                        "Beta builds may contain bugs, incomplete features, or unexpected behaviour. " +
                        "They are not recommended for daily use on your primary device.\n\n" +
                        "You can switch back to stable at any time."
                    )
                    .setNegativeButton("Cancel") { _, _ ->
                        findPreference<SwitchPreferenceCompat>("beta_channel")?.isChecked = false
                    }
                    .setPositiveButton("Enrol") { _, _ ->
                        findPreference<SwitchPreferenceCompat>("beta_channel")?.isChecked = true
                    }
                    .show()
                false
            } else {
                true
            }
        }
    }
}
