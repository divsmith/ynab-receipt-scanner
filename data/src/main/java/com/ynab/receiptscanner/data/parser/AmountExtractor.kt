package com.ynab.receiptscanner.data.parser

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts monetary amounts from OCR text
 * Handles various currency formats and patterns
 */
@Singleton
class AmountExtractor @Inject constructor() {
    
    companion object {
        // Common total keywords in multiple languages
        private val TOTAL_KEYWORDS = listOf(
            "total", "amount", "balance", "sum", "grand total", 
            "total due", "amount due", "balance due",
            // Additional patterns
            "subtotal", "net", "sale"
        )
        
        // Tax keywords
        private val TAX_KEYWORDS = listOf(
            "tax", "vat", "gst", "hst", "sales tax", "pst"
        )
        
        // Currency symbols
        private val CURRENCY_SYMBOLS = "[$€£¥₹]"
        
        // Amount pattern: optional currency symbol + digits with optional decimal
        private val AMOUNT_PATTERN = Regex(
            """$CURRENCY_SYMBOLS?\s*(\d{1,3}(?:[,\s]\d{3})*|\d+)(?:[.,]\d{2})?""",
            RegexOption.IGNORE_CASE
        )
        
        // More specific total pattern with keywords
        private val TOTAL_PATTERN = Regex(
            """(?:${TOTAL_KEYWORDS.joinToString("|")})[:\s]*$CURRENCY_SYMBOLS?\s*(\d{1,3}(?:[,\s]\d{3})*|\d+)(?:[.,]\d{2})?""",
            RegexOption.IGNORE_CASE
        )
    }
    
    /**
     * Extract the total amount from text
     * Returns the amount and confidence score
     */
    fun extractTotal(text: String): ExtractionResult {
        val lines = text.split("\n")
        
        // Try to find amount with "total" keyword first
        val totalMatch = findTotalWithKeyword(lines)
        if (totalMatch != null) {
            return totalMatch
        }
        
        // Fall back to finding the largest amount (likely the total)
        return findLargestAmount(lines)
    }
    
    /**
     * Extract tax amount from text
     */
    fun extractTax(text: String): ExtractionResult {
        val lines = text.split("\n")
        
        for (line in lines) {
            val lowerLine = line.toLowerCase()
            
            // Check if line contains tax keyword
            if (TAX_KEYWORDS.any { lowerLine.contains(it) }) {
                val amounts = AMOUNT_PATTERN.findAll(line).toList()
                if (amounts.isNotEmpty()) {
                    val amount = parseAmount(amounts.first().value)
                    if (amount != null) {
                        return ExtractionResult(amount, 0.9f)
                    }
                }
            }
        }
        
        return ExtractionResult(null, 0.0f)
    }
    
    /**
     * Extract all amounts from text
     * Useful for finding line items
     */
    fun extractAllAmounts(text: String): List<AmountWithLine> {
        val results = mutableListOf<AmountWithLine>()
        val lines = text.split("\n")
        
        lines.forEachIndexed { index, line ->
            AMOUNT_PATTERN.findAll(line).forEach { match ->
                val amount = parseAmount(match.value)
                if (amount != null) {
                    results.add(AmountWithLine(amount, line, index))
                }
            }
        }
        
        return results
    }
    
    /**
     * Find total amount using keyword matching
     */
    private fun findTotalWithKeyword(lines: List<String>): ExtractionResult? {
        // Search from bottom up (total usually at bottom)
        for (i in lines.indices.reversed()) {
            val line = lines[i]
            val lowerLine = line.toLowerCase()
            
            // Check if line contains "total" keyword
            if (TOTAL_KEYWORDS.any { lowerLine.contains(it) }) {
                val matches = AMOUNT_PATTERN.findAll(line).toList()
                
                if (matches.isNotEmpty()) {
                    // Take the last amount on the line (usually the total)
                    val amount = parseAmount(matches.last().value)
                    if (amount != null && amount > 0) {
                        // Higher confidence if "total" keyword is present
                        return ExtractionResult(amount, 0.95f)
                    }
                }
            }
        }
        
        return null
    }
    
    /**
     * Find the largest amount (fallback strategy)
     */
    private fun findLargestAmount(lines: List<String>): ExtractionResult {
        var maxAmount: Double? = null
        var maxLine = ""
        
        lines.forEach { line ->
            AMOUNT_PATTERN.findAll(line).forEach { match ->
                val amount = parseAmount(match.value)
                if (amount != null && (maxAmount == null || amount > maxAmount!!)) {
                    maxAmount = amount
                    maxLine = line
                }
            }
        }
        
        return if (maxAmount != null) {
            // Lower confidence for fallback method
            ExtractionResult(maxAmount, 0.6f)
        } else {
            ExtractionResult(null, 0.0f)
        }
    }
    
    /**
     * Parse amount string to double
     * Handles various formats: $12.34, 12,34, 1 234.56, etc.
     */
    fun parseAmount(amountStr: String): Double? {
        return try {
            // Remove currency symbols and spaces
            val cleaned = amountStr
                .replace(Regex("[$€£¥₹]"), "")
                .replace(" ", "")
                .trim()
            
            // Handle comma as decimal separator (European format)
            val normalized = if (cleaned.count { it == ',' } == 1 && cleaned.count { it == '.' } == 0) {
                cleaned.replace(',', '.')
            } else {
                // Remove thousands separators
                cleaned.replace(",", "")
            }
            
            normalized.toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Result of amount extraction
     */
    data class ExtractionResult(
        val amount: Double?,
        val confidence: Float
    )
    
    /**
     * Amount with its source line
     */
    data class AmountWithLine(
        val amount: Double,
        val line: String,
        val lineNumber: Int
    )
}
