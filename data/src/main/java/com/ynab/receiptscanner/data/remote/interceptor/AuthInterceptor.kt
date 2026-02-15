package com.ynab.receiptscanner.data.remote.interceptor

import com.ynab.receiptscanner.data.local.preference.AuthPreferences
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor to add authentication token to requests
 * Adds "Authorization: Bearer {token}" header to all YNAB API requests
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val authPreferences: AuthPreferences
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        
        // Get the access token (or personal access token)
        val token = runBlocking {
            authPreferences.getAccessToken() ?: authPreferences.getPersonalAccessToken()
        }
        
        // If no token is available, proceed without authentication
        if (token.isNullOrBlank()) {
            return chain.proceed(original)
        }
        
        // Add Authorization header
        val request = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .method(original.method, original.body)
            .build()
        
        return chain.proceed(request)
    }
}
