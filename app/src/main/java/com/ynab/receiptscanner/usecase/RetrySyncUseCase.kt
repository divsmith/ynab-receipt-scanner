package com.ynab.receiptscanner.usecase

import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.domain.repository.YnabRepository
import javax.inject.Inject

/**
 * Use case for manually retrying failed syncs
 * Triggers immediate sync of pending transactions
 */
class RetrySyncUseCase @Inject constructor(
    private val ynabRepository: YnabRepository
) {
    /**
     * Retry sync for a specific receipt
     */
    suspend operator fun invoke(receiptId: String): Result<Unit> {
        return try {
            // Trigger sync for specific receipt
            ynabRepository.syncPendingTransactions()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Retry sync for all failed transactions
     */
    suspend fun retryAll(): Result<Int> {
        return ynabRepository.syncPendingTransactions()
    }
}
