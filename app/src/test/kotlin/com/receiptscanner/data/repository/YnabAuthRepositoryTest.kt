package com.receiptscanner.data.repository

import com.receiptscanner.data.local.KeystoreManager
import com.receiptscanner.data.remote.TokenResponse
import com.receiptscanner.data.remote.YnabAuthService
import com.receiptscanner.domain.model.YnabToken
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant

class YnabAuthRepositoryTest {

    private lateinit var repository: YnabAuthRepositoryImpl
    private lateinit var authService: YnabAuthService
    private lateinit var keystoreManager: KeystoreManager

    @Before
    fun setup() {
        authService = mockk()
        keystoreManager = mockk()
        repository = YnabAuthRepositoryImpl(authService, keystoreManager)
    }

    @After
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `getAuthorizationUrl generates correct URL with all parameters`() {
        // Given
        val clientId = "test_client_id"
        val redirectUri = "receiptscanner://oauth/callback"
        val state = "random_state_value"
        
        // When
        val url = repository.getAuthorizationUrl(clientId, redirectUri, state)
        
        // Then
        assertTrue(url.contains("https://app.ynab.com/oauth/authorize"))
        assertTrue(url.contains("client_id=$clientId"))
        assertTrue(url.contains("redirect_uri="))
        assertTrue(url.contains("response_type=code"))
        assertTrue(url.contains("state=$state"))
    }

    @Test
    fun `exchangeCodeForToken with valid code returns success`() = runTest {
        // Given
        val code = "auth_code_123"
        val clientId = "test_client_id"
        val clientSecret = "test_client_secret"
        val redirectUri = "receiptscanner://oauth/callback"
        
        val tokenResponse = TokenResponse(
            accessToken = "access_token_123",
            tokenType = "Bearer",
            expiresIn = 7200L,
            refreshToken = "refresh_token_123"
        )
        
        coEvery { 
            authService.exchangeToken(
                clientId = clientId,
                clientSecret = clientSecret,
                redirectUri = redirectUri,
                grantType = "authorization_code",
                code = code,
                refreshToken = null
            )
        } returns tokenResponse
        
        every { keystoreManager.saveToken(any()) } returns true
        
        // When
        val result = repository.exchangeCodeForToken(code, clientId, clientSecret, redirectUri)
        
        // Then
        assertTrue(result.isSuccess)
        val token = result.getOrNull()
        assertNotNull(token)
        assertEquals("access_token_123", token?.accessToken)
        assertEquals("refresh_token_123", token?.refreshToken)
        assertEquals(7200L, token?.expiresIn)
        assertEquals("Bearer", token?.tokenType)
        
        verify { keystoreManager.saveToken(any()) }
    }

    @Test
    fun `exchangeCodeForToken with network error returns failure`() = runTest {
        // Given
        val code = "auth_code_123"
        val clientId = "test_client_id"
        val clientSecret = "test_client_secret"
        val redirectUri = "receiptscanner://oauth/callback"
        
        coEvery { 
            authService.exchangeToken(any(), any(), any(), any(), any(), any())
        } throws Exception("Network error")
        
        // When
        val result = repository.exchangeCodeForToken(code, clientId, clientSecret, redirectUri)
        
        // Then
        assertTrue(result.isFailure)
        
        verify(exactly = 0) { keystoreManager.saveToken(any()) }
    }

    @Test
    fun `refreshToken with valid refresh token returns success`() = runTest {
        // Given
        val refreshToken = "refresh_token_123"
        val clientId = "test_client_id"
        val clientSecret = "test_client_secret"
        
        val tokenResponse = TokenResponse(
            accessToken = "new_access_token",
            tokenType = "Bearer",
            expiresIn = 7200L,
            refreshToken = "new_refresh_token"
        )
        
        coEvery { 
            authService.exchangeToken(
                clientId = clientId,
                clientSecret = clientSecret,
                redirectUri = "",
                grantType = "refresh_token",
                code = null,
                refreshToken = refreshToken
            )
        } returns tokenResponse
        
        every { keystoreManager.saveToken(any()) } returns true
        
        // When
        val result = repository.refreshToken(refreshToken, clientId, clientSecret)
        
        // Then
        assertTrue(result.isSuccess)
        val token = result.getOrNull()
        assertNotNull(token)
        assertEquals("new_access_token", token?.accessToken)
        
        verify { keystoreManager.saveToken(any()) }
    }

