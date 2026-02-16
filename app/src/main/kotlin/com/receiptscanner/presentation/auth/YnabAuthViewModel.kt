package com.receiptscanner.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.receiptscanner.domain.repository.YnabAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for managing YNAB OAuth 2.0 authentication flow
 */
@HiltViewModel
class YnabAuthViewModel @Inject constructor(
    private val authRepository: YnabAuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<YnabAuthState>(YnabAuthState.Idle)
    val authState: StateFlow<YnabAuthState> = _authState.asStateFlow()

    // Store the expected state for CSRF protection
    private var expectedState: String? = null

    /**
     * Starts the OAuth flow by generating the authorization URL
     * @param clientId YNAB client ID
     * @param redirectUri OAuth redirect URI
     */
    fun startOAuthFlow(clientId: String, redirectUri: String) {
        viewModelScope.launch {
            try {
                _authState.value = YnabAuthState.Loading
                
                // Generate random state for CSRF protection
                expectedState = UUID.randomUUID().toString()
                
                val authUrl = authRepository.getAuthorizationUrl(
                    clientId = clientId,
                    redirectUri = redirectUri,
                    state = expectedState!!
                )
                
                _authState.value = YnabAuthState.AuthorizationReady(authUrl)
            } catch (e: Exception) {
                Timber.e(e, "Error starting OAuth flow")
                _authState.value = YnabAuthState.Error("Failed to start authentication", e)
            }
        }
    }

    /**
     * Handles the OAuth callback with authorization code
     * @param code Authorization code from callback
     * @param state State parameter from callback
     * @param clientId YNAB client ID
     * @param clientSecret YNAB client secret
     * @param redirectUri OAuth redirect URI
     */
    fun handleAuthCallback(
        code: String,
        state: String,
        clientId: String,
        clientSecret: String,
        redirectUri: String
    ) {
        viewModelScope.launch {
            try {
                // Verify state matches for CSRF protection
                if (state != expectedState) {
                    _authState.value = YnabAuthState.Error("Invalid state parameter - possible CSRF attack")
                    return@launch
                }
                
                _authState.value = YnabAuthState.Loading
                
                val result = authRepository.exchangeCodeForToken(
                    code = code,
                    clientId = clientId,
                    clientSecret = clientSecret,
                    redirectUri = redirectUri
                )
                
                result.fold(
                    onSuccess = { token ->
                        _authState.value = YnabAuthState.Authenticated(token)
                        // Clear expected state after successful auth
                        expectedState = null
                    },
                    onFailure = { error ->
                        Timber.e(error, "Error exchanging code for token")
                        _authState.value = YnabAuthState.Error(
                            "Authentication failed: ${error.message}",
                            error
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error handling auth callback")
                _authState.value = YnabAuthState.Error("Authentication failed", e)
            }
        }
    }

    /**
     * Checks if a valid authentication token exists
     */
    fun checkAuthStatus() {
        viewModelScope.launch {
            try {
                val hasValidToken = authRepository.hasValidToken()
                
                if (hasValidToken) {
                    val token = authRepository.getStoredToken()
                    if (token != null) {
                        _authState.value = YnabAuthState.Authenticated(token)
                    } else {
                        _authState.value = YnabAuthState.Idle
                    }
                } else {
                    _authState.value = YnabAuthState.Idle
                }
            } catch (e: Exception) {
                Timber.e(e, "Error checking auth status")
                _authState.value = YnabAuthState.Idle
            }
        }
    }

    /**
     * Logs out by deleting the stored token
     */
    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.deleteToken()
                _authState.value = YnabAuthState.Idle
                expectedState = null
            } catch (e: Exception) {
                Timber.e(e, "Error during logout")
                _authState.value = YnabAuthState.Error("Logout failed", e)
            }
        }
    }

    /**
     * Refreshes the token if it's expired or about to expire
     * @param clientId YNAB client ID
     * @param clientSecret YNAB client secret
     */
    fun refreshTokenIfNeeded(clientId: String, clientSecret: String) {
        viewModelScope.launch {
            try {
                val currentToken = authRepository.getStoredToken()
                
                if (currentToken != null && currentToken.isExpired()) {
                    val refreshToken = currentToken.refreshToken
                    
                    if (refreshToken != null) {
                        _authState.value = YnabAuthState.Loading
                        
                        val result = authRepository.refreshToken(
                            refreshToken = refreshToken,
                            clientId = clientId,
                            clientSecret = clientSecret
                        )
                        
                        result.fold(
                            onSuccess = { newToken ->
                                _authState.value = YnabAuthState.Authenticated(newToken)
                            },
                            onFailure = { error ->
                                Timber.e(error, "Error refreshing token")
                                _authState.value = YnabAuthState.Error(
                                    "Token refresh failed: ${error.message}",
                                    error
                                )
                            }
                        )
                    } else {
                        // No refresh token available, need to re-authenticate
                        _authState.value = YnabAuthState.Idle
                    }
                }
                // If token is valid, do nothing
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing token")
                _authState.value = YnabAuthState.Error("Token refresh failed", e)
            }
        }
    }
}
