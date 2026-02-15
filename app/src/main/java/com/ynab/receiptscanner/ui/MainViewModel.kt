package com.ynab.receiptscanner.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ynab.receiptscanner.domain.usecase.GetAuthStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for MainActivity
 * Manages authentication state and OAuth callback handling
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val getAuthStatusUseCase: GetAuthStatusUseCase
) : ViewModel() {
    
    private val _isAuthenticated = MutableLiveData<Boolean>()
    val isAuthenticated: LiveData<Boolean> = _isAuthenticated
    
    fun checkAuthStatus() {
        viewModelScope.launch {
            getAuthStatusUseCase().collect { isAuthenticated ->
                _isAuthenticated.value = isAuthenticated
            }
        }
    }
    
    fun handleOAuthCallback(code: String) {
        // OAuth callback will be handled by AuthActivity/AuthViewModel
        // This method exists for extensibility
    }
}
