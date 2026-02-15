package com.ynab.receiptscanner.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.usecase.AuthenticateWithYnabUseCase
import com.ynab.receiptscanner.usecase.GetAuthStatusUseCase
import com.ynab.receiptscanner.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing authentication state and actions
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authenticateWithYnabUseCase: AuthenticateWithYnabUseCase,
    private val getAuthStatusUseCase: GetAuthStatusUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    init {
        checkAuthStatus()
    }
    
    /**
     * Check current authentication status
     */
    fun checkAuthStatus() {
        viewModelScope.launch {
            when (val result = getAuthStatusUseCase()) {
                is Result.Success -> {
                    val status = result.data
                    _authState.value = if (status.isAuthenticated && !status.isTokenExpired) {
                        AuthState.SignedIn(status.isUsingPersonalAccessToken)
                    } else {
                        AuthState.SignedOut
                    }
                }
                is Result.Error -> {
                    _authState.value = AuthState.SignedOut
                }
                is Result.Loading -> {
                    // Keep current state during loading
                }
            }
        }
    }
    
    /**
     * Start OAuth sign-in flow
     * @return Authorization URL to open in browser
     */
    fun startOAuthFlow(): String? {
        _authState.value = AuthState.SigningIn
        
        var authUrl: String? = null
        viewModelScope.launch {
            when (val result = authenticateWithYnabUseCase()) {
                is Result.Success -> {
                    authUrl = result.data
                }
                is Result.Error -> {
                    _authState.value = AuthState.Error(
                        message = "Failed to start authentication: ${result.exception.message}",
                        exception = result.exception
                    )
                }
                is Result.Loading -> {
                    // Keep signing in state
                }
            }
        }
        
        return authUrl
    }
    
    /**
     * Handle OAuth callback with authorization code
     * @param authorizationCode Code received from OAuth redirect
     */
    fun handleOAuthCallback(authorizationCode: String) {
        viewModelScope.launch {
            _authState.value = AuthState.SigningIn
            
            when (val result = authenticateWithYnabUseCase.exchangeCode(authorizationCode)) {
                is Result.Success -> {
                    _authState.value = AuthState.SignedIn(usingPersonalAccessToken = false)
                }
                is Result.Error -> {
                    _authState.value = AuthState.Error(
                        message = "Authentication failed: ${result.exception.message}",
                        exception = result.exception
                    )
                }
                is Result.Loading -> {
                    // Keep signing in state
                }
            }
        }
    }
    
    /**
     * Set Personal Access Token for testing
     * @param token Personal access token
     */
    fun setPersonalAccessToken(token: String) {
        viewModelScope.launch {
            _authState.value = AuthState.SigningIn
            
            when (val result = getAuthStatusUseCase.setPersonalAccessToken(token)) {
                is Result.Success -> {
                    _authState.value = AuthState.SignedIn(usingPersonalAccessToken = true)
                }
                is Result.Error -> {
                    _authState.value = AuthState.Error(
                        message = "Failed to set token: ${result.exception.message}",
                        exception = result.exception
                    )
                }
                is Result.Loading -> {
                    // Keep signing in state
                }
            }
        }
    }
    
    /**
     * Sign out and revoke tokens
     */
    fun signOut() {
        viewModelScope.launch {
            when (signOutUseCase()) {
                is Result.Success -> {
                    _authState.value = AuthState.SignedOut
                }
                is Result.Error -> {
                    // Even if revocation fails, consider user signed out locally
                    _authState.value = AuthState.SignedOut
                }
                is Result.Loading -> {
                    // Keep current state
                }
            }
        }
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.SignedOut
        }
    }
}
