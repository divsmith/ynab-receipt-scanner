package com.ynab.receiptscanner.usecase

import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.domain.model.YnabCategory
import com.ynab.receiptscanner.domain.repository.YnabRepository
import javax.inject.Inject

/**
 * Use case for fetching categories for a budget
 */
class GetCategoriesUseCase @Inject constructor(
    private val repository: YnabRepository
) {
    /**
     * Get categories for a budget
     * @param budgetId Budget ID
     * @param forceRefresh Force fetch from network
     * @return Result with list of categories or error
     */
    suspend operator fun invoke(
        budgetId: String,
        forceRefresh: Boolean = false
    ): Result<List<YnabCategory>> {
        return repository.getCategories(budgetId, forceRefresh)
    }
}
