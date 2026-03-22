package com.ynab.receipts.domain.repository

import com.ynab.receipts.domain.model.QueuedTransaction
import com.ynab.receipts.domain.model.ReceiptScan
import java.time.LocalDate
import java.util.UUID

interface ReceiptRepository {
    suspend fun saveDraft(scan: ReceiptScan): ReceiptScan
    suspend fun getReceipt(receiptId: UUID): ReceiptScan?
    suspend fun markQueued(receiptId: UUID)
    suspend fun markSynced(receiptId: UUID)
    suspend fun updateQueueResult(queueId: UUID, remoteTransactionId: String?, error: String?)
    suspend fun addQueueItem(item: QueuedTransaction): QueuedTransaction
    suspend fun listPendingQueueItems(limit: Int = 50): List<QueuedTransaction>
    suspend fun findSyncedInDateWindow(fromInclusive: LocalDate, toInclusive: LocalDate): List<ReceiptScan>
}
