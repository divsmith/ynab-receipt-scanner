package com.ynab.receiptscanner.data.auth

import android.net.Uri
import android.util.Base64
import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.data.di.BaseOkHttpClient
import com.ynab.receiptscanner.data.local.preference.AuthPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages OAuth 2.0 authentication flow with YNAB API
 * Implements PKCE (Proof Key for Code Exchange) for security
 */
@Singleton
class YnabAuthManager @Inject constructor(
    private val authPreferences: AuthPreferences,
    @BaseOkHttpClient private val okHttpClient: OkHttpClient,
    private val moshi: Moshi
) {
    
    companion object {
        private const val TAG = "YnabAuthManager"
        private const val CODE_VERIFIER_LENGTH = 128
        private const val STATE_LENGTH = 32
    }
    
    /**
     * Generate OAuth authorization URL with PKCE
     * @return Authorization URL to open in browser
     */
    suspend fun generateAuthorizationUrl(): String {
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)
        val state = generateState()
        
        // Store code verifier for later use in token exchange
        authPreferences.setPKCECodeVerifier(codeVerifier)
        
        return Uri.parse(OAuthConfig.AUTHORIZE_URL).buildUpon()
            .appendQueryParameter("client_id", OAuthConfig.CLIENT_ID)
            .appendQueryParameter("redirect_uri", OAuthConfig.REDIRECT_URI)
            .appendQueryParameter("response_type", OAuthConfig.RESPONSE_TYPE)
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("state", state)
            .build()
            .toString()
    }
    
    /**
     * Exchange authorization code for access token
     * @param authorizationCode Authorization code from callback
     * @return Result with success or error
     */
    suspend fun exchangeCodeForToken(authorizationCode: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val codeVerifier = authPreferences.getPKCECodeVerifier()
                if (codeVerifier.isNullOrBlank()) {
                    return@withContext Result.Error(Exception("Code verifier not found"))
                }
                
                val requestBody = FormBody.Builder()
                    .add("grant_type", "authorization_code")
                    .add("code", authorizationCode)
                    .add("redirect_uri", OAuthConfig.REDIRECT_URI)
                    .add("client_id", OAuthConfig.CLIENT_ID)
                    .add("code_verifier", codeVerifier)
                    .build()
                
                val request = Request.Builder()
                    .url(OAuthConfig.TOKEN_URL)
                    .post(requestBody)
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (!response.isSuccessful || responseBody == null) {
                    Log.e(TAG, "Token exchange failed: ${response.code}")
                    return@withContext Result.Error(Exception("Failed to exchange code for token: ${response.code}"))
                }
                
                val tokenResponse = moshi.adapter(TokenResponse::class.java).fromJson(responseBody)
                if (tokenResponse == null) {
                    return@withContext Result.Error(Exception("Failed to parse token response"))
                }
                
                // Store tokens
                authPreferences.setAccessToken(tokenResponse.accessToken)
                if (!tokenResponse.refreshToken.isNullOrBlank()) {
                    authPreferences.setRefreshToken(tokenResponse.refreshToken)
                }
                
                // Calculate and store expiration time
                val expirationTime = System.currentTimeMillis() + (tokenResponse.expiresIn * 1000)
                authPreferences.setTokenExpirationTime(expirationTime)
                authPreferences.setAuthenticated(true)
                
                // Clear code verifier
                authPreferences.clearPKCECodeVerifier()
                
                Log.d(TAG, "Successfully exchanged code for token")
                Result.Success(Unit)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error exchanging code for token", e)
                Result.Error(e)
            }
        }
    }
    
    /**
     * Refresh expired access token
     * @return Result with success or error
     */
    suspend fun refreshToken(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val refreshToken = authPreferences.getRefreshToken()
                if (refreshToken.isNullOrBlank()) {
                    return@withContext Result.Error(Exception("No refresh token available"))
                }
                
                val requestBody = FormBody.Builder()
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", refreshToken)
                    .add("client_id", OAuthConfig.CLIENT_ID)
                    .build()
                
                val request = Request.Builder()
                    .url(OAuthConfig.TOKEN_URL)
                    .post(requestBody)
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (!response.isSuccessful || responseBody == null) {
                    Log.e(TAG, "Token refresh failed: ${response.code}")
                    
                    // If refresh token is invalid (401), clear auth data
                    if (response.code == 401) {
                        authPreferences.clearAuthData()
                    }
                    
                    return@withContext Result.Error(Exception("Failed to refresh token: ${response.code}"))
                }
                
                val tokenResponse = moshi.adapter(TokenResponse::class.java).fromJson(responseBody)
                if (tokenResponse == null) {
                    return@withContext Result.Error(Exception("Failed to parse token response"))
                }
                
                // Store new tokens
                authPreferences.setAccessToken(tokenResponse.accessToken)
                if (!tokenResponse.refreshToken.isNullOrBlank()) {
                    authPreferences.setRefreshToken(tokenResponse.refreshToken)
                }
                
                // Calculate and store expiration time
                val expirationTime = System.currentTimeMillis() + (tokenResponse.expiresIn * 1000)
                authPreferences.setTokenExpirationTime(expirationTime)
                
                Log.d(TAG, "Successfully refreshed token")
                Result.Success(Unit)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing token", e)
                Result.Error(e)
            }
        }
    }
    
    /**
     * Check if token needs refresh
     * @return true if token should be refreshed
     */
    suspend fun shouldRefreshToken(): Boolean {
        val expirationTime = authPreferences.getTokenExpirationTime()
        if (expirationTime == 0L) return false
        
        val bufferMs = OAuthConfig.TOKEN_REFRESH_BUFFER_SECONDS * 1000
        return System.currentTimeMillis() >= (expirationTime - bufferMs)
    }
    
    /**
     * Revoke access token and sign out
     * @return Result with success or error
     */
    suspend fun revokeToken(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val accessToken = authPreferences.getAccessToken()
                if (accessToken.isNullOrBlank()) {
                    // No token to revoke, just clear local data
                    authPreferences.clearAuthData()
                    return@withContext Result.Success(Unit)
                }
                
                val requestBody = FormBody.Builder()
                    .add("token", accessToken)
                    .add("client_id", OAuthConfig.CLIENT_ID)
                    .build()
                
                val request = Request.Builder()
                    .url(OAuthConfig.REVOKE_URL)
                    .post(requestBody)
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                
                // Clear local data regardless of revocation result
                authPreferences.clearAuthData()
                
                if (!response.isSuccessful) {
                    Log.w(TAG, "Token revocation returned ${response.code}, but local data cleared")
                } else {
                    Log.d(TAG, "Successfully revoked token")
                }
                
                Result.Success(Unit)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error revoking token", e)
                // Still clear local data
                authPreferences.clearAuthData()
                Result.Error(e)
            }
        }
    }
    
    /**
     * Generate a cryptographically secure code verifier for PKCE
     */
    private fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(CODE_VERIFIER_LENGTH)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            .take(CODE_VERIFIER_LENGTH)
    }
    
    /**
     * Generate code challenge from code verifier using SHA-256
     */
    private fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray(Charsets.US_ASCII)
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val digest = messageDigest.digest(bytes)
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
    
    /**
     * Generate a random state parameter for CSRF protection
     */
    private fun generateState(): String {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(STATE_LENGTH)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
    
    /**
     * Token response from YNAB OAuth
     */
    @JsonClass(generateAdapter = true)
    data class TokenResponse(
        @Json(name = "access_token")
        val accessToken: String,
        @Json(name = "token_type")
        val tokenType: String,
        @Json(name = "expires_in")
        val expiresIn: Long,
        @Json(name = "refresh_token")
        val refreshToken: String? = null
    )
}
