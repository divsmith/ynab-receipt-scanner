package com.receiptscanner.domain.model

import com.squareup.moshi.JsonClass
import java.time.Instant

/**
 * YNAB OAuth 2.0 token data model
 * 
 * @property accessToken The OAuth access token
 * @property refreshToken The OAuth refresh token
 * @property expiresIn Token expiration time in seconds
 * @property tokenType Token type (typically "Bearer")
 * @property createdAt Timestamp when token was created
 */
@JsonClass(generateAdapter = true)
data class YnabToken(
    val accessToken: String,
    val refreshToken: String?,
    val expiresIn: Long,
    val tokenType: String,
    val createdAt: Instant = Instant.now()
) {
    companion object {
        // 5 minute buffer before actual expiration
        private const val EXPIRATION_BUFFER_SECONDS = 300L
    }
    
    /**
     * Calculates the exact expiration instant
     */
    val expiresAt: Instant
        get() = createdAt.plusSeconds(expiresIn)
    
    /**
     * Checks if the token is expired or about to expire (within buffer)
     * @return true if token is expired or will expire within 5 minutes
     */
    fun isExpired(): Boolean {
        val now = Instant.now()
        val expirationWithBuffer = expiresAt.minusSeconds(EXPIRATION_BUFFER_SECONDS)
        return now.isAfter(expirationWithBuffer)
    }
}
