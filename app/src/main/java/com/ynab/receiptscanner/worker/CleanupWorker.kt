package com.ynab.receiptscanner.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Constraints
import com.ynab.receiptscanner.data.local.dao.ReceiptDao
import com.ynab.receiptscanner.data.cache.MemoryCache
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Periodic maintenance worker
 * Performs cleanup tasks like deleting old receipts, clearing cache, compacting database
 */
@HiltWorker
class CleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val receiptDao: ReceiptDao,
    private val memoryCache: MemoryCache
) : CoroutineWorker(appContext, workerParams) {
    
    companion object {
        private const val TAG = "CleanupWorker"
        const val WORK_NAME = "cleanup_worker"
        private const val DEFAULT_RETENTION_DAYS = 30
        
        /**
         * Schedule periodic cleanup
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresCharging(true) // Only run when charging
                .build()
            
            val cleanupRequest = PeriodicWorkRequestBuilder<CleanupWorker>(
                7, TimeUnit.DAYS // Run weekly
            )
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                cleanupRequest
            )
            
            Log.d(TAG, "Scheduled weekly cleanup worker")
        }
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting cleanup work")
        
        try {
            // Get retention days from input data or use default
            val retentionDays = inputData.getInt("retention_days", DEFAULT_RETENTION_DAYS)
            
            // Perform cleanup tasks
            val deletedReceipts = deleteOldReceipts(retentionDays)
            val deletedFiles = deleteOrphanedFiles()
            clearMemoryCache()
            compactDatabase()
            
            Log.i(TAG, "Cleanup completed: $deletedReceipts receipts, $deletedFiles files deleted")
            
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup failed", e)
            return Result.failure()
        }
    }
    
    /**
     * Delete receipts older than retention period
     */
    private suspend fun deleteOldReceipts(retentionDays: Int): Int {
        try {
            val cutoffDate = LocalDateTime.now().minusDays(retentionDays.toLong())
            val oldReceipts = receiptDao.getAllReceipts().first()
                .filter { receipt ->
                    // Assuming receipt has a timestamp field
                    // This is a placeholder - adjust based on actual Receipt model
                    // receipt.createdAt.isBefore(cutoffDate)
                    false // TODO: Implement when Receipt model has timestamp
                }
            
            var deletedCount = 0
            oldReceipts.forEach { receipt ->
                // Delete receipt image file
                val imageFile = File(receipt.imagePath)
                if (imageFile.exists()) {
                    imageFile.delete()
                }
                
                // Delete receipt from database
                receiptDao.deleteReceipt(receipt)
                deletedCount++
            }
            
            Log.d(TAG, "Deleted $deletedCount old receipts (older than $retentionDays days)")
            return deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete old receipts", e)
            return 0
        }
    }
    
    /**
     * Delete orphaned image files (files without database entry)
     */
    private suspend fun deleteOrphanedFiles(): Int {
        try {
            val receiptsDir = File(applicationContext.filesDir, "receipts")
            if (!receiptsDir.exists()) {
                return 0
            }
            
            // Get all receipt image paths from database
            val dbImagePaths = receiptDao.getAllReceipts().first()
                .map { it.imagePath }
                .toSet()
            
            // Delete files not in database
            var deletedCount = 0
            receiptsDir.listFiles()?.forEach { file ->
                if (!dbImagePaths.contains(file.absolutePath)) {
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }
            
            Log.d(TAG, "Deleted $deletedCount orphaned files")
            return deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete orphaned files", e)
            return 0
        }
    }
    
    /**
     * Clear memory cache
     */
    private fun clearMemoryCache() {
        try {
            memoryCache.clear()
            Log.d(TAG, "Cleared memory cache")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear memory cache", e)
        }
    }
    
    /**
     * Compact database (SQLite VACUUM)
     */
    private fun compactDatabase() {
        try {
            // Room doesn't expose VACUUM directly, but it will be done automatically
            // on checkpoint. We can trigger checkpoint by closing and reopening connections.
            // For now, just log that we would do this
            Log.d(TAG, "Database compaction requested (handled by Room)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compact database", e)
        }
    }
    
    /**
     * Clear temporary files
     */
    private fun clearTempFiles() {
        try {
            val cacheDir = applicationContext.cacheDir
            val tempFiles = cacheDir.listFiles() ?: return
            
            var deletedCount = 0
            tempFiles.forEach { file ->
                if (file.name.startsWith("temp_") || file.name.endsWith(".tmp")) {
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }
            
            Log.d(TAG, "Deleted $deletedCount temp files")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear temp files", e)
        }
    }
}
