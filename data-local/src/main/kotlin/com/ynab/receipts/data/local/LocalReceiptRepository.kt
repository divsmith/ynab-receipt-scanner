package com.ynab.receipts.data.local

import com.ynab.receipts.data.local.db.QueueDao
import com.ynab.receipts.data.local.db.QueuedTransactionEntity
import com.ynab.receipts.data.local.db.ReceiptDao
import com.ynab.receipts.data.local.db.ReceiptEntity
import com.ynab.receipts.domain.model.QueuedTransaction
import com.ynab.receipts.domain.model.ReceiptScan
import com.ynab.receipts.domain.model.ReceiptStatus
import com.ynab.receipts.domain.repository.ReceiptRepository
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

class LocalReceiptRepository @Inject constructor(
    private val receiptDao: ReceiptDao,
    private val queueDao: QueueDao
) : ReceiptRepository {
    override suspend fun saveDraft(scan: ReceiptScan): ReceiptScan {
        receiptDao.upsert(scan.toEntity())
        return scan
    }

    override suspend fun getReceipt(receiptId: UUID): ReceiptScan? {
        return receiptDao.findById(receiptId)?.toDomain()
    }

    override suspend fun markQueued(receiptId: UUID) {
        receiptDao.updateStatus(receiptId, ReceiptStatus.Queued.name)
    }

    override suspend fun markSynced(receiptId: UUID) {
        receiptDao.updateStatus(receiptId, ReceiptStatus.Synced.name)
    }

    override suspend fun updateQueueResult(queueId: UUID, remoteTransactionId: String?, error: String?) {
        queueDao.updateResult(queueId, remoteTransactionId, error)
    }

    override suspend fun addQueueItem(item: QueuedTransaction): QueuedTransaction {
        queueDao.upsert(item.toEntity())
        return item
    }

    override suspend fun listPendingQueueItems(limit: Int): List<QueuedTransaction> {
        return queueDao.listPending(limit).map { it.toDomain() }
    }

    override suspend fun findSyncedInDateWindow(fromInclusive: LocalDate, toInclusive: LocalDate): List<ReceiptScan> {
        return receiptDao.findSyncedInDateWindow(fromInclusive, toInclusive).map { it.toDomain() }
    }
}

private fun ReceiptScan.toEntity(): ReceiptEntity = ReceiptEntity(
    id = id,
    createdAt = createdAt,
    imageUriEncrypted = imageUriEncrypted,
    ocrText = ocrText,
    parseVersion = parseVersion,
    payee = payee,
    amountMinor = amountMinor,
    currency = currency,
    date = date,
    taxMinor = taxMinor,
    lineItemsJson = lineItemsJson,
    confidenceJson = confidenceJson,
    userEditedFieldsJson = userEditedFieldsJson,
    status = status
)

private fun ReceiptEntity.toDomain(): ReceiptScan = ReceiptScan(
    id = id,
    createdAt = createdAt,
    imageUriEncrypted = imageUriEncrypted,
    ocrText = ocrText,
    parseVersion = parseVersion,
    payee = payee,
    amountMinor = amountMinor,
    currency = currency,
    date = date,
    taxMinor = taxMinor,
    lineItemsJson = lineItemsJson,
    confidenceJson = confidenceJson,
    userEditedFieldsJson = userEditedFieldsJson,
    status = status
)

private fun QueuedTransaction.toEntity(): QueuedTransactionEntity = QueuedTransactionEntity(
    id = id,
    receiptScanId = receiptScanId,
    ynabBudgetId = ynabBudgetId,
    ynabAccountId = ynabAccountId,
    ynabCategoryId = ynabCategoryId,
    dedupeFingerprint = dedupeFingerprint,
    attemptCount = attemptCount,
    lastError = lastError,
    nextRetryAt = nextRetryAt,
    createdAt = createdAt,
    remoteTransactionId = remoteTransactionId
)

private fun QueuedTransactionEntity.toDomain(): QueuedTransaction = QueuedTransaction(
    id = id,
    receiptScanId = receiptScanId,
    ynabBudgetId = ynabBudgetId,
    ynabAccountId = ynabAccountId,
    ynabCategoryId = ynabCategoryId,
    dedupeFingerprint = dedupeFingerprint,
    attemptCount = attemptCount,
    lastError = lastError,
    nextRetryAt = nextRetryAt,
    createdAt = createdAt,
    remoteTransactionId = remoteTransactionId
)
