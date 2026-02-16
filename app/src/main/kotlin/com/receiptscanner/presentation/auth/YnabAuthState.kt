package com.receiptscanner.presentation.auth

import com.receiptscanner.domain.model.YnabToken

/**
 * Sealed class representing the state of YNAB authentication
 */
sealed class YnabAuthState {
    /**
     * Initial idle state - not authenticated
     */
    object Idle : YnabAuthState()
    
    /**
     * Loading state during authentication operations
     */
    object Loading : YnabAuthState()
    
    /**
     * Authorization URL is ready to be opened
     */
    data class AuthorizationReady(val authorizationUrl: String) : YnabAuthState()
    
    /**
     * Successfully authenticated with a valid token
     */
    data class Authenticated(val token: YnabToken) : YnabAuthState()
    
    /**
     * Error occurred during authentication
     */
    data class Error(val message: String, val throwable: Throwable? = null) : YnabAuthState()
}
