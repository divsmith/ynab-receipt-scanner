package com.ynab.receiptscanner.ui.review

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ynab.receiptscanner.R
import com.ynab.receiptscanner.databinding.ItemAccountBinding
import com.ynab.receiptscanner.domain.model.YnabAccount
import java.text.NumberFormat
import java.util.Locale

/**
 * RecyclerView adapter for YNAB account selection
 */
class AccountAdapter(
    private val onAccountClick: (YnabAccount) -> Unit
) : ListAdapter<YnabAccount, AccountAdapter.AccountViewHolder>(AccountDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val binding = ItemAccountBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AccountViewHolder(binding, onAccountClick)
    }
    
    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class AccountViewHolder(
        private val binding: ItemAccountBinding,
        private val onAccountClick: (YnabAccount) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
        
        fun bind(account: YnabAccount) {
            binding.root.setOnClickListener { onAccountClick(account) }
            
            binding.accountName.text = account.name
            binding.accountType.text = account.type.replaceFirstChar { it.uppercase() }
            binding.accountBalance.text = currencyFormat.format(account.getBalanceInCurrency())
            
            // Set appropriate icon based on account type
            val iconRes = when (account.type) {
                "checking" -> R.drawable.ic_account_checking
                "savings" -> R.drawable.ic_account_savings
                "creditCard" -> R.drawable.ic_account_credit
                else -> R.drawable.ic_account
            }
            binding.accountIcon.setImageResource(iconRes)
            
            // Color balance based on positive/negative
            val balanceColor = if (account.balance >= 0) {
                R.color.success
            } else {
                R.color.error
            }
            binding.accountBalance.setTextColor(
                ContextCompat.getColor(binding.root.context, balanceColor)
            )
        }
    }
    
    class AccountDiffCallback : DiffUtil.ItemCallback<YnabAccount>() {
        override fun areItemsTheSame(oldItem: YnabAccount, newItem: YnabAccount): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: YnabAccount, newItem: YnabAccount): Boolean {
            return oldItem == newItem
        }
    }
}
