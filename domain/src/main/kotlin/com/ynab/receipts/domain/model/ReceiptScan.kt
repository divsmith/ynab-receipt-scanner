package com.ynab.receipts.domain.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class ReceiptScan(
    val id: UUID,
    val createdAt: Instant,
    val imageUriEncrypted: String,
    val ocrText: String,
    val parseVersion: String,
    val payee: String?,
    val amountMinor: Long?,
    val currency: String,
    val date: LocalDate?,
    val taxMinor: Long? = null,
    val lineItemsJson: String? = null,
    val confidenceJson: String,
    val userEditedFieldsJson: String? = null,
    val status: ReceiptStatus
)
