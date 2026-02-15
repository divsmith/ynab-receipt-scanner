package com.ynab.receiptscanner.data.repository

import com.ynab.receiptscanner.domain.model.YnabTransaction
import org.junit.Assert.*
import org.junit.Test
import java.util.*

/**
 * Unit tests for DuplicateDetector
 */
class DuplicateDetectorTest {
    
    @Test
    fun `findDuplicates returns empty list when no duplicates exist`() {
        // Given
        val transaction = createTransaction(
            amount = -10000,
            payee = "Store A",
            date = Date()
        )
        
        val existingTransactions = listOf(
            createTransaction(amount = -20000, payee = "Store B", date = Date()),
            createTransaction(amount = -30000, payee = "Store C", date = Date())
        )
        
        // When
        val result = DuplicateDetector.findDuplicates(transaction, existingTransactions)
        
        // Then
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `findDuplicates finds exact duplicate`() {
        // Given
        val date = Date()
        val transaction = createTransaction(
            amount = -10000,
            payee = "Store A",
            date = date
        )
        
        val duplicate = createTransaction(
            amount = -10000,
            payee = "Store A",
            date = date
        )
        
        val existingTransactions = listOf(
            duplicate,
            createTransaction(amount = -20000, payee = "Store B", date = date)
        )
        
        // When
        val result = DuplicateDetector.findDuplicates(transaction, existingTransactions)
        
        // Then
        assertEquals(1, result.size)
        assertEquals(duplicate.id, result[0].id)
    }
    
    @Test
    fun `findDuplicates finds similar payee names`() {
        // Given
        val date = Date()
        val transaction = createTransaction(
            amount = -10000,
            payee = "Starbucks Coffee",
            date = date
        )
        
        val similar = createTransaction(
            amount = -10000,
            payee = "Starbucks",
            date = date
        )
        
        val existingTransactions = listOf(similar)
        
        // When
        val result = DuplicateDetector.findDuplicates(transaction, existingTransactions)
        
        // Then
        assertEquals(1, result.size)
    }
    
    @Test
    fun `findDuplicates matches within date tolerance`() {
        // Given
        val calendar = Calendar.getInstance()
        val today = calendar.time
        
        calendar.add(Calendar.DAY_OF_YEAR, -2) // 2 days ago
        val twoDaysAgo = calendar.time
        
        val transaction = createTransaction(
            amount = -10000,
            payee = "Store A",
            date = today
        )
        
        val withinTolerance = createTransaction(
            amount = -10000,
            payee = "Store A",
            date = twoDaysAgo
        )
        
        val existingTransactions = listOf(withinTolerance)
        
        // When
        val result = DuplicateDetector.findDuplicates(transaction, existingTransactions)
        
        // Then
        assertEquals(1, result.size)
    }
    
    @Test
    fun `findDuplicates ignores transactions outside date tolerance`() {
        // Given
        val calendar = Calendar.getInstance()
        val today = calendar.time
        
        calendar.add(Calendar.DAY_OF_YEAR, -5) // 5 days ago (outside tolerance)
        val fiveDaysAgo = calendar.time
        
        val transaction = createTransaction(
            amount = -10000,
            payee = "Store A",
            date = today
        )
        
        val outsideTolerance = createTransaction(
            amount = -10000,
            payee = "Store A",
            date = fiveDaysAgo
        )
        
        val existingTransactions = listOf(outsideTolerance)
        
        // When
        val result = DuplicateDetector.findDuplicates(transaction, existingTransactions)
        
        // Then
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `findDuplicates requires same account`() {
        // Given
        val transaction = createTransaction(
            accountId = "account-1",
            amount = -10000,
            payee = "Store A",
            date = Date()
        )
        
        val differentAccount = createTransaction(
            accountId = "account-2",
            amount = -10000,
            payee = "Store A",
            date = Date()
        )
        
        val existingTransactions = listOf(differentAccount)
        
        // When
        val result = DuplicateDetector.findDuplicates(transaction, existingTransactions)
        
        // Then
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `generateImportId creates deterministic ID`() {
        // Given
        val transaction = createTransaction(
            amount = -10000,
            payee = "Store A",
            date = Date()
        )
        
        // When
        val id1 = DuplicateDetector.generateImportId(transaction)
        val id2 = DuplicateDetector.generateImportId(transaction)
        
        // Then
        assertEquals(id1, id2) // Should be same for same transaction
        assertTrue(id1.startsWith("YNABRECEIPT:"))
    }
    
    @Test
    fun `generateImportId creates unique IDs for different transactions`() {
        // Given
        val transaction1 = createTransaction(
            amount = -10000,
            payee = "Store A",
            date = Date()
        )
        
        val transaction2 = createTransaction(
            amount = -20000,
            payee = "Store B",
            date = Date()
        )
        
        // When
        val id1 = DuplicateDetector.generateImportId(transaction1)
        val id2 = DuplicateDetector.generateImportId(transaction2)
        
        // Then
        assertNotEquals(id1, id2)
    }
    
    @Test
    fun `calculateDuplicateConfidence returns high score for exact duplicate`() {
        // Given
        val date = Date()
        val transaction = createTransaction(amount = -10000, payee = "Store A", date = date)
        val duplicate = createTransaction(amount = -10000, payee = "Store A", date = date)
        
        // When
        val confidence = DuplicateDetector.calculateDuplicateConfidence(transaction, duplicate)
        
        // Then
        assertTrue(confidence > 0.9f) // Should be very high confidence
    }
    
    @Test
    fun `calculateDuplicateConfidence returns lower score for different payee`() {
        // Given
        val date = Date()
        val transaction = createTransaction(amount = -10000, payee = "Store A", date = date)
        val different = createTransaction(amount = -10000, payee = "Completely Different Store", date = date)
        
        // When
        val confidence = DuplicateDetector.calculateDuplicateConfidence(transaction, different)
        
        // Then
        assertTrue(confidence < 0.6f) // Should be lower confidence
    }
    
    // Helper function to create test transactions
    private fun createTransaction(
        id: String = UUID.randomUUID().toString(),
        accountId: String = "account-123",
        amount: Long,
        payee: String,
        date: Date,
        categoryId: String? = "category-456",
        memo: String? = null
    ): YnabTransaction {
        return YnabTransaction(
            id = id,
            accountId = accountId,
            categoryId = categoryId,
            date = date,
            amount = amount,
            payee = payee,
            memo = memo,
            cleared = "cleared",
            approved = false,
            importId = null
        )
    }
}
