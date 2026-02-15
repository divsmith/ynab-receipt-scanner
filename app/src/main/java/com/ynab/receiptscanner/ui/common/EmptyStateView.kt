package com.ynab.receiptscanner.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.ynab.receiptscanner.databinding.ViewEmptyStateBinding

/**
 * Custom view for displaying empty states
 */
class EmptyStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    
    private val binding: ViewEmptyStateBinding
    
    init {
        binding = ViewEmptyStateBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )
    }
    
    fun setEmptyState(iconRes: Int, title: String, message: String) {
        binding.emptyIcon.setImageResource(iconRes)
        binding.emptyTitle.text = title
        binding.emptyMessage.text = message
    }
    
    fun setActionButton(text: String, onClick: () -> Unit) {
        binding.emptyAction.text = text
        binding.emptyAction.setOnClickListener { onClick() }
        binding.emptyAction.visibility = VISIBLE
    }
    
    fun hideActionButton() {
        binding.emptyAction.visibility = GONE
    }
}
