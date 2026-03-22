package com.ynab.receipts.domain.usecase

import com.ynab.receipts.domain.model.ReceiptScan
import com.ynab.receipts.domain.repository.YnabTransaction
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

data class DuplicateCandidate(
    val source: DuplicateSource,
    val id: String,
    val score: Float
)

enum class DuplicateSource {
    Local,
    Ynab
}

class DetectDuplicateUseCase @Inject constructor() {
    fun fingerprint(payee: String, amountMinor: Long, date: LocalDate): String {
        val normalizedPayee = payee.lowercase(Locale.US)
            .replace("[^a-z0-9]".toRegex(), "")
        return "$normalizedPayee|$amountMinor|$date"
    }

    fun findPotentialDuplicates(
        payee: String,
        amountMinor: Long,
        date: LocalDate,
        localSynced: List<ReceiptScan>,
        ynabTransactions: List<YnabTransaction>,
        threshold: Float = 1.0f
    ): List<DuplicateCandidate> {
        val targetFingerprint = fingerprint(payee, amountMinor, date)

        val localMatches = localSynced.mapNotNull { scan ->
            val candidatePayee = scan.payee ?: return@mapNotNull null
            val candidateAmount = scan.amountMinor ?: return@mapNotNull null
            val candidateDate = scan.date ?: return@mapNotNull null
            val score = if (fingerprint(candidatePayee, candidateAmount, candidateDate) == targetFingerprint) 1.0f else 0.0f
            if (score >= threshold) DuplicateCandidate(DuplicateSource.Local, scan.id.toString(), score) else null
        }

        val ynabMatches = ynabTransactions.mapNotNull { transaction ->
            val score = if (fingerprint(transaction.payee, transaction.amountMinor, transaction.date) == targetFingerprint) 1.0f else 0.0f
            if (score >= threshold) DuplicateCandidate(DuplicateSource.Ynab, transaction.id, score) else null
        }

        return (localMatches + ynabMatches).sortedByDescending { it.score }
    }
}
