package com.ynab.receipts.data.ynab

import com.ynab.receipts.domain.model.SubmittedTransaction
import com.ynab.receipts.domain.repository.YnabRepository
import com.ynab.receipts.domain.repository.YnabTransaction
import java.time.LocalDate
import javax.inject.Inject

class RetrofitYnabRepository @Inject constructor(
    private val ynabApi: YnabApi
) : YnabRepository {
    override suspend fun createTransaction(transaction: SubmittedTransaction): String {
        val request = CreateTransactionRequest(
            transaction = mapOf(
                "account_id" to transaction.accountId,
                "date" to transaction.date.toString(),
                "amount" to transaction.amountMinor,
                "payee_name" to transaction.payee
            )
        )
        return ynabApi.createTransaction(transaction.budgetId, request).data.transaction.id
    }

    override suspend fun listTransactions(
        budgetId: String,
        accountId: String,
        fromInclusive: LocalDate,
        toInclusive: LocalDate
    ): List<YnabTransaction> {
        return ynabApi.listTransactions(
            budgetId = budgetId,
            accountId = accountId,
            sinceDate = fromInclusive.toString()
        ).data.transactions
            .mapNotNull { payload ->
                payload.date.takeIf { it <= toInclusive.toString() }?.let {
                    YnabTransaction(
                        id = payload.id,
                        payee = payload.payee_name,
                        amountMinor = payload.amount,
                        date = LocalDate.parse(payload.date)
                    )
                }
            }
    }
}
