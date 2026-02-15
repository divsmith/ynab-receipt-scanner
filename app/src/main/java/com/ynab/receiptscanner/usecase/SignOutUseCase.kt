package com.ynab.receiptscanner.usecase

import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.data.auth.YnabAuthManager
import com.ynab.receiptscanner.data.local.preference.AuthPreferences
import javax.inject.Inject

/**
 * Use case for signing out and revoking tokens
 */
class SignOutUseCase @Inject constructor(
    private val authManager: YnabAuthManager,
    private val authPreferences: AuthPreferences
) {
    /**
     * Sign out user and revoke tokens
     * @return Result with success or error
     */
    suspend operator fun invoke(): Result<Unit> {
        return try {
            // Revoke token if using OAuth
            if (!authPreferences.isUsingPersonalAccessToken()) {
                authManager.revokeToken()
            }
            
            // Clear all auth data
            authPreferences.clearAuthData()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            // Even if revocation fails, clear local data
            authPreferences.clearAuthData()
            Result.Error(e)
        }
    }
}
