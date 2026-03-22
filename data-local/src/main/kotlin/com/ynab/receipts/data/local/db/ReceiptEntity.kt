package com.ynab.receipts.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ynab.receipts.domain.model.ReceiptStatus
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity(tableName = "receipt_scans")
data class ReceiptEntity(
    @PrimaryKey val id: UUID,
    val createdAt: Instant,
    val imageUriEncrypted: String,
    val ocrText: String,
    val parseVersion: String,
    val payee: String?,
    val amountMinor: Long?,
    val currency: String,
    val date: LocalDate?,
    val taxMinor: Long?,
    val lineItemsJson: String?,
    val confidenceJson: String,
    val userEditedFieldsJson: String?,
    val status: ReceiptStatus
)
