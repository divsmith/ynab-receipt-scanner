package com.ynab.receiptscanner.usecase

import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.domain.model.YnabAccount
import com.ynab.receiptscanner.domain.repository.YnabRepository
import javax.inject.Inject

/**
 * Use case for fetching accounts for a budget
 */
class GetAccountsUseCase @Inject constructor(
    private val repository: YnabRepository
) {
    /**
     * Get accounts for a budget
     * @param budgetId Budget ID
     * @param forceRefresh Force fetch from network
     * @return Result with list of accounts or error
     */
    suspend operator fun invoke(
        budgetId: String,
        forceRefresh: Boolean = false
    ): Result<List<YnabAccount>> {
        return repository.getAccounts(budgetId, forceRefresh)
    }
}
