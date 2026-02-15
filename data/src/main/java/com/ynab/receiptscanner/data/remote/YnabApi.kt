package com.ynab.receiptscanner.data.remote

import com.ynab.receiptscanner.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit interface for YNAB API endpoints
 */
interface YnabApi {
    
    /**
     * Get all budgets
     * @return List of budgets for the authenticated user
     */
    @GET("budgets")
    suspend fun getBudgets(): Response<YnabResponse<BudgetsWrapper>>
    
    /**
     * Get a specific budget with details
     * @param budgetId Budget ID
     * @param lastKnowledgeOfServer Optional server knowledge for delta requests
     * @return Budget details with accounts and categories
     */
    @GET("budgets/{budget_id}")
    suspend fun getBudget(
        @Path("budget_id") budgetId: String,
        @Query("last_knowledge_of_server") lastKnowledgeOfServer: Long? = null
    ): Response<YnabResponse<BudgetDto>>
    
    /**
     * Get all accounts for a budget
     * @param budgetId Budget ID
     * @param lastKnowledgeOfServer Optional server knowledge for delta requests
     * @return List of accounts
     */
    @GET("budgets/{budget_id}/accounts")
    suspend fun getAccounts(
        @Path("budget_id") budgetId: String,
        @Query("last_knowledge_of_server") lastKnowledgeOfServer: Long? = null
    ): Response<YnabResponse<AccountsWrapper>>
    
    /**
     * Get categories for a budget
     * @param budgetId Budget ID
     * @param lastKnowledgeOfServer Optional server knowledge for delta requests
     * @return List of categories grouped by category groups
     */
    @GET("budgets/{budget_id}/categories")
    suspend fun getCategories(
        @Path("budget_id") budgetId: String,
        @Query("last_knowledge_of_server") lastKnowledgeOfServer: Long? = null
    ): Response<YnabResponse<CategoriesWrapper>>
    
    /**
     * Get transactions for a budget
     * @param budgetId Budget ID
     * @param sinceDate Optional filter by date (ISO format: 2016-12-30)
     * @param type Optional filter by type (uncategorized, unapproved)
     * @param lastKnowledgeOfServer Optional server knowledge for delta requests
     * @return List of transactions
     */
    @GET("budgets/{budget_id}/transactions")
    suspend fun getTransactions(
        @Path("budget_id") budgetId: String,
        @Query("since_date") sinceDate: String? = null,
        @Query("type") type: String? = null,
        @Query("last_knowledge_of_server") lastKnowledgeOfServer: Long? = null
    ): Response<YnabResponse<TransactionsWrapper>>
    
    /**
     * Get transactions for a specific account
     * @param budgetId Budget ID
     * @param accountId Account ID
     * @param sinceDate Optional filter by date
     * @param lastKnowledgeOfServer Optional server knowledge for delta requests
     * @return List of transactions
     */
    @GET("budgets/{budget_id}/accounts/{account_id}/transactions")
    suspend fun getAccountTransactions(
        @Path("budget_id") budgetId: String,
        @Path("account_id") accountId: String,
        @Query("since_date") sinceDate: String? = null,
        @Query("last_knowledge_of_server") lastKnowledgeOfServer: Long? = null
    ): Response<YnabResponse<TransactionsWrapper>>
    
    /**
     * Create a single transaction
     * @param budgetId Budget ID
     * @param request Transaction creation request
     * @return Created transaction
     */
    @POST("budgets/{budget_id}/transactions")
    suspend fun createTransaction(
        @Path("budget_id") budgetId: String,
        @Body request: CreateTransactionRequest
    ): Response<YnabResponse<SaveTransactionsResponse>>
    
    /**
     * Create multiple transactions
     * @param budgetId Budget ID
     * @param request Transactions creation request
     * @return Created transactions
     */
    @POST("budgets/{budget_id}/transactions")
    suspend fun createTransactions(
        @Path("budget_id") budgetId: String,
        @Body request: CreateTransactionsRequest
    ): Response<YnabResponse<SaveTransactionsResponse>>
    
    /**
     * Update an existing transaction
     * @param budgetId Budget ID
     * @param transactionId Transaction ID
     * @param request Transaction update request
     * @return Updated transaction
     */
    @PUT("budgets/{budget_id}/transactions/{transaction_id}")
    suspend fun updateTransaction(
        @Path("budget_id") budgetId: String,
        @Path("transaction_id") transactionId: String,
        @Body request: CreateTransactionRequest
    ): Response<YnabResponse<TransactionWrapper>>
}