    @Test
    fun `refreshToken with network error returns failure`() = runTest {
        // Given
        val refreshToken = "refresh_token_123"
        val clientId = "test_client_id"
        val clientSecret = "test_client_secret"
        
        coEvery { 
            authService.exchangeToken(any(), any(), any(), any(), any(), any())
        } throws Exception("Network error")
        
        // When
        val result = repository.refreshToken(refreshToken, clientId, clientSecret)
        
        // Then
        assertTrue(result.isFailure)
        
        verify(exactly = 0) { keystoreManager.saveToken(any()) }
    }

    @Test
    fun `getStoredToken returns token from keystore`() = runTest {
        // Given
        val token = YnabToken(
            accessToken = "stored_token",
            refreshToken = "stored_refresh",
            expiresIn = 7200L,
            tokenType = "Bearer",
            createdAt = Instant.now()
        )
        every { keystoreManager.getToken() } returns token
        
        // When
        val result = repository.getStoredToken()
        
        // Then
        assertNotNull(result)
        assertEquals("stored_token", result?.accessToken)
    }

    @Test
    fun `getStoredToken returns null when no token stored`() = runTest {
        // Given
        every { keystoreManager.getToken() } returns null
        
        // When
        val result = repository.getStoredToken()
        
        // Then
        assertNull(result)
    }

    @Test
    fun `saveToken stores token in keystore`() = runTest {
        // Given
        val token = YnabToken(
            accessToken = "token_to_save",
            refreshToken = "refresh_to_save",
            expiresIn = 7200L,
            tokenType = "Bearer",
            createdAt = Instant.now()
        )
        every { keystoreManager.saveToken(token) } returns true
        
        // When
        val result = repository.saveToken(token)
        
        // Then
        assertTrue(result)
        verify { keystoreManager.saveToken(token) }
    }

    @Test
    fun `deleteToken removes token from keystore`() = runTest {
        // Given
        every { keystoreManager.deleteToken() } just Runs
        
        // When
        repository.deleteToken()
        
        // Then
        verify { keystoreManager.deleteToken() }
    }

    @Test
    fun `hasValidToken returns true when non-expired token exists`() = runTest {
        // Given
        val token = YnabToken(
            accessToken = "valid_token",
            refreshToken = "refresh",
            expiresIn = 7200L,
            tokenType = "Bearer",
            createdAt = Instant.now()
        )
        every { keystoreManager.getToken() } returns token
        
        // When
        val result = repository.hasValidToken()
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `hasValidToken returns false when token is expired`() = runTest {
        // Given - token created 3 hours ago with 2 hour expiry
        val token = YnabToken(
            accessToken = "expired_token",
            refreshToken = "refresh",
            expiresIn = 7200L,
            tokenType = "Bearer",
            createdAt = Instant.now().minusSeconds(3 * 3600)
        )
        every { keystoreManager.getToken() } returns token
        
        // When
        val result = repository.hasValidToken()
        
        // Then
        assertFalse(result)
    }

    @Test
    fun `hasValidToken returns false when no token exists`() = runTest {
        // Given
        every { keystoreManager.getToken() } returns null
        
        // When
        val result = repository.hasValidToken()
        
        // Then
        assertFalse(result)
    }

    @Test
    fun `exchangeCodeForToken saves token to keystore after successful exchange`() = runTest {
        // Given
        val code = "auth_code_123"
        val clientId = "test_client_id"
        val clientSecret = "test_client_secret"
        val redirectUri = "receiptscanner://oauth/callback"
        
        val tokenResponse = TokenResponse(
            accessToken = "access_token",
            tokenType = "Bearer",
            expiresIn = 7200L,
            refreshToken = "refresh_token"
        )
        
        val capturedToken = slot<YnabToken>()
        
        coEvery { 
            authService.exchangeToken(any(), any(), any(), any(), any(), any())
        } returns tokenResponse
        
        every { keystoreManager.saveToken(capture(capturedToken)) } returns true
        
        // When
        val result = repository.exchangeCodeForToken(code, clientId, clientSecret, redirectUri)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals("access_token", capturedToken.captured.accessToken)
        assertEquals("refresh_token", capturedToken.captured.refreshToken)
    }
}
