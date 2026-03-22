package com.ynab.receipts.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Entity(tableName = "queued_transactions")
data class QueuedTransactionEntity(
    @PrimaryKey val id: UUID,
    val receiptScanId: UUID,
    val ynabBudgetId: String,
    val ynabAccountId: String,
    val ynabCategoryId: String?,
    val dedupeFingerprint: String,
    val attemptCount: Int,
    val lastError: String?,
    val nextRetryAt: Instant?,
    val createdAt: Instant,
    val remoteTransactionId: String?
)
