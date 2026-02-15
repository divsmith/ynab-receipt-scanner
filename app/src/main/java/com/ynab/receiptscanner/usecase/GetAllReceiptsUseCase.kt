package com.ynab.receiptscanner.usecase

import com.ynab.receiptscanner.domain.model.Receipt
import com.ynab.receiptscanner.domain.repository.ReceiptRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving all receipts from the database
 * Returns a Flow for reactive updates
 */
class GetAllReceiptsUseCase @Inject constructor(
    private val receiptRepository: ReceiptRepository
) {
    
    /**
     * Get all receipts
     * @return Flow of list of receipts
     */
    operator fun invoke(): Flow<List<Receipt>> {
        return receiptRepository.getAllReceipts()
    }
}
