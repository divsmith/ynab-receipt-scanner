package com.ynab.receiptscanner.ui.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for OnboardingActivity
 * Tracks onboarding completion
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor() : ViewModel() {
    
    private val _navigateToAuth = MutableLiveData<Boolean>()
    val navigateToAuth: LiveData<Boolean> = _navigateToAuth
    
    fun completeOnboarding() {
        // TODO: Save onboarding completion to SharedPreferences
        _navigateToAuth.value = true
    }
    
    fun skipOnboarding() {
        completeOnboarding()
    }
}
