package com.ynab.receipts.data.ynab

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class CreateTransactionRequest(
    val transaction: Map<String, Any>
)

data class CreateTransactionResponse(
    val data: CreateTransactionData
)

data class CreateTransactionData(
    val transaction: YnabTransactionPayload
)

data class YnabTransactionPayload(
    val id: String,
    val payee_name: String,
    val amount: Long,
    val date: String
)

data class ListTransactionsResponse(
    val data: ListTransactionsData
)

data class ListTransactionsData(
    val transactions: List<YnabTransactionPayload>
)

interface YnabApi {
    @POST("budgets/{budgetId}/transactions")
    suspend fun createTransaction(
        @Path("budgetId") budgetId: String,
        @Body request: CreateTransactionRequest
    ): CreateTransactionResponse

    @GET("budgets/{budgetId}/accounts/{accountId}/transactions")
    suspend fun listTransactions(
        @Path("budgetId") budgetId: String,
        @Path("accountId") accountId: String,
        @Query("since_date") sinceDate: String
    ): ListTransactionsResponse
}
