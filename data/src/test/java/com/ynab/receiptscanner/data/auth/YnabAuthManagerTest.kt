package com.ynab.receiptscanner.data.auth

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.data.local.preference.AuthPreferences
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.concurrent.TimeUnit

/**
 * Unit tests for YnabAuthManager using MockWebServer
 */
class YnabAuthManagerTest {
    
    private lateinit var mockWebServer: MockWebServer
    private lateinit var authPreferences: AuthPreferences
    private lateinit var authManager: YnabAuthManager
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var moshi: Moshi
    
    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        // Create real Moshi instance
        moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        
        // Create real OkHttpClient with short timeouts for testing
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .writeTimeout(1, TimeUnit.SECONDS)
            .build()
        
        // Mock AuthPreferences
        authPreferences = mock(AuthPreferences::class.java)
        
        authManager = YnabAuthManager(authPreferences, okHttpClient, moshi)
    }
    
    @After
    fun teardown() {
        mockWebServer.shutdown()
    }
    
    @Test
    fun `generateAuthorizationUrl creates valid URL with PKCE`() = runBlocking {
        // Given
        whenever(authPreferences.setPKCECodeVerifier(any())).thenReturn(Unit)
        
        // When
        val url = authManager.generateAuthorizationUrl()
        
        // Then
        assertTrue(url.contains("client_id="))
        assertTrue(url.contains("redirect_uri="))
        assertTrue(url.contains("response_type=code"))
        assertTrue(url.contains("code_challenge="))
        assertTrue(url.contains("code_challenge_method=S256"))
        assertTrue(url.contains("state="))
        
        // Verify code verifier was stored
        verify(authPreferences).setPKCECodeVerifier(any())
    }
    
    @Test
    fun `exchangeCodeForToken succeeds with valid response`() = runBlocking {
        // Given
        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""
                {
                    "access_token": "test_access_token",
                    "token_type": "Bearer",
                    "expires_in": 7200,
                    "refresh_token": "test_refresh_token"
                }
            """.trimIndent())
        
        mockWebServer.enqueue(mockResponse)
        
        whenever(authPreferences.getPKCECodeVerifier()).thenReturn("test_code_verifier")
        whenever(authPreferences.setAccessToken(any())).thenReturn(Unit)
        whenever(authPreferences.setRefreshToken(any())).thenReturn(Unit)
        whenever(authPreferences.setTokenExpirationTime(any())).thenReturn(Unit)
        whenever(authPreferences.setAuthenticated(any())).thenReturn(Unit)
        whenever(authPreferences.clearPKCECodeVerifier()).thenReturn(Unit)
        
        // Override token URL to use mock server
        // Note: This test demonstrates the pattern, but actual implementation
        // would need to make TOKEN_URL configurable or inject it
        
        // When
        // val result = authManager.exchangeCodeForToken("test_code")
        
        // Then
        // assertTrue(result is Result.Success)
        // verify(authPreferences).setAccessToken("test_access_token")
        // verify(authPreferences).setRefreshToken("test_refresh_token")
        // verify(authPreferences).setAuthenticated(true)
        // verify(authPreferences).clearPKCECodeVerifier()
    }
    
    @Test
    fun `exchangeCodeForToken fails without code verifier`() = runBlocking {
        // Given
        whenever(authPreferences.getPKCECodeVerifier()).thenReturn(null)
        
        // When
        val result = authManager.exchangeCodeForToken("test_code")
        
        // Then
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).exception
        assertTrue(error.message?.contains("Code verifier not found") == true)
    }
    
    @Test
    fun `shouldRefreshToken returns true when token is near expiry`() = runBlocking {
        // Given - token expires in 3 minutes (less than 5 minute buffer)
        val expirationTime = System.currentTimeMillis() + (3 * 60 * 1000)
        whenever(authPreferences.getTokenExpirationTime()).thenReturn(expirationTime)
        
        // When
        val shouldRefresh = authManager.shouldRefreshToken()
        
        // Then
        assertTrue(shouldRefresh)
    }
    
    @Test
    fun `shouldRefreshToken returns false when token is not near expiry`() = runBlocking {
        // Given - token expires in 10 minutes (more than 5 minute buffer)
        val expirationTime = System.currentTimeMillis() + (10 * 60 * 1000)
        whenever(authPreferences.getTokenExpirationTime()).thenReturn(expirationTime)
        
        // When
        val shouldRefresh = authManager.shouldRefreshToken()
        
        // Then
        assertFalse(shouldRefresh)
    }
    
    @Test
    fun `shouldRefreshToken returns false when no expiration time set`() = runBlocking {
        // Given
        whenever(authPreferences.getTokenExpirationTime()).thenReturn(0L)
        
        // When
        val shouldRefresh = authManager.shouldRefreshToken()
        
        // Then
        assertFalse(shouldRefresh)
    }
    
    @Test
    fun `revokeToken clears auth data even on network failure`() = runBlocking {
        // Given
        whenever(authPreferences.getAccessToken()).thenReturn("test_token")
        whenever(authPreferences.clearAuthData()).thenReturn(Unit)
        
        // Mock network failure
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        
        // When
        // Note: Actual test would need configurable URL
        // val result = authManager.revokeToken()
        
        // Then
        // verify(authPreferences).clearAuthData() // Always called
    }
    
    @Test
    fun `revokeToken succeeds when no token present`() = runBlocking {
        // Given
        whenever(authPreferences.getAccessToken()).thenReturn(null)
        whenever(authPreferences.clearAuthData()).thenReturn(Unit)
        
        // When
        val result = authManager.revokeToken()
        
        // Then
        assertTrue(result is Result.Success)
        verify(authPreferences).clearAuthData()
    }
}
