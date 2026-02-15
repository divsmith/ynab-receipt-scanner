package com.ynab.receiptscanner.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.ynab.receiptscanner.R
import com.ynab.receiptscanner.databinding.ViewSyncStatusBadgeBinding
import com.ynab.receiptscanner.domain.model.SyncStatus

/**
 * Custom view for displaying sync status badge
 */
class SyncStatusBadge @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    
    private val binding: ViewSyncStatusBadgeBinding
    
    init {
        binding = ViewSyncStatusBadgeBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )
    }
    
    fun setStatus(status: SyncStatus) {
        when (status) {
            SyncStatus.PENDING -> {
                binding.statusText.text = context.getString(R.string.status_pending)
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.warning)
                )
                binding.statusIcon.setImageResource(R.drawable.ic_sync_pending)
            }
            SyncStatus.SYNCING -> {
                binding.statusText.text = context.getString(R.string.status_syncing)
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.info)
                )
                binding.statusIcon.setImageResource(R.drawable.ic_sync)
            }
            SyncStatus.SYNCED -> {
                binding.statusText.text = context.getString(R.string.status_synced)
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.success)
                )
                binding.statusIcon.setImageResource(R.drawable.ic_check)
            }
            SyncStatus.ERROR -> {
                binding.statusText.text = context.getString(R.string.status_error)
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.error)
                )
                binding.statusIcon.setImageResource(R.drawable.ic_sync_error)
            }
        }
    }
}
