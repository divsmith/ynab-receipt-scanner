package com.ynab.receiptscanner.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules and manages background sync work
 * Handles periodic sync and one-time sync requests
 */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SyncScheduler"
        
        // Default sync interval
        const val DEFAULT_SYNC_INTERVAL_MINUTES = 60L // 1 hour
        
        // Sync work tags
        const val SYNC_WORK_TAG = "sync_work"
        const val PERIODIC_SYNC_TAG = "periodic_sync"
        const val ONE_TIME_SYNC_TAG = "one_time_sync"
    }
    
    private val workManager = WorkManager.getInstance(context)
    
    /**
     * Schedule periodic sync work
     * @param intervalMinutes Interval between syncs in minutes
     * @param requireUnmetered Whether to require WiFi (unmetered network)
     */
    fun schedulePeriodicSync(
        intervalMinutes: Long = DEFAULT_SYNC_INTERVAL_MINUTES,
        requireUnmetered: Boolean = false
    ) {
        Log.d(TAG, "Scheduling periodic sync every $intervalMinutes minutes, unmetered: $requireUnmetered")
        
        // Create constraints
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (requireUnmetered) NetworkType.UNMETERED else NetworkType.CONNECTED
            )
            .setRequiresBatteryNotLow(false) // Allow sync even on low battery
            .build()
        
        // Create periodic work request
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            intervalMinutes,
            TimeUnit.MINUTES,
            15, // Flex interval - can run up to 15 minutes before the interval
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(SYNC_WORK_TAG)
            .addTag(PERIODIC_SYNC_TAG)
            .build()
        
        // Enqueue work with replace policy
        workManager.enqueueUniquePeriodicWork(
            SyncWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest
        )
        
        Log.d(TAG, "Periodic sync scheduled successfully")
    }
    
    /**
     * Trigger immediate one-time sync
     * @param manual Whether this is a manual user-triggered sync
     */
    fun triggerImmediateSync(manual: Boolean = false) {
        Log.d(TAG, "Triggering immediate sync (manual: $manual)")
        
        // Create constraints - require network
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        // Create input data
        val inputData = Data.Builder()
            .putBoolean(SyncWorker.KEY_MANUAL_TRIGGER, manual)
            .build()
        
        // Create one-time work request
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(SYNC_WORK_TAG)
            .addTag(ONE_TIME_SYNC_TAG)
            .build()
        
        // Enqueue work
        workManager.enqueueUniqueWork(
            SyncWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
        
        Log.d(TAG, "Immediate sync triggered")
    }
    
    /**
     * Cancel all sync work
     */
    fun cancelAllSync() {
        Log.d(TAG, "Cancelling all sync work")
        workManager.cancelUniqueWork(SyncWorker.PERIODIC_WORK_NAME)
        workManager.cancelUniqueWork(SyncWorker.WORK_NAME)
    }
    
    /**
     * Cancel periodic sync only
     */
    fun cancelPeriodicSync() {
        Log.d(TAG, "Cancelling periodic sync")
        workManager.cancelUniqueWork(SyncWorker.PERIODIC_WORK_NAME)
    }
    
    /**
     * Update sync interval for periodic work
     */
    fun updateSyncInterval(intervalMinutes: Long, requireUnmetered: Boolean = false) {
        Log.d(TAG, "Updating sync interval to $intervalMinutes minutes")
        schedulePeriodicSync(intervalMinutes, requireUnmetered)
    }
    
    /**
     * Get info about sync work status
     */
    fun getSyncWorkInfo() = workManager.getWorkInfosByTagLiveData(SYNC_WORK_TAG)
}
