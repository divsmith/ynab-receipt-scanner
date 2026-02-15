package com.ynab.receiptscanner.domain.model

import java.util.Date

/**
 * Domain model representing a YNAB budget
 * @property id YNAB budget ID
 * @property name Budget name
 * @property lastModifiedOn Date when budget was last modified
 * @property accounts List of accounts in this budget
 * @property categories List of categories in this budget
 */
data class YnabBudget(
    val id: String,
    val name: String,
    val lastModifiedOn: Date,
    val accounts: List<YnabAccount> = emptyList(),
    val categories: List<YnabCategory> = emptyList()
) {
    /**
     * Returns all active (non-closed) accounts
     */
    fun getActiveAccounts(): List<YnabAccount> {
        return accounts.filter { it.isActive() }
    }
    
    /**
     * Returns account by ID if it exists
     */
    fun getAccount(accountId: String): YnabAccount? {
        return accounts.find { it.id == accountId }
    }
    
    /**
     * Returns category by ID if it exists
     */
    fun getCategory(categoryId: String): YnabCategory? {
        return categories.find { it.id == categoryId }
    }
    
    /**
     * Returns all categories with available funds
     */
    fun getCategoriesWithFunds(): List<YnabCategory> {
        return categories.filter { it.hasAvailableFunds() }
    }
}
