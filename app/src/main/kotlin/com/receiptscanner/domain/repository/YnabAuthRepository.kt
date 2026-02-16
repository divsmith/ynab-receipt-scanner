package com.receiptscanner.domain.repository

import com.receiptscanner.domain.model.YnabToken

/**
 * Repository interface for YNAB OAuth 2.0 authentication
 */
interface YnabAuthRepository {
    
    /**
     * Generates the OAuth authorization URL for YNAB
     * @param clientId The YNAB client ID
     * @param redirectUri The OAuth redirect URI
     * @param state CSRF protection state parameter
     * @return The complete authorization URL
     */
    fun getAuthorizationUrl(
        clientId: String,
        redirectUri: String,
        state: String
    ): String
    
    /**
     * Exchanges an authorization code for access and refresh tokens
     * @param code The authorization code from OAuth callback
     * @param clientId The YNAB client ID
     * @param clientSecret The YNAB client secret
     * @param redirectUri The OAuth redirect URI
     * @return Result with the token or error
     */
    suspend fun exchangeCodeForToken(
        code: String,
        clientId: String,
        clientSecret: String,
        redirectUri: String
    ): Result<YnabToken>
    
    /**
     * Refreshes an expired access token
     * @param refreshToken The refresh token
     * @param clientId The YNAB client ID
     * @param clientSecret The YNAB client secret
     * @return Result with the new token or error
     */
    suspend fun refreshToken(
        refreshToken: String,
        clientId: String,
        clientSecret: String
    ): Result<YnabToken>
    
    /**
     * Gets the currently stored token
     * @return The stored token or null
     */
    suspend fun getStoredToken(): YnabToken?
    
    /**
     * Saves a token securely
     * @param token The token to save
     * @return true if saved successfully
     */
    suspend fun saveToken(token: YnabToken): Boolean
    
    /**
     * Deletes the stored token (logout)
     */
    suspend fun deleteToken()
    
    /**
     * Checks if a valid (non-expired) token exists
     * @return true if a valid token exists
     */
    suspend fun hasValidToken(): Boolean
}
