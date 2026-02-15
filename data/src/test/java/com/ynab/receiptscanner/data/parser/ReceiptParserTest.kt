package com.ynab.receiptscanner.data.parser

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ReceiptParser
 * Tests parsing with sample receipt text
 */
class ReceiptParserTest {
    
    private lateinit var receiptParser: ReceiptParser
    private lateinit var amountExtractor: AmountExtractor
    private lateinit var dateExtractor: DateExtractor
    private lateinit var payeeExtractor: PayeeExtractor
    private lateinit var taxExtractor: TaxExtractor
    private lateinit var lineItemExtractor: LineItemExtractor
    
    @Before
    fun setup() {
        amountExtractor = AmountExtractor()
        dateExtractor = DateExtractor()
        payeeExtractor = PayeeExtractor()
        taxExtractor = TaxExtractor(amountExtractor)
        lineItemExtractor = LineItemExtractor(amountExtractor)
        receiptParser = ReceiptParser(
            amountExtractor,
            dateExtractor,
            payeeExtractor,
            taxExtractor,
            lineItemExtractor
        )
    }
    
    @Test
    fun `parse complete receipt successfully`() {
        val ocrText = """
            COFFEE HOUSE
            123 Main Street
            New York, NY 10001
            
            Date: 10/15/2023
            Time: 09:30 AM
            
            Latte             4.50
            Croissant         3.25
            
            Subtotal          7.75
            Tax               0.70
            Total             8.45
            
            Thank you for visiting!
        """.trimIndent()
        
        val ocrResult = createMockOcrResult(ocrText)
        val result = receiptParser.parse(ocrResult)
        
        assertTrue(result.isSuccess)
        val receipt = (result as com.ynab.receiptscanner.core.util.Result.Success).data
        
        assertNotNull(receipt.payee)
        assertTrue(receipt.payee!!.contains("COFFEE HOUSE"))
        assertEquals(8.45, receipt.amount!!, 0.001)
        assertNotNull(receipt.date)
        assertEquals(0.70, receipt.tax!!, 0.001)
    }
    
    @Test
    fun `parse receipt with missing payee still succeeds`() {
        val ocrText = """
            Date: 10/15/2023
            
            Item 1            5.99
            Item 2            8.99
            
            Tax               1.20
            Total            16.18
        """.trimIndent()
        
        val ocrResult = createMockOcrResult(ocrText)
        val result = receiptParser.parse(ocrResult)
        
        assertTrue(result.isSuccess)
        val receipt = (result as com.ynab.receiptscanner.core.util.Result.Success).data
        
        assertEquals(16.18, receipt.amount!!, 0.001)
        assertNotNull(receipt.date)
    }
    
    @Test
    fun `parse receipt handles various currency symbols`() {
        val currencies = listOf(
            "$" to com.ynab.receiptscanner.domain.model.Currency.USD,
            "€" to com.ynab.receiptscanner.domain.model.Currency.EUR,
            "£" to com.ynab.receiptscanner.domain.model.Currency.GBP
        )
        
        currencies.forEach { (symbol, expectedCurrency) ->
            val ocrText = """
                Store Name
                Total ${symbol}25.00
            """.trimIndent()
            
            val ocrResult = createMockOcrResult(ocrText)
            val result = receiptParser.parse(ocrResult)
            
            assertTrue("Failed for $symbol", result.isSuccess)
            val receipt = (result as com.ynab.receiptscanner.core.util.Result.Success).data
            assertEquals("Failed for $symbol", expectedCurrency, receipt.currency)
        }
    }
    
    @Test
    fun `parse handles grocery store receipt format`() {
        val ocrText = """
            WALMART SUPERCENTER
            Store #1234
            
            BANANAS           2.49
            MILK              3.99
            BREAD             2.50
            EGGS              4.25
            
            SUBTOTAL         13.23
            TAX 8.5%          1.12
            TOTAL            14.35
            
            VISA ****1234    14.35
            
            11/20/2023 14:35:22
        """.trimIndent()
        
        val ocrResult = createMockOcrResult(ocrText)
        val result = receiptParser.parse(ocrResult)
        
        assertTrue(result.isSuccess)
        val receipt = (result as com.ynab.receiptscanner.core.util.Result.Success).data
        
        assertTrue(receipt.payee!!.contains("WALMART"))
        assertEquals(14.35, receipt.amount!!, 0.001)
        assertEquals(1.12, receipt.tax!!, 0.001)
    }
    
