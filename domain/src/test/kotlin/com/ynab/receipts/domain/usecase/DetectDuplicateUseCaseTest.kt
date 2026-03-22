package com.ynab.receipts.domain.usecase

import com.ynab.receipts.domain.model.ReceiptScan
import com.ynab.receipts.domain.model.ReceiptStatus
import com.ynab.receipts.domain.repository.YnabTransaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class DetectDuplicateUseCaseTest {
    private val useCase = DetectDuplicateUseCase()

    @Test
    fun `returns duplicates from local and ynab when fingerprint matches`() {
        val date = LocalDate.of(2026, 2, 20)
        val localReceipt = ReceiptScan(
            id = UUID.randomUUID(),
            createdAt = Instant.now(),
            imageUriEncrypted = "enc://img1",
            ocrText = "receipt",
            parseVersion = "v1",
            payee = "Coffee Shop",
            amountMinor = 1450,
            currency = "USD",
            date = date,
            confidenceJson = "{}",
            status = ReceiptStatus.Synced
        )
        val ynabReceipt = YnabTransaction(
            id = "txn_1",
            payee = "Coffee Shop",
            amountMinor = 1450,
            date = date
        )

        val result = useCase.findPotentialDuplicates(
            payee = "Coffee Shop",
            amountMinor = 1450,
            date = date,
            localSynced = listOf(localReceipt),
            ynabTransactions = listOf(ynabReceipt)
        )

        assertEquals(2, result.size)
    }
}
