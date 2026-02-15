package com.ynab.receiptscanner.ui.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ynab.receiptscanner.ui.common.ErrorHandler

/**
 * Base ViewModel with common error handling
 */
abstract class BaseViewModel : ViewModel() {
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    protected fun handleError(throwable: Throwable) {
        _error.value = ErrorHandler.getErrorMessage(throwable)
        _isLoading.value = false
    }
    
    protected fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
    
    protected fun clearError() {
        _error.value = null
    }
}
