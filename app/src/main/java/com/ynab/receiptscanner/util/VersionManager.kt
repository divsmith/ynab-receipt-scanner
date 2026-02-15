package com.ynab.receiptscanner.util

import android.content.Context
import android.content.pm.PackageManager
import com.ynab.receiptscanner.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App version management utilities
 * Provides version information and update checking
 */
@Singleton
class VersionManager @Inject constructor(
    private val context: Context
) {
    
    /**
     * Get app version name (e.g., "1.0.0")
     */
    fun getVersionName(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            BuildConfig.VERSION_NAME
        }
    }
    
    /**
     * Get app version code (e.g., 1)
     */
    fun getVersionCode(): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            BuildConfig.VERSION_CODE.toLong()
        }
    }
    
    /**
     * Get formatted version string (e.g., "v1.0.0 (1)")
     */
    fun getFormattedVersion(): String {
        return "v${getVersionName()} (${getVersionCode()})"
    }
    
    /**
     * Check if this is first launch of this version
     */
    fun isFirstLaunchOfVersion(): Boolean {
        val prefs = context.getSharedPreferences("version_prefs", Context.MODE_PRIVATE)
        val lastVersionCode = prefs.getLong("last_version_code", -1)
        val currentVersionCode = getVersionCode()
        
        return if (lastVersionCode != currentVersionCode) {
            prefs.edit().putLong("last_version_code", currentVersionCode).apply()
            true
        } else {
            false
        }
    }
    
    /**
     * Check if this is first launch ever
     */
    fun isFirstLaunchEver(): Boolean {
        val prefs = context.getSharedPreferences("version_prefs", Context.MODE_PRIVATE)
        val hasLaunched = prefs.getBoolean("has_launched", false)
        
        return if (!hasLaunched) {
            prefs.edit().putBoolean("has_launched", true).apply()
            true
        } else {
            false
        }
    }
    
    /**
     * Get what's new message for current version
     */
    fun getWhatsNewMessage(): String? {
        return if (isFirstLaunchOfVersion()) {
            when (getVersionName()) {
                "1.0.0" -> "Welcome to YNAB Receipt Scanner!"
                "1.1.0" -> "New: Offline sync support\nImproved: OCR accuracy"
                "1.2.0" -> "New: Dark mode\nImproved: Performance optimizations"
                else -> null
            }
        } else {
            null
        }
    }
    
    /**
     * Check if app needs update (placeholder for future implementation)
     */
    fun checkForUpdates(onResult: (Boolean, String?) -> Unit) {
        // Placeholder - implement with play store API or custom backend
        // For now, always return false
        onResult(false, null)
    }
    
    /**
     * Get build type (debug/release)
     */
    fun getBuildType(): String {
        return BuildConfig.BUILD_TYPE
    }
    
    /**
     * Is debug build
     */
    fun isDebugBuild(): Boolean {
        return BuildConfig.DEBUG
    }
    
    /**
     * Get application ID
     */
    fun getApplicationId(): String {
        return BuildConfig.APPLICATION_ID
    }
}
