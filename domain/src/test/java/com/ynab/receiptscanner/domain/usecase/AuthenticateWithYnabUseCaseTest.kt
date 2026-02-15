package com.ynab.receiptscanner.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.domain.repository.YnabRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AuthenticateWithYnabUseCase
 */
class AuthenticateWithYnabUseCaseTest {
    
    private lateinit var ynabRepository: YnabRepository
    private lateinit var useCase: AuthenticateWithYnabUseCase
    
    @Before
    fun setup() {
        ynabRepository = mockk()
        useCase = AuthenticateWithYnabUseCase(ynabRepository)
    }
    
    @Test
    fun `invoke with valid token authenticates successfully`() = runTest {
        // Given
        val accessToken = "valid-access-token-123"
        coEvery { 
            ynabRepository.authenticate(accessToken) 
        } returns Result.Success(Unit)
        
        // When
        val result = useCase(accessToken)
        
        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        coVerify(exactly = 1) { ynabRepository.authenticate(accessToken) }
    }
    
    @Test
    fun `invoke with invalid token returns error`() = runTest {
        // Given
        val accessToken = "invalid-token"
        val exception = Exception("Invalid token")
        coEvery { 
            ynabRepository.authenticate(accessToken) 
        } returns Result.Error(exception, "Authentication failed")
        
        // When
        val result = useCase(accessToken)
        
        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.exception).isEqualTo(exception)
        assertThat(error.message).contains("Authentication failed")
    }
    
    @Test
    fun `invoke with empty token returns error`() = runTest {
        // Given
        val accessToken = ""
        val exception = Exception("Token is required")
        coEvery { 
            ynabRepository.authenticate(accessToken) 
        } returns Result.Error(exception, "Token cannot be empty")
        
        // When
        val result = useCase(accessToken)
        
        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        assertThat((result as Result.Error).message).contains("empty")
    }
    
    @Test
    fun `invoke handles network error`() = runTest {
        // Given
        val accessToken = "token-123"
        val networkException = Exception("Network timeout")
        coEvery { 
            ynabRepository.authenticate(accessToken) 
        } returns Result.Error(networkException, "Network error")
        
        // When
        val result = useCase(accessToken)
        
        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        assertThat((result as Result.Error).exception).isEqualTo(networkException)
    }
    
    @Test
    fun `invoke calls repository exactly once`() = runTest {
        // Given
        val accessToken = "token-123"
        coEvery { 
            ynabRepository.authenticate(accessToken) 
        } returns Result.Success(Unit)
        
        // When
        useCase(accessToken)
        
        // Then
        coVerify(exactly = 1) { ynabRepository.authenticate(accessToken) }
    }
    
    @Test
    fun `invoke with different tokens calls repository with correct token`() = runTest {
        // Given
        val token1 = "token-111"
        val token2 = "token-222"
        coEvery { ynabRepository.authenticate(token1) } returns Result.Success(Unit)
        coEvery { ynabRepository.authenticate(token2) } returns Result.Success(Unit)
        
        // When
        val result1 = useCase(token1)
        val result2 = useCase(token2)
        
        // Then
        assertThat(result1).isInstanceOf(Result.Success::class.java)
        assertThat(result2).isInstanceOf(Result.Success::class.java)
        coVerify(exactly = 1) { ynabRepository.authenticate(token1) }
        coVerify(exactly = 1) { ynabRepository.authenticate(token2) }
    }
    
    @Test
    fun `invoke propagates all repository errors`() = runTest {
        // Given
        val accessToken = "token-123"
        val unauthorizedException = Exception("401 Unauthorized")
        coEvery { 
            ynabRepository.authenticate(accessToken) 
        } returns Result.Error(unauthorizedException, "Unauthorized access")
        
        // When
        val result = useCase(accessToken)
        
        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.exception).isEqualTo(unauthorizedException)
        assertThat(error.exception?.message).contains("401")
    }
}
