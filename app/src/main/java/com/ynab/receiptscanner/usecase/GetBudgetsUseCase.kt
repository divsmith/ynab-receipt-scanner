package com.ynab.receiptscanner.usecase

import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.domain.model.YnabBudget
import com.ynab.receiptscanner.domain.repository.YnabRepository
import javax.inject.Inject

/**
 * Use case for fetching user budgets
 */
class GetBudgetsUseCase @Inject constructor(
    private val repository: YnabRepository
) {
    /**
     * Get all budgets for authenticated user
     * @param forceRefresh Force fetch from network
     * @return Result with list of budgets or error
     */
    suspend operator fun invoke(forceRefresh: Boolean = false): Result<List<YnabBudget>> {
        return repository.getBudgets(forceRefresh)
    }
}
