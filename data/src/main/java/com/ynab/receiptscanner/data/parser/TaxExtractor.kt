package com.ynab.receiptscanner.data.parser

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts tax information from OCR text
 * Handles various tax formats and calculations
 */
@Singleton
class TaxExtractor @Inject constructor(
    private val amountExtractor: AmountExtractor
) {
    
    companion object {
        // Tax keywords
        private val TAX_KEYWORDS = listOf(
            "tax", "vat", "gst", "hst", "pst", "qst",
            "sales tax", "state tax", "local tax",
            "taxable", "tax amount"
        )
        
        // Tax rate pattern (e.g., "8.5%", "TAX 13%")
        private val TAX_RATE_PATTERN = Regex(
            """(\d+(?:\.\d+)?)\s*%""",
            RegexOption.IGNORE_CASE
        )
    }
    
    /**
     * Extract tax amount and rate from text
     */
    fun extractTax(text: String): TaxResult {
        val lines = text.split("\n")
        
        var taxAmount: Double? = null
        var taxRate: Double? = null
        var confidence = 0.0f
        
        for (line in lines) {
            val lowerLine = line.toLowerCase()
            
            // Check if line contains tax keyword
            if (TAX_KEYWORDS.any { lowerLine.contains(it) }) {
                // Extract amount from this line
                val amounts = amountExtractor.extractAllAmounts(line)
                if (amounts.isNotEmpty()) {
                    taxAmount = amounts.first().amount
                    confidence = 0.85f
                }
                
                // Extract tax rate if present
                val rateMatch = TAX_RATE_PATTERN.find(line)
                if (rateMatch != null) {
                    taxRate = rateMatch.groupValues[1].toDoubleOrNull()
                    confidence = 0.9f // Higher confidence if rate is specified
                }
                
                // Found tax line, no need to continue
                break
            }
        }
        
        return TaxResult(
            amount = taxAmount,
            rate = taxRate,
            confidence = confidence
        )
    }
    
    /**
     * Calculate tax from subtotal and total if not explicitly stated
     */
    fun calculateTaxFromAmounts(subtotal: Double?, total: Double?): Double? {
        if (subtotal == null || total == null) {
            return null
        }
        
        val calculated = total - subtotal
        
        // Only return if reasonable (positive and less than 30% of subtotal)
        return if (calculated > 0 && calculated < subtotal * 0.3) {
            calculated
        } else {
            null
        }
    }
    
    /**
     * Validate tax amount against total
     */
    fun validateTax(tax: Double, total: Double): Boolean {
        // Tax should be positive and less than 30% of total
        return tax > 0 && tax < total * 0.3
    }
    
    /**
     * Result of tax extraction
     */
    data class TaxResult(
        val amount: Double?,
        val rate: Double?, // Percentage (e.g., 8.5 for 8.5%)
        val confidence: Float
    )
}
