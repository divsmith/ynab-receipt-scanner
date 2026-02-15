package com.ynab.receiptscanner.ui.sync

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.ynab.receiptscanner.databinding.ViewSyncStatusBinding
import com.ynab.receiptscanner.domain.model.ConnectivityStatus
import com.ynab.receiptscanner.domain.model.SyncStatus
import java.text.SimpleDateFormat
import java.util.*

/**
 * Custom compound view for displaying sync status
 * Shows connectivity status, sync progress, and last sync time
 */
class SyncStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    private val binding: ViewSyncStatusBinding
    private var onRetryClickListener: (() -> Unit)? = null
    
    init {
        binding = ViewSyncStatusBinding.inflate(LayoutInflater.from(context), this, true)
        
        // Set up click listener for retry
        binding.root.setOnClickListener {
            onRetryClickListener?.invoke()
        }
    }
    
    /**
     * Update sync status display
     */
    fun setSyncStatus(status: SyncStatus, lastSyncTime: Date? = null) {
        when (status) {
            SyncStatus.PENDING -> {
                binding.syncIcon.setImageResource(com.ynab.receiptscanner.R.drawable.ic_sync_pending)
                binding.syncText.text = "Pending Sync"
                binding.syncIcon.clearAnimation()
            }
            SyncStatus.SYNCING -> {
                binding.syncIcon.setImageResource(com.ynab.receiptscanner.R.drawable.ic_sync)
                binding.syncText.text = "Syncing..."
                startSyncAnimation()
            }
            SyncStatus.SYNCED -> {
                binding.syncIcon.setImageResource(com.ynab.receiptscanner.R.drawable.ic_check)
                binding.syncText.text = "Synced"
                binding.syncIcon.clearAnimation()
            }
            SyncStatus.FAILED -> {
                binding.syncIcon.setImageResource(com.ynab.receiptscanner.R.drawable.ic_sync_error)
                binding.syncText.text = "Sync Failed - Tap to Retry"
                binding.syncIcon.clearAnimation()
            }
        }
        
        // Update last sync time
        if (lastSyncTime != null) {
            val timeStr = formatLastSyncTime(lastSyncTime)
            binding.lastSyncTime.text = "Last sync: $timeStr"
            binding.lastSyncTime.isVisible = true
        } else {
            binding.lastSyncTime.isVisible = false
        }
    }
    
    /**
     * Update connectivity status display
     */
    fun setConnectivityStatus(status: ConnectivityStatus) {
        when (status) {
            is ConnectivityStatus.Connected -> {
                if (status.isMetered) {
                    binding.connectivityBadge.text = "📱"
                    binding.connectivityBadge.isVisible = true
                } else {
                    binding.connectivityBadge.isVisible = false
                }
            }
            is ConnectivityStatus.Disconnected -> {
                binding.connectivityBadge.text = "⚠️"
                binding.connectivityBadge.isVisible = true
            }
            is ConnectivityStatus.Unknown -> {
                binding.connectivityBadge.isVisible = false
            }
        }
    }
    
    /**
     * Set pending transaction count
     */
    fun setPendingCount(count: Int) {
        if (count > 0) {
            binding.pendingBadge.text = count.toString()
            binding.pendingBadge.isVisible = true
        } else {
            binding.pendingBadge.isVisible = false
        }
    }
    
    /**
     * Set retry click listener
     */
    fun setOnRetryClickListener(listener: () -> Unit) {
        onRetryClickListener = listener
    }
    
    /**
     * Start rotating animation for sync icon
     */
    private fun startSyncAnimation() {
        binding.syncIcon.animate()
            .rotation(360f)
            .setDuration(1000)
            .setInterpolator(LinearInterpolator())
            .withEndAction {
                if (binding.syncIcon.rotation == 360f) {
                    binding.syncIcon.rotation = 0f
                    startSyncAnimation()
                }
            }
            .start()
    }
    
    /**
     * Format last sync time as relative time
     */
    private fun formatLastSyncTime(date: Date): String {
        val now = Date()
        val diffMs = now.time - date.time
        val diffMinutes = diffMs / (60 * 1000)
        val diffHours = diffMs / (60 * 60 * 1000)
        val diffDays = diffMs / (24 * 60 * 60 * 1000)
        
        return when {
            diffMinutes < 1 -> "Just now"
            diffMinutes < 60 -> "$diffMinutes minute${if (diffMinutes == 1L) "" else "s"} ago"
            diffHours < 24 -> "$diffHours hour${if (diffHours == 1L) "" else "s"} ago"
            diffDays < 7 -> "$diffDays day${if (diffDays == 1L) "" else "s"} ago"
            else -> {
                val format = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                format.format(date)
            }
        }
    }
}
