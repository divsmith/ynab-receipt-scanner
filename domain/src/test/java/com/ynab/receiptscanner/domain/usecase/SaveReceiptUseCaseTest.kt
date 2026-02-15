package com.ynab.receiptscanner.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.domain.model.Receipt
import com.ynab.receiptscanner.domain.repository.ReceiptRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Unit tests for SaveReceiptUseCase
 */
class SaveReceiptUseCaseTest {
    
    private lateinit var receiptRepository: ReceiptRepository
    private lateinit var useCase: SaveReceiptUseCase
    
    @Before
    fun setup() {
        receiptRepository = mockk()
        useCase = SaveReceiptUseCase(receiptRepository)
    }
    
    @Test
    fun `invoke with valid receipt saves successfully`() = runTest {
        // Given
        val receipt = createTestReceipt(id = "receipt-123")
        coEvery { receiptRepository.saveReceipt(receipt) } returns Result.Success(receipt)
        
        // When
        val result = useCase(receipt)
        
        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo(receipt)
        coVerify(exactly = 1) { receiptRepository.saveReceipt(receipt) }
    }
    
    @Test
    fun `invoke with blank ID returns error`() = runTest {
        // Given
        val receipt = createTestReceipt(id = "")
        
        // When
        val result = useCase(receipt)
        
        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.exception).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(error.message).contains("ID is required")
        coVerify(exactly = 0) { receiptRepository.saveReceipt(any()) }
    }
    
    @Test
    fun `invoke when repository fails returns error`() = runTest {
        // Given
        val receipt = createTestReceipt(id = "receipt-123")
        val exception = Exception("Database error")
        coEvery { receiptRepository.saveReceipt(receipt) } returns Result.Error(exception, "Failed to save")
        
        // When
        val result = useCase(receipt)
        
        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.exception).isEqualTo(exception)
        assertThat(error.message).isEqualTo("Failed to save")
    }
    
    @Test
    fun `saveAll with multiple receipts saves all successfully`() = runTest {
        // Given
        val receipts = listOf(
            createTestReceipt(id = "receipt-1"),
            createTestReceipt(id = "receipt-2"),
            createTestReceipt(id = "receipt-3")
        )
        receipts.forEach { receipt ->
            coEvery { receiptRepository.saveReceipt(receipt) } returns Result.Success(receipt)
        }
        
        // When
        val result = useCase.saveAll(receipts)
        
        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).hasSize(3)
        receipts.forEach { receipt ->
            coVerify(exactly = 1) { receiptRepository.saveReceipt(receipt) }
        }
    }
    
    @Test
    fun `saveAll fails when one receipt fails`() = runTest {
        // Given
        val receipts = listOf(
            createTestReceipt(id = "receipt-1"),
            createTestReceipt(id = "receipt-2"),
            createTestReceipt(id = "receipt-3")
        )
        coEvery { receiptRepository.saveReceipt(receipts[0]) } returns Result.Success(receipts[0])
        coEvery { receiptRepository.saveReceipt(receipts[1]) } returns Result.Error(Exception("Error"), "Failed")
        coEvery { receiptRepository.saveReceipt(receipts[2]) } returns Result.Success(receipts[2])
        
        // When
        val result = useCase.saveAll(receipts)
        
        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        assertThat((result as Result.Error).message).contains("receipt-2")
    }
    
    @Test
    fun `saveAll with empty list succeeds`() = runTest {
        // Given
        val receipts = emptyList<Receipt>()
        
        // When
        val result = useCase.saveAll(receipts)
        
        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEmpty()
    }
    
    private fun createTestReceipt(
        id: String = "test-receipt-id",
        payee: String = "Test Store",
        amount: Double = 25.50,
        date: Date = Date()
    ) = Receipt(
        id = id,
        payee = payee,
        amount = amount,
        date = date,
        imagePath = "/path/to/image.jpg",
        ocrText = "Sample OCR text",
        syncStatus = Receipt.SyncStatus.PENDING,
        createdAt = Date(),
        updatedAt = Date()
    )
}
