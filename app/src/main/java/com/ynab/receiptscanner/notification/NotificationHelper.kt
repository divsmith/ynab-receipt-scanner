package com.ynab.receiptscanner.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ynab.receiptscanner.R
import com.ynab.receiptscanner.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for showing sync-related notifications
 * Handles notification channels and permission checks
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Channel IDs
        private const val CHANNEL_ID_SYNC = "sync_channel"
        private const val CHANNEL_ID_ERRORS = "sync_errors_channel"
        
        // Notification IDs
        const val SYNC_NOTIFICATION_ID = 1001
        private const val NOTIFICATION_ID_SYNC_PROGRESS = 1001
        private const val NOTIFICATION_ID_SYNC_COMPLETE = 1002
        private const val NOTIFICATION_ID_SYNC_ERROR = 1003
        
        // Channel names
        private const val CHANNEL_NAME_SYNC = "Sync Status"
        private const val CHANNEL_NAME_ERRORS = "Sync Errors"
    }
    
    init {
        createNotificationChannels()
    }
    
    /**
     * Create notification channels for Android O+
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Create sync status channel
            val syncChannel = NotificationChannel(
                CHANNEL_ID_SYNC,
                CHANNEL_NAME_SYNC,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows sync progress and completion"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(syncChannel)
            
            // Create errors channel
            val errorsChannel = NotificationChannel(
                CHANNEL_ID_ERRORS,
                CHANNEL_NAME_ERRORS,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows sync errors that need attention"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(errorsChannel)
        }
    }
    
    /**
     * Show sync in progress notification
     */
    fun showSyncInProgress(pendingCount: Int) {
        if (!hasNotificationPermission()) return
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SYNC)
            .setSmallIcon(R.drawable.ic_sync)
            .setContentTitle(context.getString(R.string.notification_sync_in_progress))
            .setContentText(
                context.resources.getQuantityString(
                    R.plurals.notification_syncing_transactions,
                    pendingCount,
                    pendingCount
                )
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SYNC_PROGRESS, notification)
    }
    
    /**
     * Show sync complete notification
     */
    fun showSyncComplete(successCount: Int, failedCount: Int) {
        if (!hasNotificationPermission()) return
        
        // Cancel progress notification
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_SYNC_PROGRESS)
        
        // Build completion notification
        val contentText = if (failedCount > 0) {
            context.getString(
                R.string.notification_sync_complete_with_errors,
                successCount,
                failedCount
            )
        } else {
            context.resources.getQuantityString(
                R.plurals.notification_synced_transactions,
                successCount,
                successCount
            )
        }
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SYNC)
            .setSmallIcon(R.drawable.ic_check)
            .setContentTitle(context.getString(R.string.notification_sync_complete))
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SYNC_COMPLETE, notification)
    }
    
    /**
     * Show sync error notification with retry action
     */
    fun showSyncError(errorMessage: String) {
        if (!hasNotificationPermission()) return
        
        // Cancel progress notification
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_SYNC_PROGRESS)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ERRORS)
            .setSmallIcon(R.drawable.ic_sync_error)
            .setContentTitle(context.getString(R.string.notification_sync_error))
            .setContentText(errorMessage)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SYNC_ERROR, notification)
    }
    
    /**
     * Dismiss all sync notifications
     */
    fun dismissAllSyncNotifications() {
        NotificationManagerCompat.from(context).apply {
            cancel(NOTIFICATION_ID_SYNC_PROGRESS)
            cancel(NOTIFICATION_ID_SYNC_COMPLETE)
            cancel(NOTIFICATION_ID_SYNC_ERROR)
        }
    }
    
    /**
     * Create sync progress notification for foreground service
     * Used by SyncWorker to run as foreground service
     */
    fun createSyncProgressNotification(): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(context, CHANNEL_ID_SYNC)
            .setSmallIcon(R.drawable.ic_sync)
            .setContentTitle(context.getString(R.string.notification_sync_in_progress))
            .setContentText("Syncing pending transactions...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()
    }
    
    /**
     * Check if app has notification permission (Android 13+)
     */
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // No permission needed before Android 13
        }
    }
}
