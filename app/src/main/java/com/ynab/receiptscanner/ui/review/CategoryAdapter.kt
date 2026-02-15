package com.ynab.receiptscanner.ui.review

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ynab.receiptscanner.R
import com.ynab.receiptscanner.databinding.ItemCategoryBinding
import com.ynab.receiptscanner.domain.model.YnabCategory
import java.text.NumberFormat
import java.util.Locale

/**
 * RecyclerView adapter for YNAB category selection
 */
class CategoryAdapter(
    private val onCategoryClick: (YnabCategory) -> Unit
) : ListAdapter<YnabCategory, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding, onCategoryClick)
    }
    
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class CategoryViewHolder(
        private val binding: ItemCategoryBinding,
        private val onCategoryClick: (YnabCategory) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
        
        fun bind(category: YnabCategory) {
            binding.root.setOnClickListener { onCategoryClick(category) }
            
            binding.categoryName.text = category.name
            binding.categoryBalance.text = currencyFormat.format(category.getBalanceInCurrency())
            
            // Set budgeted amount if available
            if (category.budgeted > 0) {
                binding.categoryBudgeted.text = "Budgeted: ${currencyFormat.format(category.getBudgetedInCurrency())}"
            } else {
                binding.categoryBudgeted.text = ""
            }
            
            // Color balance based on availability
            val balanceColor = if (category.hasAvailableFunds()) {
                R.color.success
            } else {
                R.color.warning
            }
            binding.categoryBalance.setTextColor(
                ContextCompat.getColor(binding.root.context, balanceColor)
            )
        }
    }
    
    class CategoryDiffCallback : DiffUtil.ItemCallback<YnabCategory>() {
        override fun areItemsTheSame(oldItem: YnabCategory, newItem: YnabCategory): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: YnabCategory, newItem: YnabCategory): Boolean {
            return oldItem == newItem
        }
    }
}
