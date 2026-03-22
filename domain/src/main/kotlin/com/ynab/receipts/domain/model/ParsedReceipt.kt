package com.ynab.receipts.domain.model

import java.time.LocalDate

data class ReceiptLineItem(
    val description: String,
    val amountMinor: Long
)

data class ParsedReceipt(
    val payee: String?,
    val amountMinor: Long?,
    val currency: String,
    val date: LocalDate?,
    val taxMinor: Long? = null,
    val lineItems: List<ReceiptLineItem> = emptyList(),
    val confidence: Map<String, Float> = emptyMap(),
    val sourceOcrText: String
)
