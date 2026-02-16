package com.receiptscanner.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.receiptscanner.domain.model.YnabToken
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class KeystoreManagerTest {

    private lateinit var context: Context
    private lateinit var keystoreManager: KeystoreManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        keystoreManager = KeystoreManager(context)
    }

    @After
    fun teardown() {
        // Clean up stored tokens
        keystoreManager.deleteToken()
    }

    @Test
    fun `storing token succeeds`() {
        // Given
        val token = createTestToken()
        
        // When
        val result = keystoreManager.saveToken(token)
        
        // Then
        assertTrue("Token should be saved successfully", result)
    }

    @Test
    fun `retrieving stored token returns correct token`() {
        // Given
        val token = createTestToken()
        keystoreManager.saveToken(token)
        
        // When
        val retrieved = keystoreManager.getToken()
        
        // Then
        assertNotNull("Retrieved token should not be null", retrieved)
        assertEquals(token.accessToken, retrieved?.accessToken)
        assertEquals(token.refreshToken, retrieved?.refreshToken)
        assertEquals(token.tokenType, retrieved?.tokenType)
        assertEquals(token.expiresIn, retrieved?.expiresIn)
    }

    @Test
    fun `retrieving token when none exists returns null`() {
        // When
        val retrieved = keystoreManager.getToken()
        
        // Then
        assertNull("Retrieved token should be null when none stored", retrieved)
    }

    @Test
    fun `deleting token removes it from storage`() {
        // Given
        val token = createTestToken()
        keystoreManager.saveToken(token)
        
        // When
        keystoreManager.deleteToken()
        val retrieved = keystoreManager.getToken()
        
        // Then
        assertNull("Token should be deleted", retrieved)
    }

    @Test
    fun `storing new token overwrites existing token`() {
        // Given
        val oldToken = createTestToken(accessToken = "old_token")
        val newToken = createTestToken(accessToken = "new_token")
        keystoreManager.saveToken(oldToken)
        
        // When
        keystoreManager.saveToken(newToken)
        val retrieved = keystoreManager.getToken()
        
        // Then
        assertNotNull(retrieved)
        assertEquals("new_token", retrieved?.accessToken)
    }

    @Test
    fun `token with special characters is stored and retrieved correctly`() {
        // Given
        val token = createTestToken(
            accessToken = "token_with_special!@#$%^&*()chars",
            refreshToken = "refresh+token/with=special"
        )
        
        // When
        keystoreManager.saveToken(token)
        val retrieved = keystoreManager.getToken()
        
        // Then
        assertNotNull(retrieved)
        assertEquals(token.accessToken, retrieved?.accessToken)
        assertEquals(token.refreshToken, retrieved?.refreshToken)
    }

    @Test
    fun `token with null refresh token is stored and retrieved correctly`() {
        // Given
        val token = createTestToken(refreshToken = null)
        
        // When
        keystoreManager.saveToken(token)
        val retrieved = keystoreManager.getToken()
        
        // Then
        assertNotNull(retrieved)
        assertEquals(token.accessToken, retrieved?.accessToken)
        assertNull(retrieved?.refreshToken)
    }

    @Test
    fun `hasToken returns true when token exists`() {
        // Given
        val token = createTestToken()
        keystoreManager.saveToken(token)
        
        // When
        val hasToken = keystoreManager.hasToken()
        
        // Then
        assertTrue("hasToken should return true when token exists", hasToken)
    }

    @Test
    fun `hasToken returns false when no token exists`() {
        // When
        val hasToken = keystoreManager.hasToken()
        
        // Then
        assertFalse("hasToken should return false when no token exists", hasToken)
    }

    private fun createTestToken(
        accessToken: String = "test_access_token",
        refreshToken: String? = "test_refresh_token",
        expiresIn: Long = 7200L,
        tokenType: String = "Bearer"
    ): YnabToken {
        return YnabToken(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = expiresIn,
            tokenType = tokenType,
            createdAt = Instant.now()
        )
    }
}
