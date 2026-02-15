package com.ynab.receiptscanner.data.sync

import android.util.Log
import com.ynab.receiptscanner.core.Constants
import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.data.local.dao.PendingTransactionDao
import com.ynab.receiptscanner.data.local.entity.PendingTransactionEntity
import com.ynab.receiptscanner.data.local.preference.AuthPreferences
import com.ynab.receiptscanner.data.mapper.YnabMapper
import com.ynab.receiptscanner.data.remote.YnabApi
import com.ynab.receiptscanner.data.remote.dto.CreateTransactionRequest
import com.ynab.receiptscanner.domain.model.YnabTransaction
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow

/**
 * Manages background sync of pending transactions
 * Handles retry logic with exponential backoff
 */
@Singleton
class TransactionSyncManager @Inject constructor(
    private val api: YnabApi,
    private val pendingTransactionDao: PendingTransactionDao,
    private val authPreferences: AuthPreferences,
    private val ynabMapper: YnabMapper
) {
    
    companion object {
        private const val TAG = "TransactionSyncManager"
    }
    
    /**
     * Sync all pending transactions
     * @return Number of successfully synced transactions
     */
    suspend fun syncPendingTransactions(): Result<Int> {
        return try {
            // Check authentication
            if (!authPreferences.isAuthenticated()) {
                return Result.Error(Exception("Not authenticated"))
            }
            
            // Get budget ID (should be stored in preferences)
            val budgetId = authPreferences.getSelectedBudgetId()
            if (budgetId.isNullOrBlank()) {
                return Result.Error(Exception("No budget selected"))
            }
            
            // Get pending transactions
            val pendingTransactions = pendingTransactionDao
                .getPendingTransactionsByStatus("PENDING")
                .first()
            
            if (pendingTransactions.isEmpty()) {
                Log.d(TAG, "No pending transactions to sync")
                return Result.Success(0)
            }
            
            Log.d(TAG, "Syncing ${pendingTransactions.size} pending transactions")
            
            var successCount = 0
            
            for (pending in pendingTransactions) {
                when (val result = syncTransaction(pending, budgetId)) {
                    is Result.Success -> {
                        successCount++
                        // Mark as completed
                        pendingTransactionDao.updateStatus(pending.id, "COMPLETED", null)
                        Log.d(TAG, "Successfully synced transaction ${pending.id}")
                    }
                    is Result.Error -> {
                        // Update retry count and status
                        handleSyncError(pending, result.exception as? Exception ?: Exception(result.exception))
                    }
                    is Result.Loading -> {
                        // Should not happen in this context
                        Log.w(TAG, "Unexpected Loading state for transaction ${pending.id}")
                    }
                }
            }
            
            Result.Success(successCount)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing pending transactions", e)
            Result.Error(e)
        }
    }
    
    /**
     * Sync a single transaction
     */
    private suspend fun syncTransaction(
        pending: PendingTransactionEntity,
        budgetId: String
    ): Result<String> {
        return try {
            // Convert to YnabTransaction
            val transaction = YnabTransaction(
                id = pending.id,
                accountId = pending.accountId,
                categoryId = pending.categoryId,
                date = pending.date,
                amount = pending.amount,
                payee = pending.payee,
                memo = pending.memo,
                cleared = pending.cleared,
                approved = pending.approved,
                importId = pending.importId
            )
            
            // Create API request
            val dto = ynabMapper.mapTransactionToDto(transaction)
            val request = CreateTransactionRequest(dto)
            
            // Update status to processing
            pendingTransactionDao.updateStatus(pending.id, "PROCESSING", null)
            
            // Call API
            val response = api.createTransaction(budgetId, request)
            
            if (response.isSuccessful && response.body() != null) {
                val transactionId = response.body()!!.data.transaction?.id
                    ?: response.body()!!.data.transactionIds.firstOrNull()
                    ?: pending.id
                
                Result.Success(transactionId)
            } else {
                val errorMessage = "API error: ${response.code()}"
                Result.Error(Exception(errorMessage))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing transaction ${pending.id}", e)
            Result.Error(e)
        }
    }
    
    /**
     * Handle sync error with retry logic
     */
    private suspend fun handleSyncError(
        pending: PendingTransactionEntity,
        exception: Exception
    ) {
        val newRetryCount = pending.retryCount + 1
        
        if (newRetryCount >= Constants.SYNC_MAX_RETRIES) {
            // Max retries reached, mark as failed
            pendingTransactionDao.updateStatus(
                pending.id,
                "FAILED",
                exception.message ?: "Max retries exceeded"
            )
            Log.e(TAG, "Transaction ${pending.id} marked as FAILED after $newRetryCount attempts")
        } else {
            // Update retry count and keep as pending
            pendingTransactionDao.updateRetryCount(pending.id, newRetryCount)
            Log.w(TAG, "Transaction ${pending.id} retry count: $newRetryCount")
        }
    }
    
    /**
     * Retry failed transactions
     * Uses exponential backoff for retry delays
     */
    suspend fun retryFailedTransactions(): Result<Int> {
        return try {
            val budgetId = authPreferences.getSelectedBudgetId()
            if (budgetId.isNullOrBlank()) {
                return Result.Error(Exception("No budget selected"))
            }
            
            // Get transactions ready for retry
            val failedTransactions = pendingTransactionDao
                .getTransactionsReadyForRetry(Constants.SYNC_MAX_RETRIES)
                .first()
            
            if (failedTransactions.isEmpty()) {
                return Result.Success(0)
            }
            
            Log.d(TAG, "Retrying ${failedTransactions.size} failed transactions")
            
            var successCount = 0
            
            for (pending in failedTransactions) {
                // Calculate backoff delay
                val delayMs = calculateBackoffDelay(pending.retryCount)
                
                // Check if enough time has passed since last retry
                val timeSinceUpdate = System.currentTimeMillis() - pending.updatedAt.time
                if (timeSinceUpdate < delayMs) {
                    continue // Skip, not time to retry yet
                }
                
                // Reset status to pending and increment retry count
                pendingTransactionDao.updateStatus(pending.id, "PENDING", null)
                
                when (syncTransaction(pending, budgetId)) {
                    is Result.Success -> {
                        successCount++
                        pendingTransactionDao.updateStatus(pending.id, "COMPLETED", null)
                    }
                    is Result.Error -> {
                        handleSyncError(pending, Exception("Retry failed"))
                    }
                    is Result.Loading -> {
                        // Should not happen in this context
                        Log.w(TAG, "Unexpected Loading state during retry for ${pending.id}")
                    }
                }
                
                // Small delay between retries
                delay(500)
            }
            
            Result.Success(successCount)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error retrying failed transactions", e)
            Result.Error(e)
        }
    }
    
    /**
     * Calculate exponential backoff delay
     */
    private fun calculateBackoffDelay(retryCount: Int): Long {
        val baseDelay = Constants.SYNC_RETRY_INITIAL_DELAY_MS
        val maxDelay = Constants.SYNC_RETRY_MAX_DELAY_MS
        val multiplier = Constants.SYNC_RETRY_BACKOFF_MULTIPLIER
        
        val delay = (baseDelay * multiplier.pow(retryCount.toDouble())).toLong()
        return min(delay, maxDelay)
    }
    
    /**
     * Clear completed transactions (cleanup)
     * @param olderThanDays Remove completed transactions older than this many days
     */
    suspend fun clearCompletedTransactions(olderThanDays: Int = 7): Result<Int> {
        return try {
            val cutoffDate = Date(System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L))
            val count = pendingTransactionDao.deleteCompletedBefore(cutoffDate)
            Log.d(TAG, "Cleared $count completed transactions older than $olderThanDays days")
            Result.Success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing completed transactions", e)
            Result.Error(e)
        }
    }
    
    /**
     * Get sync status summary
     */
    suspend fun getSyncStatus(): SyncStatus {
        return try {
            val pending = pendingTransactionDao.getCountByStatus("PENDING").first()
            val processing = pendingTransactionDao.getCountByStatus("PROCESSING").first()
            val failed = pendingTransactionDao.getCountByStatus("FAILED").first()
            val completed = pendingTransactionDao.getCountByStatus("COMPLETED").first()
            
            SyncStatus(
                pending = pending,
                processing = processing,
                failed = failed,
                completed = completed,
                total = pending + processing + failed + completed
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting sync status", e)
            SyncStatus(0, 0, 0, 0, 0)
        }
    }
    
    /**
     * Data class for sync status summary
     */
    data class SyncStatus(
        val pending: Int,
        val processing: Int,
        val failed: Int,
        val completed: Int,
        val total: Int
    )
}
