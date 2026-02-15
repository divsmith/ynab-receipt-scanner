package com.ynab.receiptscanner.ui.common

import android.content.Context
import com.ynab.receiptscanner.R
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized error handling utility
 * Converts exceptions to user-friendly messages and provides retry strategies
 */
@Singleton
class ErrorHandler @Inject constructor(
    private val context: Context
) {
    
    /**
     * Specific error types for better error handling
     */
    sealed class AppError(
        val message: String,
        val userMessage: String,
        val canRetry: Boolean = false,
        val retryDelayMs: Long = 0
    ) {
        data class NetworkError(
            val exception: Throwable,
            override val userMessage: String = "Network error. Please check your connection.",
            override val canRetry: Boolean = true,
            override val retryDelayMs: Long = 2000
        ) : AppError(
            message = exception.message ?: "Network error",
            userMessage = userMessage,
            canRetry = canRetry,
            retryDelayMs = retryDelayMs
        )
        
        data class AuthError(
            val exception: Throwable,
            override val userMessage: String = "Authentication failed. Please log in again.",
            override val canRetry: Boolean = false
        ) : AppError(
            message = exception.message ?: "Authentication failed",
            userMessage = userMessage,
            canRetry = canRetry
        )
        
        data class OcrError(
            val exception: Throwable,
            override val userMessage: String = "Failed to read receipt. Please try again or enter details manually.",
            override val canRetry: Boolean = true,
            override val retryDelayMs: Long = 1000
        ) : AppError(
            message = exception.message ?: "OCR failed",
            userMessage = userMessage,
            canRetry = canRetry,
            retryDelayMs = retryDelayMs
        )
        
        data class ValidationError(
            val field: String,
            val reason: String,
            override val userMessage: String = "Invalid $field: $reason",
            override val canRetry: Boolean = false
        ) : AppError(
            message = "Validation error: $field - $reason",
            userMessage = userMessage,
            canRetry = canRetry
        )
        
        data class RateLimitError(
            val retryAfterSeconds: Int = 60,
            override val userMessage: String = "Rate limit exceeded. Please try again in $retryAfterSeconds seconds.",
            override val canRetry: Boolean = true,
            override val retryDelayMs: Long = retryAfterSeconds * 1000L
        ) : AppError(
            message = "Rate limit exceeded",
            userMessage = userMessage,
            canRetry = canRetry,
            retryDelayMs = retryDelayMs
        )
        
        data class ServerError(
            val statusCode: Int,
            val exception: Throwable,
            override val userMessage: String = "Server error. Please try again later.",
            override val canRetry: Boolean = true,
            override val retryDelayMs: Long = 5000
        ) : AppError(
            message = "Server error: $statusCode",
            userMessage = userMessage,
            canRetry = canRetry,
            retryDelayMs = retryDelayMs
        )
        
        data class StorageError(
            val exception: Throwable,
            override val userMessage: String = "Storage error. Please check available space.",
            override val canRetry: Boolean = true,
            override val retryDelayMs: Long = 1000
        ) : AppError(
            message = exception.message ?: "Storage error",
            userMessage = userMessage,
            canRetry = canRetry,
            retryDelayMs = retryDelayMs
        )
        
        data class UnknownError(
            val exception: Throwable,
            override val userMessage: String = "An unexpected error occurred.",
            override val canRetry: Boolean = true,
            override val retryDelayMs: Long = 2000
        ) : AppError(
            message = exception.message ?: "Unknown error",
            userMessage = userMessage,
            canRetry = canRetry,
            retryDelayMs = retryDelayMs
        )
    }
    
    /**
     * Map throwable to specific AppError type
     */
    fun mapError(throwable: Throwable): AppError {
        return when (throwable) {
            is UnknownHostException,
            is SocketTimeoutException -> AppError.NetworkError(throwable)
            
            is HttpException -> {
                when (throwable.code()) {
                    401, 403 -> AppError.AuthError(throwable)
                    429 -> {
                        val retryAfter = throwable.response()?.headers()?.get("Retry-After")?.toIntOrNull() ?: 60
                        AppError.RateLimitError(retryAfter)
                    }
                    in 500..599 -> AppError.ServerError(throwable.code(), throwable)
                    else -> AppError.UnknownError(throwable)
                }
            }
            
            is IOException -> AppError.NetworkError(throwable, "Connection error. Please try again.")
            
            is IllegalArgumentException -> AppError.ValidationError(
                field = "input",
                reason = throwable.message ?: "invalid value"
            )
            
            else -> AppError.UnknownError(throwable)
        }
    }
    
    /**
     * Get user-friendly error message
     */
    fun getErrorMessage(throwable: Throwable): String {
        return mapError(throwable).userMessage
    }
    
    /**
     * Get error message resource ID
     */
    fun getErrorMessageResource(throwable: Throwable): Int {
        return when (throwable) {
            is UnknownHostException,
            is SocketTimeoutException -> R.string.error_network
            is HttpException -> when (throwable.code()) {
                401, 403 -> R.string.error_auth
                429 -> R.string.error_rate_limit
                in 500..599 -> R.string.error_server
                else -> R.string.error_unknown
            }
            else -> R.string.error_unknown
        }
    }
    
    /**
     * Check if error is retryable
     */
    fun canRetry(throwable: Throwable): Boolean {
        return mapError(throwable).canRetry
    }
    
    /**
     * Get retry delay for error
     */
    fun getRetryDelay(throwable: Throwable): Long {
        return mapError(throwable).retryDelayMs
    }
    
    /**
     * Handle error and log to analytics
     */
    fun handleError(throwable: Throwable, context: String = "unknown") {
        val appError = mapError(throwable)
        android.util.Log.e("ErrorHandler", "[$context] ${appError.message}", throwable)
        
        // Log to analytics (will be implemented)
        // analyticsManager.logError(context, appError)
    }
}
