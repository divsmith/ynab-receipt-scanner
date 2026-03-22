package com.ynab.receipts.domain.usecase

import com.ynab.receipts.domain.model.QueuedTransaction
import com.ynab.receipts.domain.model.ReceiptScan
import com.ynab.receipts.domain.service.TransactionSyncService
import javax.inject.Inject

class EnqueueTransactionUseCase @Inject constructor(
    private val syncService: TransactionSyncService
) {
    suspend operator fun invoke(receipt: ReceiptScan): QueuedTransaction = syncService.enqueue(receipt)
}
