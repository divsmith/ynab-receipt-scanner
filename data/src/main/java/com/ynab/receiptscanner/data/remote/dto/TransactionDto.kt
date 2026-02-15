package com.ynab.receiptscanner.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data class for YNAB transaction from API
 */
@JsonClass(generateAdapter = true)
data class TransactionDto(
    @Json(name = "id")
    val id: String,
    @Json(name = "date")
    val date: String,
    @Json(name = "amount")
    val amount: Long,
    @Json(name = "memo")
    val memo: String? = null,
    @Json(name = "cleared")
    val cleared: String,
    @Json(name = "approved")
    val approved: Boolean,
    @Json(name = "account_id")
    val accountId: String,
    @Json(name = "account_name")
    val accountName: String? = null,
    @Json(name = "payee_id")
    val payeeId: String? = null,
    @Json(name = "payee_name")
    val payeeName: String? = null,
    @Json(name = "category_id")
    val categoryId: String? = null,
    @Json(name = "category_name")
    val categoryName: String? = null,
    @Json(name = "transfer_account_id")
    val transferAccountId: String? = null,
    @Json(name = "transfer_transaction_id")
    val transferTransactionId: String? = null,
    @Json(name = "matched_transaction_id")
    val matchedTransactionId: String? = null,
    @Json(name = "import_id")
    val importId: String? = null,
    @Json(name = "deleted")
    val deleted: Boolean = false,
    @Json(name = "subtransactions")
    val subtransactions: List<SubtransactionDto>? = null
)

/**
 * Wrapper for transactions list response
 */
@JsonClass(generateAdapter = true)
data class TransactionsWrapper(
    @Json(name = "transactions")
    val transactions: List<TransactionDto>,
    @Json(name = "server_knowledge")
    val serverKnowledge: Long
)

/**
 * Wrapper for single transaction response
 */
@JsonClass(generateAdapter = true)
data class TransactionWrapper(
    @Json(name = "transaction")
    val transaction: TransactionDto
)

@JsonClass(generateAdapter = true)
data class SubtransactionDto(
    @Json(name = "id")
    val id: String,
    @Json(name = "transaction_id")
    val transactionId: String,
    @Json(name = "amount")
    val amount: Long,
    @Json(name = "memo")
    val memo: String? = null,
    @Json(name = "payee_id")
    val payeeId: String? = null,
    @Json(name = "payee_name")
    val payeeName: String? = null,
    @Json(name = "category_id")
    val categoryId: String? = null,
    @Json(name = "category_name")
    val categoryName: String? = null,
    @Json(name = "deleted")
    val deleted: Boolean = false
)
