package com.ynab.receiptscanner.performance

import android.util.Log
import com.ynab.receiptscanner.data.analytics.AnalyticsEvent
import com.ynab.receiptscanner.data.analytics.AnalyticsManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

/**
 * Performance monitoring utility
 * Tracks and reports performance metrics for critical operations
 */
@Singleton
class PerformanceMonitor @Inject constructor(
    private val analyticsManager: AnalyticsManager
) {
    
    companion object {
        private const val TAG = "PerformanceMonitor"
        
        // Performance thresholds (milliseconds)
        private const val THRESHOLD_OCR = 5000L // 5 seconds
        private const val THRESHOLD_IMAGE_PROCESSING = 3000L // 3 seconds
        private const val THRESHOLD_API_CALL = 10000L // 10 seconds
        private const val THRESHOLD_DATABASE = 1000L // 1 second
    }
    
    /**
     * Measure execution time of a block
     */
    inline fun <T> measure(
        operationName: String,
        threshold: Long? = null,
        block: () -> T
    ): T {
        var result: T
        val duration = measureTimeMillis {
            result = block()
        }
        
        logMetric(operationName, duration, threshold)
        return result
    }
    
    /**
     * Measure execution time of a suspend block
     */
    suspend inline fun <T> measureSuspend(
        operationName: String,
        threshold: Long? = null,
        crossinline block: suspend () -> T
    ): T {
        val startTime = System.currentTimeMillis()
        val result = block()
        val duration = System.currentTimeMillis() - startTime
        
        logMetric(operationName, duration, threshold)
        return result
    }
    
    /**
     * Start timing an operation
     */
    fun start(operationName: String): OperationTimer {
        return OperationTimer(operationName, this)
    }
    
    /**
     * Log performance metric
     */
    private fun logMetric(operationName: String, durationMs: Long, threshold: Long?) {
        val effectiveThreshold = threshold ?: getDefaultThreshold(operationName)
        val status = if (durationMs < effectiveThreshold) "success" else "slow"
        
        Log.d(TAG, "$operationName: ${durationMs}ms (threshold: ${effectiveThreshold}ms) [$status]")
        
        // Log to analytics if operation is slow
        if (durationMs >= effectiveThreshold) {
            Log.w(TAG, "SLOW: $operationName took ${durationMs}ms (threshold: ${effectiveThreshold}ms)")
            analyticsManager.trackEvent(
                AnalyticsEvent.PerformanceMetric(
                    metricName = operationName,
                    durationMs = durationMs,
                    status = status
                )
            )
        }
    }
    
    /**
     * Get default threshold for operation type
     */
    private fun getDefaultThreshold(operationName: String): Long {
        return when {
            operationName.contains("ocr", ignoreCase = true) -> THRESHOLD_OCR
            operationName.contains("image", ignoreCase = true) ||
            operationName.contains("preprocess", ignoreCase = true) -> THRESHOLD_IMAGE_PROCESSING
            operationName.contains("api", ignoreCase = true) ||
            operationName.contains("network", ignoreCase = true) -> THRESHOLD_API_CALL
            operationName.contains("database", ignoreCase = true) ||
            operationName.contains("db", ignoreCase = true) -> THRESHOLD_DATABASE
            else -> 1000L // Default 1 second
        }
    }
    
    /**
     * Track OCR performance
     */
    fun trackOcrPerformance(durationMs: Long, success: Boolean) {
        Log.d(TAG, "OCR Performance: ${durationMs}ms (success: $success)")
        
        if (durationMs > THRESHOLD_OCR) {
            Log.w(TAG, "Slow OCR: ${durationMs}ms")
        }
        
        analyticsManager.trackEvent(
            AnalyticsEvent.PerformanceMetric(
                metricName = "ocr_processing",
                durationMs = durationMs,
                status = if (success) "success" else "failure"
            )
        )
    }
    
    /**
     * Track image preprocessing performance
     */
    fun trackImagePreprocessing(durationMs: Long) {
        Log.d(TAG, "Image Preprocessing: ${durationMs}ms")
        
        if (durationMs > THRESHOLD_IMAGE_PROCESSING) {
            Log.w(TAG, "Slow image preprocessing: ${durationMs}ms")
        }
        
        analyticsManager.trackEvent(
            AnalyticsEvent.PerformanceMetric(
                metricName = "image_preprocessing",
                durationMs = durationMs,
                status = if (durationMs < THRESHOLD_IMAGE_PROCESSING) "success" else "slow"
            )
        )
    }
    
    /**
     * Track API call performance
     */
    fun trackApiCall(endpoint: String, durationMs: Long, success: Boolean) {
        Log.d(TAG, "API Call [$endpoint]: ${durationMs}ms (success: $success)")
        
        if (durationMs > THRESHOLD_API_CALL) {
            Log.w(TAG, "Slow API call [$endpoint]: ${durationMs}ms")
        }
        
        analyticsManager.trackEvent(
            AnalyticsEvent.PerformanceMetric(
                metricName = "api_${endpoint.replace("/", "_")}",
                durationMs = durationMs,
                status = if (success) "success" else "failure"
            )
        )
    }
    
    /**
     * Track database operation performance
     */
    fun trackDatabaseOperation(operation: String, durationMs: Long) {
        Log.d(TAG, "Database [$operation]: ${durationMs}ms")
        
        if (durationMs > THRESHOLD_DATABASE) {
            Log.w(TAG, "Slow database operation [$operation]: ${durationMs}ms")
        }
        
        analyticsManager.trackEvent(
            AnalyticsEvent.PerformanceMetric(
                metricName = "db_$operation",
                durationMs = durationMs,
                status = if (durationMs < THRESHOLD_DATABASE) "success" else "slow"
            )
        )
    }
    
    /**
     * Operation timer for manual timing
     */
    class OperationTimer internal constructor(
        private val operationName: String,
        private val monitor: PerformanceMonitor
    ) {
        private val startTime = System.currentTimeMillis()
        
        fun stop(threshold: Long? = null) {
            val duration = System.currentTimeMillis() - startTime
            monitor.logMetric(operationName, duration, threshold)
        }
    }
}
