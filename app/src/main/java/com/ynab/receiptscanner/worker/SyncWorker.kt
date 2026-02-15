package com.ynab.receiptscanner.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.data.local.dao.PendingTransactionDao
import com.ynab.receiptscanner.data.sync.SyncStatusTracker
import com.ynab.receiptscanner.data.sync.TransactionSyncManager
import com.ynab.receiptscanner.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Background worker for syncing pending transactions to YNAB
 * Uses Hilt for dependency injection
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionSyncManager: TransactionSyncManager,
    private val pendingTransactionDao: PendingTransactionDao,
    private val syncStatusTracker: SyncStatusTracker,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, workerParams) {
    
    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME = "sync_pending_transactions"
        const val PERIODIC_WORK_NAME = "periodic_sync"
        
        // Worker input/output keys
        const val KEY_MANUAL_TRIGGER = "manual_trigger"
        const val KEY_SUCCESS_COUNT = "success_count"
        const val KEY_FAILURE_COUNT = "failure_count"
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting sync work (attempt ${runAttemptCount + 1})")
        
        try {
            // Check if there are pending transactions
            val pendingCount = pendingTransactionDao
                .getPendingTransactionsByStatus("PENDING")
                .first()
                .size
            
            if (pendingCount == 0) {
                Log.d(TAG, "No pending transactions to sync")
                return Result.success()
            }
            
            // Show notification that sync is starting
            val isManualTrigger = inputData.getBoolean(KEY_MANUAL_TRIGGER, false)
            if (isManualTrigger) {
                notificationHelper.showSyncInProgress(pendingCount)
            }
            
            // Perform sync
            val syncResult = transactionSyncManager.syncPendingTransactions()
            
            when (syncResult) {
                is com.ynab.receiptscanner.core.util.Result.Success -> {
                    val successCount = syncResult.data
                    Log.d(TAG, "Successfully synced $successCount transactions")
                    
                    // Check if there are any failed transactions
                    val failedCount = pendingTransactionDao
                        .getPendingTransactionsByStatus("FAILED")
                        .first()
                        .size
                    
                    // Show completion notification
                    if (isManualTrigger || successCount > 0) {
                        notificationHelper.showSyncComplete(successCount, failedCount)
                    }
                    
                    // Return success with statistics
                    val outputData = androidx.work.Data.Builder()
                        .putInt(KEY_SUCCESS_COUNT, successCount)
                        .putInt(KEY_FAILURE_COUNT, failedCount)
                        .build()
                    
                    return Result.success(outputData)
                }
                is com.ynab.receiptscanner.core.util.Result.Error -> {
                    val error = syncResult.exception
                    Log.e(TAG, "Sync failed", error)
                    
                    // Check if it's a rate limit error (429)
                    if (error.message?.contains("429") == true) {
                        Log.w(TAG, "Rate limited, will retry later")
                        // Let WorkManager handle retry with exponential backoff
                        return Result.retry()
                    }
                    
                    // Show error notification
                    if (isManualTrigger) {
                        notificationHelper.showSyncError(error.message ?: "Unknown error")
                    }
                    
                    // Retry for transient errors
                    if (runAttemptCount < 3) {
                        return Result.retry()
                    }
                    
                    return Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during sync", e)
            
            // Show error notification for manual triggers
            val isManualTrigger = inputData.getBoolean(KEY_MANUAL_TRIGGER, false)
            if (isManualTrigger) {
                notificationHelper.showSyncError(e.message ?: "Unexpected error")
            }
            
            // Retry on unexpected errors
            if (runAttemptCount < 3) {
                return Result.retry()
            }
            
            return Result.failure()
        }
    }
    
    override suspend fun getForegroundInfo(): ForegroundInfo {
        // Create notification for long-running sync
        val notification = notificationHelper.createSyncProgressNotification()
        
        return ForegroundInfo(
            NotificationHelper.SYNC_NOTIFICATION_ID,
            notification
        )
    }
}
