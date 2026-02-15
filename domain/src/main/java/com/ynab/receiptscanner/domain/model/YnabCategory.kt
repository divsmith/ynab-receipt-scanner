package com.ynab.receiptscanner.domain.model

/**
 * Domain model representing a YNAB category
 * @property id YNAB category ID
 * @property categoryGroupId YNAB category group ID this category belongs to
 * @property name Category name
 * @property budgeted Amount budgeted for this category in milliunits
 * @property activity Activity (spending) in this category in milliunits
 * @property balance Available balance in this category in milliunits
 */
data class YnabCategory(
    val id: String,
    val categoryGroupId: String,
    val name: String,
    val budgeted: Long,
    val activity: Long,
    val balance: Long
) {
    /**
     * Returns the budgeted amount in standard currency units
     */
    fun getBudgetedInCurrency(): Double {
        return budgeted / 1000.0
    }
    
    /**
     * Returns the activity in standard currency units
     */
    fun getActivityInCurrency(): Double {
        return activity / 1000.0
    }
    
    /**
     * Returns the balance in standard currency units
     */
    fun getBalanceInCurrency(): Double {
        return balance / 1000.0
    }
    
    /**
     * Returns true if this category has available funds
     */
    fun hasAvailableFunds(): Boolean {
        return balance > 0
    }
}
