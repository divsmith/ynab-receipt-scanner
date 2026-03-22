package com.ynab.receipts.domain.usecase

import com.ynab.receipts.domain.service.TransactionSyncService
import javax.inject.Inject

class SyncQueuedTransactionsUseCase @Inject constructor(
    private val syncService: TransactionSyncService
) {
    suspend operator fun invoke() = syncService.syncPending()
}
