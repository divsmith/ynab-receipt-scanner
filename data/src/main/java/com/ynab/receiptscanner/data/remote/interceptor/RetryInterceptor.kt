package com.ynab.receiptscanner.data.remote.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Automatic retry interceptor for transient network failures
 * Implements exponential backoff for retries
 */
@Singleton
class RetryInterceptor @Inject constructor() : Interceptor {
    
    companion object {
        private const val TAG = "RetryInterceptor"
        private const val MAX_RETRIES = 3
        private const val INITIAL_BACKOFF_MS = 1000L
        private const val MAX_BACKOFF_MS = 10000L
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var exception: IOException? = null
        var tryCount = 0
        
        while (tryCount < MAX_RETRIES) {
            try {
                // Clear previous response
                response?.close()
                response = null
                exception = null
                
                // Attempt request
                response = chain.proceed(request)
                
                // Check if response is successful or should not be retried
                if (response.isSuccessful || !shouldRetry(response.code)) {
                    return response
                }
                
                // Close response before retry
                response.close()
                
            } catch (e: IOException) {
                exception = e
                Log.w(TAG, "Request failed (attempt ${tryCount + 1}/$MAX_RETRIES): ${e.message}")
                
                // Don't retry if it's not a transient error
                if (!isRetryableException(e)) {
                    throw e
                }
            }
            
            tryCount++
            
            // If we've exhausted retries, throw exception or return error response
            if (tryCount >= MAX_RETRIES) {
                exception?.let { throw it }
                return response ?: throw IOException("Max retries exceeded")
            }
            
            // Calculate backoff delay with exponential increase
            val backoffDelay = calculateBackoff(tryCount)
            Log.d(TAG, "Retrying in ${backoffDelay}ms (attempt ${tryCount + 1}/$MAX_RETRIES)")
            
            try {
                Thread.sleep(backoffDelay)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IOException("Retry interrupted", e)
            }
        }
        
        // Should never reach here
        return response ?: throw IOException("Unexpected retry state")
    }
    
    /**
     * Check if HTTP status code should trigger retry
     */
    private fun shouldRetry(statusCode: Int): Boolean {
        return when (statusCode) {
            // Retry on server errors (5xx)
            in 500..599 -> true
            
            // Retry on rate limiting (with backoff)
            429 -> true
            
            // Retry on request timeout
            408 -> true
            
            // Don't retry on client errors (4xx)
            in 400..499 -> false
            
            else -> false
        }
    }
    
    /**
     * Check if exception is retryable
     */
    private fun isRetryableException(exception: IOException): Boolean {
        return when (exception) {
            is SocketTimeoutException -> true
            is UnknownHostException -> true
            else -> {
                // Check exception message for common transient errors
                val message = exception.message?.lowercase() ?: ""
                message.contains("timeout") ||
                message.contains("connection reset") ||
                message.contains("broken pipe") ||
                message.contains("socket closed")
            }
        }
    }
    
    /**
     * Calculate exponential backoff delay
     */
    private fun calculateBackoff(attemptNumber: Int): Long {
        val exponentialDelay = INITIAL_BACKOFF_MS * (1 shl (attemptNumber - 1))
        return exponentialDelay.coerceAtMost(MAX_BACKOFF_MS)
    }
}
