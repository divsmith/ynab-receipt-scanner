package com.ynab.receiptscanner

import android.graphics.Bitmap
import com.google.mlkit.vision.text.Text
import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.data.ocr.OcrEngine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake OCR engine for testing
 * Returns predictable results for consistent UI tests
 */
@Singleton
class FakeOcrEngine @Inject constructor() : OcrEngine {
    
    // Configurable OCR results for testing
    var nextResult: Result<Text>? = null
    var shouldFail: Boolean = false
    var delayMs: Long = 0L
    
    override suspend fun processImage(bitmap: Bitmap): Result<Text> {
        if (delayMs > 0) {
            kotlinx.coroutines.delay(delayMs)
        }
        
        return when {
            shouldFail -> Result.Error(
                Exception("OCR processing failed"),
                "Failed to process image"
            )
            nextResult != null -> nextResult!!
            else -> createDefaultResult()
        }
    }
    
    private fun createDefaultResult(): Result<Text> {
        val mockText = """
            TEST STORE
            123 Main Street
            
            Date: 01/15/2024
            Time: 10:30 AM
            
            Coffee         ${'$'}4.50
            Sandwich       ${'$'}8.99
            
            Subtotal      ${'$'}13.49
            Tax           ${'$'}1.21
            Total         ${'$'}14.70
        """.trimIndent()
        
        // Create a mock Text object
        // Note: This is simplified; real implementation might need more complex mocking
        val text = createMockTextObject(mockText)
        return Result.Success(text)
    }
    
    private fun createMockTextObject(text: String): Text {
        // This is a simplified mock
        // In real tests, you might need to use reflection or more sophisticated mocking
        return object : Text() {
            override fun getText(): String = text
            override fun getTextBlocks(): List<TextBlock> = emptyList()
        }
    }
    
    /**
     * Set a custom OCR result for the next processImage call
     */
    fun setNextResult(
        payee: String = "Test Store",
        amount: String = "14.70",
        date: String = "01/15/2024"
    ) {
        val mockText = """
            $payee
            123 Main Street
            
            Date: $date
            
            Item 1         ${'$'}10.00
            Item 2         ${'$'}${amount.toDouble() - 10.00}
            
            Total         ${'$'}$amount
        """.trimIndent()
        
        nextResult = Result.Success(createMockTextObject(mockText))
    }
    
    /**
     * Reset to default behavior
     */
    fun reset() {
        nextResult = null
        shouldFail = false
        delayMs = 0L
    }
}
