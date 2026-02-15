package com.ynab.receiptscanner.mock

import com.ynab.receiptscanner.domain.model.*
import java.util.Date
import java.util.UUID

/**
 * Factory for creating mock YNAB data entities
 * Provides budgets, accounts, categories, and transactions
 */
object MockYnabDataFactory {
    
    // ================== Budgets ==================
    
    fun createBudget(
        id: String = UUID.randomUUID().toString(),
        name: String = "My Budget"
    ): YnabBudget = YnabBudget(
        id = id,
        name = name,
        lastModifiedOn = Date(),
        firstMonth = Date(System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000), // 1 year ago
        lastMonth = Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000), // 1 year ahead
        currencyFormat = createCurrencyFormat()
    )
    
    fun createCurrencyFormat(
        isoCode: String = "USD",
        symbol: String = "$"
    ): YnabBudget.CurrencyFormat = YnabBudget.CurrencyFormat(
        isoCode = isoCode,
        exampleFormat = "$123.45",
        decimalDigits = 2,
        decimalSeparator = ".",
        symbolFirst = true,
        groupSeparator = ",",
        currencySymbol = symbol,
        displaySymbol = true
    )
    
    fun createMultipleBudgets(count: Int = 3): List<YnabBudget> {
        return (1..count).map { index ->
            createBudget(name = "Budget $index")
        }
    }
    
    // ================== Accounts ==================
    
    fun createAccount(
        id: String = UUID.randomUUID().toString(),
        name: String = "Checking Account",
        type: YnabAccount.AccountType = YnabAccount.AccountType.CHECKING,
        balance: Long = 100000 // $1000.00 in milliunits
    ): YnabAccount = YnabAccount(
        id = id,
        name = name,
        type = type,
        onBudget = true,
        closed = false,
        balance = balance,
        clearedBalance = balance,
        unclearedBalance = 0,
        deleted = false
    )
    
    fun createCheckingAccount(name: String = "Checking"): YnabAccount =
        createAccount(name = name, type = YnabAccount.AccountType.CHECKING, balance = 250000)
    
    fun createSavingsAccount(name: String = "Savings"): YnabAccount =
        createAccount(name = name, type = YnabAccount.AccountType.SAVINGS, balance = 500000)
    
    fun createCreditCardAccount(name: String = "Credit Card"): YnabAccount =
        createAccount(name = name, type = YnabAccount.AccountType.CREDIT_CARD, balance = -50000)
    
    fun createTypicalAccounts(): List<YnabAccount> = listOf(
        createCheckingAccount("Main Checking"),
        createSavingsAccount("Emergency Fund"),
        createCreditCardAccount("Visa"),
        createAccount(name = "Cash", type = YnabAccount.AccountType.CASH, balance = 10000)
    )
    
    // ================== Categories ==================
    
    fun createCategory(
        id: String = UUID.randomUUID().toString(),
        name: String = "Groceries",
        categoryGroupId: String = "group-${UUID.randomUUID()}",
        categoryGroupName: String = "Everyday Expenses"
    ): YnabCategory = YnabCategory(
        id = id,
        categoryGroupId = categoryGroupId,
        categoryGroupName = categoryGroupName,
        name = name,
        hidden = false,
        deleted = false,
        budgeted = 50000, // $500.00
        activity = -35000, // -$350.00 spent
        balance = 15000 // $150.00 remaining
    )
    
    fun createCategoryGroup(
        groupName: String,
        categories: List<String>
    ): List<YnabCategory> {
        val groupId = "group-${UUID.randomUUID()}"
        return categories.map { categoryName ->
            createCategory(
                name = categoryName,
                categoryGroupId = groupId,
                categoryGroupName = groupName
            )
        }
    }
    
    fun createTypicalCategories(): List<YnabCategory> {
        return listOf(
            // Everyday Expenses
            *createCategoryGroup("Everyday Expenses", listOf(
                "Groceries", "Dining Out", "Gas/Fuel", "Household Items"
            )).toTypedArray(),
            
            // Bills
            *createCategoryGroup("Bills", listOf(
                "Rent/Mortgage", "Electric", "Water", "Internet", "Phone"
            )).toTypedArray(),
            
            // Fun
            *createCategoryGroup("Fun", listOf(
                "Entertainment", "Hobbies", "Vacation"
            )).toTypedArray()
        )
    }
    
    // ================== Transactions ==================
    
    fun createTransaction(
        accountId: String = "account-${UUID.randomUUID()}",
        categoryId: String? = "category-${UUID.randomUUID()}",
        amount: Long = -10000, // -$100.00 (negative = outflow)
        payeeName: String? = "Test Merchant",
        date: Date = Date()
    ): YnabTransaction = YnabTransaction(
        accountId = accountId,
        date = date,
        amount = amount,
        payeeName = payeeName,
        categoryId = categoryId,
        memo = "Test transaction",
        cleared = YnabTransaction.ClearedStatus.CLEARED,
        approved = true,
        importId = "YNAB:${UUID.randomUUID()}"
    )
    
    fun createTransactionFromReceipt(
        receipt: Receipt,
        accountId: String,
        categoryId: String?
    ): YnabTransaction = YnabTransaction(
        accountId = accountId,
        date = receipt.date,
        amount = -(receipt.amount * 1000).toLong(), // Convert to negative milliunits
        payeeName = receipt.payee,
        categoryId = categoryId,
        memo = "Imported from receipt",
        cleared = YnabTransaction.ClearedStatus.CLEARED,
        approved = true,
        importId = "YNAB:receipt:${receipt.id}"
    )
    
    fun createRecentTransactions(count: Int = 10): List<YnabTransaction> {
        return (0 until count).map { index ->
            val daysAgo = count - index
            val date = Date(System.currentTimeMillis() - daysAgo * 24L * 60 * 60 * 1000)
            
            createTransaction(
                payeeName = "Merchant $index",
                amount = -(1000L + index * 500),
                date = date
            )
        }
    }
    
    // ================== Complete Mock Data Sets ==================
    
    data class CompleteMockData(
        val budget: YnabBudget,
        val accounts: List<YnabAccount>,
        val categories: List<YnabCategory>,
        val transactions: List<YnabTransaction>
    )
    
    fun createCompleteDataSet(): CompleteMockData {
        val budget = createBudget()
        val accounts = createTypicalAccounts()
        val categories = createTypicalCategories()
        val transactions = createRecentTransactions(20)
        
        return CompleteMockData(budget, accounts, categories, transactions)
    }
}
