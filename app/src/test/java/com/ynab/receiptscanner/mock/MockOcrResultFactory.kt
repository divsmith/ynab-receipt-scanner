package com.ynab.receiptscanner.mock

import com.google.mlkit.vision.text.Text
import java.util.UUID

/**
 * Factory for creating mock OCR results
 * Provides various OCR result types for testing
 */
object MockOcrResultFactory {
    
    /**
     * Create a standard receipt OCR result
     */
    fun createStandardReceipt(
        storeName: String = "Test Store",
        amount: Double = 25.50,
        date: String = "01/15/2024"
    ): String = """
        $storeName
        123 Main Street
        City, ST 12345
        Tel: (555) 123-4567
        
        Date: $date
        Time: 10:30 AM
        
        Coffee            ${'$'}4.50
        Sandwich          ${'$'}8.99
        Chips             ${'$'}2.50
        
        Subtotal         ${'$'}${amount - 1.00}
        Tax              ${'$'}1.00
        Total            ${'$'}$amount
        
        Payment: VISA ****1234
        Thank you!
    """.trimIndent()
    
    /**
     * Create OCR result with poor quality (faded/unclear)
     */
    fun createPoorQualityReceipt(): String = """
        TE5T 5T0RE
        123 Ma1n Street
        
        Date: 01/?5/2024
        
        C0ffee           4.5O
        5andw1ch         8.99
        
        T0ta1           $14.7O
    """.trimIndent()
    
    /**
     * Create OCR result with multiple columns
     */
    fun createMultiColumnReceipt(): String = """
        GROCERY STORE
        
        Bananas  3 @ 0.99    2.97
        Apples   5 @ 1.29    6.45
        Milk     1 @ 3.99    3.99
        Bread    2 @ 2.49    4.98
        
        Subtotal            18.39
        Tax                  1.56
        Total               19.95
    """.trimIndent()
    
    /**
     * Create OCR result for restaurant receipt with tip
     */
    fun createRestaurantReceipt(): String = """
        THE ITALIAN KITCHEN
        
        Date: 01/15/2024
        Server: John D.
        Table: 12
        
        Margherita Pizza    18.00
        Caesar Salad        12.00
        House Wine (2)      20.00
        
        Subtotal            50.00
        Tax                  4.50
        
        Subtotal            54.50
        Suggested Tip 20%   10.90
        
        TOTAL               65.40
    """.trimIndent()
    
    /**
     * Create OCR result for gas station receipt
     */
    fun createGasReceipt(): String = """
        SHELL GAS STATION
        123 Highway Road
        
        Date: 01/15/2024
        Time: 08:45 AM
        
        Fuel Type: Regular
        Gallons: 12.458
        Price/Gal: ${'$'}4.179
        
        Total: ${'$'}52.07
        
        Pump #3
    """.trimIndent()
    
    /**
     * Create OCR result with line items and quantities
     */
    fun createDetailedReceipt(): String = """
        TARGET STORE #1234
        
        T-Shirt (M)     3 x 19.99    59.97
        Jeans           1 x 39.99    39.99
        Socks 6pk       1 x 14.36    14.36
        
        Subtotal                    114.32
        Tax (8.5%)                    9.72
        
        Total                       124.04
    """.trimIndent()
    
    /**
     * Create OCR result with minimal information
     */
    fun createMinimalReceipt(): String = """
        Store Name
        Total: ${'$'}15.00
    """.trimIndent()
    
    /**
     * Create OCR result with foreign characters
     */
    fun createInternationalReceipt(): String = """
        Café François
        123 Rue de la Paix
        
        Café au lait         €4.50
        Croissant            €3.25
        
        Total                €7.75
    """.trimIndent()
    
    /**
     * Create OCR result with handwritten elements (simulated)
     */
    fun createHandwrittenReceipt(): String = """
        Joe's Diner
        
        Burger              10.00
        Fries                3.50
        Soda                 2.00
        
        subtotal            15.50
        tax                  1.24
        
        Total -             16.74
    """.trimIndent()
    
    /**
     * Create OCR result completely garbled (worst case)
     */
    fun createGarbledReceipt(): String = """
        83jf 88FJ fj3j
        fjd88 fn3j4 f8j3
        1234 5678 9012
        fjjf3j f8j3f
        8.45 92.1? 15.00??
    """.trimIndent()
    
    /**
     * Create OCR result with date in various formats
     */
    fun createVariousDateFormats(dateFormat: String): String {
        val dateStr = when (dateFormat) {
            "MM/DD/YYYY" -> "01/15/2024"
            "DD/MM/YYYY" -> "15/01/2024"
            "YYYY-MM-DD" -> "2024-01-15"
            "Month DD, YYYY" -> "January 15, 2024"
            else -> "01/15/2024"
        }
        
        return """
            Store Name
            Date: $dateStr
            
            Item 1          10.00
            Total           10.00
        """.trimIndent()
    }
    
    /**
     * Create list of diverse OCR results for testing
     */
    fun createDiverseResults(): List<String> = listOf(
        createStandardReceipt(),
        createPoorQualityReceipt(),
        createMultiColumnReceipt(),
        createRestaurantReceipt(),
        createGasReceipt(),
        createDetailedReceipt(),
        createMinimalReceipt(),
        createHandwrittenReceipt()
    )
    
    /**
     * Create OCR result with specific amount
     */
    fun createReceiptWithAmount(amount: Double): String = """
        Test Store
        Date: 01/15/2024
        
        Items            ${String.format("%.2f", amount - 1.00)}
        Tax               1.00
        
        Total            ${String.format("%.2f", amount)}
    """.trimIndent()
}
