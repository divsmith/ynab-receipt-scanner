package com.ynab.receiptscanner.data.mapper

import com.ynab.receiptscanner.data.remote.dto.*
import com.ynab.receiptscanner.domain.model.YnabTransaction
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*

/**
 * Unit tests for YnabMapper
 */
class YnabMapperTest {
    
    private lateinit var mapper: YnabMapper
    
    @Before
    fun setup() {
        mapper = YnabMapper()
    }
    
    @Test
    fun `mapBudgetToDomain converts BudgetDto correctly`() {
        // Given
        val dto = BudgetDto(
            id = "budget-123",
            name = "My Budget",
            lastModifiedOn = "2024-01-15T10:30:00Z",
            firstMonth = "2024-01",
            lastMonth = "2024-12",
            dateFormat = null,
            currencyFormat = null,
            accounts = emptyList(),
            categories = emptyList()
        )
        
        // When
        val result = mapper.mapBudgetToDomain(dto)
        
        // Then
        assertEquals("budget-123", result.id)
        assertEquals("My Budget", result.name)
        assertNotNull(result.lastModifiedOn)
        assertTrue(result.accounts.isEmpty())
        assertTrue(result.categories.isEmpty())
    }
    
    @Test
    fun `mapAccountToDomain converts AccountDto correctly`() {
        // Given
        val dto = AccountDto(
            id = "account-123",
            name = "Checking",
            type = "checking",
            onBudget = true,
            closed = false,
            balance = 100000, // $100.00
            clearedBalance = 90000,
            unclearedBalance = 10000,
            transferPayeeId = null,
            deleted = false
        )
        
        // When
        val result = mapper.mapAccountToDomain(dto, "budget-123")
        
        // Then
        assertEquals("account-123", result.id)
        assertEquals("budget-123", result.budgetId)
        assertEquals("Checking", result.name)
        assertEquals("checking", result.type)
        assertEquals(100000L, result.balance)
        assertEquals(100.0, result.getBalanceInCurrency(), 0.01)
        assertFalse(result.closed)
    }
    
    @Test
    fun `mapCategoryToDomain converts CategoryDto correctly`() {
        // Given
        val dto = CategoryDto(
            id = "category-123",
            categoryGroupId = "group-456",
            categoryGroupName = "Monthly Bills",
            name = "Groceries",
            hidden = false,
            budgeted = 50000, // $50.00
            activity = -30000, // -$30.00
            balance = 20000, // $20.00
            deleted = false
        )
        
        // When
        val result = mapper.mapCategoryToDomain(dto)
        
        // Then
        assertEquals("category-123", result.id)
        assertEquals("group-456", result.categoryGroupId)
        assertEquals("Groceries", result.name)
        assertEquals(50000L, result.budgeted)
        assertEquals(-30000L, result.activity)
        assertEquals(20000L, result.balance)
    }
    
    @Test
    fun `mapTransactionToDomain converts TransactionDto correctly`() {
        // Given
        val dto = TransactionDto(
            id = "transaction-123",
            date = "2024-01-15",
            amount = -12340, // -$12.34
            memo = "Test transaction",
            cleared = "cleared",
            approved = true,
            accountId = "account-123",
            accountName = "Checking",
            payeeId = "payee-456",
            payeeName = "Test Store",
            categoryId = "category-789",
            categoryName = "Groceries",
            transferAccountId = null,
            transferTransactionId = null,
            matchedTransactionId = null,
            importId = "IMPORT:123",
            deleted = false,
            subtransactions = null
        )
        
        // When
        val result = mapper.mapTransactionToDomain(dto)
        
        // Then
        assertEquals("transaction-123", result.id)
        assertEquals("account-123", result.accountId)
        assertEquals("category-789", result.categoryId)
        assertEquals(-12340L, result.amount)
        assertEquals("Test Store", result.payee)
        assertEquals("Test transaction", result.memo)
        assertEquals("cleared", result.cleared)
        assertTrue(result.approved)
        assertEquals("IMPORT:123", result.importId)
    }
    
    @Test
    fun `mapTransactionToDto converts domain model correctly`() {
        // Given
        val transaction = YnabTransaction(
            id = "transaction-123",
            accountId = "account-123",
            categoryId = "category-789",
            date = Date(),
            amount = -12340,
            payee = "Test Store",
            memo = "Test transaction",
            cleared = "cleared",
            approved = true,
            importId = "IMPORT:123"
        )
        
        // When
        val result = mapper.mapTransactionToDto(transaction)
        
        // Then
        assertEquals("account-123", result.accountId)
        assertEquals("category-789", result.categoryId)
        assertEquals(-12340L, result.amount)
        assertEquals("Test Store", result.payeeName)
        assertEquals("Test transaction", result.memo)
        assertEquals("cleared", result.cleared)
        assertTrue(result.approved == true)
        assertEquals("IMPORT:123", result.importId)
        assertNull(result.payeeId) // We use payee name
    }
    
    @Test
    fun `flattenCategoryGroups returns only non-deleted non-hidden categories`() {
        // Given
        val categoryGroups = listOf(
            CategoryGroupDto(
                id = "group-1",
                name = "Group 1",
                hidden = false,
                deleted = false,
                categories = listOf(
                    CategoryDto("cat-1", "group-1", "Group 1", "Category 1", false, 0, 0, 0, false),
                    CategoryDto("cat-2", "group-1", "Group 1", "Category 2", true, 0, 0, 0, false), // hidden
                    CategoryDto("cat-3", "group-1", "Group 1", "Category 3", false, 0, 0, 0, true) // deleted
                )
            ),
            CategoryGroupDto(
                id = "group-2",
                name = "Group 2",
                hidden = true, // entire group hidden
                deleted = false,
                categories = listOf(
                    CategoryDto("cat-4", "group-2", "Group 2", "Category 4", false, 0, 0, 0, false)
                )
            )
        )
        
        // When
        val result = mapper.flattenCategoryGroups(categoryGroups)
        
        // Then
        assertEquals(1, result.size) // Only cat-1 should be included
        assertEquals("cat-1", result[0].id)
    }
}
