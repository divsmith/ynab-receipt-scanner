package com.ynab.receiptscanner.data.analytics

/**
 * Sealed class representing all trackable events in the app
 * Privacy-first: No PII (Personally Identifiable Information) is tracked
 */
sealed class AnalyticsEvent(
    val eventName: String,
    val parameters: Map<String, Any> = emptyMap()
) {
    
    // Receipt Scanning Events
    data class ReceiptScanned(
        val processingTimeMs: Long,
        val imageSize: Long,
        val success: Boolean
    ) : AnalyticsEvent(
        eventName = "receipt_scanned",
        parameters = mapOf(
            "processing_time_ms" to processingTimeMs,
            "image_size_bytes" to imageSize,
            "success" to success
        )
    )
    
    data class OcrCompleted(
        val extractionTimeMs: Long,
        val fieldsExtracted: Int,
        val confidence: Float
    ) : AnalyticsEvent(
        eventName = "ocr_completed",
        parameters = mapOf(
            "extraction_time_ms" to extractionTimeMs,
            "fields_extracted" to fieldsExtracted,
            "confidence" to confidence
        )
    )
    
    data class OcrFieldEdited(
        val fieldName: String,
        val wasEmpty: Boolean
    ) : AnalyticsEvent(
        eventName = "ocr_field_edited",
        parameters = mapOf(
            "field_name" to fieldName,
            "was_empty" to wasEmpty
        )
    )
    
    // Transaction Events
    data class TransactionSubmitted(
        val syncMode: String, // "immediate" or "offline"
        val hasCategory: Boolean,
        val hasMemo: Boolean
    ) : AnalyticsEvent(
        eventName = "transaction_submitted",
        parameters = mapOf(
            "sync_mode" to syncMode,
            "has_category" to hasCategory,
            "has_memo" to hasMemo
        )
    )
    
    data class TransactionSyncCompleted(
        val successCount: Int,
        val failureCount: Int,
        val totalTimeMs: Long
    ) : AnalyticsEvent(
        eventName = "transaction_sync_completed",
        parameters = mapOf(
            "success_count" to successCount,
            "failure_count" to failureCount,
            "total_time_ms" to totalTimeMs
        )
    )
    
    // Authentication Events
    data class UserLoggedIn(
        val method: String // "oauth", "token"
    ) : AnalyticsEvent(
        eventName = "user_logged_in",
        parameters = mapOf(
            "method" to method
        )
    )
    
    object UserLoggedOut : AnalyticsEvent(
        eventName = "user_logged_out"
    )
    
    // Error Events
    data class ErrorOccurred(
        val errorType: String,
        val errorMessage: String,
        val screen: String,
        val canRetry: Boolean
    ) : AnalyticsEvent(
        eventName = "error_occurred",
        parameters = mapOf(
            "error_type" to errorType,
            "error_message" to errorMessage,
            "screen" to screen,
            "can_retry" to canRetry
        )
    )
    
    // Performance Events
    data class PerformanceMetric(
        val metricName: String,
        val durationMs: Long,
        val status: String // "success", "failure", "timeout"
    ) : AnalyticsEvent(
        eventName = "performance_metric",
        parameters = mapOf(
            "metric_name" to metricName,
            "duration_ms" to durationMs,
            "status" to status
        )
    )
    
    // Navigation Events
    data class ScreenViewed(
        val screenName: String,
        val source: String? = null
    ) : AnalyticsEvent(
        eventName = "screen_viewed",
        parameters = buildMap {
            put("screen_name", screenName)
            source?.let { put("source", it) }
        }
    )
    
    // Settings Events
    data class SettingChanged(
        val settingName: String,
        val newValue: String
    ) : AnalyticsEvent(
        eventName = "setting_changed",
        parameters = mapOf(
            "setting_name" to settingName,
            "new_value" to newValue
        )
    )
    
    // Feature Usage Events
    data class FeatureUsed(
        val featureName: String,
        val context: String? = null
    ) : AnalyticsEvent(
        eventName = "feature_used",
        parameters = buildMap {
            put("feature_name", featureName)
            context?.let { put("context", it) }
        }
    )
    
    // App Lifecycle Events
    data class AppLaunched(
        val isFirstLaunch: Boolean,
        val coldStart: Boolean
    ) : AnalyticsEvent(
        eventName = "app_launched",
        parameters = mapOf(
            "is_first_launch" to isFirstLaunch,
            "cold_start" to coldStart
        )
    )
    
    // Budget/Account Selection Events
    data class BudgetSelected(
        val budgetName: String // Hashed for privacy
    ) : AnalyticsEvent(
        eventName = "budget_selected",
        parameters = mapOf(
            "budget_name_hash" to budgetName
        )
    )
}
