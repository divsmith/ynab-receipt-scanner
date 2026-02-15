package com.ynab.receiptscanner.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.domain.model.YnabTransaction
import com.ynab.receiptscanner.domain.repository.YnabRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Unit tests for CreateTransactionUseCase
 */
class CreateTransactionUseCaseTest {
    
    private lateinit var ynabRepository: YnabRepository
    private lateinit var useCase: CreateTransactionUseCase
    
    @Before
    fun setup() {
        ynabRepository = mockk()
        useCase = CreateTransactionUseCase(ynabRepository)
    }
    
    @Test
    fun `invoke with transaction without importId generates importId`() = runTest {
        // Given
        val budgetId = "budget-123"
        val transaction = createTestTransaction(importId = null)
        val transactionSlot = slot<YnabTransaction>()
        
        coEvery { 
            ynabRepository.createTransaction(budgetId, capture(transactionSlot)) 
        } returns Result.Success("transaction-id-456")
        
        // When
        val result = useCase(budgetId, transaction)
        
        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo("transaction-id-456")
        
        // Verify importId was generated
        val capturedTransaction = transactionSlot.captured
        assertThat(capturedTransaction.importId).isNotNull()
        assertThat(capturedTransaction.importId).isNotEmpty()
        assertThat(capturedTransaction.importId).startsWith("YNAB:")
    }
    
    @Test
    fun `invoke with transaction with importId keeps existing importId`() = runTest {
        // Given
        val budgetId = "budget-123"
        val existingImportId = "YNAB:existing-import-id"
        val transaction = createTestTransaction(importId = existingImportId)
        val transactionSlot = slot<YnabTransaction>()
        
        coEvery { 
            ynabRepository.createTransaction(budgetId, capture(transactionSlot)) 
        } returns Result.Success("transaction-id-456")
        
        // When
        val result = useCase(budgetId, transaction)
        
        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        
        // Verify importId was not changed
        val capturedTransaction = transactionSlot.captured
        assertThat(capturedTransaction.importId).isEqualTo(existingImportId)
    }
    
    @Test
    fun `invoke handles repository error`() = runTest {
        // Given
        val budgetId = "budget-123"
        val transaction = createTestTransaction()
        val exception = Exception("Network error")
        
        coEvery { 
            ynabRepository.createTransaction(budgetId, any()) 
        } returns Result.Error(exception, "Failed to create transaction")
        
        // When
        val result = useCase(budgetId, transaction)
        
        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.exception).isEqualTo(exception)
        assertThat(error.message).contains("Failed to create")
    }
    
    @Test
    fun `invoke with empty importId generates new importId`() = runTest {
        // Given
        val budgetId = "budget-123"
        val transaction = createTestTransaction(importId = "")
        val transactionSlot = slot<YnabTransaction>()
        
        coEvery { 
            ynabRepository.createTransaction(budgetId, capture(transactionSlot)) 
        } returns Result.Success("transaction-id-456")
        
        // When
        val result = useCase(budgetId, transaction)
        
        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        
        // Verify importId was generated (empty string counts as blank)
        val capturedTransaction = transactionSlot.captured
        assertThat(capturedTransaction.importId).isNotEmpty()
    }
    
    @Test
    fun `invoke passes correct budgetId to repository`() = runTest {
        // Given
        val budgetId = "specific-budget-id-789"
        val transaction = createTestTransaction()
        
        coEvery { 
            ynabRepository.createTransaction(budgetId, any()) 
        } returns Result.Success("transaction-id")
        
        // When
        useCase(budgetId, transaction)
        
        // Then
        coVerify(exactly = 1) { 
            ynabRepository.createTransaction(budgetId, any()) 
        }
    }
    
    private fun createTestTransaction(
        accountId: String = "account-123",
        categoryId: String? = "category-456",
        amount: Long = -25500, // Negative for outflow in milliunits
        payeeName: String? = "Test Store",
        memo: String? = "Test transaction",
        importId: String? = null
    ) = YnabTransaction(
        accountId = accountId,
        date = Date(),
        amount = amount,
        payeeName = payeeName,
        categoryId = categoryId,
        memo = memo,
        cleared = YnabTransaction.ClearedStatus.CLEARED,
        approved = true,
        importId = importId
    )
}
