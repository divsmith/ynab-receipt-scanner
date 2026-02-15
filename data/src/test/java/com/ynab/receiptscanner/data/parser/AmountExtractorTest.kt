package com.ynab.receiptscanner.data.parser

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AmountExtractor
 * Tests currency parsing edge cases
 */
class AmountExtractorTest {
    
    private lateinit var amountExtractor: AmountExtractor
    
    @Before
    fun setup() {
        amountExtractor = AmountExtractor()
    }
    
    @Test
    fun `parseAmount handles US format with dollar sign`() {
        val result = amountExtractor.parseAmount("$12.34")
        assertEquals(12.34, result, 0.001)
    }
    
    @Test
    fun `parseAmount handles European format with comma`() {
        val result = amountExtractor.parseAmount("12,34")
        assertEquals(12.34, result, 0.001)
    }
    
    @Test
    fun `parseAmount handles thousands separator`() {
        val result = amountExtractor.parseAmount("$1,234.56")
        assertEquals(1234.56, result, 0.001)
    }
    
    @Test
    fun `parseAmount handles space as thousands separator`() {
        val result = amountExtractor.parseAmount("1 234.56")
        assertEquals(1234.56, result, 0.001)
    }
    
    @Test
    fun `parseAmount handles Euro symbol`() {
        val result = amountExtractor.parseAmount("€45.67")
        assertEquals(45.67, result, 0.001)
    }
    
    @Test
    fun `parseAmount handles amount without currency symbol`() {
        val result = amountExtractor.parseAmount("89.99")
        assertEquals(89.99, result, 0.001)
    }
    
    @Test
    fun `parseAmount returns null for invalid input`() {
        val result = amountExtractor.parseAmount("abc")
        assertNull(result)
    }
    
    @Test
    fun `extractTotal finds amount with total keyword`() {
        val text = """
            Item 1       5.99
            Item 2       8.99
            Subtotal    14.98
            Tax          1.20
            TOTAL       16.18
        """.trimIndent()
        
        val result = amountExtractor.extractTotal(text)
        assertNotNull(result.amount)
        assertEquals(16.18, result.amount!!, 0.001)
        assertTrue(result.confidence > 0.8f)
    }
    
    @Test
    fun `extractTotal finds largest amount when no keyword`() {
        val text = """
            Coffee Shop
            Item 1       5.99
            Item 2       8.99
            Amount      14.98
        """.trimIndent()
        
        val result = amountExtractor.extractTotal(text)
        assertNotNull(result.amount)
        assertEquals(14.98, result.amount!!, 0.001)
    }
    
    @Test
    fun `extractTotal handles multiple total keywords`() {
        val text = """
            Subtotal    50.00
            Tax          4.50
            Grand Total 54.50
        """.trimIndent()
        
        val result = amountExtractor.extractTotal(text)
        assertEquals(54.50, result.amount!!, 0.001)
    }
    
    @Test
    fun `extractTax finds tax amount`() {
        val text = """
            Subtotal    50.00
            Tax          4.50
            Total       54.50
        """.trimIndent()
        
        val result = amountExtractor.extractTax(text)
        assertNotNull(result.amount)
        assertEquals(4.50, result.amount!!, 0.001)
        assertTrue(result.confidence > 0.5f)
    }
    
    @Test
    fun `extractTax returns null when no tax found`() {
        val text = """
            Coffee Shop
            Total       10.00
        """.trimIndent()
        
        val result = amountExtractor.extractTax(text)
        assertNull(result.amount)
        assertEquals(0.0f, result.confidence, 0.001f)
    }
    
    @Test
    fun `extractAllAmounts finds all amounts in text`() {
        val text = """
            Item 1       5.99
            Item 2       8.50
            Item 3      12.25
            Total       26.74
        """.trimIndent()
        
        val results = amountExtractor.extractAllAmounts(text)
        assertEquals(4, results.size)
        assertTrue(results.any { it.amount == 5.99 })
        assertTrue(results.any { it.amount == 8.50 })
        assertTrue(results.any { it.amount == 12.25 })
        assertTrue(results.any { it.amount == 26.74 })
    }
    
    @Test
    fun `extractTotal handles various formats`() {
        val formats = listOf(
            "Total: $25.50" to 25.50,
            "TOTAL DUE $30.00" to 30.00,
            "Amount Due: 15.75" to 15.75,
            "Grand Total: $99.99" to 99.99,
            "Balance: 50.00" to 50.00
        )
        
        formats.forEach { (text, expected) ->
            val result = amountExtractor.extractTotal(text)
            assertNotNull("Failed for: $text", result.amount)
            assertEquals("Failed for: $text", expected, result.amount!!, 0.001)
        }
    }
    
    @Test
    fun `parseAmount handles edge cases`() {
        // Zero amount
        assertEquals(0.0, amountExtractor.parseAmount("$0.00"), 0.001)
        
        // Large amount
        assertEquals(9999.99, amountExtractor.parseAmount("$9,999.99"), 0.001)
        
        // No decimal
        assertEquals(25.0, amountExtractor.parseAmount("$25"), 0.001)
        
        // Multiple currency symbols (should still work)
        val result = amountExtractor.parseAmount("$$12.34")
        assertNotNull(result)
    }
    
    @Test
    fun `extractTotal prefers keyword match over largest amount`() {
        val text = """
            Refund credited: $1000.00
            Purchase amount: $50.00
            Tax: $4.50
            Total: $54.50
        """.trimIndent()
        
        val result = amountExtractor.extractTotal(text)
        // Should find $54.50 with "Total" keyword, not $1000.00
        assertEquals(54.50, result.amount!!, 0.001)
        assertTrue(result.confidence > 0.9f)
    }
}
