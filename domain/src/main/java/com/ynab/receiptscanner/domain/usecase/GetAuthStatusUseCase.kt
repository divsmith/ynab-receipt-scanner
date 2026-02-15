package com.ynab.receiptscanner.domain.usecase

import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.data.local.preference.AuthPreferences
import javax.inject.Inject

/**
 * Use case for checking authentication status
 */
class GetAuthStatusUseCase @Inject constructor(
    private val authPreferences: AuthPreferences
) {
    /**
     * Check if user is authenticated
     * @return Result with authentication status
     */
    suspend operator fun invoke(): Result<AuthStatus> {
        return try {
            val isAuthenticated = authPreferences.isAuthenticated()
            val isUsingPAT = authPreferences.isUsingPersonalAccessToken()
            val isExpired = if (!isUsingPAT) authPreferences.isTokenExpired() else false
            val selectedBudgetId = authPreferences.getSelectedBudgetId()
            
            val status = AuthStatus(
                isAuthenticated = isAuthenticated,
                isUsingPersonalAccessToken = isUsingPAT,
                isTokenExpired = isExpired,
                hasSelectedBudget = !selectedBudgetId.isNullOrBlank()
            )
            
            Result.Success(status)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Set Personal Access Token (for testing)
     * @param token Personal access token
     */
    suspend fun setPersonalAccessToken(token: String): Result<Unit> {
        return try {
            authPreferences.setPersonalAccessToken(token)
            authPreferences.setAuthenticated(true)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

/**
 * Data class representing authentication status
 */
data class AuthStatus(
    val isAuthenticated: Boolean,
    val isUsingPersonalAccessToken: Boolean,
    val isTokenExpired: Boolean,
    val hasSelectedBudget: Boolean
)
