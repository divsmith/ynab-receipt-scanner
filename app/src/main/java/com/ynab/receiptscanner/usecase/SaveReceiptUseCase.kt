package com.ynab.receiptscanner.usecase

import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.domain.model.Receipt
import com.ynab.receiptscanner.domain.repository.ReceiptRepository
import javax.inject.Inject

/**
 * Use case for saving receipts to the database
 * Handles receipt persistence
 */
class SaveReceiptUseCase @Inject constructor(
    private val receiptRepository: ReceiptRepository
) {
    
    /**
     * Save a receipt to the database
     * @param receipt Receipt to save
     * @return Result containing saved receipt or error
     */
    suspend operator fun invoke(receipt: Receipt): Result<Receipt> {
        // Validate receipt
        if (receipt.id.isBlank()) {
            return Result.Error(
                IllegalArgumentException("Receipt ID is required"),
                "Cannot save receipt without ID"
            )
        }
        
        // Save to repository
        return receiptRepository.saveReceipt(receipt)
    }
    
    /**
     * Save multiple receipts
     * @param receipts List of receipts to save
     * @return Result containing list of saved receipts or error
     */
    suspend fun saveAll(receipts: List<Receipt>): Result<List<Receipt>> {
        return try {
            val savedReceipts = receipts.map { receipt ->
                when (val result = receiptRepository.saveReceipt(receipt)) {
                    is Result.Success -> result.data
                    is Result.Error -> return Result.Error(
                        result.exception,
                        "Failed to save receipt ${receipt.id}: ${result.message}"
                    )
                    else -> return Result.Error(
                        Exception("Unknown error"),
                        "Failed to save receipt ${receipt.id}"
                    )
                }
            }
            Result.Success(savedReceipts)
        } catch (e: Exception) {
            Result.Error(e, "Failed to save receipts: ${e.message}")
        }
    }
}
