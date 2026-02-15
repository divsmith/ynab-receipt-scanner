package com.ynab.receiptscanner.ui.home

import android.graphics.BitmapFactory
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ynab.receiptscanner.R
import com.ynab.receiptscanner.databinding.ItemReceiptBinding
import com.ynab.receiptscanner.domain.model.Receipt
import com.ynab.receiptscanner.domain.model.SyncStatus
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * ViewHolder for receipt list items
 * Displays receipt preview with thumbnail, payee, amount, date, and sync status
 */
class ReceiptViewHolder(
    private val binding: ItemReceiptBinding,
    private val onItemClick: (Receipt) -> Unit,
    private val onDeleteClick: (Receipt) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
    
    fun bind(receipt: Receipt) {
        binding.root.setOnClickListener { onItemClick(receipt) }
        
        // Load thumbnail
        receipt.imagePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(path)
                binding.receiptThumbnail.setImageBitmap(bitmap)
            } else {
                binding.receiptThumbnail.setImageResource(R.drawable.ic_receipt)
            }
        } ?: run {
            binding.receiptThumbnail.setImageResource(R.drawable.ic_receipt)
        }
        
        // Payee
        binding.payeeText.text = receipt.payee ?: binding.root.context.getString(R.string.unknown_payee)
        
        // Amount
        if (receipt.amount != null) {
            binding.amountText.text = currencyFormat.format(receipt.amount)
        } else {
            binding.amountText.text = binding.root.context.getString(R.string.unknown_amount)
        }
        
        // Date
        if (receipt.date != null) {
            binding.dateText.text = dateFormat.format(receipt.date)
        } else {
            binding.dateText.text = binding.root.context.getString(R.string.unknown_date)
        }
        
        // Sync status badge
        updateSyncStatusBadge(receipt.syncStatus)
        
        // Delete button
        binding.deleteButton.setOnClickListener {
            onDeleteClick(receipt)
        }
    }
    
    private fun updateSyncStatusBadge(status: SyncStatus) {
        val context = binding.root.context
        
        when (status) {
            SyncStatus.PENDING -> {
                binding.syncBadge.text = context.getString(R.string.status_pending)
                binding.syncBadge.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.warning)
                )
                binding.syncBadge.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_sync_pending, 0, 0, 0
                )
            }
            SyncStatus.SYNCING -> {
                binding.syncBadge.text = context.getString(R.string.status_syncing)
                binding.syncBadge.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.info)
                )
                binding.syncBadge.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_sync, 0, 0, 0
                )
            }
            SyncStatus.SYNCED -> {
                binding.syncBadge.text = context.getString(R.string.status_synced)
                binding.syncBadge.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.success)
                )
                binding.syncBadge.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_check, 0, 0, 0
                )
            }
            SyncStatus.FAILED -> {
                binding.syncBadge.text = context.getString(R.string.status_error)
                binding.syncBadge.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.error)
                )
                binding.syncBadge.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_sync_error, 0, 0, 0
                )
            }
        }
    }
}
