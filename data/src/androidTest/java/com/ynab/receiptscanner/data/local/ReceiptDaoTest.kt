package com.ynab.receiptscanner.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.ynab.receiptscanner.data.local.dao.LineItemDao
import com.ynab.receiptscanner.data.local.dao.ReceiptDao
import com.ynab.receiptscanner.data.local.entity.LineItemEntity
import com.ynab.receiptscanner.data.local.entity.ReceiptEntity
import com.ynab.receiptscanner.domain.model.SyncStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import java.util.UUID

/**
 * Integration tests for ReceiptDao
 * Tests CRUD operations and queries
 */
@RunWith(AndroidJUnit4::class)
class ReceiptDaoTest {
    
    private lateinit var database: YnabDatabase
    private lateinit var receiptDao: ReceiptDao
    private lateinit var lineItemDao: LineItemDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Use in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            YnabDatabase::class.java
        ).allowMainThreadQueries().build()
        
        receiptDao = database.receiptDao()
        lineItemDao = database.lineItemDao()
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun insertReceipt_thenRetrieve_returnsReceipt() = runTest {
        // Given
        val receipt = createTestReceipt()
        
        // When
        receiptDao.insertReceipt(receipt)
        val retrieved = receiptDao.getReceiptById(receipt.id).first()
        
        // Then
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.id).isEqualTo(receipt.id)
        assertThat(retrieved?.payee).isEqualTo(receipt.payee)
        assertThat(retrieved?.amount).isEqualTo(receipt.amount)
    }
    
    @Test
    fun insertMultipleReceipts_getAllReceipts_returnsAllInDescOrder() = runTest {
        // Given
        val receipts = listOf(
            createTestReceipt(id = "receipt-1", createdAt = Date(1000)),
            createTestReceipt(id = "receipt-2", createdAt = Date(2000)),
            createTestReceipt(id = "receipt-3", createdAt = Date(3000))
        )
        
        // When
        receiptDao.insertReceipts(receipts)
        val retrieved = receiptDao.getAllReceipts().first()
        
        // Then
        assertThat(retrieved).hasSize(3)
        // Should be newest first
        assertThat(retrieved[0].id).isEqualTo("receipt-3")
        assertThat(retrieved[1].id).isEqualTo("receipt-2")
        assertThat(retrieved[2].id).isEqualTo("receipt-1")
    }
    
    @Test
    fun updateReceipt_updatesValues() = runTest {
        // Given
        val receipt = createTestReceipt(payee = "Original Payee")
        receiptDao.insertReceipt(receipt)
        
        // When
        val updated = receipt.copy(
            payee = "Updated Payee",
            amount = 99.99,
            syncStatus = SyncStatus.SYNCED
        )
        receiptDao.updateReceipt(updated)
        val retrieved = receiptDao.getReceiptById(receipt.id).first()
        
        // Then
        assertThat(retrieved?.payee).isEqualTo("Updated Payee")
        assertThat(retrieved?.amount).isEqualTo(99.99)
        assertThat(retrieved?.syncStatus).isEqualTo(SyncStatus.SYNCED)
    }
    
    @Test
    fun deleteReceipt_removesFromDatabase() = runTest {
        // Given
        val receipt = createTestReceipt()
        receiptDao.insertReceipt(receipt)
        
        // When
        receiptDao.deleteReceipt(receipt)
        val retrieved = receiptDao.getReceiptById(receipt.id).first()
        
        // Then
        assertThat(retrieved).isNull()
    }
    
    @Test
    fun getReceiptsByStatus_returnsOnlyMatchingReceipts() = runTest {
        // Given
        receiptDao.insertReceipts(listOf(
            createTestReceipt(id = "receipt-1", syncStatus = SyncStatus.PENDING),
            createTestReceipt(id = "receipt-2", syncStatus = SyncStatus.SYNCED),
            createTestReceipt(id = "receipt-3", syncStatus = SyncStatus.PENDING),
            createTestReceipt(id = "receipt-4", syncStatus = SyncStatus.FAILED)
        ))
        
        // When
        val pendingReceipts = receiptDao.getReceiptsByStatus(SyncStatus.PENDING).first()
        
        // Then
        assertThat(pendingReceipts).hasSize(2)
        assertThat(pendingReceipts.map { it.id }).containsExactly("receipt-1", "receipt-3")
    }
    
    @Test
    fun getReceiptCountByStatus_returnsCorrectCount() = runTest {
        // Given
        receiptDao.insertReceipts(listOf(
            createTestReceipt(id = "receipt-1", syncStatus = SyncStatus.PENDING),
            createTestReceipt(id = "receipt-2", syncStatus = SyncStatus.PENDING),
            createTestReceipt(id = "receipt-3", syncStatus = SyncStatus.SYNCED)
        ))
        
        // When
        val pendingCount = receiptDao.getReceiptCountByStatus(SyncStatus.PENDING).first()
        val syncedCount = receiptDao.getReceiptCountByStatus(SyncStatus.SYNCED).first()
        
        // Then
        assertThat(pendingCount).isEqualTo(2)
        assertThat(syncedCount).isEqualTo(1)
    }
    
    @Test
    fun getPendingSyncReceipts_returnsPendingAndFailed() = runTest {
        // Given
        receiptDao.insertReceipts(listOf(
            createTestReceipt(id = "receipt-1", syncStatus = SyncStatus.PENDING),
            createTestReceipt(id = "receipt-2", syncStatus = SyncStatus.SYNCED),
            createTestReceipt(id = "receipt-3", syncStatus = SyncStatus.FAILED)
        ))
        
        // When
        val pendingSync = receiptDao.getPendingSyncReceipts().first()
        
        // Then
        assertThat(pendingSync).hasSize(2)
        assertThat(pendingSync.map { it.id }).containsExactly("receipt-1", "receipt-3")
    }
    
    @Test
    fun searchReceiptsByPayee_returnsMatchingReceipts() = runTest {
        // Given
        receiptDao.insertReceipts(listOf(
            createTestReceipt(id = "receipt-1", payee = "Starbucks Coffee"),
            createTestReceipt(id = "receipt-2", payee = "Walmart"),
            createTestReceipt(id = "receipt-3", payee = "Coffee Bean")
        ))
        
        // When
        val coffeeReceipts = receiptDao.searchReceiptsByPayee("Coffee").first()
        
        // Then
        assertThat(coffeeReceipts).hasSize(2)
        assertThat(coffeeReceipts.map { it.payee }).containsExactly("Starbucks Coffee", "Coffee Bean")
    }
    
    @Test
    fun cascadingDelete_deletesLineItemsWhenReceiptDeleted() = runTest {
        // Given
        val receipt = createTestReceipt(id = "receipt-with-items")
        receiptDao.insertReceipt(receipt)
        
        val lineItems = listOf(
            createTestLineItem(id = "item-1", receiptId = receipt.id),
            createTestLineItem(id = "item-2", receiptId = receipt.id)
        )
        lineItems.forEach { lineItemDao.insertLineItem(it) }
        
        // When
        receiptDao.deleteReceipt(receipt)
        val remainingItems = lineItemDao.getLineItemsForReceipt(receipt.id).first()
        
        // Then
        assertThat(remainingItems).isEmpty()
    }
    
    @Test
    fun getReceiptsByDateRange_returnsReceiptsInRange() = runTest {
        // Given
        val date1 = Date(1000000)
        val date2 = Date(2000000)
        val date3 = Date(3000000)
        
        receiptDao.insertReceipts(listOf(
            createTestReceipt(id = "receipt-1", date = date1),
            createTestReceipt(id = "receipt-2", date = date2),
            createTestReceipt(id = "receipt-3", date = date3)
        ))
        
        // When
        val receiptsInRange = receiptDao.getReceiptsByDateRange(
            Date(1500000),
            Date(2500000)
        ).first()
        
        // Then
        assertThat(receiptsInRange).hasSize(1)
        assertThat(receiptsInRange[0].id).isEqualTo("receipt-2")
    }
    
    @Test
    fun insertReceipt_withConflict_replacesExisting() = runTest {
        // Given
        val receipt = createTestReceipt(id = "receipt-1", payee = "Original")
        receiptDao.insertReceipt(receipt)
        
        // When
        val replacement = receipt.copy(payee = "Replacement")
        receiptDao.insertReceipt(replacement)
        val retrieved = receiptDao.getReceiptById("receipt-1").first()
        
        // Then
        assertThat(retrieved?.payee).isEqualTo("Replacement")
        assertThat(receiptDao.getAllReceipts().first()).hasSize(1)
    }
    
    private fun createTestReceipt(
        id: String = UUID.randomUUID().toString(),
        payee: String = "Test Store",
        amount: Double = 25.50,
        date: Date = Date(),
        syncStatus: SyncStatus = SyncStatus.PENDING,
        createdAt: Date = Date()
    ) = ReceiptEntity(
        id = id,
        payee = payee,
        amount = amount,
        date = date,
        imagePath = "/path/to/image.jpg",
        ocrText = "Sample OCR text",
        syncStatus = syncStatus,
        ynabTransactionId = null,
        accountId = null,
        categoryId = null,
        createdAt = createdAt,
        updatedAt = Date()
    )
    
    private fun createTestLineItem(
        id: String = UUID.randomUUID().toString(),
        receiptId: String,
        description: String = "Test Item",
        amount: Double = 10.00
    ) = LineItemEntity(
        id = id,
        receiptId = receiptId,
        description = description,
        quantity = 1,
        unitPrice = amount,
        totalPrice = amount
    )
}
