package com.ynab.receiptscanner.data

import com.ynab.receiptscanner.data.local.entity.*
import com.ynab.receiptscanner.domain.model.*
import java.util.Date
import java.util.UUID

/**
 * Test data objects for data layer tests
 * Provides consistent test data across test files
 */
object TestData {
    
    // ================== Entities ==================
    
    fun createReceiptEntity(
        id: String = UUID.randomUUID().toString(),
        payee: String = "Test Store",
        amount: Double = 25.50,
        syncStatus: SyncStatus = SyncStatus.PENDING
    ): ReceiptEntity = ReceiptEntity(
        id = id,
        payee = payee,
        amount = amount,
        date = Date(),
        imagePath = "/test/receipt_$id.jpg",
        ocrText = "OCR text for $payee",
        syncStatus = syncStatus,
        ynabTransactionId = null,
        accountId = null,
        categoryId = null,
        createdAt = Date(),
        updatedAt = Date()
    )
    
    fun createLineItemEntity(
        id: String = UUID.randomUUID().toString(),
        receiptId: String,
        description: String = "Test Item"
    ): LineItemEntity = LineItemEntity(
        id = id,
        receiptId = receiptId,
        description = description,
        quantity = 1,
        unitPrice = 10.00,
        totalPrice = 10.00
    )
    
    fun createPendingTransactionEntity(
        id: String = UUID.randomUUID().toString(),
        receiptId: String? = null,
        status: String = "PENDING"
    ): PendingTransactionEntity = PendingTransactionEntity(
        id = id,
        receiptId = receiptId,
        budgetId = "budget-123",
        accountId = "account-456",
        date = Date(),
        amount = -25500,
        payeeName = "Test Store",
        categoryId = "category-789",
        memo = "Test transaction",
        cleared = "cleared",
        approved = true,
        importId = "YNAB:$id",
        status = status,
        retryCount = 0,
        errorMessage = null,
        lastAttemptAt = null,
        createdAt = Date()
    )
    
    fun createBudgetEntity(
        id: String = "budget-123",
        name: String = "Test Budget"
    ): BudgetEntity = BudgetEntity(
        id = id,
        name = name,
        lastModifiedOn = Date(),
        currencyCode = "USD",
        currencySymbol = "$"
    )
    
    fun createAccountEntity(
        id: String = "account-123",
        budgetId: String = "budget-123",
        name: String = "Test Account"
    ): AccountEntity = AccountEntity(
        id = id,
        budgetId = budgetId,
        name = name,
        type = "checking",
        onBudget = true,
        closed = false,
        balance = 100000,
        clearedBalance = 100000,
        unclearedBalance = 0,
        deleted = false
    )
    
    fun createCategoryEntity(
        id: String = "category-123",
        budgetId: String = "budget-123",
        name: String = "Groceries"
    ): CategoryEntity = CategoryEntity(
        id = id,
        budgetId = budgetId,
        categoryGroupId = "group-456",
        categoryGroupName = "Everyday Expenses",
        name = name,
        hidden = false,
        deleted = false
    )
    
    // ================== DTOs ==================
    
    fun createBudgetDto(): Map<String, Any> = mapOf(
        "id" to "budget-123",
        "name" to "Test Budget",
        "last_modified_on" to "2024-01-15T00:00:00Z",
        "first_month" to "2024-01-01",
        "last_month" to "2024-12-31",
        "currency_format" to mapOf(
            "iso_code" to "USD",
            "example_format" to "$123.45",
            "decimal_digits" to 2,
            "decimal_separator" to ".",
            "symbol_first" to true,
            "group_separator" to ",",
            "currency_symbol" to "$",
            "display_symbol" to true
        )
    )
    
    fun createAccountDto(): Map<String, Any> = mapOf(
        "id" to "account-123",
        "name" to "Checking Account",
        "type" to "checking",
        "on_budget" to true,
        "closed" to false,
        "balance" to 100000,
        "cleared_balance" to 100000,
        "uncleared_balance" to 0,
        "deleted" to false,
        "transfer_payee_id" to "payee-123"
    )
    
    fun createCategoryDto(): Map<String, Any> = mapOf(
        "id" to "category-123",
        "category_group_id" to "group-456",
        "name" to "Groceries",
        "hidden" to false,
        "deleted" to false,
        "budgeted" to 50000,
        "activity" to -35000,
        "balance" to 15000
    )
    
    // ================== API Responses ==================
    
    fun createBudgetsResponse(): String = """
    {
      "data": {
        "budgets": [
          ${createBudgetJson()}
        ]
      }
    }
    """.trimIndent()
    
    fun createAccountsResponse(): String = """
    {
      "data": {
        "accounts": [
          ${createAccountJson()}
        ]
      }
    }
    """.trimIndent()
    
    fun createTransactionResponse(): String = """
    {
      "data": {
        "transaction": {
          "id": "transaction-123",
          "date": "2024-01-15",
          "amount": -25500,
          "memo": "Test transaction",
          "cleared": "cleared",
          "approved": true,
          "account_id": "account-123",
          "category_id": "category-456",
          "payee_id": "payee-789",
          "deleted": false,
          "import_id": "YNAB:test-import-id"
        }
      }
    }
    """.trimIndent()
    
    private fun createBudgetJson(): String = """
    {
      "id": "budget-123",
      "name": "Test Budget",
      "last_modified_on": "2024-01-15T00:00:00Z",
      "first_month": "2024-01-01",
      "last_month": "2024-12-31",
      "currency_format": {
        "iso_code": "USD",
        "example_format": "${'$'}123.45",
        "decimal_digits": 2,
        "decimal_separator": ".",
        "symbol_first": true,
        "group_separator": ",",
        "currency_symbol": "${'$'}",
        "display_symbol": true
      }
    }
    """.trimIndent()
    
    private fun createAccountJson(): String = """
    {
      "id": "account-123",
      "name": "Checking Account",
      "type": "checking",
      "on_budget": true,
      "closed": false,
      "balance": 100000,
      "cleared_balance": 100000,
      "uncleared_balance": 0,
      "deleted": false,
      "transfer_payee_id": "payee-123"
    }
    """.trimIndent()
}
