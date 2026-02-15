package com.ynab.receiptscanner.performance

import android.util.Log
import com.ynab.receiptscanner.data.analytics.AnalyticsEvent
import com.ynab.receiptscanner.data.analytics.AnalyticsManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks app launch times (cold start, warm start)
 * Helps monitor app startup performance
 */
@Singleton
class LaunchTimeTracker @Inject constructor(
    private val analyticsManager: AnalyticsManager
) {
    
    companion object {
        private const val TAG = "LaunchTimeTracker"
        private const val COLD_START_THRESHOLD_MS = 2000L
        private const val WARM_START_THRESHOLD_MS = 1000L
    }
    
    private var appStartTime: Long = 0
    private var firstActivityCreated = false
    private var firstActivityResumed = false
    private var isColdStart = true
    
    /**
     * Called when Application.onCreate() is called
     */
    fun onApplicationCreate() {
        appStartTime = System.currentTimeMillis()
        Log.d(TAG, "Application created at $appStartTime")
    }
    
    /**
     * Called when first activity is created
     */
    fun onFirstActivityCreated() {
        if (!firstActivityCreated) {
            firstActivityCreated = true
            val timeToCreate = System.currentTimeMillis() - appStartTime
            Log.d(TAG, "First activity created in ${timeToCreate}ms")
        }
    }
    
    /**
     * Called when first activity is resumed
     */
    fun onFirstActivityResumed() {
        if (!firstActivityResumed) {
            firstActivityResumed = true
            val launchTime = System.currentTimeMillis() - appStartTime
            val threshold = if (isColdStart) COLD_START_THRESHOLD_MS else WARM_START_THRESHOLD_MS
            
            Log.i(TAG, "${if (isColdStart) "Cold" else "Warm"} start completed in ${launchTime}ms")
            
            // Log to analytics
            analyticsManager.trackEvent(
                AnalyticsEvent.PerformanceMetric(
                    metricName = if (isColdStart) "cold_start" else "warm_start",
                    durationMs = launchTime,
                    status = if (launchTime < threshold) "success" else "slow"
                )
            )
            
            if (launchTime > threshold) {
                Log.w(TAG, "Slow ${if (isColdStart) "cold" else "warm"} start: ${launchTime}ms (threshold: ${threshold}ms)")
            }
            
            // Reset for next launch
            isColdStart = false
        }
    }
    
    /**
     * Called when app goes to background
     */
    fun onAppBackgrounded() {
        Log.d(TAG, "App backgrounded")
        // Next launch will be warm start
        isColdStart = false
    }
    
    /**
     * Called when app comes back to foreground
     */
    fun onAppForegrounded() {
        Log.d(TAG, "App foregrounded")
        appStartTime = System.currentTimeMillis()
        firstActivityResumed = false
    }
    
    /**
     * Reset to cold start state (used when app is killed)
     */
    fun reset() {
        isColdStart = true
        firstActivityCreated = false
        firstActivityResumed = false
    }
    
    /**
     * Track time to interactive (when app is fully ready)
     */
    fun trackTimeToInteractive() {
        val timeToInteractive = System.currentTimeMillis() - appStartTime
        Log.i(TAG, "Time to interactive: ${timeToInteractive}ms")
        
        analyticsManager.trackEvent(
            AnalyticsEvent.PerformanceMetric(
                metricName = "time_to_interactive",
                durationMs = timeToInteractive,
                status = "success"
            )
        )
    }
}
