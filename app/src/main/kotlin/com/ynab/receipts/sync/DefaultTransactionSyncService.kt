package com.ynab.receipts.sync

import com.ynab.receipts.domain.model.QueuedTransaction
import com.ynab.receipts.domain.model.ReceiptScan
import com.ynab.receipts.domain.model.SubmittedTransaction
import com.ynab.receipts.domain.repository.ReceiptRepository
import com.ynab.receipts.domain.repository.YnabRepository
import com.ynab.receipts.domain.service.TransactionSyncService
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

class DefaultTransactionSyncService @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val ynabRepository: YnabRepository
) : TransactionSyncService {
    override suspend fun enqueue(receipt: ReceiptScan): QueuedTransaction {
        val queueItem = QueuedTransaction(
            id = UUID.randomUUID(),
            receiptScanId = receipt.id,
            ynabBudgetId = "default-budget",
            ynabAccountId = "default-account",
            ynabCategoryId = null,
            dedupeFingerprint = listOf(receipt.payee, receipt.amountMinor, receipt.date).joinToString("|"),
            attemptCount = 0,
            lastError = null,
            nextRetryAt = null,
            createdAt = Instant.now()
        )
        val added = receiptRepository.addQueueItem(queueItem)
        receiptRepository.markQueued(receipt.id)
        return added
    }

    override suspend fun syncPending() {
        val queueItems = receiptRepository.listPendingQueueItems(limit = 25)
        queueItems.forEach { queueItem ->
            val receipt = receiptRepository.getReceipt(queueItem.receiptScanId)
            if (receipt == null) {
                receiptRepository.updateQueueResult(queueItem.id, null, "Missing receipt for queue item")
                return@forEach
            }

            val payee = receipt.payee
            val amountMinor = receipt.amountMinor
            val date = receipt.date
            if (payee == null || amountMinor == null || date == null) {
                receiptRepository.updateQueueResult(queueItem.id, null, "Missing required fields for YNAB transaction")
                return@forEach
            }

            runCatching {
                ynabRepository.createTransaction(
                    SubmittedTransaction(
                        payee = payee,
                        amountMinor = amountMinor,
                        date = date,
                        budgetId = queueItem.ynabBudgetId,
                        accountId = queueItem.ynabAccountId
                    )
                )
            }.onSuccess { remoteId ->
                receiptRepository.updateQueueResult(queueItem.id, remoteId, null)
                receiptRepository.markSynced(receipt.id)
            }.onFailure { throwable ->
                receiptRepository.updateQueueResult(queueItem.id, null, throwable.message ?: "Unknown sync error")
            }
        }
    }
}
