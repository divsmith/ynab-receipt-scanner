package com.ynab.receiptscanner.mock

import com.ynab.receiptscanner.domain.model.Receipt
import com.ynab.receiptscanner.domain.model.SyncStatus
import java.util.Date
import java.util.UUID

/**
 * Factory for creating mock Receipt objects
 * Provides various receipt states for testing
 */
object MockReceiptFactory {
    
    /**
     * Create a basic mock receipt with default values
     */
    fun createReceipt(
        id: String = UUID.randomUUID().toString(),
        payee: String = "Test Store",
        amount: Double = 25.50,
        date: Date = Date(),
        syncStatus: SyncStatus = SyncStatus.PENDING
    ): Receipt = Receipt(
        id = id,
        payee = payee,
        amount = amount,
        date = date,
        imagePath = "/test/receipt_$id.jpg",
        ocrText = "Sample OCR text for $payee",
        syncStatus = syncStatus,
        ynabTransactionId = if (syncStatus == SyncStatus.SYNCED) "ynab-tx-$id" else null,
        accountId = null,
        categoryId = null,
        createdAt = Date(),
        updatedAt = Date()
    )
    
    /**
     * Create a pending receipt (not yet synced)
     */
    fun createPendingReceipt(
        payee: String = "Coffee Shop"
    ): Receipt = createReceipt(
        payee = payee,
        amount = 4.50 + Math.random() * 20,
        syncStatus = SyncStatus.PENDING
    )
    
    /**
     * Create a synced receipt (successfully uploaded to YNAB)
     */
    fun createSyncedReceipt(
        payee: String = "Grocery Store"
    ): Receipt = createReceipt(
        payee = payee,
        amount = 50.00 + Math.random() * 150,
        syncStatus = SyncStatus.SYNCED
    ).copy(
        ynabTransactionId = "ynab-tx-${UUID.randomUUID()}",
        accountId = "account-123",
        categoryId = "category-456"
    )
    
    /**
     * Create a failed receipt (sync error)
     */
    fun createFailedReceipt(
        payee: String = "Restaurant"
    ): Receipt = createReceipt(
        payee = payee,
        amount = 35.00 + Math.random() * 50,
        syncStatus = SyncStatus.FAILED
    )
    
    /**
     * Create a receipt with minimal data (edge case)
     */
    fun createMinimalReceipt(): Receipt = Receipt(
        id = UUID.randomUUID().toString(),
        payee = "Unknown",
        amount = 0.0,
        date = Date(),
        imagePath = null,
        ocrText = "",
        syncStatus = SyncStatus.PENDING,
        ynabTransactionId = null,
        accountId = null,
        categoryId = null,
        createdAt = Date(),
        updatedAt = Date()
    )
    
    /**
     * Create a receipt with maximum data
     */
    fun createCompleteReceipt(): Receipt = Receipt(
        id = UUID.randomUUID().toString(),
        payee = "Complete Store Inc.",
        amount = 123.45,
        date = Date(),
        imagePath = "/complete/receipt.jpg",
        ocrText = """
            COMPLETE STORE INC.
            123 Main Street
            City, ST 12345
            
            Date: ${Date()}
            Item 1      ${'$'}50.00
            Item 2      ${'$'}73.45
            
            Subtotal    ${'$'}123.45
            Tax         ${'$'}11.11
            Total       ${'$'}134.56
        """.trimIndent(),
        syncStatus = SyncStatus.SYNCED,
        ynabTransactionId = "ynab-complete-tx",
        accountId = "account-complete",
        categoryId = "category-complete",
        createdAt = Date(System.currentTimeMillis() - 86400000), // 1 day ago
        updatedAt = Date()
    )
    
    /**
     * Create multiple receipts with different states
     */
    fun createMixedReceipts(count: Int = 10): List<Receipt> {
        return (1..count).map { index ->
            when (index % 4) {
                0 -> createPendingReceipt("Store $index")
                1 -> createSyncedReceipt("Store $index")
                2 -> createFailedReceipt("Store $index")
                else -> createReceipt(payee = "Store $index")
            }
        }
    }
    
    /**
     * Create receipts for a date range
     */
    fun createReceiptsForDateRange(
        startDate: Date,
        endDate: Date,
        count: Int = 5
    ): List<Receipt> {
        val timeRange = endDate.time - startDate.time
        val interval = timeRange / count
        
        return (0 until count).map { index ->
            val date = Date(startDate.time + (interval * index))
            createReceipt(
                payee = "Store ${index + 1}",
                date = date
            )
        }
    }
    
    /**
     * Create a receipt with large amount
     */
    fun createLargeAmountReceipt(): Receipt = createReceipt(
        payee = "Electronics Store",
        amount = 1299.99
    )
    
    /**
     * Create a receipt with small amount
     */
    fun createSmallAmountReceipt(): Receipt = createReceipt(
        payee = "Parking Meter",
        amount = 0.25
    )
    
    /**
     * Create receipts by category
     */
    fun createReceiptsByCategory(category: String, count: Int = 5): List<Receipt> {
        val merchants = when (category.lowercase()) {
            "groceries" -> listOf("Whole Foods", "Safeway", "Trader Joe's", "Kroger", "Albertsons")
            "dining" -> listOf("McDonald's", "Chipotle", "Subway", "Starbucks", "Pizza Hut")
            "gas" -> listOf("Shell", "Chevron", "BP", "Exxon", "Mobil")
            "shopping" -> listOf("Target", "Walmart", "Amazon", "Best Buy", "Costco")
            else -> listOf("Store 1", "Store 2", "Store 3", "Store 4", "Store 5")
        }
        
        return merchants.take(count).map { merchant ->
            createReceipt(payee = merchant)
        }
    }
}
