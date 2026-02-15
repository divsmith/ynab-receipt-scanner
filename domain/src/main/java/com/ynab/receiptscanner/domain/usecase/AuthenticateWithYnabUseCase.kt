package com.ynab.receiptscanner.domain.usecase

import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.data.auth.YnabAuthManager
import javax.inject.Inject

/**
 * Use case for starting OAuth authentication flow
 */
class AuthenticateWithYnabUseCase @Inject constructor(
    private val authManager: YnabAuthManager
) {
    /**
     * Generate authorization URL to start OAuth flow
     * @return Authorization URL to open in browser
     */
    suspend operator fun invoke(): Result<String> {
        return try {
            val url = authManager.generateAuthorizationUrl()
            Result.Success(url)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Exchange authorization code for access token
     * @param authorizationCode Code received from OAuth callback
     * @return Result with success or error
     */
    suspend fun exchangeCode(authorizationCode: String): Result<Unit> {
        return authManager.exchangeCodeForToken(authorizationCode)
    }
}
