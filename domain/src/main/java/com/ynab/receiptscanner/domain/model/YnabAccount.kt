package com.ynab.receiptscanner.domain.model

/**
 * Domain model representing a YNAB account
 * @property id YNAB account ID
 * @property budgetId YNAB budget ID this account belongs to
 * @property name Account name
 * @property type Account type (checking, savings, creditCard, etc.)
 * @property balance Current account balance in milliunits
 * @property closed Whether the account is closed
 */
data class YnabAccount(
    val id: String,
    val budgetId: String,
    val name: String,
    val type: String,
    val balance: Long,
    val closed: Boolean = false
) {
    /**
     * Returns the balance in standard currency units
     */
    fun getBalanceInCurrency(): Double {
        return balance / 1000.0
    }
    
    /**
     * Returns true if this is a credit card account
     */
    fun isCreditCard(): Boolean {
        return type == "creditCard"
    }
    
    /**
     * Returns true if this account can be used for transactions
     */
    fun isActive(): Boolean {
        return !closed
    }
}
