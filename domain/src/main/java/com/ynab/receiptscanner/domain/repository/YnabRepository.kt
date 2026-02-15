package com.ynab.receiptscanner.domain.repository

import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for YNAB operations
 * Defines the contract for YNAB data access
 */
interface YnabRepository {
    
    /**
     * Get all budgets for the authenticated user
     * @param forceRefresh Force fetch from network instead of cache
     * @return Result with list of budgets or error
     */
    suspend fun getBudgets(forceRefresh: Boolean = false): Result<List<YnabBudget>>
    
    /**
     * Get a specific budget with details
     * @param budgetId Budget ID
     * @param forceRefresh Force fetch from network instead of cache
     * @return Result with budget or error
     */
    suspend fun getBudget(budgetId: String, forceRefresh: Boolean = false): Result<YnabBudget>
    
    /**
     * Get accounts for a budget
     * @param budgetId Budget ID
     * @param forceRefresh Force fetch from network instead of cache
     * @return Result with list of accounts or error
     */
    suspend fun getAccounts(budgetId: String, forceRefresh: Boolean = false): Result<List<YnabAccount>>
    
    /**
     * Get categories for a budget
     * @param budgetId Budget ID
     * @param forceRefresh Force fetch from network instead of cache
     * @return Result with list of categories or error
     */
    suspend fun getCategories(budgetId: String, forceRefresh: Boolean = false): Result<List<YnabCategory>>
    
    /**
     * Get transactions for a budget
     * @param budgetId Budget ID
     * @param sinceDate Optional filter by date
     * @param forceRefresh Force fetch from network instead of cache
     * @return Result with list of transactions or error
     */
    suspend fun getTransactions(
        budgetId: String,
        sinceDate: String? = null,
        forceRefresh: Boolean = false
    ): Result<List<YnabTransaction>>
    
    /**
     * Create a transaction in YNAB
     * If offline, queues the transaction for later sync
     * @param budgetId Budget ID
     * @param transaction Transaction to create
     * @return Result with created transaction ID or error
     */
    suspend fun createTransaction(
        budgetId: String,
        transaction: YnabTransaction
    ): Result<String>
    
    /**
     * Create multiple transactions in YNAB
     * @param budgetId Budget ID
     * @param transactions Transactions to create
     * @return Result with created transaction IDs or error
     */
    suspend fun createTransactions(
        budgetId: String,
        transactions: List<YnabTransaction>
    ): Result<List<String>>
    
    /**
     * Check for duplicate transactions
     * @param budgetId Budget ID
     * @param transaction Transaction to check
     * @return Result with list of potential duplicates or error
     */
    suspend fun checkDuplicates(
        budgetId: String,
        transaction: YnabTransaction
    ): Result<List<YnabTransaction>>
    
    /**
     * Observe cached budgets
     * @return Flow of budgets from local cache
     */
    fun observeBudgets(): Flow<List<YnabBudget>>
    
    /**
     * Observe cached accounts for a budget
     * @param budgetId Budget ID
     * @return Flow of accounts from local cache
     */
    fun observeAccounts(budgetId: String): Flow<List<YnabAccount>>
    
    /**
     * Observe cached categories for a budget
     * @param budgetId Budget ID
     * @return Flow of categories from local cache
     */
    fun observeCategories(budgetId: String): Flow<List<YnabCategory>>
    
    /**
     * Sync all pending transactions to YNAB
     * @return Result with number of successfully synced transactions or error
     */
    suspend fun syncPendingTransactions(): Result<Int>
}
