package com.ynab.receiptscanner.data.auth

import com.ynab.receiptscanner.core.Constants

/**
 * OAuth 2.0 configuration for YNAB API
 */
object OAuthConfig {
    
    /**
     * OAuth client ID - Replace with your registered app client ID
     * Register your app at: https://app.youneedabudget.com/settings/developer
     */
    const val CLIENT_ID = Constants.OAUTH_CLIENT_ID
    
    /**
     * OAuth redirect URI
     * Must match the redirect URI registered in your YNAB app settings
     */
    const val REDIRECT_URI = Constants.OAUTH_REDIRECT_URI
    
    /**
     * OAuth authorization URL
     */
    const val AUTHORIZE_URL = Constants.OAUTH_AUTHORIZE_URL
    
    /**
     * OAuth token URL
     */
    const val TOKEN_URL = Constants.OAUTH_TOKEN_URL
    
    /**
     * OAuth token revocation URL
     */
    const val REVOKE_URL = Constants.OAUTH_REVOKE_URL
    
    /**
     * OAuth response type (code for authorization code flow)
     */
    const val RESPONSE_TYPE = "code"
    
    /**
     * OAuth scopes - YNAB uses implicit all-access scope
     * No specific scope parameter is required
     */
    val scopes: List<String> = emptyList()
    
    /**
     * Token refresh buffer - refresh token this many seconds before expiry
     */
    const val TOKEN_REFRESH_BUFFER_SECONDS = Constants.OAUTH_TOKEN_REFRESH_BUFFER_SECONDS
    
    /**
     * OAuth client secret (not used for mobile apps with PKCE)
     */
    const val CLIENT_SECRET: String? = null
}
