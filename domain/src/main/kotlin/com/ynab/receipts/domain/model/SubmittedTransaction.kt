package com.ynab.receipts.domain.model

import java.time.LocalDate

data class SubmittedTransaction(
    val payee: String,
    val amountMinor: Long,
    val date: LocalDate,
    val budgetId: String,
    val accountId: String
)
