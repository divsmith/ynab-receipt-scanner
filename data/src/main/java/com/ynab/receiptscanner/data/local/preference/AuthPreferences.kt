package com.ynab.receiptscanner.data.local.preference

import com.ynab.receiptscanner.data.security.SecureStorage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages authentication-related preferences
 * Stores OAuth tokens, user info, and authentication state securely
 */
@Singleton
class AuthPreferences @Inject constructor(
    private val secureStorage: SecureStorage
) {
    
    /**
     * Store OAuth access token
     */
    suspend fun setAccessToken(token: String) {
        secureStorage.putString(KEY_ACCESS_TOKEN, token)
    }
    
    /**
     * Get OAuth access token
     */
    suspend fun getAccessToken(): String? {
        return secureStorage.getString(KEY_ACCESS_TOKEN)
    }
    
    /**
     * Store OAuth refresh token
     */
    suspend fun setRefreshToken(token: String) {
        secureStorage.putString(KEY_REFRESH_TOKEN, token)
    }
    
    /**
     * Get OAuth refresh token
     */
    suspend fun getRefreshToken(): String? {
        return secureStorage.getString(KEY_REFRESH_TOKEN)
    }
    
    /**
     * Store token expiration time (Unix timestamp in milliseconds)
     */
    suspend fun setTokenExpirationTime(expirationTime: Long) {
        secureStorage.putLong(KEY_TOKEN_EXPIRATION, expirationTime)
    }
    
    /**
     * Get token expiration time (Unix timestamp in milliseconds)
     */
    suspend fun getTokenExpirationTime(): Long {
        return secureStorage.getLong(KEY_TOKEN_EXPIRATION, 0L)
    }
    
    /**
     * Check if access token is expired
     */
    suspend fun isTokenExpired(): Boolean {
        val expirationTime = getTokenExpirationTime()
        return expirationTime > 0 && System.currentTimeMillis() >= expirationTime
    }
    
    /**
     * Store user ID
     */
    suspend fun setUserId(userId: String) {
        secureStorage.putString(KEY_USER_ID, userId)
    }
    
    /**
     * Get user ID
     */
    suspend fun getUserId(): String? {
        return secureStorage.getString(KEY_USER_ID)
    }
    
    /**
     * Store selected budget ID
     */
    suspend fun setSelectedBudgetId(budgetId: String) {
        secureStorage.putString(KEY_SELECTED_BUDGET_ID, budgetId)
    }
    
    /**
     * Get selected budget ID
     */
    suspend fun getSelectedBudgetId(): String? {
        return secureStorage.getString(KEY_SELECTED_BUDGET_ID)
    }
    
    /**
     * Store authentication state
     */
    suspend fun setAuthenticated(authenticated: Boolean) {
        secureStorage.putBoolean(KEY_IS_AUTHENTICATED, authenticated)
    }
    
    /**
     * Check if user is authenticated
     */
    suspend fun isAuthenticated(): Boolean {
        return secureStorage.getBoolean(KEY_IS_AUTHENTICATED, false)
    }
    
    /**
     * Store Personal Access Token (for testing/alternative auth)
     */
    suspend fun setPersonalAccessToken(token: String) {
        secureStorage.putString(KEY_PERSONAL_ACCESS_TOKEN, token)
    }
    
    /**
     * Get Personal Access Token
     */
    suspend fun getPersonalAccessToken(): String? {
        return secureStorage.getString(KEY_PERSONAL_ACCESS_TOKEN)
    }
    
    /**
     * Check if using Personal Access Token instead of OAuth
     */
    suspend fun isUsingPersonalAccessToken(): Boolean {
        val pat = getPersonalAccessToken()
        return !pat.isNullOrBlank()
    }
    
    /**
     * Store PKCE code verifier for OAuth flow
     */
    suspend fun setPKCECodeVerifier(codeVerifier: String) {
        secureStorage.putString(KEY_PKCE_CODE_VERIFIER, codeVerifier)
    }
    
    /**
     * Get PKCE code verifier
     */
    suspend fun getPKCECodeVerifier(): String? {
        return secureStorage.getString(KEY_PKCE_CODE_VERIFIER)
    }
    
    /**
     * Clear PKCE code verifier
     */
    suspend fun clearPKCECodeVerifier() {
        secureStorage.remove(KEY_PKCE_CODE_VERIFIER)
    }
    
    /**
     * Clear all authentication data
     */
    suspend fun clearAuthData() {
        secureStorage.remove(KEY_ACCESS_TOKEN)
        secureStorage.remove(KEY_REFRESH_TOKEN)
        secureStorage.remove(KEY_TOKEN_EXPIRATION)
        secureStorage.remove(KEY_USER_ID)
        secureStorage.remove(KEY_IS_AUTHENTICATED)
        secureStorage.remove(KEY_PERSONAL_ACCESS_TOKEN)
        secureStorage.remove(KEY_PKCE_CODE_VERIFIER)
        // Keep selected budget ID for convenience
    }
    
    /**
     * Clear all data including budget selection
     */
    suspend fun clearAll() {
        clearAuthData()
        secureStorage.remove(KEY_SELECTED_BUDGET_ID)
    }
    
    companion object {
        private const val KEY_ACCESS_TOKEN = "auth_access_token"
        private const val KEY_REFRESH_TOKEN = "auth_refresh_token"
        private const val KEY_TOKEN_EXPIRATION = "auth_token_expiration"
        private const val KEY_USER_ID = "auth_user_id"
        private const val KEY_PERSONAL_ACCESS_TOKEN = "auth_personal_access_token"
        private const val KEY_PKCE_CODE_VERIFIER = "auth_pkce_code_verifier"
        private const val KEY_SELECTED_BUDGET_ID = "auth_selected_budget_id"
        private const val KEY_IS_AUTHENTICATED = "auth_is_authenticated"
    }
}
