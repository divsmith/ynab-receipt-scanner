package com.ynab.receiptscanner.ui.settings

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ynab.receiptscanner.BuildConfig
import com.ynab.receiptscanner.domain.usecase.SignOutUseCase
import com.ynab.receiptscanner.worker.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for SettingsFragment
 * Manages user preferences and account operations
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val signOutUseCase: SignOutUseCase,
    private val syncScheduler: SyncScheduler
) : ViewModel() {
    
    private val _userInfo = MutableLiveData<String>()
    val userInfo: LiveData<String> = _userInfo
    
    private val _signOutEvent = MutableLiveData<Boolean>()
    val signOutEvent: LiveData<Boolean> = _signOutEvent
    
    private val _lastSyncTime = MutableLiveData<String?>()
    val lastSyncTime: LiveData<String?> = _lastSyncTime
    
    init {
        loadUserInfo()
        loadLastSyncTime()
    }
    
    private fun loadUserInfo() {
        // TODO: Get actual user info from repository
        _userInfo.value = "Signed in"
    }
    
    private fun loadLastSyncTime() {
        // TODO: Get actual last sync time from preferences or database
        _lastSyncTime.value = null
    }
    
    fun signOut() {
        viewModelScope.launch {
            try {
                signOutUseCase()
                _signOutEvent.value = true
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun clearCache() {
        viewModelScope.launch {
            try {
                // TODO: Implement cache clearing
                context.cacheDir.deleteRecursively()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun setImageRetention(enabled: Boolean) {
        // Preference is automatically saved by PreferenceFragmentCompat
        // Additional logic can be added here if needed
    }
    
    fun setAutoSync(enabled: Boolean) {
        // Preference is automatically saved by PreferenceFragmentCompat
        if (enabled) {
            // Re-schedule periodic sync with current settings
            syncScheduler.schedulePeriodicSync()
        } else {
            // Cancel periodic sync
            syncScheduler.cancelPeriodicSync()
        }
    }
    
    fun setSyncInterval(intervalMinutes: Long) {
        // Update sync interval
        if (intervalMinutes > 0) {
            syncScheduler.updateSyncInterval(intervalMinutes)
        } else {
            // Manual only - cancel periodic sync
            syncScheduler.cancelPeriodicSync()
        }
    }
    
    fun setSyncOnMetered(enabled: Boolean) {
        // Re-schedule with new metered network setting
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        val intervalMinutes = prefs.getLong("sync_interval", 60L)
        syncScheduler.schedulePeriodicSync(intervalMinutes, requireUnmetered = !enabled)
    }
    
    fun setTheme(theme: String) {
        // Preference is automatically saved by PreferenceFragmentCompat
        // Apply theme change
        // AppCompatDelegate.setDefaultNightMode() can be called here
    }
    
    fun getAppVersion(): String {
        return BuildConfig.VERSION_NAME
    }
}
