package com.ynab.receiptscanner.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ynab.receiptscanner.usecase.GetAuthStatusUseCase
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
            when (val result = getAuthStatusUseCase()) {
                is com.ynab.receiptscanner.core.util.Result.Success -> {
                    _isAuthenticated.value = result.data.isAuthenticated
                }
                is com.ynab.receiptscanner.core.util.Result.Error -> {
                    _isAuthenticated.value = false
                }
                is com.ynab.receiptscanner.core.util.Result.Loading -> {
                    // Loading state
                }
            }
        }
    }
    
    fun handleOAuthCallback(code: String) {
        // OAuth callback will be handled by AuthActivity/AuthViewModel
        // This method exists for extensibility
    }
}
