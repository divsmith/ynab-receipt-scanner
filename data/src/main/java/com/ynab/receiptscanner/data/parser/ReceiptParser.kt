package com.ynab.receiptscanner.data.parser

import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.domain.model.Currency
import com.ynab.receiptscanner.domain.model.OcrResult
import com.ynab.receiptscanner.domain.model.Receipt
import com.ynab.receiptscanner.domain.model.SyncStatus
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Main receipt parser orchestrator
 * Coordinates all extraction components to parse a complete receipt
 */
@Singleton
class ReceiptParser @Inject constructor(
    private val amountExtractor: AmountExtractor,
    private val dateExtractor: DateExtractor,
    private val payeeExtractor: PayeeExtractor,
    private val taxExtractor: TaxExtractor,
    private val lineItemExtractor: LineItemExtractor
) {
    
    /**
     * Parse OCR result into a structured Receipt
     */
    fun parse(ocrResult: OcrResult): Result<Receipt> {
        return try {
            val text = ocrResult.rawText
            
            // Extract all fields
            val payeeResult = payeeExtractor.extractPayee(text)
            val totalResult = amountExtractor.extractTotal(text)
            val dateResult = dateExtractor.extractDate(text)
            val taxResult = taxExtractor.extractTax(text)
            
            // Determine currency (default to USD for now)
            val currency = detectCurrency(text)
            
            // Create receipt with extracted data
            val receipt = Receipt(
                payee = payeeResult.payee?.let { payeeExtractor.cleanMerchantName(it) },
                amount = totalResult.amount,
                date = dateResult.date,
                currency = currency,
                tax = taxResult.amount,
                ocrText = text,
                syncStatus = SyncStatus.PENDING,
                createdAt = Date(),
                updatedAt = Date()
            )
            
            // Calculate overall confidence
            val confidence = calculateOverallConfidence(
                payeeResult.confidence,
                totalResult.confidence,
                dateResult.confidence,
                taxResult.confidence
            )
            
            // Validate extracted data
            val validationResult = validateReceipt(receipt, confidence)
            if (!validationResult.isValid) {
                return Result.Error(
                    Exception("Receipt validation failed"),
                    validationResult.message
                )
            }
            
            Result.Success(receipt)
            
        } catch (e: Exception) {
            Result.Error(e, "Failed to parse receipt: ${e.message}")
        }
    }
    
    /**
     * Parse receipt with line items
     * More detailed parsing including individual items
     */
    fun parseWithLineItems(ocrResult: OcrResult): ParseResult {
        val receiptResult = parse(ocrResult)
        
        return when (receiptResult) {
            is Result.Success -> {
                val receipt = receiptResult.data
                
                // Extract line items
                val lineItems = lineItemExtractor.extractLineItems(
                    ocrResult.rawText,
                    receipt.id
                )
                
                ParseResult.Success(receipt, lineItems)
            }
            is Result.Error -> {
                ParseResult.Error(receiptResult.message)
            }
            else -> {
                ParseResult.Error("Unknown error occurred during parsing")
            }
        }
    }
    
    /**
     * Detect currency from text
     */
    private fun detectCurrency(text: String): Currency {
        return when {
            text.contains("€") -> Currency.EUR
            text.contains("£") -> Currency.GBP
            text.contains("¥") -> Currency.JPY
            text.contains("₹") -> Currency.INR
            text.contains("$") -> Currency.USD
            text.contains("USD", ignoreCase = true) -> Currency.USD
            text.contains("EUR", ignoreCase = true) -> Currency.EUR
            text.contains("GBP", ignoreCase = true) -> Currency.GBP
            else -> Currency.USD // Default
        }
    }
    
    /**
     * Calculate overall parsing confidence
     */
    private fun calculateOverallConfidence(
        payeeConfidence: Float,
        amountConfidence: Float,
        dateConfidence: Float,
        taxConfidence: Float
    ): Float {
        // Weighted average (amount is most important)
        val weights = mapOf(
            "payee" to 0.25f,
            "amount" to 0.40f,
            "date" to 0.25f,
            "tax" to 0.10f
        )
        
        return (payeeConfidence * weights["payee"]!! +
                amountConfidence * weights["amount"]!! +
                dateConfidence * weights["date"]!! +
                taxConfidence * weights["tax"]!!)
    }
    
    /**
     * Validate parsed receipt
     */
    private fun validateReceipt(receipt: Receipt, confidence: Float): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Check if minimum required fields are present
        if (receipt.amount == null) {
            errors.add("Total amount not found")
        }
        
        if (receipt.payee == null) {
            errors.add("Merchant name not found")
        }
        
        if (receipt.date == null) {
            errors.add("Transaction date not found")
        }
        
        // Validate amount
        if (receipt.amount != null && receipt.amount <= 0) {
            errors.add("Invalid amount: must be positive")
        }
        
        // Validate tax
        if (receipt.tax != null && receipt.amount != null) {
            if (!taxExtractor.validateTax(receipt.tax, receipt.amount)) {
                errors.add("Tax amount seems incorrect")
            }
        }
        
        // Check confidence threshold
        if (confidence < 0.5f) {
            errors.add("Low confidence in extracted data")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult(true, "Receipt validation passed")
        } else {
            ValidationResult(false, "Validation errors: ${errors.joinToString(", ")}")
        }
    }
    
    /**
     * Result of validation
     */
    private data class ValidationResult(
        val isValid: Boolean,
        val message: String
    )
    
    /**
     * Result of parsing with line items
     */
    sealed class ParseResult {
        data class Success(
            val receipt: Receipt,
            val lineItems: List<com.ynab.receiptscanner.domain.model.LineItem>
        ) : ParseResult()
        
        data class Error(val message: String) : ParseResult()
    }
}
