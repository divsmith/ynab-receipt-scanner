package com.ynab.receiptscanner.data.auth

import android.util.Log
import com.ynab.receiptscanner.data.local.preference.AuthPreferences
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp Authenticator for automatic token refresh on 401 responses
 * When a request fails with 401, this will attempt to refresh the token
 * and retry the request with the new token
 */
@Singleton
class TokenRefreshAuthenticator @Inject constructor(
    private val authManager: YnabAuthManager,
    private val authPreferences: AuthPreferences
) : Authenticator {
    
    companion object {
        private const val TAG = "TokenRefreshAuthenticator"
        private const val MAX_REFRESH_ATTEMPTS = 3
        private const val HEADER_AUTHORIZATION = "Authorization"
    }
    
    override fun authenticate(route: Route?, response: Response): Request? {
        // Check if we're using Personal Access Token (which doesn't expire)
        val usingPAT = runBlocking {
            authPreferences.isUsingPersonalAccessToken()
        }
        
        if (usingPAT) {
            // Personal Access Tokens don't expire, so 401 means it's invalid
            Log.w(TAG, "Personal Access Token appears to be invalid")
            return null // Don't retry
        }
        
        // Check if this is a retry (to prevent infinite loop)
        val retryCount = response.request.header("X-Retry-Count")?.toIntOrNull() ?: 0
        if (retryCount >= MAX_REFRESH_ATTEMPTS) {
            Log.e(TAG, "Max refresh attempts reached")
            return null
        }
        
        // Synchronize to prevent multiple simultaneous refresh attempts
        synchronized(this) {
            // Get current token
            val currentToken = runBlocking {
                authPreferences.getAccessToken()
            }
            
            // Check if token has already been refreshed by another request
            val responseToken = response.request.header(HEADER_AUTHORIZATION)?.removePrefix("Bearer ")
            if (responseToken != currentToken) {
                // Token has been refreshed, retry with new token
                return buildRetryRequest(response.request, currentToken, retryCount)
            }
            
            // Attempt to refresh token
            val refreshResult = runBlocking {
                authManager.refreshToken()
            }
            
            return when (refreshResult) {
                is com.ynab.receiptscanner.core.util.Result.Success -> {
                    val newToken = runBlocking {
                        authPreferences.getAccessToken()
                    }
                    Log.d(TAG, "Token refreshed successfully, retrying request")
                    buildRetryRequest(response.request, newToken, retryCount + 1)
                }
                is com.ynab.receiptscanner.core.util.Result.Error -> {
                    Log.e(TAG, "Failed to refresh token: ${refreshResult.exception.message}")
                    null // Don't retry
                }
            }
        }
    }
    
    /**
     * Build a retry request with new token and increased retry count
     */
    private fun buildRetryRequest(original: Request, newToken: String?, retryCount: Int): Request {
        return if (newToken != null) {
            original.newBuilder()
                .header(HEADER_AUTHORIZATION, "Bearer $newToken")
                .header("X-Retry-Count", retryCount.toString())
                .build()
        } else {
            original
        }
    }
}
