package com.ynab.receiptscanner.data.remote.interceptor

import android.util.Log
import com.squareup.moshi.Moshi
import com.ynab.receiptscanner.data.remote.dto.YnabErrorResponse
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor to handle YNAB API errors
 * Parses error responses and provides meaningful error messages
 * Handles rate limiting (429) and unauthorized (401) responses
 */
@Singleton
class ErrorInterceptor @Inject constructor(
    private val moshi: Moshi
) : Interceptor {
    
    companion object {
        private const val TAG = "ErrorInterceptor"
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403
        private const val HTTP_NOT_FOUND = 404
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private const val HTTP_INTERNAL_SERVER_ERROR = 500
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        
        // If response is successful, return as is
        if (response.isSuccessful) {
            return response
        }
        
        // Parse error response
        val errorBody = response.body?.string()
        val errorMessage = parseErrorMessage(errorBody, response.code)
        
        Log.e(TAG, "API Error ${response.code}: $errorMessage")
        
        // Handle specific error codes
        when (response.code) {
            HTTP_UNAUTHORIZED -> {
                Log.w(TAG, "Unauthorized - Token may be expired or invalid")
            }
            HTTP_TOO_MANY_REQUESTS -> {
                val retryAfter = response.header("Retry-After")
                Log.w(TAG, "Rate limited - Retry after: $retryAfter seconds")
            }
            HTTP_FORBIDDEN -> {
                Log.w(TAG, "Forbidden - Check API permissions")
            }
            HTTP_NOT_FOUND -> {
                Log.w(TAG, "Resource not found")
            }
            HTTP_INTERNAL_SERVER_ERROR -> {
                Log.e(TAG, "YNAB server error")
            }
        }
        
        // Rebuild response with parsed error message (for logging/debugging)
        return response.newBuilder()
            .body(errorBody?.toResponseBody(response.body?.contentType()))
            .build()
    }
    
    /**
     * Parse YNAB error response format
     */
    private fun parseErrorMessage(errorBody: String?, statusCode: Int): String {
        if (errorBody.isNullOrBlank()) {
            return getDefaultErrorMessage(statusCode)
        }
        
        return try {
            val adapter = moshi.adapter(YnabErrorResponse::class.java)
            val errorResponse = adapter.fromJson(errorBody)
            errorResponse?.error?.detail ?: getDefaultErrorMessage(statusCode)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse error response", e)
            getDefaultErrorMessage(statusCode)
        }
    }
    
    /**
     * Get default error message for status code
     */
    private fun getDefaultErrorMessage(statusCode: Int): String {
        return when (statusCode) {
            HTTP_UNAUTHORIZED -> "Authentication failed. Please sign in again."
            HTTP_FORBIDDEN -> "Access denied. Check your API permissions."
            HTTP_NOT_FOUND -> "The requested resource was not found."
            HTTP_TOO_MANY_REQUESTS -> "Too many requests. Please try again later."
            HTTP_INTERNAL_SERVER_ERROR -> "Server error. Please try again later."
            in 500..599 -> "Server error occurred. Please try again later."
            else -> "An error occurred (HTTP $statusCode)"
        }
    }
}
