package com.ynab.receipts.domain.service

import com.ynab.receipts.domain.model.QueuedTransaction
import com.ynab.receipts.domain.model.ReceiptScan

interface TransactionSyncService {
    suspend fun enqueue(receipt: ReceiptScan): QueuedTransaction
    suspend fun syncPending()
}
