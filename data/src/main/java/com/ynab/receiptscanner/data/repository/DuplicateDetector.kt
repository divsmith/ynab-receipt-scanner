package com.ynab.receiptscanner.data.repository

import com.ynab.receiptscanner.core.Constants
import com.ynab.receiptscanner.domain.model.YnabTransaction
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility for detecting duplicate transactions
 * Compares transactions by payee, amount, and date
 */
@Singleton
class DuplicateDetector @Inject constructor() {
    
    companion object {
        /**
         * Find potential duplicate transactions
         * @param transaction Transaction to check
         * @param existingTransactions List of existing transactions to compare against
         * @return List of potential duplicates with confidence scores
         */
        fun findDuplicates(
            transaction: YnabTransaction,
            existingTransactions: List<YnabTransaction>
        ): List<YnabTransaction> {
            return existingTransactions.filter { existing ->
                isDuplicate(transaction, existing)
            }
        }
        
        /**
         * Check if two transactions are likely duplicates
         * Criteria:
         * - Same account
         * - Same amount (exact match)
         * - Date within tolerance (±2 days)
         * - Payee similarity above threshold
         */
        private fun isDuplicate(
            transaction: YnabTransaction,
            existing: YnabTransaction
        ): Boolean {
            // Must be same account
            if (transaction.accountId != existing.accountId) {
                return false
            }
            
            // Must be same amount (exact match in milliunits)
            if (transaction.amount != existing.amount) {
                return false
            }
            
            // Date must be within tolerance
            if (!isDateWithinTolerance(transaction.date, existing.date)) {
                return false
            }
            
            // Payee must be similar
            val payeeSimilarity = calculatePayeeSimilarity(
                transaction.payee ?: "",
                existing.payee ?: ""
            )
            
            return payeeSimilarity >= Constants.DUPLICATE_PAYEE_SIMILARITY_THRESHOLD
        }
        
        /**
         * Check if dates are within tolerance period
         */
        private fun isDateWithinTolerance(date1: Date, date2: Date): Boolean {
            val diffMs = Math.abs(date1.time - date2.time)
            val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs)
            return diffDays <= Constants.DUPLICATE_DATE_TOLERANCE_DAYS
        }
        
        /**
         * Calculate similarity between two payee names
         * Uses Levenshtein distance normalized to 0-1 range
         */
        private fun calculatePayeeSimilarity(payee1: String, payee2: String): Float {
            if (payee1.isEmpty() && payee2.isEmpty()) return 1.0f
            if (payee1.isEmpty() || payee2.isEmpty()) return 0.0f
            
            // Normalize strings for comparison
            val s1 = payee1.trim().lowercase()
            val s2 = payee2.trim().lowercase()
            
            // Exact match
            if (s1 == s2) return 1.0f
            
            // Calculate Levenshtein distance
            val distance = levenshteinDistance(s1, s2)
            val maxLength = maxOf(s1.length, s2.length)
            
            // Convert distance to similarity (0-1 range)
            return 1.0f - (distance.toFloat() / maxLength)
        }
        
        /**
         * Calculate Levenshtein distance between two strings
         * Returns the minimum number of edits needed to transform one string into another
         */
        private fun levenshteinDistance(s1: String, s2: String): Int {
            val m = s1.length
            val n = s2.length
            
            val dp = Array(m + 1) { IntArray(n + 1) }
            
            for (i in 0..m) {
                dp[i][0] = i
            }
            
            for (j in 0..n) {
                dp[0][j] = j
            }
            
            for (i in 1..m) {
                for (j in 1..n) {
                    val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                    
                    dp[i][j] = minOf(
                        dp[i - 1][j] + 1,      // deletion
                        dp[i][j - 1] + 1,      // insertion
                        dp[i - 1][j - 1] + cost // substitution
                    )
                }
            }
            
            return dp[m][n]
        }
        
        /**
         * Generate deterministic import_id for YNAB deduplication
         * Format: YNABRECEIPT:{date}:{amount}:{hash}
         * This allows YNAB to automatically detect and reject duplicates
         */
        fun generateImportId(transaction: YnabTransaction): String {
            val dateFormat = java.text.SimpleDateFormat("yyyyMMdd", Locale.US)
            val dateString = dateFormat.format(transaction.date)
            
            // Create hash from payee and memo for uniqueness
            val hashSource = "${transaction.payee ?: ""}:${transaction.memo ?: ""}"
            val hash = hashString(hashSource).take(8)
            
            return "YNABRECEIPT:$dateString:${transaction.amount}:$hash"
        }
        
        /**
         * Generate SHA-256 hash of a string
         */
        private fun hashString(input: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
        
        /**
         * Calculate confidence score for a potential duplicate
         * Returns a value between 0 and 1
         */
        fun calculateDuplicateConfidence(
            transaction: YnabTransaction,
            potentialDuplicate: YnabTransaction
        ): Float {
            var score = 0f
            var weights = 0f
            
            // Account match (required)
            if (transaction.accountId == potentialDuplicate.accountId) {
                score += 0.2f
                weights += 0.2f
            }
            
            // Amount match (required)
            if (transaction.amount == potentialDuplicate.amount) {
                score += 0.3f
                weights += 0.3f
            }
            
            // Date proximity (weighted by closeness)
            val dateSimilarity = calculateDateSimilarity(transaction.date, potentialDuplicate.date)
            score += dateSimilarity * 0.2f
            weights += 0.2f
            
            // Payee similarity
            val payeeSimilarity = calculatePayeeSimilarity(
                transaction.payee ?: "",
                potentialDuplicate.payee ?: ""
            )
            score += payeeSimilarity * 0.3f
            weights += 0.3f
            
            return if (weights > 0) score / weights else 0f
        }
        
        /**
         * Calculate date similarity (1.0 = same day, decreasing with distance)
         */
        private fun calculateDateSimilarity(date1: Date, date2: Date): Float {
            val diffMs = Math.abs(date1.time - date2.time)
            val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs)
            
            return when {
                diffDays == 0L -> 1.0f
                diffDays <= Constants.DUPLICATE_DATE_TOLERANCE_DAYS -> {
                    1.0f - (diffDays.toFloat() / Constants.DUPLICATE_DATE_TOLERANCE_DAYS)
                }
                else -> 0.0f
            }
        }
    }
}
