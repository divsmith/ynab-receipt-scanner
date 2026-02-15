package com.ynab.receiptscanner.data.sync

import android.util.Log
import com.ynab.receiptscanner.data.local.dao.ReceiptDao
import com.ynab.receiptscanner.data.mapper.toDomain
import com.ynab.receiptscanner.domain.model.Receipt
import com.ynab.receiptscanner.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow

/**
 * Tracks sync status and manages retry logic for receipts
 * Implements exponential backoff for failed syncs
 */
@Singleton
class SyncStatusTracker @Inject constructor(
    private val receiptDao: ReceiptDao
) {
    companion object {
        private const val TAG = "SyncStatusTracker"
        
        // Retry configuration
        private const val BASE_DELAY_MS = 5000L // 5 seconds
        private const val MAX_DELAY_MS = 300000L // 5 minutes
        private const val BACKOFF_MULTIPLIER = 2.0
    }
    
    /**
     * Get all receipts with a specific sync status
     */
    fun getReceiptsByStatus(status: SyncStatus): Flow<List<Receipt>> {
        return receiptDao.getReceiptsBySyncStatus(status)
            .map { entities -> entities.map { it.toDomain() } }
    }
    
    /**
     * Get all pending or failed receipts that need syncing
     */
    fun getPendingSyncReceipts(): Flow<List<Receipt>> {
        return receiptDao.getAllReceipts()
            .map { entities ->
                entities
                    .filter { it.syncStatus == SyncStatus.PENDING || it.syncStatus == SyncStatus.FAILED }
                    .map { it.toDomain() }
            }
    }
    
    /**
     * Update sync status for a receipt
     */
    suspend fun updateSyncStatus(
        receiptId: String,
        status: SyncStatus,
        errorMessage: String? = null
    ) {
        Log.d(TAG, "Updating sync status for receipt $receiptId to $status")
        receiptDao.updateSyncStatus(receiptId, status)
        // Error messages are typically handled in PendingTransaction entity
    }
    
    /**
     * Mark receipt as syncing
     */
    suspend fun markAsSyncing(receiptId: String) {
        updateSyncStatus(receiptId, SyncStatus.SYNCING)
    }
    
    /**
     * Mark receipt as synced successfully
     */
    suspend fun markAsSynced(receiptId: String) {
        updateSyncStatus(receiptId, SyncStatus.SYNCED)
        Log.d(TAG, "Receipt $receiptId synced successfully")
    }
    
    /**
     * Mark receipt as failed with error message
     */
    suspend fun markAsFailed(receiptId: String, errorMessage: String) {
        updateSyncStatus(receiptId, SyncStatus.FAILED, errorMessage)
        Log.w(TAG, "Receipt $receiptId sync failed: $errorMessage")
    }
    
    /**
     * Calculate retry delay using exponential backoff
     * @param retryCount Number of previous retry attempts
     * @return Delay in milliseconds before next retry
     */
    fun calculateRetryDelay(retryCount: Int): Long {
        val delay = (BASE_DELAY_MS * BACKOFF_MULTIPLIER.pow(retryCount.toDouble())).toLong()
        return min(delay, MAX_DELAY_MS)
    }
    
    /**
     * Check if enough time has passed for a retry
     * @param lastAttemptTime Timestamp of last sync attempt
     * @param retryCount Number of previous retry attempts
     * @return True if ready for retry
     */
    fun isReadyForRetry(lastAttemptTime: Date, retryCount: Int): Boolean {
        val now = Date()
        val requiredDelay = calculateRetryDelay(retryCount)
        val timeSinceLastAttempt = now.time - lastAttemptTime.time
        
        val ready = timeSinceLastAttempt >= requiredDelay
        if (ready) {
            Log.d(TAG, "Ready for retry (attempt ${retryCount + 1})")
        } else {
            val remainingMs = requiredDelay - timeSinceLastAttempt
            Log.d(TAG, "Not ready for retry, ${remainingMs}ms remaining")
        }
        return ready
    }
    
    /**
     * Get human-readable description of retry delay
     */
    fun getRetryDelayDescription(retryCount: Int): String {
        val delayMs = calculateRetryDelay(retryCount)
        val seconds = delayMs / 1000
        return when {
            seconds < 60 -> "$seconds seconds"
            seconds < 3600 -> "${seconds / 60} minutes"
            else -> "${seconds / 3600} hours"
        }
    }
}
