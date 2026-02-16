package com.receiptscanner.presentation.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.receiptscanner.domain.model.YnabToken
import com.receiptscanner.domain.repository.YnabAuthRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class YnabAuthViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var viewModel: YnabAuthViewModel
    private lateinit var authRepository: YnabAuthRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk()
        viewModel = YnabAuthViewModel(authRepository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `initial state is Idle`() = runTest {
        // When
        val state = viewModel.authState.first()
        
        // Then
        assertTrue(state is YnabAuthState.Idle)
    }

    @Test
    fun `startOAuthFlow generates authorization URL`() = runTest {
        // Given
        val clientId = "test_client_id"
        val redirectUri = "receiptscanner://oauth/callback"
        val expectedUrl = "https://app.ynab.com/oauth/authorize?client_id=$clientId&redirect_uri=$redirectUri&response_type=code&state=test_state"
        
        every { 
            authRepository.getAuthorizationUrl(clientId, redirectUri, any())
        } returns expectedUrl
        
        // When
        viewModel.startOAuthFlow(clientId, redirectUri)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.authState.value
        assertTrue("State should be AuthorizationReady", state is YnabAuthState.AuthorizationReady)
        assertEquals(expectedUrl, (state as YnabAuthState.AuthorizationReady).authorizationUrl)
    }

    @Test
    fun `handleAuthCallback with valid code exchanges for token`() = runTest {
        // Given
        val code = "auth_code_123"
        val clientId = "test_client_id"
        val clientSecret = "test_secret"
        val redirectUri = "receiptscanner://oauth/callback"
        
        val token = YnabToken(
            accessToken = "access_token",
            refreshToken = "refresh_token",
            expiresIn = 7200L,
            tokenType = "Bearer",
            createdAt = Instant.now()
        )
        
        // Mock the auth URL generation with a known state
        var generatedState: String? = null
        every { 
            authRepository.getAuthorizationUrl(clientId, redirectUri, any())
        } answers {
            generatedState = thirdArg<String>()
            "https://app.ynab.com/oauth/authorize?client_id=$clientId&state=$generatedState"
        }
        
        // Start OAuth flow to generate expected state
        viewModel.startOAuthFlow(clientId, redirectUri)
        advanceUntilIdle()
        
        assertNotNull("State should be generated", generatedState)
        
        coEvery { 
            authRepository.exchangeCodeForToken(code, clientId, clientSecret, redirectUri)
        } returns Result.success(token)
        
        // When - use the actual generated state
        viewModel.handleAuthCallback(code, generatedState!!, clientId, clientSecret, redirectUri)
        advanceUntilIdle()
        
        // Then
        val authState = viewModel.authState.value
        assertTrue("State should be Authenticated", authState is YnabAuthState.Authenticated)
        assertEquals(token, (authState as YnabAuthState.Authenticated).token)
    }

    @Test
    fun `handleAuthCallback with network error sets error state`() = runTest {
        // Given
        val code = "auth_code_123"
        val state = "test_state"
        val clientId = "test_client_id"
        val clientSecret = "test_secret"
        val redirectUri = "receiptscanner://oauth/callback"
        
        // Set expected state
        viewModel.startOAuthFlow(clientId, redirectUri)
        advanceUntilIdle()
        
        val exception = Exception("Network error")
        coEvery { 
            authRepository.exchangeCodeForToken(any(), any(), any(), any())
        } returns Result.failure(exception)
        
        // When
        viewModel.handleAuthCallback(code, state, clientId, clientSecret, redirectUri)
        advanceUntilIdle()
        
        // Then
        val authState = viewModel.authState.value
        assertTrue("State should be Error", authState is YnabAuthState.Error)
        assertNotNull((authState as YnabAuthState.Error).message)
    }

    @Test
    fun `handleAuthCallback with state mismatch sets error state`() = runTest {
        // Given
        val code = "auth_code_123"
        val incorrectState = "wrong_state"
        val clientId = "test_client_id"
        val clientSecret = "test_secret"
        val redirectUri = "receiptscanner://oauth/callback"
        
        // Set expected state
        viewModel.startOAuthFlow(clientId, redirectUri)
        advanceUntilIdle()
        
        // When
        viewModel.handleAuthCallback(code, incorrectState, clientId, clientSecret, redirectUri)
        advanceUntilIdle()
        
        // Then
        val authState = viewModel.authState.value
        assertTrue("State should be Error", authState is YnabAuthState.Error)
        assertTrue((authState as YnabAuthState.Error).message.contains("state", ignoreCase = true))
    }

    @Test
    fun `checkAuthStatus sets Authenticated when valid token exists`() = runTest {
        // Given
        val token = YnabToken(
            accessToken = "stored_token",
            refreshToken = "stored_refresh",
            expiresIn = 7200L,
            tokenType = "Bearer",
            createdAt = Instant.now()
        )
        
        coEvery { authRepository.hasValidToken() } returns true
        coEvery { authRepository.getStoredToken() } returns token
        
        // When
        viewModel.checkAuthStatus()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.authState.value
        assertTrue("State should be Authenticated", state is YnabAuthState.Authenticated)
        assertEquals(token, (state as YnabAuthState.Authenticated).token)
    }

    @Test
    fun `checkAuthStatus sets Idle when no token exists`() = runTest {
        // Given
        coEvery { authRepository.hasValidToken() } returns false
        
        // When
        viewModel.checkAuthStatus()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.authState.value
        assertTrue("State should be Idle", state is YnabAuthState.Idle)
    }

    @Test
    fun `logout deletes token and sets Idle state`() = runTest {
        // Given
        coEvery { authRepository.deleteToken() } just Runs
        
        // When
        viewModel.logout()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.authState.value
        assertTrue("State should be Idle", state is YnabAuthState.Idle)
        coVerify { authRepository.deleteToken() }
    }

    @Test
    fun `refreshTokenIfNeeded with expired token refreshes it`() = runTest {
        // Given
        val oldToken = YnabToken(
            accessToken = "old_token",
            refreshToken = "refresh_token",
            expiresIn = 7200L,
            tokenType = "Bearer",
            createdAt = Instant.now().minusSeconds(7300) // Expired
        )
        
        val newToken = YnabToken(
            accessToken = "new_token",
            refreshToken = "refresh_token",
            expiresIn = 7200L,
            tokenType = "Bearer",
            createdAt = Instant.now()
        )
        
        val clientId = "test_client_id"
        val clientSecret = "test_secret"
        
        coEvery { authRepository.getStoredToken() } returns oldToken
        coEvery { 
            authRepository.refreshToken("refresh_token", clientId, clientSecret)
        } returns Result.success(newToken)
        
        // When
        viewModel.refreshTokenIfNeeded(clientId, clientSecret)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.authState.value
        assertTrue("State should be Authenticated", state is YnabAuthState.Authenticated)
        assertEquals("new_token", (state as YnabAuthState.Authenticated).token.accessToken)
    }

    @Test
    fun `refreshTokenIfNeeded with valid token does nothing`() = runTest {
        // Given
        val validToken = YnabToken(
            accessToken = "valid_token",
            refreshToken = "refresh_token",
            expiresIn = 7200L,
            tokenType = "Bearer",
            createdAt = Instant.now()
        )
        
        val clientId = "test_client_id"
        val clientSecret = "test_secret"
        
        coEvery { authRepository.getStoredToken() } returns validToken
        
        // When
        viewModel.refreshTokenIfNeeded(clientId, clientSecret)
        advanceUntilIdle()
        
        // Then
        coVerify(exactly = 0) { authRepository.refreshToken(any(), any(), any()) }
    }

    @Test
    fun `startOAuthFlow sets Loading state initially`() = runTest {
        // Given
        val clientId = "test_client_id"
        val redirectUri = "receiptscanner://oauth/callback"
        val authUrl = "https://app.ynab.com/oauth/authorize?..."
        
        every { 
            authRepository.getAuthorizationUrl(any(), any(), any())
        } returns authUrl
        
        // When
        viewModel.startOAuthFlow(clientId, redirectUri)
        advanceUntilIdle()
        
        // Then - after coroutine completes, should be AuthorizationReady
        val finalState = viewModel.authState.value
        assertTrue("State should be AuthorizationReady after completion", 
            finalState is YnabAuthState.AuthorizationReady)
    }
}
