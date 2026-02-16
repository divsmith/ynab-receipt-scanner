package com.receiptscanner.domain.model

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class YnabTokenTest {

    @Test
    fun `token creation with valid data succeeds`() {
        // Given
        val accessToken = "test_access_token"
        val refreshToken = "test_refresh_token"
        val expiresIn = 7200L
        val tokenType = "Bearer"
        val createdAt = Instant.now()
        
        // When
        val token = YnabToken(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = expiresIn,
            tokenType = tokenType,
            createdAt = createdAt
        )
        
        // Then
        assertEquals(accessToken, token.accessToken)
        assertEquals(refreshToken, token.refreshToken)
        assertEquals(expiresIn, token.expiresIn)
        assertEquals(tokenType, token.tokenType)
        assertEquals(createdAt, token.createdAt)
    }
    
    @Test
    fun `token is not expired when within validity period`() {
        // Given - token created now with 2 hour expiry
        val token = YnabToken(
            accessToken = "test_token",
            refreshToken = "test_refresh",
            expiresIn = 7200L,
            tokenType = "Bearer",
            createdAt = Instant.now()
        )
        
        // When
        val isExpired = token.isExpired()
        
        // Then
        assertFalse(isExpired)
    }
    
    @Test
    fun `token is expired when past expiration time`() {
        // Given - token created 3 hours ago with 2 hour expiry
        val threeHoursAgo = Instant.now().minusSeconds(3 * 3600)
        val token = YnabToken(
            accessToken = "test_token",
            refreshToken = "test_refresh",
            expiresIn = 7200L, // 2 hours
            tokenType = "Bearer",
            createdAt = threeHoursAgo
        )
        
        // When
        val isExpired = token.isExpired()
        
        // Then
        assertTrue(isExpired)
    }
    
    @Test
    fun `token is considered expired with 5 minute buffer before actual expiry`() {
        // Given - token that expires in 4 minutes (within 5 min buffer)
        val almostExpired = Instant.now().minusSeconds(7200 - 240) // Expires in 4 minutes
        val token = YnabToken(
            accessToken = "test_token",
            refreshToken = "test_refresh",
            expiresIn = 7200L,
            tokenType = "Bearer",
            createdAt = almostExpired
        )
        
        // When
        val isExpired = token.isExpired()
        
        // Then
        assertTrue("Token should be expired within 5 minute buffer", isExpired)
    }
    
    @Test
    fun `token with no refresh token is valid`() {
        // Given - token without refresh token
        val token = YnabToken(
            accessToken = "test_token",
            refreshToken = null,
            expiresIn = 7200L,
            tokenType = "Bearer",
            createdAt = Instant.now()
        )
        
        // Then
        assertNotNull(token.accessToken)
        assertNull(token.refreshToken)
    }
    
    @Test
    fun `expiresAt property returns correct expiration instant`() {
        // Given
        val createdAt = Instant.now()
        val expiresIn = 3600L
        val token = YnabToken(
            accessToken = "test_token",
            refreshToken = "test_refresh",
            expiresIn = expiresIn,
            tokenType = "Bearer",
            createdAt = createdAt
        )
        
        // When
        val expiresAt = token.expiresAt
        
        // Then
        assertEquals(createdAt.plusSeconds(expiresIn), expiresAt)
    }
}
