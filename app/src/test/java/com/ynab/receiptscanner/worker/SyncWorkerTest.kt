package com.ynab.receiptscanner.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.data.local.dao.PendingTransactionDao
import com.ynab.receiptscanner.data.local.entity.PendingTransactionEntity
import com.ynab.receiptscanner.data.sync.SyncStatusTracker
import com.ynab.receiptscanner.data.sync.TransactionSyncManager
import com.ynab.receiptscanner.notification.NotificationHelper
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.util.*

/**
 * Unit tests for SyncWorker
 */
@RunWith(RobolectricTestRunner::class)
class SyncWorkerTest {
    
    private lateinit var context: Context
    
    @Mock
    private lateinit var transactionSyncManager: TransactionSyncManager
    
    @Mock
    private lateinit var pendingTransactionDao: PendingTransactionDao
    
    @Mock
    private lateinit var syncStatusTracker: SyncStatusTracker
    
    @Mock
    private lateinit var notificationHelper: NotificationHelper
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
    }
    
    @Test
    fun `test successful sync`() = runBlocking {
        // Given: 3 pending transactions
        val pendingTransactions = listOf(
            createPendingTransaction("1"),
            createPendingTransaction("2"),
            createPendingTransaction("3")
        )
        whenever(pendingTransactionDao.getPendingTransactionsByStatus("PENDING"))
            .thenReturn(flowOf(pendingTransactions))
        
        // And: Sync manager returns success
        whenever(transactionSyncManager.syncPendingTransactions())
            .thenReturn(Result.Success(3))
        
        // And: No failed transactions
        whenever(pendingTransactionDao.getPendingTransactionsByStatus("FAILED"))
            .thenReturn(flowOf(emptyList()))
        
        // When: Worker executes
        val worker = TestListenableWorkerBuilder<TestSyncWorker>(context)
            .build()
        worker.transactionSyncManager = transactionSyncManager
        worker.pendingTransactionDao = pendingTransactionDao
        worker.syncStatusTracker = syncStatusTracker
        worker.notificationHelper = notificationHelper
        
        val result = worker.doWork()
        
        // Then: Result is success
        assertTrue(result is ListenableWorker.Result.Success)
        
        // And: Sync was called
        verify(transactionSyncManager).syncPendingTransactions()
    }
    
    @Test
    fun `test no pending transactions`() = runBlocking {
        // Given: No pending transactions
        whenever(pendingTransactionDao.getPendingTransactionsByStatus("PENDING"))
            .thenReturn(flowOf(emptyList()))
        
        // When: Worker executes
        val worker = TestListenableWorkerBuilder<TestSyncWorker>(context)
            .build()
        worker.transactionSyncManager = transactionSyncManager
        worker.pendingTransactionDao = pendingTransactionDao
        worker.syncStatusTracker = syncStatusTracker
        worker.notificationHelper = notificationHelper
        
        val result = worker.doWork()
        
        // Then: Result is success
        assertTrue(result is ListenableWorker.Result.Success)
        
        // And: Sync was not called
        verify(transactionSyncManager, never()).syncPendingTransactions()
    }
    
    @Test
    fun `test sync with failures`() = runBlocking {
        // Given: 5 pending transactions
        val pendingTransactions = List(5) { createPendingTransaction(it.toString()) }
        whenever(pendingTransactionDao.getPendingTransactionsByStatus("PENDING"))
            .thenReturn(flowOf(pendingTransactions))
        
        // And: Sync manager returns partial success (3 succeeded)
        whenever(transactionSyncManager.syncPendingTransactions())
            .thenReturn(Result.Success(3))
        
        // And: 2 failed transactions remain
        val failedTransactions = listOf(
            createPendingTransaction("4", "FAILED"),
            createPendingTransaction("5", "FAILED")
        )
        whenever(pendingTransactionDao.getPendingTransactionsByStatus("FAILED"))
            .thenReturn(flowOf(failedTransactions))
        
        // When: Worker executes
        val worker = TestListenableWorkerBuilder<TestSyncWorker>(context)
            .build()
        worker.transactionSyncManager = transactionSyncManager
        worker.pendingTransactionDao = pendingTransactionDao
        worker.syncStatusTracker = syncStatusTracker
        worker.notificationHelper = notificationHelper
        
        val result = worker.doWork()
        
        // Then: Result is success (partial success is still success)
        assertTrue(result is ListenableWorker.Result.Success)
    }
    
    @Test
    fun `test rate limiting triggers retry`() = runBlocking {
        // Given: Pending transactions
        val pendingTransactions = listOf(createPendingTransaction("1"))
        whenever(pendingTransactionDao.getPendingTransactionsByStatus("PENDING"))
            .thenReturn(flowOf(pendingTransactions))
        
        // And: Sync fails with rate limit error
        whenever(transactionSyncManager.syncPendingTransactions())
            .thenReturn(Result.Error(Exception("HTTP 429: Rate limited")))
        
        // When: Worker executes
        val worker = TestListenableWorkerBuilder<TestSyncWorker>(context)
            .build()
        worker.transactionSyncManager = transactionSyncManager
        worker.pendingTransactionDao = pendingTransactionDao
        worker.syncStatusTracker = syncStatusTracker
        worker.notificationHelper = notificationHelper
        
        val result = worker.doWork()
        
        // Then: Result is retry
        assertTrue(result is ListenableWorker.Result.Retry)
    }
    
    private fun createPendingTransaction(id: String, status: String = "PENDING"): PendingTransactionEntity {
        return PendingTransactionEntity(
            id = id,
            receiptId = null,
            accountId = "account_$id",
            categoryId = null,
            date = Date(),
            amount = 10000,
            payee = "Test Payee $id",
            memo = null,
            cleared = "uncleared",
            approved = false,
            importId = null,
            status = status,
            retryCount = 0,
            lastError = null,
            createdAt = Date(),
            updatedAt = Date()
        )
    }
    
    /**
     * Test implementation of SyncWorker that allows dependency injection
     */
    class TestSyncWorker(
        appContext: Context,
        workerParams: androidx.work.WorkerParameters
    ) : SyncWorker(
        appContext,
        workerParams,
        mock(TransactionSyncManager::class.java),
        mock(PendingTransactionDao::class.java),
        mock(SyncStatusTracker::class.java),
        mock(NotificationHelper::class.java)
    ) {
        lateinit var transactionSyncManager: TransactionSyncManager
        lateinit var pendingTransactionDao: PendingTransactionDao
        lateinit var syncStatusTracker: SyncStatusTracker
        lateinit var notificationHelper: NotificationHelper
    }
}
