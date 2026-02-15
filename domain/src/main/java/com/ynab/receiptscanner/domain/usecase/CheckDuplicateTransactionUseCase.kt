package com.ynab.receiptscanner.domain.usecase

import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.data.repository.DuplicateDetector
import com.ynab.receiptscanner.domain.model.YnabTransaction
import com.ynab.receiptscanner.domain.repository.YnabRepository
import javax.inject.Inject

/**
 * Use case for checking duplicate transactions before submitting
 */
class CheckDuplicateTransactionUseCase @Inject constructor(
    private val repository: YnabRepository
) {
    /**
     * Check for potential duplicate transactions
     * @param budgetId Budget ID
     * @param transaction Transaction to check
     * @return Result with list of potential duplicates and their confidence scores
     */
    suspend operator fun invoke(
        budgetId: String,
        transaction: YnabTransaction
    ): Result<List<DuplicateMatch>> {
        return when (val result = repository.checkDuplicates(budgetId, transaction)) {
            is Result.Success -> {
                val matches = result.data.map { duplicate ->
                    DuplicateMatch(
                        transaction = duplicate,
                        confidence = DuplicateDetector.calculateDuplicateConfidence(
                            transaction,
                            duplicate
                        )
                    )
                }.sortedByDescending { it.confidence }
                
                Result.Success(matches)
            }
            is Result.Error -> result
        }
    }
}

/**
 * Data class representing a potential duplicate match with confidence score
 */
data class DuplicateMatch(
    val transaction: YnabTransaction,
    val confidence: Float // 0.0 to 1.0
)
