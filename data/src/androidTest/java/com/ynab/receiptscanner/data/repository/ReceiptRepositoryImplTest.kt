package com.ynab.receiptscanner.data.repository

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.ynab.receiptscanner.data.local.YnabDatabase
import com.ynab.receiptscanner.data.local.dao.ReceiptDao
import com.ynab.receiptscanner.data.storage.ImageStorageManager
import com.ynab.receiptscanner.domain.model.Receipt
import com.ynab.receiptscanner.domain.model.SyncStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Date
import java.util.UUID
import com.ynab.receiptscanner.core.util.Result

/**
 * Integration tests for ReceiptRepositoryImpl
 * Tests repository operations with real Room database
 */
@RunWith(AndroidJUnit4::class)
class ReceiptRepositoryImplTest {
    
    private lateinit var database: YnabDatabase
    private lateinit var receiptDao: ReceiptDao
    private lateinit var imageStorageManager: ImageStorageManager
    private lateinit var repository: ReceiptRepositoryImpl
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Create in-memory database
        database = Room.inMemoryDatabaseBuilder(
            context,
            YnabDatabase::class.java
        ).allowMainThreadQueries().build()
        
        receiptDao = database.receiptDao()
        
        // Mock image storage manager
        imageStorageManager = mockk(relaxed = true)
        coEvery { imageStorageManager.saveImage(any()) } returns Result.Success("/test/image/path.jpg")
        coEvery { imageStorageManager.deleteImage(any()) } returns Result.Success(Unit)
        
