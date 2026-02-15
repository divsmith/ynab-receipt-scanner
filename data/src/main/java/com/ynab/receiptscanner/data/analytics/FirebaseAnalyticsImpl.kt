package com.ynab.receiptscanner.data.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Analytics implementation
 * 
 * IMPORTANT: This is a stub implementation. Firebase must be configured before use.
 * 
 * To enable Firebase Analytics:
 * 1. Add Firebase to your project in Firebase Console
 * 2. Download google-services.json
 * 3. Place google-services.json in app/
 * 4. Uncomment Firebase dependencies in build.gradle.kts
 * 5. Uncomment the actual Firebase implementation below
 * 
 * For now, this logs events to Logcat for debugging.
 */
@Singleton
class FirebaseAnalyticsImpl @Inject constructor(
    private val context: Context
) : AnalyticsManager {
    
    companion object {
        private const val TAG = "FirebaseAnalytics"
    }
    
    // Uncomment when Firebase is configured:
    // private val firebaseAnalytics: FirebaseAnalytics by lazy {
    //     FirebaseAnalytics.getInstance(context)
    // }
    
    private var analyticsEnabled = true
    
    override fun trackEvent(event: AnalyticsEvent) {
        if (!analyticsEnabled) return
        
        // Log to console for now
        Log.d(TAG, "Event: ${event.eventName}, Parameters: ${event.parameters}")
        
        // Uncomment when Firebase is configured:
        // val bundle = Bundle().apply {
        //     event.parameters.forEach { (key, value) ->
        //         when (value) {
        //             is String -> putString(key, value)
        //             is Int -> putInt(key, value)
        //             is Long -> putLong(key, value)
        //             is Float -> putFloat(key, value)
        //             is Double -> putDouble(key, value)
        //             is Boolean -> putBoolean(key, value)
        //             else -> putString(key, value.toString())
        //         }
        //     }
        // }
        // firebaseAnalytics.logEvent(event.eventName, bundle)
    }
    
    override fun setUserProperty(key: String, value: String) {
        if (!analyticsEnabled) return
        
        Log.d(TAG, "User Property: $key = $value")
        
        // Uncomment when Firebase is configured:
        // firebaseAnalytics.setUserProperty(key, value)
    }
    
    override fun logScreenView(screenName: String, screenClass: String) {
        if (!analyticsEnabled) return
        
        Log.d(TAG, "Screen View: $screenName ($screenClass)")
        
        // Uncomment when Firebase is configured:
        // val bundle = Bundle().apply {
        //     putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        //     putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        // }
        // firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }
    
    override fun setCurrentScreen(screenName: String, screenClass: String) {
        if (!analyticsEnabled) return
        
        Log.d(TAG, "Set Current Screen: $screenName")
        
        // Uncomment when Firebase is configured:
        // firebaseAnalytics.setCurrentScreen(activity, screenName, screenClass)
    }
    
    override fun setAnalyticsEnabled(enabled: Boolean) {
        analyticsEnabled = enabled
        Log.d(TAG, "Analytics Enabled: $enabled")
        
        // Uncomment when Firebase is configured:
        // firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
    }
    
    override fun isAnalyticsEnabled(): Boolean {
        return analyticsEnabled
    }
}
