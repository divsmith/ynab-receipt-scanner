package com.ynab.receiptscanner.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.domain.model.YnabAccount
import com.ynab.receiptscanner.domain.repository.YnabRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for GetAccountsUseCase
 */
class GetAccountsUseCaseTest {
    
    private lateinit var ynabRepository: YnabRepository
    private lateinit var useCase: GetAccountsUseCase
    
    @Before
    fun setup() {
        ynabRepository = mockk()
        useCase = GetAccountsUseCase(ynabRepository)
    }
    
    @Test
    fun `invoke returns accounts successfully`() = runTest {
        // Given
        val budgetId = "budget-123"
        val accounts = listOf(
            createTestAccount(id = "account-1", name = "Checking"),
            createTestAccount(id = "account-2", name = "Savings"),
            createTestAccount(id = "account-3", name = "Credit Card")
        )
        
        coEvery { 
            ynabRepository.getAccounts(budgetId, false) 
        } returns Result.Success(accounts)
        
        // When
        val result = useCase(budgetId)
        
        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val successResult = result as Result.Success
        assertThat(successResult.data).hasSize(3)
        assertThat(successResult.data).containsExactlyElementsIn(accounts)
    }
    
    @Test
    fun `invoke with forceRefresh true forces network fetch`() = runTest {
        // Given
        val budgetId = "budget-123"
        val accounts = listOf(createTestAccount())
        
        coEvery { 
            ynabRepository.getAccounts(budgetId, true) 
        } returns Result.Success(accounts)
        
        // When
        val result = useCase(budgetId, forceRefresh = true)
        
        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        coVerify(exactly = 1) { ynabRepository.getAccounts(budgetId, true) }
    }
    
    @Test
    fun `invoke with forceRefresh false uses cache`() = runTest {
        // Given
        val budgetId = "budget-123"
        val accounts = listOf(createTestAccount())
        
        coEvery { 
            ynabRepository.getAccounts(budgetId, false) 
        } returns Result.Success(accounts)
        
        // When
        val result = useCase(budgetId, forceRefresh = false)
        
        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        coVerify(exactly = 1) { ynabRepository.getAccounts(budgetId, false) }
    }
    
    @Test
    fun `invoke without forceRefresh parameter defaults to false`() = runTest {
        // Given
        val budgetId = "budget-123"
        val accounts = listOf(createTestAccount())
        
        coEvery { 
            ynabRepository.getAccounts(budgetId, false) 
        } returns Result.Success(accounts)
        
        // When
        val result = useCase(budgetId)
        
        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        coVerify(exactly = 1) { ynabRepository.getAccounts(budgetId, false) }
    }
    
    @Test
    fun `invoke returns empty list when no accounts exist`() = runTest {
        // Given
        val budgetId = "budget-123"
        val emptyAccounts = emptyList<YnabAccount>()
        
        coEvery { 
            ynabRepository.getAccounts(budgetId, false) 
        } returns Result.Success(emptyAccounts)
        
        // When
        val result = useCase(budgetId)
        
        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEmpty()
    }
    
    @Test
    fun `invoke handles repository error`() = runTest {
        // Given
        val budgetId = "budget-123"
        val exception = Exception("Network error")
        
        coEvery { 
            ynabRepository.getAccounts(budgetId, false) 
        } returns Result.Error(exception, "Failed to fetch accounts")
        
        // When
        val result = useCase(budgetId)
        
        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.exception).isEqualTo(exception)
        assertThat(error.message).contains("Failed to fetch")
    }
    
    @Test
    fun `invoke with different budgetIds calls repository with correct id`() = runTest {
        // Given
        val budgetId1 = "budget-111"
        val budgetId2 = "budget-222"
        val accounts1 = listOf(createTestAccount(id = "account-1"))
        val accounts2 = listOf(createTestAccount(id = "account-2"))
        
        coEvery { ynabRepository.getAccounts(budgetId1, false) } returns Result.Success(accounts1)
        coEvery { ynabRepository.getAccounts(budgetId2, false) } returns Result.Success(accounts2)
        
        // When
        val result1 = useCase(budgetId1)
        val result2 = useCase(budgetId2)
        
        // Then
        assertThat((result1 as Result.Success).data).isEqualTo(accounts1)
        assertThat((result2 as Result.Success).data).isEqualTo(accounts2)
        coVerify(exactly = 1) { ynabRepository.getAccounts(budgetId1, false) }
        coVerify(exactly = 1) { ynabRepository.getAccounts(budgetId2, false) }
    }
    
    private fun createTestAccount(
        id: String = "account-123",
        name: String = "Test Account",
        type: YnabAccount.AccountType = YnabAccount.AccountType.CHECKING,
        balance: Long = 100000,
        closed: Boolean = false
    ) = YnabAccount(
        id = id,
        name = name,
        type = type,
        onBudget = true,
        closed = closed,
        balance = balance,
        clearedBalance = balance,
        unclearedBalance = 0,
        deleted = false
    )
}
