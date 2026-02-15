package com.ynab.receiptscanner.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.ynab.receiptscanner.data.local.dao.PendingTransactionDao
import com.ynab.receiptscanner.data.local.entity.PendingTransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import java.util.UUID

/**
 * Integration tests for PendingTransactionDao
 * Tests transaction queue operations
 */
@RunWith(AndroidJUnit4::class)
class PendingTransactionDaoTest {
    
    private lateinit var database: YnabDatabase
    private lateinit var dao: PendingTransactionDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            YnabDatabase::class.java
        ).allowMainThreadQueries().build()
        
        dao = database.pendingTransactionDao()
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun insertPendingTransaction_thenRetrieve_returnsTransaction() = runTest {
        // Given
        val transaction = createTestTransaction()
        
        // When
        dao.insertPendingTransaction(transaction)
        val retrieved = dao.getPendingTransactionById(transaction.id).first()
        
        // Then
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.id).isEqualTo(transaction.id)
        assertThat(retrieved?.amount).isEqualTo(transaction.amount)
        assertThat(retrieved?.payeeName).isEqualTo(transaction.payeeName)
    }
    
    @Test
    fun getAllPendingTransactions_returnsInCreationOrder() = runTest {
        // Given
        val transactions = listOf(
            createTestTransaction(id = "tx-1", createdAt = Date(1000)),
            createTestTransaction(id = "tx-2", createdAt = Date(2000)),
            createTestTransaction(id = "tx-3", createdAt = Date(3000))
        )
        
        // When
        dao.insertPendingTransactions(transactions)
        val retrieved = dao.getAllPendingTransactions().first()
        
        // Then
        assertThat(retrieved).hasSize(3)
        // Should be oldest first (ASC order for queue)
        assertThat(retrieved[0].id).isEqualTo("tx-1")
        assertThat(retrieved[1].id).isEqualTo("tx-2")
        assertThat(retrieved[2].id).isEqualTo("tx-3")
    }
    
    @Test
    fun getPendingTransactionsByStatus_returnsOnlyMatchingStatus() = runTest {
        // Given
        dao.insertPendingTransactions(listOf(
            createTestTransaction(id = "tx-1", status = "PENDING"),
            createTestTransaction(id = "tx-2", status = "SYNCED"),
            createTestTransaction(id = "tx-3", status = "PENDING"),
            createTestTransaction(id = "tx-4", status = "FAILED")
        ))
        
        // When
        val pendingTransactions = dao.getPendingTransactionsByStatus("PENDING").first()
        
        // Then
        assertThat(pendingTransactions).hasSize(2)
        assertThat(pendingTransactions.map { it.id }).containsExactly("tx-1", "tx-3")
    }
    
    @Test
    fun updatePendingTransaction_updatesStatus() = runTest {
        // Given
        val transaction = createTestTransaction(status = "PENDING", retryCount = 0)
        dao.insertPendingTransaction(transaction)
        
        // When
        val updated = transaction.copy(
            status = "SYNCED",
            retryCount = 0,
            lastAttemptAt = Date()
        )
        dao.updatePendingTransaction(updated)
        val retrieved = dao.getPendingTransactionById(transaction.id).first()
        
        // Then
        assertThat(retrieved?.status).isEqualTo("SYNCED")
    }
    
    @Test
    fun updatePendingTransaction_incrementsRetryCount() = runTest {
        // Given
        val transaction = createTestTransaction(status = "PENDING", retryCount = 0)
        dao.insertPendingTransaction(transaction)
        
        // When - Simulate multiple failures
        val attempt1 = transaction.copy(retryCount = 1, status = "FAILED")
        dao.updatePendingTransaction(attempt1)
        
        val attempt2 = attempt1.copy(retryCount = 2)
        dao.updatePendingTransaction(attempt2)
        
        val retrieved = dao.getPendingTransactionById(transaction.id).first()
        
        // Then
        assertThat(retrieved?.retryCount).isEqualTo(2)
        assertThat(retrieved?.status).isEqualTo("FAILED")
    }
    
    @Test
    fun deletePendingTransaction_removesFromQueue() = runTest {
        // Given
        val transaction = createTestTransaction()
        dao.insertPendingTransaction(transaction)
        
        // When
        dao.deletePendingTransaction(transaction)
        val retrieved = dao.getPendingTransactionById(transaction.id).first()
        
        // Then
        assertThat(retrieved).isNull()
    }
    
    @Test
    fun getCountByStatus_returnsCorrectCount() = runTest {
        // Given
        dao.insertPendingTransactions(listOf(
            createTestTransaction(status = "PENDING"),
            createTestTransaction(status = "PENDING"),
            createTestTransaction(status = "SYNCED"),
            createTestTransaction(status = "FAILED")
        ))
        
        // When
        val pendingCount = dao.getCountByStatus("PENDING").first()
        val syncedCount = dao.getCountByStatus("SYNCED").first()
        val failedCount = dao.getCountByStatus("FAILED").first()
        
        // Then
        assertThat(pendingCount).isEqualTo(2)
        assertThat(syncedCount).isEqualTo(1)
        assertThat(failedCount).isEqualTo(1)
    }
    
    @Test
    fun getPendingTransactionsForReceipt_returnsLinkedTransactions() = runTest {
        // Given
        val receiptId = "receipt-123"
        dao.insertPendingTransactions(listOf(
            createTestTransaction(id = "tx-1", receiptId = receiptId),
            createTestTransaction(id = "tx-2", receiptId = "other-receipt"),
            createTestTransaction(id = "tx-3", receiptId = receiptId)
        ))
        
        // When
        val transactions = dao.getPendingTransactionsForReceipt(receiptId).first()
        
        // Then
        assertThat(transactions).hasSize(2)
        assertThat(transactions.map { it.id }).containsExactly("tx-1", "tx-3")
    }
    
    @Test
    fun getTransactionsReadyForRetry_returnsOnlyEligibleTransactions() = runTest {
        // Given
        val maxRetries = 3
        dao.insertPendingTransactions(listOf(
            createTestTransaction(id = "tx-1", status = "FAILED", retryCount = 0),
            createTestTransaction(id = "tx-2", status = "FAILED", retryCount = 2),
            createTestTransaction(id = "tx-3", status = "FAILED", retryCount = 3), // Max reached
            createTestTransaction(id = "tx-4", status = "PENDING", retryCount = 0), // Not failed
            createTestTransaction(id = "tx-5", status = "FAILED", retryCount = 1)
        ))
        
        // When
        val retryable = dao.getTransactionsReadyForRetry(maxRetries).first()
        
        // Then
        assertThat(retryable).hasSize(3)
        assertThat(retryable.map { it.id }).containsExactly("tx-1", "tx-2", "tx-5")
    }
    
    @Test
    fun getOldestPending_returnsOldestPendingTransaction() = runTest {
        // Given
        dao.insertPendingTransactions(listOf(
            createTestTransaction(id = "tx-1", status = "PENDING", createdAt = Date(3000)),
            createTestTransaction(id = "tx-2", status = "PENDING", createdAt = Date(1000)),
            createTestTransaction(id = "tx-3", status = "SYNCED", createdAt = Date(500)),
            createTestTransaction(id = "tx-4", status = "PENDING", createdAt = Date(2000))
        ))
        
        // When
        val oldest = dao.getOldestPending().first()
        
        // Then
        assertThat(oldest).isNotNull()
        assertThat(oldest?.id).isEqualTo("tx-2") // Oldest PENDING
    }
    
    @Test
    fun insertPendingTransaction_withConflict_replacesExisting() = runTest {
        // Given
        val transaction = createTestTransaction(id = "tx-1", payeeName = "Original")
        dao.insertPendingTransaction(transaction)
        
        // When
        val replacement = transaction.copy(payeeName = "Updated")
        dao.insertPendingTransaction(replacement)
        val retrieved = dao.getPendingTransactionById("tx-1").first()
        
        // Then
        assertThat(retrieved?.payeeName).isEqualTo("Updated")
        assertThat(dao.getAllPendingTransactions().first()).hasSize(1)
    }
    
    @Test
    fun multipleRetryCycles_tracksRetryCountCorrectly() = runTest {
        // Given
        val transaction = createTestTransaction(status = "PENDING", retryCount = 0)
        dao.insertPendingTransaction(transaction)
        
        // When - Simulate retry cycle
        val maxRetries = 3
        repeat(maxRetries + 1) { attempt ->
            val updated = transaction.copy(
                retryCount = attempt,
                status = if (attempt < maxRetries) "FAILED" else "MAX_RETRIES_EXCEEDED",
                lastAttemptAt = Date()
            )
            dao.updatePendingTransaction(updated)
        }
        
        val retrieved = dao.getPendingTransactionById(transaction.id).first()
        
        // Then
        assertThat(retrieved?.retryCount).isEqualTo(maxRetries)
        assertThat(retrieved?.status).isEqualTo("MAX_RETRIES_EXCEEDED")
        
        // Should not be in retry queue anymore
        val retryable = dao.getTransactionsReadyForRetry(maxRetries).first()
        assertThat(retryable).isEmpty()
    }
    
    private fun createTestTransaction(
        id: String = UUID.randomUUID().toString(),
        receiptId: String? = "receipt-123",
        budgetId: String = "budget-456",
        accountId: String = "account-789",
        amount: Long = -25500, // Negative for outflow
        payeeName: String? = "Test Store",
        status: String = "PENDING",
        retryCount: Int = 0,
        createdAt: Date = Date()
    ) = PendingTransactionEntity(
        id = id,
        receiptId = receiptId,
        budgetId = budgetId,
        accountId = accountId,
        date = Date(),
        amount = amount,
        payeeName = payeeName,
        categoryId = null,
        memo = "Test transaction",
        cleared = "cleared",
        approved = true,
        importId = "YNAB:$id",
        status = status,
        retryCount = retryCount,
        errorMessage = null,
        lastAttemptAt = null,
        createdAt = createdAt
    )
}
