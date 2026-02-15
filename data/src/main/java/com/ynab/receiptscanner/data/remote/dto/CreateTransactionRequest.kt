package com.ynab.receiptscanner.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request body for creating a transaction
 */
@JsonClass(generateAdapter = true)
data class CreateTransactionRequest(
    @Json(name = "transaction")
    val transaction: SaveTransactionDto
)

/**
 * Request body for creating multiple transactions
 */
@JsonClass(generateAdapter = true)
data class CreateTransactionsRequest(
    @Json(name = "transactions")
    val transactions: List<SaveTransactionDto>
)

/**
 * Transaction data for saving
 */
@JsonClass(generateAdapter = true)
data class SaveTransactionDto(
    @Json(name = "account_id")
    val accountId: String,
    @Json(name = "date")
    val date: String,
    @Json(name = "amount")
    val amount: Long,
    @Json(name = "payee_id")
    val payeeId: String? = null,
    @Json(name = "payee_name")
    val payeeName: String? = null,
    @Json(name = "category_id")
    val categoryId: String? = null,
    @Json(name = "memo")
    val memo: String? = null,
    @Json(name = "cleared")
    val cleared: String? = null,
    @Json(name = "approved")
    val approved: Boolean? = null,
    @Json(name = "import_id")
    val importId: String? = null
)

/**
 * Response for created transactions
 */
@JsonClass(generateAdapter = true)
data class SaveTransactionsResponse(
    @Json(name = "transaction_ids")
    val transactionIds: List<String>,
    @Json(name = "transaction")
    val transaction: TransactionDto? = null,
    @Json(name = "transactions")
    val transactions: List<TransactionDto>? = null,
    @Json(name = "duplicate_import_ids")
    val duplicateImportIds: List<String>? = null,
    @Json(name = "server_knowledge")
    val serverKnowledge: Long
)
