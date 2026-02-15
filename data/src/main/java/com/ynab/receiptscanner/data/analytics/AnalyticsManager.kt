package com.ynab.receiptscanner.data.analytics

/**
 * Analytics abstraction layer
 * Provides interface for tracking events without coupling to specific analytics provider
 */
interface AnalyticsManager {
    
    /**
     * Track an analytics event
     */
    fun trackEvent(event: AnalyticsEvent)
    
    /**
     * Set user property (non-PII only)
     * Example: user preferences, settings, feature flags
     */
    fun setUserProperty(key: String, value: String)
    
    /**
     * Log a screen view
     */
    fun logScreenView(screenName: String, screenClass: String)
    
    /**
     * Set current screen (for automatic tracking)
     */
    fun setCurrentScreen(screenName: String, screenClass: String)
    
    /**
     * Enable/disable analytics collection
     */
    fun setAnalyticsEnabled(enabled: Boolean)
    
    /**
     * Check if analytics is enabled
     */
    fun isAnalyticsEnabled(): Boolean
}

/**
 * No-op implementation for when analytics is disabled or not configured
 */
class NoOpAnalyticsManager : AnalyticsManager {
    override fun trackEvent(event: AnalyticsEvent) {
        // No-op
    }
    
    override fun setUserProperty(key: String, value: String) {
        // No-op
    }
    
    override fun logScreenView(screenName: String, screenClass: String) {
        // No-op
    }
    
    override fun setCurrentScreen(screenName: String, screenClass: String) {
        // No-op
    }
    
    override fun setAnalyticsEnabled(enabled: Boolean) {
        // No-op
    }
    
    override fun isAnalyticsEnabled(): Boolean = false
}
