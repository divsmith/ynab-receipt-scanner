package com.ynab.receiptscanner.data.parser

import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts dates from OCR text
 * Handles multiple date formats and patterns
 */
@Singleton
class DateExtractor @Inject constructor() {
    
    companion object {
        // Common date formats
        private val DATE_FORMATS = listOf(
            "MM/dd/yyyy",
            "MM-dd-yyyy",
            "MM.dd.yyyy",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "dd.MM.yyyy",
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "MMM dd, yyyy",
            "dd MMM yyyy",
            "MMMM dd, yyyy",
            "dd MMMM yyyy",
            "MMM dd yyyy",
            "dd MMM yyyy",
            "MM/dd/yy",
            "dd/MM/yy",
            "yyyy-MM-dd'T'HH:mm:ss",
            "EEE MMM dd yyyy",
            "EEE, MMM dd, yyyy"
        )
        
        // Date patterns to search for
        private val DATE_PATTERNS = listOf(
            // ISO format
            Regex("""\d{4}-\d{2}-\d{2}"""),
            // Common US format
            Regex("""\d{1,2}/\d{1,2}/\d{2,4}"""),
            // European format
            Regex("""\d{1,2}\.\d{1,2}\.\d{2,4}"""),
            // With dashes
            Regex("""\d{1,2}-\d{1,2}-\d{2,4}"""),
            // Month name formats
            Regex("""(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+\d{1,2},?\s+\d{4}""", RegexOption.IGNORE_CASE),
            Regex("""\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+\d{4}""", RegexOption.IGNORE_CASE),
            // Day of week formats
            Regex("""(?:Mon|Tue|Wed|Thu|Fri|Sat|Sun)[a-z]*\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+\d{1,2}\s+\d{4}""", RegexOption.IGNORE_CASE)
        )
        
        // Keywords that often precede dates
        private val DATE_KEYWORDS = listOf(
            "date", "dated", "on", "transaction", "purchase", "sale"
        )
    }
    
    /**
     * Extract date from text
     * Returns the date and confidence score
     */
    fun extractDate(text: String): ExtractionResult {
        val lines = text.split("\n")
        
        // Try to find date with keyword first
        val keywordDate = findDateWithKeyword(lines)
        if (keywordDate != null && keywordDate.confidence > 0.7f) {
            return keywordDate
        }
        
        // Try to find date using patterns
        val patternDate = findDateWithPattern(text)
        if (patternDate != null) {
            return patternDate
        }
        
        // Return keyword date if found, even with lower confidence
        if (keywordDate != null) {
            return keywordDate
        }
        
        return ExtractionResult(null, 0.0f)
    }
    
    /**
     * Find date near date-related keywords
     */
    private fun findDateWithKeyword(lines: List<String>): ExtractionResult? {
        for (line in lines.take(10)) { // Focus on top of receipt
            val lowerLine = line.toLowerCase()
            
            if (DATE_KEYWORDS.any { lowerLine.contains(it) }) {
                // Try to parse dates from this line and next few lines
                val searchLines = lines.indexOf(line).let { idx ->
                    lines.subList(idx, minOf(idx + 3, lines.size))
                }
                
                for (searchLine in searchLines) {
                    val date = tryParseDate(searchLine)
                    if (date != null) {
                        return ExtractionResult(date, 0.9f)
                    }
                }
            }
        }
        
        return null
    }
    
    /**
     * Find date using regex patterns
     */
    private fun findDateWithPattern(text: String): ExtractionResult? {
        for (pattern in DATE_PATTERNS) {
            val matches = pattern.findAll(text)
            for (match in matches) {
                val date = tryParseDate(match.value)
                if (date != null && isReasonableDate(date)) {
                    return ExtractionResult(date, 0.75f)
                }
            }
        }
        
        return null
    }
    
    /**
     * Try to parse a date string using multiple formats
     */
    private fun tryParseDate(dateStr: String): Date? {
        val cleaned = dateStr.trim()
        
        for (format in DATE_FORMATS) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                sdf.isLenient = false
                val date = sdf.parse(cleaned)
                
                if (date != null && isReasonableDate(date)) {
                    return date
                }
            } catch (e: Exception) {
                // Try next format
                continue
            }
        }
        
        return null
    }
    
    /**
     * Check if date is reasonable (not too far in past/future)
     */
    private fun isReasonableDate(date: Date): Boolean {
        val now = Date()
        val yearInMillis = 365L * 24 * 60 * 60 * 1000
        
        // Accept dates within 5 years in the past and 1 day in the future
        val minDate = Date(now.time - 5 * yearInMillis)
        val maxDate = Date(now.time + 24 * 60 * 60 * 1000)
        
        return date.after(minDate) && date.before(maxDate)
    }
    
    /**
     * Extract all potential dates from text
     */
    fun extractAllDates(text: String): List<DateMatch> {
        val results = mutableListOf<DateMatch>()
        
        for (pattern in DATE_PATTERNS) {
            pattern.findAll(text).forEach { match ->
                val date = tryParseDate(match.value)
                if (date != null && isReasonableDate(date)) {
                    results.add(DateMatch(date, match.value, match.range))
                }
            }
        }
        
        return results
    }
    
    /**
     * Result of date extraction
     */
    data class ExtractionResult(
        val date: Date?,
        val confidence: Float
    )
    
    /**
     * A matched date with its position
     */
    data class DateMatch(
        val date: Date,
        val text: String,
        val range: IntRange
    )
}