    @Test
    fun `parse handles restaurant receipt format`() {
        val ocrText = """
            Mario's Italian Restaurant
            
            Guest Check #5678
            Server: John
            Table: 12
            Date: 12/05/2023
            
            Spaghetti         15.99
            Caesar Salad       8.99
            Tiramisu           7.50
            
            Subtotal          32.48
            Tax                2.92
            
            Total             35.40
            
            Gratuity Guide:
            18% 6.37  20% 7.08
        """.trimIndent()
        
        val ocrResult = createMockOcrResult(ocrText)
        val result = receiptParser.parse(ocrResult)
        
        assertTrue(result.isSuccess)
        val receipt = (result as com.ynab.receiptscanner.core.util.Result.Success).data
        
        assertTrue(receipt.payee!!.contains("Mario"))
        assertEquals(35.40, receipt.amount!!, 0.001)
        assertNotNull(receipt.date)
    }
    
    @Test
    fun `parseWithLineItems extracts line items`() {
        val ocrText = """
            STORE NAME
            
            Coffee            4.50
            Muffin            2.95
            Juice             3.50
            
            Total            10.95
        """.trimIndent()
        
        val ocrResult = createMockOcrResult(ocrText)
        val result = receiptParser.parseWithLineItems(ocrResult)
        
        assertTrue(result is ReceiptParser.ParseResult.Success)
        val success = result as ReceiptParser.ParseResult.Success
        
        assertEquals(10.95, success.receipt.amount!!, 0.001)
        assertTrue(success.lineItems.isNotEmpty())
        
        // Verify at least some items were extracted
        val descriptions = success.lineItems.map { it.description.toLowerCase() }
        assertTrue(descriptions.any { "coffee" in it })
    }
    
    @Test
    fun `parse handles poorly formatted receipt`() {
        val ocrText = """
            STORE
            Item1 5.99
            Item2 8.50
            TOT 14.49
        """.trimIndent()
        
        val ocrResult = createMockOcrResult(ocrText)
        val result = receiptParser.parse(ocrResult)
        
        assertTrue(result.isSuccess)
        val receipt = (result as com.ynab.receiptscanner.core.util.Result.Success).data
        
        assertNotNull(receipt.amount)
        assertTrue(receipt.amount!! > 0)
    }
    
    @Test
    fun `parse fails gracefully with empty text`() {
        val ocrResult = createMockOcrResult("")
        val result = receiptParser.parse(ocrResult)
        
        assertTrue(result.isError)
    }
    
    @Test
    fun `parse extracts date from various formats`() {
        val dateFormats = listOf(
            "Date: 10/15/2023",
            "15/10/2023",
            "2023-10-15",
            "Oct 15, 2023",
            "15 October 2023"
        )
        
        dateFormats.forEach { dateStr ->
            val ocrText = """
                Store Name
                $dateStr
                Total $25.00
            """.trimIndent()
            
            val ocrResult = createMockOcrResult(ocrText)
            val result = receiptParser.parse(ocrResult)
            
            assertTrue("Failed for: $dateStr", result.isSuccess)
            val receipt = (result as com.ynab.receiptscanner.core.util.Result.Success).data
            assertNotNull("Failed for: $dateStr", receipt.date)
        }
    }
    
    /**
     * Helper to create mock OCR result
     */
    private fun createMockOcrResult(text: String): com.ynab.receiptscanner.domain.model.OcrResult {
        return com.ynab.receiptscanner.domain.model.OcrResult(
            rawText = text,
            blocks = emptyList(),
            lines = emptyList(),
            elements = emptyList(),
            confidence = 0.9f
        )
    }
}
