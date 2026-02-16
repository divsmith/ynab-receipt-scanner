package com.receiptscanner.data.repository

import com.receiptscanner.data.local.KeystoreManager
import com.receiptscanner.data.remote.YnabAuthService
import com.receiptscanner.domain.model.YnabToken
import com.receiptscanner.domain.repository.YnabAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.URLEncoder
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of YNAB OAuth 2.0 authentication repository
 */
@Singleton
class YnabAuthRepositoryImpl @Inject constructor(
    private val authService: YnabAuthService,
    private val keystoreManager: KeystoreManager
) : YnabAuthRepository {

    companion object {
        private const val YNAB_AUTH_BASE_URL = "https://app.ynab.com/oauth/authorize"
    }

    override fun getAuthorizationUrl(
        clientId: String,
        redirectUri: String,
        state: String
    ): String {
        // URL encode parameters
        val encodedRedirectUri = URLEncoder.encode(redirectUri, "UTF-8")
        val encodedState = URLEncoder.encode(state, "UTF-8")
        
        return buildString {
            append(YNAB_AUTH_BASE_URL)
            append("?client_id=").append(clientId)
            append("&redirect_uri=").append(encodedRedirectUri)
            append("&response_type=code")
            append("&state=").append(encodedState)
        }
    }

    override suspend fun exchangeCodeForToken(
        code: String,
        clientId: String,
        clientSecret: String,
        redirectUri: String
    ): Result<YnabToken> = withContext(Dispatchers.IO) {
        try {
            val response = authService.exchangeToken(
                clientId = clientId,
                clientSecret = clientSecret,
                redirectUri = redirectUri,
                grantType = "authorization_code",
                code = code,
                refreshToken = null
            )
            
            val token = YnabToken(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                expiresIn = response.expiresIn,
                tokenType = response.tokenType,
                createdAt = Instant.now()
            )
            
            // Save token to keystore
            keystoreManager.saveToken(token)
            
            Result.success(token)
        } catch (e: Exception) {
            Timber.e(e, "Error exchanging authorization code for token")
            Result.failure(e)
        }
    }

    override suspend fun refreshToken(
        refreshToken: String,
        clientId: String,
        clientSecret: String
    ): Result<YnabToken> = withContext(Dispatchers.IO) {
        try {
            val response = authService.exchangeToken(
                clientId = clientId,
                clientSecret = clientSecret,
                redirectUri = "", // Not required for refresh
                grantType = "refresh_token",
                code = null,
                refreshToken = refreshToken
            )
            
            val token = YnabToken(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                expiresIn = response.expiresIn,
                tokenType = response.tokenType,
                createdAt = Instant.now()
            )
            
            // Save refreshed token to keystore
            keystoreManager.saveToken(token)
            
            Result.success(token)
        } catch (e: Exception) {
            Timber.e(e, "Error refreshing token")
            Result.failure(e)
        }
    }

    override suspend fun getStoredToken(): YnabToken? = withContext(Dispatchers.IO) {
        keystoreManager.getToken()
    }

    override suspend fun saveToken(token: YnabToken): Boolean = withContext(Dispatchers.IO) {
        keystoreManager.saveToken(token)
    }

    override suspend fun deleteToken() = withContext(Dispatchers.IO) {
        keystoreManager.deleteToken()
    }

    override suspend fun hasValidToken(): Boolean = withContext(Dispatchers.IO) {
        val token = keystoreManager.getToken()
        token != null && !token.isExpired()
    }
}
