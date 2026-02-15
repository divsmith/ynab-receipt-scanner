package com.ynab.receiptscanner.domain.model

import java.util.Date
import java.util.UUID

/**
 * Domain model representing a scanned receipt
 * @property id Unique identifier for the receipt
 * @property payee Merchant or payee name extracted from receipt
 * @property amount Total amount on the receipt
 * @property date Date of the transaction
 * @property currency Currency of the transaction
 * @property tax Tax amount if available
 * @property imagePath Local file path to the receipt image
 * @property ocrText Raw OCR text extracted from the receipt
 * @property syncStatus Current synchronization status with YNAB
 * @property createdAt Timestamp when the receipt was created
 * @property updatedAt Timestamp when the receipt was last updated
 */
data class Receipt(
    val id: String = UUID.randomUUID().toString(),
    val payee: String? = null,
    val amount: Double? = null,
    val date: Date? = null,
    val currency: Currency = Currency.USD,
    val tax: Double? = null,
    val imagePath: String? = null,
    val ocrText: String? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    /**
     * Returns true if the receipt has minimum required fields for YNAB sync
     */
    fun isReadyForSync(): Boolean {
        return payee != null && amount != null && date != null
    }
    
    /**
     * Returns true if the receipt has been successfully synced
     */
    fun isSynced(): Boolean {
        return syncStatus == SyncStatus.SYNCED
    }
}
