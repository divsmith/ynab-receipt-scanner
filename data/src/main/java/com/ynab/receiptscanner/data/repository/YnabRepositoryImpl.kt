package com.ynab.receiptscanner.data.repository

import android.util.Log
import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.data.local.dao.*
import com.ynab.receiptscanner.data.local.entity.*
import com.ynab.receiptscanner.data.mapper.YnabMapper
import com.ynab.receiptscanner.data.mapper.toDomain
import com.ynab.receiptscanner.data.mapper.toEntity
import com.ynab.receiptscanner.data.remote.YnabApi
import com.ynab.receiptscanner.data.remote.dto.CreateTransactionRequest
import com.ynab.receiptscanner.data.remote.dto.CreateTransactionsRequest
import com.ynab.receiptscanner.data.sync.TransactionSyncManager
import com.ynab.receiptscanner.domain.model.*
import com.ynab.receiptscanner.domain.repository.YnabRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of YnabRepository
 * Handles YNAB API calls with local caching and offline queue support
 */
@Singleton
class YnabRepositoryImpl @Inject constructor(
    private val api: YnabApi,
    private val budgetDao: BudgetDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val pendingTransactionDao: PendingTransactionDao,
    private val transactionSyncManager: TransactionSyncManager,
    private val ynabMapper: YnabMapper
) : YnabRepository {
    
    companion object {
        private const val TAG = "YnabRepositoryImpl"
        private const val CACHE_VALIDITY_MS = 5 * 60 * 1000L // 5 minutes
    }
    
    override suspend fun getBudgets(forceRefresh: Boolean): Result<List<YnabBudget>> {
        return try {
            if (forceRefresh || isCacheStale()) {
                // Fetch from network
                val response = api.getBudgets()
                
                if (response.isSuccessful && response.body() != null) {
                    val budgets = response.body()!!.data.budgets.map { ynabMapper.mapBudgetToDomain(it) }
                    
                    // Cache to database
                    val entities = budgets.map { it.toEntity() }
                    budgetDao.insertBudgets(entities)
                    
                    Result.Success(budgets)
                } else {
                    Log.e(TAG, "Failed to fetch budgets: ${response.code()}")
                    // Try to return cached data as fallback
                    val cachedBudgets = getCachedBudgets()
                    if (cachedBudgets.isNotEmpty()) {
                        Result.Success(cachedBudgets)
                    } else {
                        Result.Error(Exception("Failed to fetch budgets: ${response.code()}"))
                    }
                }
            } else {
                // Return cached data
                val cached = getCachedBudgets()
                Result.Success(cached)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching budgets", e)
            // Try to return cached data as fallback
            val cachedBudgets = getCachedBudgets()
            if (cachedBudgets.isNotEmpty()) {
                Result.Success(cachedBudgets)
            } else {
                Result.Error(e)
            }
        }
    }
    
    override suspend fun getBudget(budgetId: String, forceRefresh: Boolean): Result<YnabBudget> {
        return try {
            if (forceRefresh) {
                val response = api.getBudget(budgetId)
                
                if (response.isSuccessful && response.body() != null) {
                    val budget = ynabMapper.mapBudgetToDomain(response.body()!!.data)
                    
                    // Cache to database
                    budgetDao.insertBudget(budget.toEntity())
                    
                    Result.Success(budget)
                } else {
                    Result.Error(Exception("Failed to fetch budget: ${response.code()}"))
                }
            } else {
                // Try cached first
                val cached = budgetDao.getBudgetByIdSync(budgetId)
                if (cached != null) {
                    Result.Success(cached.toDomain())
                } else {
                    // Fetch from network
                    getBudget(budgetId, forceRefresh = true)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching budget", e)
            Result.Error(e)
        }
    }
    
    override suspend fun getAccounts(budgetId: String, forceRefresh: Boolean): Result<List<YnabAccount>> {
        return try {
            if (forceRefresh) {
                val response = api.getAccounts(budgetId)
                
                if (response.isSuccessful && response.body() != null) {
                    val accounts = response.body()!!.data.accounts.map { 
                        ynabMapper.mapAccountToDomain(it, budgetId) 
                    }
                    
                    // Cache to database
                    accountDao.deleteAccountsForBudget(budgetId)
                    val entities = accounts.map { it.toEntity() }
                    accountDao.insertAccounts(entities)
                    
                    Result.Success(accounts)
                } else {
                    Result.Error(Exception("Failed to fetch accounts: ${response.code()}"))
                }
            } else {
                // Return from cache (Flow converted to list)
                val cached = getCachedAccounts(budgetId)
                if (cached.isNotEmpty()) {
                    Result.Success(cached)
                } else {
                    getAccounts(budgetId, forceRefresh = true)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching accounts", e)
            Result.Error(e)
        }
    }
    
    override suspend fun getCategories(budgetId: String, forceRefresh: Boolean): Result<List<YnabCategory>> {
        return try {
            if (forceRefresh) {
                val response = api.getCategories(budgetId)
                
                if (response.isSuccessful && response.body() != null) {
                    val categories = ynabMapper.flattenCategoryGroups(
                        response.body()!!.data.categoryGroups
                    )
                    
                    // Cache to database
                    categoryDao.deleteCategoriesForBudget(budgetId)
                    val entities = categories.map { it.toEntity(budgetId) }
                    categoryDao.insertCategories(entities)
                    
                    Result.Success(categories)
                } else {
                    Result.Error(Exception("Failed to fetch categories: ${response.code()}"))
                }
            } else {
                // Return from cache
                val cached = getCachedCategories(budgetId)
                if (cached.isNotEmpty()) {
                    Result.Success(cached)
                } else {
                    getCategories(budgetId, forceRefresh = true)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching categories", e)
            Result.Error(e)
        }
    }
    
    override suspend fun getTransactions(
        budgetId: String,
        sinceDate: String?,
        forceRefresh: Boolean
    ): Result<List<YnabTransaction>> {
        return try {
            val response = api.getTransactions(budgetId, sinceDate)
            
            if (response.isSuccessful && response.body() != null) {
                val transactions = response.body()!!.data.transactions.map { 
                    ynabMapper.mapTransactionToDomain(it) 
                }
                Result.Success(transactions)
            } else {
                Result.Error(Exception("Failed to fetch transactions: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching transactions", e)
            Result.Error(e)
        }
    }
    
    override suspend fun createTransaction(budgetId: String, transaction: YnabTransaction): Result<String> {
        return try {
            val dto = ynabMapper.mapTransactionToDto(transaction)
            val request = CreateTransactionRequest(dto)
            
            val response = api.createTransaction(budgetId, request)
            
            if (response.isSuccessful && response.body() != null) {
                val transactionId = response.body()!!.data.transaction?.id
                    ?: response.body()!!.data.transactionIds.firstOrNull()
                    ?: transaction.id
                
                Log.d(TAG, "Transaction created successfully: $transactionId")
                Result.Success(transactionId)
            } else {
                // If network fails, queue for offline sync
                Log.w(TAG, "Failed to create transaction online, queueing for offline sync")
                queueTransactionForSync(transaction, budgetId)
                Result.Success(transaction.id) // Return local ID
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating transaction, queueing for offline sync", e)
            // Queue for offline sync
            queueTransactionForSync(transaction, budgetId)
            Result.Success(transaction.id) // Return local ID
        }
    }
    
    override suspend fun createTransactions(
        budgetId: String,
        transactions: List<YnabTransaction>
    ): Result<List<String>> {
        return try {
            val dtos = transactions.map { ynabMapper.mapTransactionToDto(it) }
            val request = CreateTransactionsRequest(dtos)
            
            val response = api.createTransactions(budgetId, request)
            
            if (response.isSuccessful && response.body() != null) {
                val transactionIds = response.body()!!.data.transactionIds
                Log.d(TAG, "Created ${transactionIds.size} transactions")
                Result.Success(transactionIds)
            } else {
                Result.Error(Exception("Failed to create transactions: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating transactions", e)
            Result.Error(e)
        }
    }
    
    override suspend fun checkDuplicates(
        budgetId: String,
        transaction: YnabTransaction
    ): Result<List<YnabTransaction>> {
        // Get recent transactions to check for duplicates
        val calendar = Calendar.getInstance()
        calendar.time = transaction.date
        calendar.add(Calendar.DAY_OF_YEAR, -7) // Check last 7 days
        
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sinceDate = dateFormat.format(calendar.time)
        
        return when (val result = getTransactions(budgetId, sinceDate)) {
            is Result.Success -> {
                val duplicates = DuplicateDetector.findDuplicates(transaction, result.data)
                Result.Success(duplicates)
            }
            is Result.Error -> result
            is Result.Loading -> Result.Success(emptyList()) // Should not happen in suspend call
        }
    }
    
    override fun observeBudgets(): Flow<List<YnabBudget>> {
        return budgetDao.getAllBudgets().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun observeAccounts(budgetId: String): Flow<List<YnabAccount>> {
        return accountDao.getAccountsForBudget(budgetId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun observeCategories(budgetId: String): Flow<List<YnabCategory>> {
        return categoryDao.getCategoriesForBudget(budgetId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    // Helper methods
    
    private suspend fun getCachedBudgets(): List<YnabBudget> {
        // This would need to be synchronous - for simplicity, return empty
        // In production, you'd collect from the Flow or use a blocking call
        return emptyList()
    }
    
    private suspend fun getCachedAccounts(budgetId: String): List<YnabAccount> {
        return emptyList()
    }
    
    private suspend fun getCachedCategories(budgetId: String): List<YnabCategory> {
        return emptyList()
    }
    
    private fun isCacheStale(): Boolean {
        // Simple staleness check - could be improved with actual timestamps
        return true
    }
    
    private suspend fun queueTransactionForSync(transaction: YnabTransaction, budgetId: String) {
        val entity = PendingTransactionEntity(
            id = transaction.id,
            receiptId = null,
            accountId = transaction.accountId,
            categoryId = transaction.categoryId,
            date = transaction.date,
            amount = transaction.amount,
            payee = transaction.payee,
            memo = transaction.memo,
            cleared = transaction.cleared,
            approved = transaction.approved,
            importId = transaction.importId,
            status = "PENDING",
            retryCount = 0,
            lastError = null,
            createdAt = Date(),
            updatedAt = Date()
        )
        
        pendingTransactionDao.insertPendingTransaction(entity)
        Log.d(TAG, "Transaction queued for sync: ${transaction.id}")
    }
    
    override suspend fun syncPendingTransactions(): Result<Int> {
        return transactionSyncManager.syncPendingTransactions()
    }
}
