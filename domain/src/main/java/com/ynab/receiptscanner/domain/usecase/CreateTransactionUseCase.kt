package com.ynab.receiptscanner.domain.usecase

import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.data.repository.DuplicateDetector
import com.ynab.receiptscanner.domain.model.YnabTransaction
import com.ynab.receiptscanner.domain.repository.YnabRepository
import javax.inject.Inject

/**
 * Use case for creating a transaction in YNAB
 */
class CreateTransactionUseCase @Inject constructor(
    private val repository: YnabRepository
) {
    /**
     * Create a transaction
     * Automatically generates import_id for YNAB deduplication
     * If offline, queues the transaction for later sync
     * 
     * @param budgetId Budget ID
     * @param transaction Transaction to create
     * @return Result with created transaction ID or error
     */
    suspend operator fun invoke(
        budgetId: String,
        transaction: YnabTransaction
    ): Result<String> {
        // Generate import_id if not provided
        val transactionWithImportId = if (transaction.importId.isNullOrBlank()) {
            transaction.copy(
                importId = DuplicateDetector.generateImportId(transaction)
            )
        } else {
            transaction
        }
        
        return repository.createTransaction(budgetId, transactionWithImportId)
    }
}
