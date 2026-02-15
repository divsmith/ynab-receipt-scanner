package com.ynab.receiptscanner.data.local.dao

import androidx.room.*
import com.ynab.receiptscanner.data.local.entity.PendingTransactionEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Data Access Object for PendingTransaction operations
 * Used for offline queue management
 */
@Dao
interface PendingTransactionDao {
    
    /**
     * Get all pending transactions ordered by creation date
     */
    @Query("SELECT * FROM pending_transactions ORDER BY created_at ASC")
    fun getAllPendingTransactions(): Flow<List<PendingTransactionEntity>>
    
    /**
     * Get a pending transaction by ID
     */
    @Query("SELECT * FROM pending_transactions WHERE id = :transactionId")
    fun getPendingTransactionById(transactionId: String): Flow<PendingTransactionEntity?>
    
    /**
     * Get pending transactions by status
     */
    @Query("SELECT * FROM pending_transactions WHERE status = :status ORDER BY created_at ASC")
    fun getPendingTransactionsByStatus(status: String): Flow<List<PendingTransactionEntity>>
    
    /**
     * Get pending transactions for a receipt
     */
    @Query("SELECT * FROM pending_transactions WHERE receipt_id = :receiptId")
    fun getPendingTransactionsForReceipt(receiptId: String): Flow<List<PendingTransactionEntity>>
    
    /**
     * Get transactions ready for retry (FAILED status and retry count below max)
     */
    @Query("SELECT * FROM pending_transactions WHERE status = 'FAILED' AND retry_count < :maxRetries ORDER BY created_at ASC")
    fun getTransactionsReadyForRetry(maxRetries: Int): Flow<List<PendingTransactionEntity>>
    
    /**
     * Get count of pending transactions by status
     */
    @Query("SELECT COUNT(*) FROM pending_transactions WHERE status = :status")
    fun getCountByStatus(status: String): Flow<Int>
    
    /**
     * Get oldest pending transaction
     */
    @Query("SELECT * FROM pending_transactions WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT 1")
    fun getOldestPending(): Flow<PendingTransactionEntity?>
    
    /**
     * Insert a pending transaction
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingTransaction(transaction: PendingTransactionEntity)
    
    /**
     * Insert multiple pending transactions
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingTransactions(transactions: List<PendingTransactionEntity>)
    
    /**
     * Update a pending transaction
     */
    @Update
    suspend fun updatePendingTransaction(transaction: PendingTransactionEntity)
    
    /**
     * Delete a pending transaction
     */
    @Delete
    suspend fun deletePendingTransaction(transaction: PendingTransactionEntity)
    
    /**
     * Delete a pending transaction by ID
     */
    @Query("DELETE FROM pending_transactions WHERE id = :transactionId")
    suspend fun deletePendingTransactionById(transactionId: String)
    
    /**
     * Update status for a pending transaction
     */
    @Query("UPDATE pending_transactions SET status = :status, last_error = :error, updated_at = datetime('now') WHERE id = :transactionId")
    suspend fun updateStatus(transactionId: String, status: String, error: String?)
    
    /**
     * Update retry count for a transaction
     */
    @Query("UPDATE pending_transactions SET retry_count = :retryCount, updated_at = datetime('now') WHERE id = :transactionId")
    suspend fun updateRetryCount(transactionId: String, retryCount: Int)
    
    /**
     * Increment retry count for a transaction
     */
    @Query("UPDATE pending_transactions SET retry_count = retry_count + 1, last_error = :error, status = :status, updated_at = datetime('now') WHERE id = :transactionId")
    suspend fun incrementRetryCount(transactionId: String, error: String?, status: String)
    
    /**
     * Delete completed transactions older than specified date
     */
    @Query("DELETE FROM pending_transactions WHERE status = 'COMPLETED' AND updated_at < :beforeDate")
    suspend fun deleteCompletedBefore(beforeDate: Date): Int
    
    /**
     * Delete all pending transactions
     */
    @Query("DELETE FROM pending_transactions")
    suspend fun deleteAllPendingTransactions()
}
