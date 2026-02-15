package com.ynab.receiptscanner.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.ynab.receiptscanner.databinding.ItemReceiptBinding
import com.ynab.receiptscanner.domain.model.Receipt

/**
 * RecyclerView adapter for receipt list
 * Uses ListAdapter with DiffUtil for efficient updates
 */
class ReceiptAdapter(
    private val onItemClick: (Receipt) -> Unit,
    private val onDeleteClick: (Receipt) -> Unit
) : ListAdapter<Receipt, ReceiptViewHolder>(ReceiptDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptViewHolder {
        val binding = ItemReceiptBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReceiptViewHolder(binding, onItemClick, onDeleteClick)
    }
    
    override fun onBindViewHolder(holder: ReceiptViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

/**
 * DiffUtil callback for efficient RecyclerView updates
 */
class ReceiptDiffCallback : DiffUtil.ItemCallback<Receipt>() {
    override fun areItemsTheSame(oldItem: Receipt, newItem: Receipt): Boolean {
        return oldItem.id == newItem.id
    }
    
    override fun areContentsTheSame(oldItem: Receipt, newItem: Receipt): Boolean {
        return oldItem == newItem
    }
}
