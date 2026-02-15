package com.ynab.receiptscanner.data.parser

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts payee/merchant name from OCR text
 * Uses heuristics based on typical receipt layout
 */
@Singleton
class PayeeExtractor @Inject constructor() {
    
    companion object {
        // Patterns to exclude (noise that's not merchant names)
        private val EXCLUDE_PATTERNS = listOf(
            Regex("""\d+\s+[A-Za-z\s]+,?\s+[A-Z]{2}\s+\d{5}"""), // Address
            Regex("""\(\d{3}\)\s*\d{3}-\d{4}"""), // Phone (123) 456-7890
            Regex("""\d{3}-\d{3}-\d{4}"""), // Phone 123-456-7890
            Regex("""\d{3}\.\d{3}\.\d{4}"""), // Phone 123.456.7890
            Regex("""www\.[a-z0-9.-]+\.[a-z]{2,}""", RegexOption.IGNORE_CASE), // URL
            Regex("""[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}""", RegexOption.IGNORE_CASE), // Email
            Regex("""^[A-Z]{2}\s+\d{5}"""), // ZIP code line
            Regex("""^Store #?\d+""", RegexOption.IGNORE_CASE), // Store number
            Regex("""^Receipt #?\d+""", RegexOption.IGNORE_CASE), // Receipt number
        )
        
        // Keywords that suggest this is not a merchant name
        private val EXCLUDE_KEYWORDS = listOf(
            "thank you", "thanks", "visit", "receipt", "tax", "total", 
            "subtotal", "change", "cash", "card", "credit", "debit",
            "customer", "cashier", "server", "date", "time", "transaction"
        )
        
        // Common merchant/business indicators
        private val MERCHANT_INDICATORS = listOf(
            "inc", "llc", "ltd", "corp", "co", "company", "restaurant",
            "cafe", "coffee", "store", "shop", "market", "supermarket",
            "pharmacy", "gym", "hotel", "motel", "gas", "fuel"
        )
        
        private const val MIN_NAME_LENGTH = 3
        private const val MAX_NAME_LENGTH = 50
    }
    
    /**
     * Extract payee/merchant name from text
     * Returns the name and confidence score
     */
    fun extractPayee(text: String): ExtractionResult {
        val lines = text.split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        
        if (lines.isEmpty()) {
            return ExtractionResult(null, 0.0f)
        }
        
        // Try multiple strategies
        val candidates = mutableListOf<PayeeCandidate>()
        
        // Strategy 1: Top lines (merchant name usually at top)
        candidates.addAll(extractFromTopLines(lines))
        
        // Strategy 2: Lines with ALL CAPS (merchant names often in caps)
        candidates.addAll(extractAllCapsLines(lines))
        
        // Strategy 3: Lines with business indicators
        candidates.addAll(extractLinesWithIndicators(lines))
        
        // Select best candidate
        val best = candidates
            .filter { it.confidence > 0.3f }
            .maxByOrNull { it.confidence }
        
        return if (best != null) {
            ExtractionResult(best.name, best.confidence)
        } else {
            // Fallback: return first non-empty line
            ExtractionResult(lines.firstOrNull(), 0.3f)
        }
    }
    
    /**
     * Extract candidates from top lines of receipt
     */
    private fun extractFromTopLines(lines: List<String>): List<PayeeCandidate> {
        val candidates = mutableListOf<PayeeCandidate>()
        
        // Check first 5 lines
        lines.take(5).forEachIndexed { index, line ->
            if (isValidMerchantName(line)) {
                // Higher confidence for earlier lines
                val baseConfidence = 0.8f
                val positionPenalty = index * 0.1f
                candidates.add(PayeeCandidate(line, baseConfidence - positionPenalty))
            }
        }
        
        return candidates
    }
    
    /**
     * Extract lines that are in ALL CAPS
     */
    private fun extractAllCapsLines(lines: List<String>): List<PayeeCandidate> {
        val candidates = mutableListOf<PayeeCandidate>()
        
        lines.take(10).forEach { line ->
            val hasLetters = line.any { it.isLetter() }
            val isAllCaps = line.all { !it.isLowerCase() || !it.isLetter() }
            
            if (hasLetters && isAllCaps && isValidMerchantName(line)) {
                candidates.add(PayeeCandidate(line, 0.7f))
            }
        }
        
        return candidates
    }
    
    /**
     * Extract lines with business indicators
     */
    private fun extractLinesWithIndicators(lines: List<String>): List<PayeeCandidate> {
        val candidates = mutableListOf<PayeeCandidate>()
        
        lines.take(10).forEach { line ->
            val lowerLine = line.toLowerCase()
            
            if (MERCHANT_INDICATORS.any { lowerLine.contains(it) } && 
                isValidMerchantName(line)) {
                candidates.add(PayeeCandidate(line, 0.75f))
            }
        }
        
        return candidates
    }
    
    /**
     * Check if a line is a valid merchant name
     */
    private fun isValidMerchantName(line: String): Boolean {
        val cleaned = line.trim()
        
        // Length check
        if (cleaned.length < MIN_NAME_LENGTH || cleaned.length > MAX_NAME_LENGTH) {
            return false
        }
        
        // Must contain at least one letter
        if (!cleaned.any { it.isLetter() }) {
            return false
        }
        
        // Check exclude patterns
        if (EXCLUDE_PATTERNS.any { it.matches(cleaned) }) {
            return false
        }
        
        // Check exclude keywords
        val lowerLine = cleaned.toLowerCase()
        if (EXCLUDE_KEYWORDS.any { lowerLine.contains(it) }) {
            return false
        }
        
        // Avoid lines with too many numbers
        val numberRatio = cleaned.count { it.isDigit() }.toFloat() / cleaned.length
        if (numberRatio > 0.5f) {
            return false
        }
        
        return true
    }
    
    /**
     * Clean up merchant name
     */
    fun cleanMerchantName(name: String): String {
        return name
            .trim()
            .replace(Regex("""\s+"""), " ") // Normalize whitespace
            .replace(Regex("""[*#]+$"""), "") // Remove trailing special chars
            .trim()
    }
    
    /**
     * Result of payee extraction
     */
    data class ExtractionResult(
        val payee: String?,
        val confidence: Float
    )
    
    /**
     * Internal candidate for payee
     */
    private data class PayeeCandidate(
        val name: String,
        val confidence: Float
    )
}
