package com.ynab.receiptscanner.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ynab.receiptscanner.R
import com.ynab.receiptscanner.ui.auth.AuthActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Settings screen using PreferenceFragmentCompat
 * Provides user preferences and account management
 */
@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {
    
    private val viewModel: SettingsViewModel by viewModels()
    
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        
        setupPreferences()
        observeViewModel()
    }
    
    private fun setupPreferences() {
        // Account info
        findPreference<Preference>("account_info")?.apply {
            viewModel.userInfo.observe(this@SettingsFragment) { info ->
                summary = info
            }
        }
        
        // Sign out
        findPreference<Preference>("sign_out")?.setOnPreferenceClickListener {
            showSignOutDialog()
            true
        }
        
        // Clear cache
        findPreference<Preference>("clear_cache")?.setOnPreferenceClickListener {
            showClearCacheDialog()
            true
        }
        
        // Image retention
        findPreference<SwitchPreferenceCompat>("image_retention")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                viewModel.setImageRetention(newValue as Boolean)
                true
            }
        }
        
        // Auto-sync
        findPreference<SwitchPreferenceCompat>("auto_sync")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                viewModel.setAutoSync(newValue as Boolean)
                true
            }
        }
        
        // Sync interval
        findPreference<ListPreference>("sync_interval")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                val intervalMinutes = (newValue as String).toLongOrNull() ?: 60L
                viewModel.setSyncInterval(intervalMinutes)
                true
            }
        }
        
        // Sync on metered networks
        findPreference<SwitchPreferenceCompat>("sync_on_metered")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                viewModel.setSyncOnMetered(newValue as Boolean)
                true
            }
        }
        
        // Last sync time
        findPreference<Preference>("last_sync_time")?.apply {
            viewModel.lastSyncTime.observe(this@SettingsFragment) { time ->
                summary = time ?: getString(R.string.never)
            }
        }
        
        // Theme
        findPreference<Preference>("theme")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                viewModel.setTheme(newValue as String)
                true
            }
        }
        
        // About
        findPreference<Preference>("about")?.apply {
            summary = "Version ${viewModel.getAppVersion()}"
        }
    }
    
    private fun observeViewModel() {
        viewModel.signOutEvent.observe(this) { shouldSignOut ->
            if (shouldSignOut) {
                navigateToAuth()
            }
        }
    }
    
    private fun showSignOutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.sign_out_title)
            .setMessage(R.string.sign_out_message)
            .setPositiveButton(R.string.sign_out) { _, _ ->
                viewModel.signOut()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun showClearCacheDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.clear_cache_title)
            .setMessage(R.string.clear_cache_message)
            .setPositiveButton(R.string.clear) { _, _ ->
                viewModel.clearCache()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun navigateToAuth() {
        val intent = Intent(requireContext(), AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}
