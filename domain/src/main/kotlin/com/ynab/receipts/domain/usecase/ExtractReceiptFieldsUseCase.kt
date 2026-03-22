package com.ynab.receipts.domain.usecase

import com.ynab.receipts.domain.model.ParsedReceipt
import com.ynab.receipts.domain.service.OcrEngine
import com.ynab.receipts.domain.service.ReceiptImagePreprocessor
import com.ynab.receipts.domain.service.ReceiptParser
import java.util.Locale
import javax.inject.Inject

class ExtractReceiptFieldsUseCase @Inject constructor(
    private val ocrEngine: OcrEngine,
    private val imagePreprocessor: ReceiptImagePreprocessor,
    private val parser: ReceiptParser
) {
    suspend operator fun invoke(imageBytes: ByteArray, locale: Locale = Locale.US): ParsedReceipt {
        val candidates = runCatching {
            imagePreprocessor.buildCandidates(imageBytes).ifEmpty { listOf(imageBytes) }
        }.getOrElse {
            listOf(imageBytes)
        }

        var bestDocument = ocrEngine.recognize(candidates.first())
        var bestScore = bestDocument.qualityScore()

        for (candidate in candidates.drop(1)) {
            val document = ocrEngine.recognize(candidate)
            val score = document.qualityScore()
            if (score > bestScore) {
                bestDocument = document
                bestScore = score
            }
            if (score >= STRONG_OCR_SCORE) {
                break
            }
        }

        return parser.parse(bestDocument, locale)
    }

    private fun com.ynab.receipts.domain.model.OcrDocument.qualityScore(): Int {
        val nonBlankLines = lines.count { line -> line.text.isNotBlank() }
        return rawText.length + (nonBlankLines * 40)
    }

    private companion object {
        const val STRONG_OCR_SCORE = 280
    }
}
