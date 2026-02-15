package com.ynab.receiptscanner.usecase

import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.data.sync.TransactionSyncManager
import javax.inject.Inject

/**
 * Use case for syncing pending transactions
 */
class SyncPendingTransactionsUseCase @Inject constructor(
    private val syncManager: TransactionSyncManager
) {
    /**
     * Sync all pending transactions
     * @return Result with number of successfully synced transactions
     */
    suspend operator fun invoke(): Result<Int> {
        return syncManager.syncPendingTransactions()
    }
    
    /**
     * Retry failed transactions
     * @return Result with number of successfully synced transactions
     */
    suspend fun retryFailed(): Result<Int> {
        return syncManager.retryFailedTransactions()
    }
    
    /**
     * Get sync status
     * @return Sync status with counts
     */
    suspend fun getStatus(): TransactionSyncManager.SyncStatus {
        return syncManager.getSyncStatus()
    }
}
