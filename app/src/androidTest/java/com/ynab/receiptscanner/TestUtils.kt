package com.ynab.receiptscanner

import android.graphics.Bitmap
import com.ynab.receiptscanner.domain.model.*
import java.util.Date
import java.util.UUID

/**
 * Test utilities for creating mock objects
 * Provides helper functions for generating test data
 */
object TestUtils {
    
    /**
     * Create a mock receipt with default or custom values
     */
    fun createMockReceipt(
        id: String = UUID.randomUUID().toString(),
        payee: String = "Test Store",
        amount: Double = 25.50,
        date: Date = Date(),
        imagePath: String? = "/test/image.jpg",
        syncStatus: SyncStatus = SyncStatus.PENDING,
        ocrText: String = "Sample OCR text",
        accountId: String? = null,
        categoryId: String? = null
    ): Receipt = Receipt(
        id = id,
        payee = payee,
        amount = amount,
        date = date,
        imagePath = imagePath,
        ocrText = ocrText,
        syncStatus = syncStatus,
        ynabTransactionId = null,
        accountId = accountId,
        categoryId = categoryId,
        createdAt = Date(),
        updatedAt = Date()
    )
    
    /**
     * Create multiple mock receipts with sequential IDs
     */
    fun createMockReceipts(count: Int): List<Receipt> {
        return (1..count).map { index ->
            createMockReceipt(
                id = "receipt-$index",
                payee = "Store $index",
                amount = 10.0 + index
            )
        }
    }
    
    /**
     * Create a mock YNAB transaction
     */
    fun createMockTransaction(
        accountId: String = "account-123",
        categoryId: String? = "category-456",
        amount: Long = -25500,
        payeeName: String? = "Test Store",
        date: Date = Date(),
        memo: String? = "Test transaction",
        importId: String? = "YNAB:${UUID.randomUUID()}"
    ): YnabTransaction = YnabTransaction(
        accountId = accountId,
        date = date,
        amount = amount,
        payeeName = payeeName,
        categoryId = categoryId,
        memo = memo,
        cleared = YnabTransaction.ClearedStatus.CLEARED,
        approved = true,
        importId = importId
    )
    
    /**
     * Create a mock YNAB account
     */
    fun createMockAccount(
        id: String = "account-123",
        name: String = "Checking Account",
        type: YnabAccount.AccountType = YnabAccount.AccountType.CHECKING,
        balance: Long = 100000,
        closed: Boolean = false
    ): YnabAccount = YnabAccount(
        id = id,
        name = name,
        type = type,
        onBudget = true,
        closed = closed,
        balance = balance,
        clearedBalance = balance,
        unclearedBalance = 0,
        deleted = false
    )
    
    /**
     * Create multiple mock accounts
     */
    fun createMockAccounts(count: Int): List<YnabAccount> {
        return (1..count).map { index ->
            createMockAccount(
                id = "account-$index",
                name = "Account $index"
            )
        }
    }
    
    /**
     * Create a mock YNAB category
     */
    fun createMockCategory(
        id: String = "category-123",
        name: String = "Groceries",
        categoryGroupId: String = "group-123",
        categoryGroupName: String = "Spending"
    ): YnabCategory = YnabCategory(
        id = id,
        categoryGroupId = categoryGroupId,
        categoryGroupName = categoryGroupName,
        name = name,
        hidden = false,
        deleted = false,
        budgeted = 0,
        activity = 0,
        balance = 0
    )
    
    /**
     * Create multiple mock categories
     */
    fun createMockCategories(count: Int): List<YnabCategory> {
        return (1..count).map { index ->
            createMockCategory(
                id = "category-$index",
                name = "Category $index"
            )
        }
    }
    
    /**
     * Create a mock YNAB budget
     */
    fun createMockBudget(
        id: String = "budget-123",
        name: String = "My Budget",
        currencyFormat: YnabBudget.CurrencyFormat = createMockCurrencyFormat()
    ): YnabBudget = YnabBudget(
        id = id,
        name = name,
        lastModifiedOn = Date(),
        firstMonth = Date(),
        lastMonth = Date(),
        currencyFormat = currencyFormat
    )
    
    /**
     * Create a mock currency format
     */
    fun createMockCurrencyFormat(
        isoCode: String = "USD",
        symbol: String = "$",
        decimalDigits: Int = 2
    ): YnabBudget.CurrencyFormat = YnabBudget.CurrencyFormat(
        isoCode = isoCode,
        exampleFormat = "$123.45",
        decimalDigits = decimalDigits,
        decimalSeparator = ".",
        symbolFirst = true,
        groupSeparator = ",",
        currencySymbol = symbol,
        displaySymbol = true
    )
    
    /**
     * Create a test bitmap for image tests
     */
    fun createTestBitmap(
        width: Int = 100,
        height: Int = 100,
        config: Bitmap.Config = Bitmap.Config.ARGB_8888
    ): Bitmap {
        return Bitmap.createBitmap(width, height, config)
    }
    
    /**
     * Create a mock OCR result
     */
    fun createMockOcrResult(
        text: String = """
            COFFEE HOUSE
            123 Main Street
            
            Date: 01/15/2024
            Latte - ${'$'}4.50
            Croissant - ${'$'}3.25
            
            Subtotal: ${'$'}7.75
            Tax: ${'$'}0.70
            Total: ${'$'}8.45
        """.trimIndent()
    ): String = text
    
    /**
     * Create a parsed receipt from OCR text
     */
    fun createParsedReceipt(
        payee: String = "Coffee House",
        amount: Double = 8.45,
        date: Date = Date(),
        tax: Double? = 0.70
    ): ParsedReceipt = ParsedReceipt(
        payee = payee,
        amount = amount,
        date = date,
        currency = "USD",
        tax = tax,
        lineItems = listOf(
            LineItem("Latte", 1, 4.50, 4.50),
            LineItem("Croissant", 1, 3.25, 3.25)
        ),
        confidence = 0.85
    )
    
    /**
     * Mock line item
     */
    data class LineItem(
        val description: String,
        val quantity: Int,
        val unitPrice: Double,
        val totalPrice: Double
    )
    
    /**
     * Mock parsed receipt
     */
    data class ParsedReceipt(
        val payee: String,
        val amount: Double,
        val date: Date,
        val currency: String,
        val tax: Double?,
        val lineItems: List<LineItem>,
        val confidence: Double
    )
}
