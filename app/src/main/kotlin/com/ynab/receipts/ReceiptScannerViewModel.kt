package com.ynab.receipts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ynab.receipts.domain.model.ParsedReceipt
import com.ynab.receipts.domain.model.ReceiptScan
import com.ynab.receipts.domain.model.ReceiptStatus
import com.ynab.receipts.domain.repository.ReceiptRepository
import com.ynab.receipts.domain.usecase.EnqueueTransactionUseCase
import com.ynab.receipts.domain.usecase.ExtractReceiptFieldsUseCase
import com.ynab.receipts.domain.usecase.SyncQueuedTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class ReceiptScannerUiState(
    val payee: String? = null,
    val amountMinor: Long? = null,
    val reviewPayeeInput: String = "",
    val reviewAmountInput: String = "",
    val reviewDateInput: String = "",
    val currency: String = "USD",
    val confidence: Map<String, Float> = emptyMap(),
    val ocrContextLines: List<String> = emptyList(),
    val isConnected: Boolean = false,
    val queuedCount: Int = 0,
    val failedCount: Int = 0,
    val latestParsed: ParsedReceipt? = null
)

@HiltViewModel
class ReceiptScannerViewModel @Inject constructor(
    private val extractReceiptFieldsUseCase: ExtractReceiptFieldsUseCase,
    private val enqueueTransactionUseCase: EnqueueTransactionUseCase,
    private val syncQueuedTransactionsUseCase: SyncQueuedTransactionsUseCase,
    private val receiptRepository: ReceiptRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReceiptScannerUiState())
    val uiState: ReceiptScannerUiState
        get() = _uiState.value

    fun captureReceipt(imageBytes: ByteArray) {
        viewModelScope.launch {
            val parsed = extractReceiptFieldsUseCase(imageBytes)
            _uiState.update {
                it.copy(
                    payee = parsed.payee,
                    amountMinor = parsed.amountMinor,
                    reviewPayeeInput = parsed.payee.orEmpty(),
                    reviewAmountInput = parsed.amountMinor?.let(::formatMinorToMajor).orEmpty(),
                    reviewDateInput = parsed.date?.toString().orEmpty(),
                    currency = parsed.currency,
                    confidence = parsed.confidence,
                    ocrContextLines = parsed.sourceOcrText
                        .lineSequence()
                        .map { line -> line.trim() }
                        .filter { line -> line.isNotEmpty() }
                        .take(6)
                        .toList(),
                    latestParsed = parsed
                )
            }
        }
    }

    fun updateReviewPayee(value: String) {
        _uiState.update { it.copy(reviewPayeeInput = value) }
    }

    fun updateReviewAmount(value: String) {
        _uiState.update { it.copy(reviewAmountInput = value) }
    }

    fun updateReviewDate(value: String) {
        _uiState.update { it.copy(reviewDateInput = value) }
    }

    fun confirmAndQueue() {
        val parsed = _uiState.value.latestParsed ?: return
        val reviewState = _uiState.value
        val editedPayee = reviewState.reviewPayeeInput.trim().ifEmpty { parsed.payee }
        val editedAmountMinor = parseMajorToMinor(reviewState.reviewAmountInput) ?: parsed.amountMinor
        val editedDate = parseFlexibleDate(reviewState.reviewDateInput) ?: parsed.date
        val hasEdits = editedPayee != parsed.payee || editedAmountMinor != parsed.amountMinor || editedDate != parsed.date

        viewModelScope.launch {
            val receipt = ReceiptScan(
                id = UUID.randomUUID(),
                createdAt = Instant.now(),
                imageUriEncrypted = "enc://receipt/${UUID.randomUUID()}",
                ocrText = parsed.sourceOcrText,
                parseVersion = "v1",
                payee = editedPayee,
                amountMinor = editedAmountMinor,
                currency = parsed.currency,
                date = editedDate,
                taxMinor = parsed.taxMinor,
                lineItemsJson = null,
                confidenceJson = parsed.confidence.toString(),
                userEditedFieldsJson = if (hasEdits) {
                    "{" +
                        "\"payee\":\"${editedPayee.orEmpty()}\"," +
                        "\"amountMinor\":\"${editedAmountMinor?.toString().orEmpty()}\"," +
                        "\"date\":\"${editedDate?.toString().orEmpty()}\"" +
                        "}"
                } else {
                    null
                },
                status = ReceiptStatus.Draft
            )
            receiptRepository.saveDraft(receipt)
            enqueueTransactionUseCase(receipt)
            syncQueuedTransactionsUseCase()
            refreshQueueCounts()
        }
    }

    fun connectYnab() {
        _uiState.update { it.copy(isConnected = true) }
    }

    fun disconnectYnab() {
        _uiState.update { it.copy(isConnected = false) }
    }

    private suspend fun refreshQueueCounts() {
        val pending = receiptRepository.listPendingQueueItems(limit = 100)
        _uiState.update {
            it.copy(
                queuedCount = pending.size,
                failedCount = pending.count { queueItem -> queueItem.lastError != null }
            )
        }
    }

    private fun parseMajorToMinor(value: String): Long? {
        val normalized = value.trim().replace(",", "")
        if (normalized.isEmpty()) return null
        return normalized.toDoubleOrNull()?.let { amount -> (amount * 100).toLong() }
    }

    private fun formatMinorToMajor(value: Long): String {
        return String.format("%.2f", value / 100.0)
    }

    private fun parseFlexibleDate(value: String): LocalDate? {
        val input = value.trim()
        if (input.isEmpty()) return null

        return runCatching { LocalDate.parse(input) }
            .getOrNull()
            ?: runCatching {
                val parts = input.split("/")
                if (parts.size != 3) return@runCatching null
                LocalDate.of(parts[2].toInt(), parts[0].toInt(), parts[1].toInt())
            }.getOrNull()
    }
}
