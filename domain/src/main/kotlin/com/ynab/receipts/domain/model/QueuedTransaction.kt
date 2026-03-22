package com.ynab.receipts.domain.model

import java.time.Instant
import java.util.UUID

data class QueuedTransaction(
    val id: UUID,
    val receiptScanId: UUID,
    val ynabBudgetId: String,
    val ynabAccountId: String,
    val ynabCategoryId: String? = null,
    val dedupeFingerprint: String,
    val attemptCount: Int,
    val lastError: String? = null,
    val nextRetryAt: Instant? = null,
    val createdAt: Instant,
    val remoteTransactionId: String? = null
)
