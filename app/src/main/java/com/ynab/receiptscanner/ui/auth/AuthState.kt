package com.ynab.receiptscanner.ui.auth

/**
 * Sealed class representing authentication states
 */
sealed class AuthState {
    /**
     * User is signed out
     */
    object SignedOut : AuthState()
    
    /**
     * OAuth sign-in process is in progress
     */
    object SigningIn : AuthState()
    
    /**
     * User is signed in and authenticated
     * @param usingPersonalAccessToken Whether using PAT instead of OAuth
     */
    data class SignedIn(val usingPersonalAccessToken: Boolean = false) : AuthState()
    
    /**
     * Authentication error occurred
     * @param message Error message to display
     * @param exception Optional exception for debugging
     */
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : AuthState()
}
