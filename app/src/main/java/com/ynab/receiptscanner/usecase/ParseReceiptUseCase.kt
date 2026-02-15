package com.ynab.receiptscanner.usecase

import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.data.parser.ReceiptParser
import com.ynab.receiptscanner.domain.model.OcrResult
import com.ynab.receiptscanner.domain.model.Receipt
import javax.inject.Inject

/**
 * Use case for parsing OCR results into structured receipt data
 * Extracts payee, amount, date, and other fields
 */
class ParseReceiptUseCase @Inject constructor(
    private val receiptParser: ReceiptParser
) {
    
    /**
     * Parse OCR result into a structured receipt
     * @param ocrResult OCR result from image processing
     * @return Result containing parsed receipt or error
     */
    suspend operator fun invoke(ocrResult: OcrResult): Result<Receipt> {
        // Validate input
        if (ocrResult.rawText.isBlank()) {
            return Result.Error(
                IllegalArgumentException("OCR result is empty"),
                "Cannot parse receipt from empty OCR result"
            )
        }
        
        // Parse the receipt
        return receiptParser.parse(ocrResult)
    }
    
    /**
     * Parse OCR result including line items
     * @param ocrResult OCR result from image processing
     * @return ParseResult with receipt and line items
     */
    suspend fun parseWithLineItems(ocrResult: OcrResult): ReceiptParser.ParseResult {
        return receiptParser.parseWithLineItems(ocrResult)
    }
}
