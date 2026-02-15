package com.ynab.receiptscanner.data.analytics

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Crash reporting wrapper for Crashlytics
 * 
 * IMPORTANT: This is a stub implementation. Firebase Crashlytics must be configured before use.
 * 
 * To enable Crashlytics:
 * 1. Configure Firebase (see FirebaseAnalyticsImpl.kt)
 * 2. Uncomment Crashlytics dependencies in build.gradle.kts
 * 3. Uncomment the actual Crashlytics implementation below
 * 
 * For now, this logs errors to Logcat.
 */
@Singleton
class CrashlyticsManager @Inject constructor() {
    
    companion object {
        private const val TAG = "Crashlytics"
    }
    
    // Uncomment when Firebase Crashlytics is configured:
    // private val crashlytics: FirebaseCrashlytics by lazy {
    //     FirebaseCrashlytics.getInstance()
    // }
    
    /**
     * Log a non-fatal exception
     */
    fun logException(throwable: Throwable) {
        Log.e(TAG, "Non-fatal exception", throwable)
        
        // Uncomment when Firebase is configured:
        // crashlytics.recordException(throwable)
    }
    
    /**
     * Log a message
     */
    fun log(message: String) {
        Log.d(TAG, message)
        
        // Uncomment when Firebase is configured:
        // crashlytics.log(message)
    }
    
    /**
     * Set custom key for crash reports
     * Useful for debugging crashes with context
     */
    fun setCustomKey(key: String, value: String) {
        Log.d(TAG, "Custom Key: $key = $value")
        
        // Uncomment when Firebase is configured:
        // crashlytics.setCustomKey(key, value)
    }
    
    fun setCustomKey(key: String, value: Int) {
        Log.d(TAG, "Custom Key: $key = $value")
        
        // Uncomment when Firebase is configured:
        // crashlytics.setCustomKey(key, value)
    }
    
    fun setCustomKey(key: String, value: Boolean) {
        Log.d(TAG, "Custom Key: $key = $value")
        
        // Uncomment when Firebase is configured:
        // crashlytics.setCustomKey(key, value)
    }
    
    /**
     * Set user identifier (hashed for privacy)
     * Never set raw PII like email or username
     */
    fun setUserId(userId: String) {
        // Hash the user ID for privacy
        val hashedId = userId.hashCode().toString()
        Log.d(TAG, "User ID (hashed): $hashedId")
        
        // Uncomment when Firebase is configured:
        // crashlytics.setUserId(hashedId)
    }
    
    /**
     * Add breadcrumb for debugging crash context
     */
    fun addBreadcrumb(message: String) {
        log("Breadcrumb: $message")
    }
    
    /**
     * Enable/disable crash reporting
     */
    fun setCrashlyticsCollectionEnabled(enabled: Boolean) {
        Log.d(TAG, "Crashlytics Enabled: $enabled")
        
        // Uncomment when Firebase is configured:
        // crashlytics.setCrashlyticsCollectionEnabled(enabled)
    }
    
    /**
     * Force a crash (for testing)
     * DO NOT call this in production code!
     */
    fun forceCrash() {
        throw RuntimeException("Test crash from CrashlyticsManager")
    }
}