        repository = ReceiptRepositoryImpl(receiptDao, imageStorageManager)
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun saveReceipt_insertsReceiptIntoDatabase() = runTest {
        // Given
        val receipt = createTestReceipt()
        
        // When
        val result = repository.saveReceipt(receipt)
        
        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val savedReceipt = (result as Result.Success).data
        assertThat(savedReceipt.id).isEqualTo(receipt.id)
        
        // Verify it's in database
        val retrieved = repository.getReceiptById(receipt.id).first()
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.id).isEqualTo(receipt.id)
    }
    
    @Test
    fun saveReceipt_updatesTimestamp() = runTest {
        // Given
        val receipt = createTestReceipt()
        val originalTime = Date(1000)
        val receiptWithOldTimestamp = receipt.copy(updatedAt = originalTime)
        
        // When
        repository.saveReceipt(receiptWithOldTimestamp)
        
        // Then
        val retrieved = repository.getReceiptById(receipt.id).first()
        assertThat(retrieved?.updatedAt).isNotEqualTo(originalTime)
    }
    
    @Test
    fun getAllReceipts_returnsAllReceipts() = runTest {
        // Given
        val receipts = listOf(
            createTestReceipt(id = "receipt-1"),
            createTestReceipt(id = "receipt-2"),
            createTestReceipt(id = "receipt-3")
        )
        receipts.forEach { repository.saveReceipt(it) }
        
        // When
        val retrieved = repository.getAllReceipts().first()
        
        // Then
        assertThat(retrieved).hasSize(3)
        assertThat(retrieved.map { it.id }).containsExactlyElementsIn(receipts.map { it.id })
    }
    
    @Test
    fun getReceiptsByStatus_returnsOnlyMatchingStatus() = runTest {
        // Given
        repository.saveReceipt(createTestReceipt(id = "1", syncStatus = SyncStatus.PENDING))
        repository.saveReceipt(createTestReceipt(id = "2", syncStatus = SyncStatus.SYNCED))
        repository.saveReceipt(createTestReceipt(id = "3", syncStatus = SyncStatus.PENDING))
        
        // When
        val pendingReceipts = repository.getReceiptsByStatus(SyncStatus.PENDING).first()
        
        // Then
        assertThat(pendingReceipts).hasSize(2)
        assertThat(pendingReceipts.map { it.id }).containsExactly("1", "3")
    }
    
    @Test
    fun searchReceiptsByPayee_findsMatchingReceipts() = runTest {
        // Given
        repository.saveReceipt(createTestReceipt(id = "1", payee = "Starbucks Coffee"))
        repository.saveReceipt(createTestReceipt(id = "2", payee = "Walmart"))
        repository.saveReceipt(createTestReceipt(id = "3", payee = "Coffee Bean"))
        
        // When
        val coffeeReceipts = repository.searchReceiptsByPayee("Coffee").first()
        
        // Then
        assertThat(coffeeReceipts).hasSize(2)
        assertThat(coffeeReceipts.map { it.payee }).containsExactly("Starbucks Coffee", "Coffee Bean")
    }
    
    @Test
    fun deleteReceipt_removesFromDatabase() = runTest {
        // Given
        val receipt = createTestReceipt(imagePath = "/test/image.jpg")
        repository.saveReceipt(receipt)
        
        // When
        val deleteResult = repository.deleteReceipt(receipt)
        
        // Then
        assertThat(deleteResult).isInstanceOf(Result.Success::class.java)
        val retrieved = repository.getReceiptById(receipt.id).first()
        assertThat(retrieved).isNull()
    }
    
    @Test
    fun deleteReceipt_deletesAssociatedImage() = runTest {
        // Given
        val imagePath = "/test/image.jpg"
        val receipt = createTestReceipt(imagePath = imagePath)
        repository.saveReceipt(receipt)
        
        // When
        repository.deleteReceipt(receipt)
        
        // Then
        coVerify { imageStorageManager.deleteImage(imagePath) }
    }
    
    @Test
    fun updateSyncStatus_changesStatus() = runTest {
        // Given
        val receipt = createTestReceipt(syncStatus = SyncStatus.PENDING)
        repository.saveReceipt(receipt)
        
        // When
        val result = repository.updateSyncStatus(receipt.id, SyncStatus.SYNCED)
        
        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val updated = repository.getReceiptById(receipt.id).first()
        assertThat(updated?.syncStatus).isEqualTo(SyncStatus.SYNCED)
    }
    
    @Test
    fun getPendingSyncReceipts_returnsPendingAndFailed() = runTest {
        // Given
        repository.saveReceipt(createTestReceipt(id = "1", syncStatus = SyncStatus.PENDING))
        repository.saveReceipt(createTestReceipt(id = "2", syncStatus = SyncStatus.SYNCED))
        repository.saveReceipt(createTestReceipt(id = "3", syncStatus = SyncStatus.FAILED))
        
        // When
        val pendingSync = repository.getPendingSyncReceipts().first()
        
        // Then
        assertThat(pendingSync).hasSize(2)
        assertThat(pendingSync.map { it.id }).containsExactly("1", "3")
    }
    
    @Test
    fun getReceiptCountByStatus_returnsCorrectCount() = runTest {
        // Given
        repository.saveReceipt(createTestReceipt(syncStatus = SyncStatus.PENDING))
        repository.saveReceipt(createTestReceipt(syncStatus = SyncStatus.PENDING))
        repository.saveReceipt(createTestReceipt(syncStatus = SyncStatus.SYNCED))
        
        // When
        val pendingCount = repository.getReceiptCountByStatus(SyncStatus.PENDING).first()
        val syncedCount = repository.getReceiptCountByStatus(SyncStatus.SYNCED).first()
        
        // Then
        assertThat(pendingCount).isEqualTo(2)
        assertThat(syncedCount).isEqualTo(1)
    }
    
    @Test
    fun getReceiptsByDateRange_returnsReceiptsInRange() = runTest {
        // Given
        val date1 = Date(1000000)
        val date2 = Date(2000000)
        val date3 = Date(3000000)
        
        repository.saveReceipt(createTestReceipt(id = "1", date = date1))
        repository.saveReceipt(createTestReceipt(id = "2", date = date2))
        repository.saveReceipt(createTestReceipt(id = "3", date = date3))
        
        // When
        val receiptsInRange = repository.getReceiptsByDateRange(
            Date(1500000),
            Date(2500000)
        ).first()
        
        // Then
        assertThat(receiptsInRange).hasSize(1)
        assertThat(receiptsInRange[0].id).isEqualTo("2")
    }
    
    @Test
    fun saveReceipt_withImageStorage_savesImageAndReceipt() = runTest {
        // Given
        val receipt = createTestReceipt()
        
        // When
        val result = repository.saveReceipt(receipt)
        
        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val retrieved = repository.getReceiptById(receipt.id).first()
        assertThat(retrieved).isNotNull()
    }
    
    @Test
    fun deleteReceiptById_removesReceipt() = runTest {
        // Given
        val receipt = createTestReceipt()
        repository.saveReceipt(receipt)
        
        // When
        val result = repository.deleteReceiptById(receipt.id)
        
        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val retrieved = repository.getReceiptById(receipt.id).first()
        assertThat(retrieved).isNull()
    }
    
    @Test
    fun repository_handlesMultipleSaveOperations() = runTest {
        // Given
        val receipt = createTestReceipt()
        
        // When - Save multiple times
        repository.saveReceipt(receipt)
        val updated = receipt.copy(amount = 99.99)
        repository.saveReceipt(updated)
        
        // Then - Should replace, not duplicate
        val allReceipts = repository.getAllReceipts().first()
        assertThat(allReceipts).hasSize(1)
        assertThat(allReceipts[0].amount).isEqualTo(99.99)
    }
    
    private fun createTestReceipt(
        id: String = UUID.randomUUID().toString(),
        payee: String = "Test Store",
        amount: Double = 25.50,
        date: Date = Date(),
        imagePath: String? = "/test/path.jpg",
        syncStatus: SyncStatus = SyncStatus.PENDING
    ) = Receipt(
        id = id,
        payee = payee,
        amount = amount,
        date = date,
        imagePath = imagePath,
        ocrText = "Sample OCR text",
        syncStatus = syncStatus,
        ynabTransactionId = null,
        accountId = null,
        categoryId = null,
        createdAt = Date(),
        updatedAt = Date()
    )
}
