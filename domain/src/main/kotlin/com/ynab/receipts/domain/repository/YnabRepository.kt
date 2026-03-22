package com.ynab.receipts.domain.repository

import com.ynab.receipts.domain.model.SubmittedTransaction
import java.time.LocalDate

data class YnabTransaction(
    val id: String,
    val payee: String,
    val amountMinor: Long,
    val date: LocalDate
)

interface YnabRepository {
    suspend fun createTransaction(transaction: SubmittedTransaction): String
    suspend fun listTransactions(budgetId: String, accountId: String, fromInclusive: LocalDate, toInclusive: LocalDate): List<YnabTransaction>
}
