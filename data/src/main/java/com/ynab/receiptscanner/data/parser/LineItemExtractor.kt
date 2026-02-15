package com.ynab.receiptscanner.data.parser

import com.ynab.receiptscanner.domain.model.LineItem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts individual line items from receipt text
 * Pairs item descriptions with their prices
 */
@Singleton
class LineItemExtractor @Inject constructor(
    private val amountExtractor: AmountExtractor
) {
    
    companion object {
        // Keywords that indicate non-item lines
        private val EXCLUDE_KEYWORDS = listOf(
            "subtotal", "total", "tax", "balance", "change", "cash", "card",
            "credit", "debit", "payment", "tender", "thank", "visit",
            "cashier", "server", "date", "time", "receipt"
        )
        
        // Quantity patterns (e.g., "2x", "3 @", "qty: 2")
        private val QUANTITY_PATTERN = Regex(
            """(\d+)\s*[x@]|qty:?\s*(\d+)""",
            RegexOption.IGNORE_CASE
        )
        
        private const val MIN_DESCRIPTION_LENGTH = 3
        private const val MAX_LINE_ITEMS = 50
    }
    
    /**
     * Extract line items from text
     */
    fun extractLineItems(text: String, receiptId: String): List<LineItem> {
        val lines = text.split("\n")
        val lineItems = mutableListOf<LineItem>()
        
        // Get all amounts with their lines
        val amountsWithLines = amountExtractor.extractAllAmounts(text)
        
        // Process each line with an amount
        for (amountLine in amountsWithLines.take(MAX_LINE_ITEMS)) {
            if (isValidLineItem(amountLine.line)) {
                val lineItem = parseLineItem(amountLine, receiptId)
                if (lineItem != null) {
                    lineItems.add(lineItem)
                }
            }
        }
        
        return lineItems
    }
    
    /**
     * Parse a single line item
     */
    private fun parseLineItem(
        amountLine: AmountExtractor.AmountWithLine,
        receiptId: String
    ): LineItem? {
        val line = amountLine.line
        val amount = amountLine.amount
        
        // Extract description (text before the amount)
        val amountStr = amount.toString()
        val amountIndex = line.lastIndexOf(amountStr)
        
        val description = if (amountIndex > 0) {
            line.substring(0, amountIndex).trim()
        } else {
            // Try to remove amount pattern from end
            line.replace(Regex("""[$€£¥₹]?\s*[\d.,]+\s*$"""), "").trim()
        }
        
        if (description.length < MIN_DESCRIPTION_LENGTH) {
            return null
        }
        
        // Extract quantity if present
        val quantityMatch = QUANTITY_PATTERN.find(description)
        val quantity = if (quantityMatch != null) {
            val qtyStr = quantityMatch.groupValues.firstOrNull { it.isNotEmpty() && it.toIntOrNull() != null }
            qtyStr?.toDoubleOrNull() ?: 1.0
        } else {
            1.0
        }
        
        // Clean up description
        val cleanDescription = description
            .replace(QUANTITY_PATTERN, "")
            .trim()
            .replace(Regex("""\s+"""), " ")
        
        // Calculate unit price
        val unitPrice = if (quantity > 0) amount / quantity else amount
        
        return LineItem(
            receiptId = receiptId,
            description = cleanDescription,
            quantity = quantity,
            unitPrice = unitPrice,
            totalPrice = amount
        )
    }
    
    /**
     * Check if a line is likely a valid line item
     */
    private fun isValidLineItem(line: String): Boolean {
        val lowerLine = line.toLowerCase()
        
        // Exclude lines with keywords
        if (EXCLUDE_KEYWORDS.any { lowerLine.contains(it) }) {
            return false
        }
        
        // Must have some text (description)
        val hasText = line.any { it.isLetter() }
        if (!hasText) {
            return false
        }
        
        // Avoid lines that are too short
        if (line.trim().length < MIN_DESCRIPTION_LENGTH) {
            return false
        }
        
        return true
    }
    
    /**
     * Group line items by similarity (optional enhancement)
     * Useful for combining duplicate items
     */
    fun groupSimilarItems(items: List<LineItem>): List<LineItem> {
        val grouped = mutableMapOf<String, LineItem>()
        
        for (item in items) {
            val key = item.description.toLowerCase().trim()
            
            if (grouped.containsKey(key)) {
                // Combine with existing item
                val existing = grouped[key]!!
                grouped[key] = existing.copy(
                    quantity = existing.quantity + item.quantity,
                    totalPrice = existing.totalPrice + item.totalPrice
                )
            } else {
                grouped[key] = item
            }
        }
        
        return grouped.values.toList()
    }
    
    /**
     * Validate line items against total
     */
    fun validateLineItems(items: List<LineItem>, total: Double): Boolean {
        if (items.isEmpty()) {
            return true // No items to validate
        }
        
        val sum = items.sumOf { it.totalPrice }
        
        // Allow 10% tolerance for rounding and missing items
        val tolerance = total * 0.1
        return kotlin.math.abs(sum - total) <= tolerance
    }
}
