package com.ynab.receiptscanner.usecase

import com.ynab.receiptscanner.domain.model.Receipt
import com.ynab.receiptscanner.domain.model.SyncStatus
import com.ynab.receiptscanner.domain.repository.ReceiptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case for getting sync status of receipts
 * Allows filtering receipts by sync status
 */
class GetSyncStatusUseCase @Inject constructor(
    private val receiptRepository: ReceiptRepository
) {
    /**
     * Get all receipts with a specific sync status
     */
    operator fun invoke(status: SyncStatus): Flow<List<Receipt>> {
        return receiptRepository.getAllReceipts().map { receipts ->
            receipts.filter { it.syncStatus == status }
        }
    }
    
    /**
     * Get all receipts that need syncing (PENDING or FAILED)
     */
    fun getPendingReceipts(): Flow<List<Receipt>> {
        return receiptRepository.getAllReceipts().map { receipts ->
            receipts.filter { 
                it.syncStatus == SyncStatus.PENDING || it.syncStatus == SyncStatus.FAILED 
            }
        }
    }
    
    /**
     * Get sync status for a specific receipt
     */
    fun getReceiptSyncStatus(receiptId: String): Flow<Receipt?> {
        return receiptRepository.getReceiptById(receiptId)
    }
}
