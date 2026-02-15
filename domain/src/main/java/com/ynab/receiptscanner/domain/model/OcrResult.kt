package com.ynab.receiptscanner.domain.model

import java.util.Date

/**
 * Represents the complete OCR result from scanning a receipt image
 * @property rawText Complete raw text extracted from the image
 * @property blocks Text blocks identified in the image
 * @property lines Text lines identified in the image
 * @property elements Individual text elements (words/characters)
 * @property confidence Overall confidence score of the OCR result (0.0 to 1.0)
 * @property timestamp When the OCR was performed
 */
data class OcrResult(
    val rawText: String,
    val blocks: List<TextBlock> = emptyList(),
    val lines: List<TextLine> = emptyList(),
    val elements: List<TextElement> = emptyList(),
    val confidence: Float,
    val timestamp: Date = Date()
) {
    /**
     * Represents a block of text in the OCR result
     * @property text Text content of the block
     * @property boundingBox Bounding box of the block
     * @property confidence Confidence score for this block
     */
    data class TextBlock(
        val text: String,
        val boundingBox: ReceiptField.BoundingBox,
        val confidence: Float
    )
    
    /**
     * Represents a line of text in the OCR result
     * @property text Text content of the line
     * @property boundingBox Bounding box of the line
     * @property confidence Confidence score for this line
     */
    data class TextLine(
        val text: String,
        val boundingBox: ReceiptField.BoundingBox,
        val confidence: Float
    )
    
    /**
     * Represents an individual text element (word or character)
     * @property text Text content of the element
     * @property boundingBox Bounding box of the element
     * @property confidence Confidence score for this element
     */
    data class TextElement(
        val text: String,
        val boundingBox: ReceiptField.BoundingBox,
        val confidence: Float
    )
    
    /**
     * Returns true if the OCR result meets the confidence threshold
     */
    fun isReliable(threshold: Float = 0.75f): Boolean {
        return confidence >= threshold && rawText.isNotBlank()
    }
}
