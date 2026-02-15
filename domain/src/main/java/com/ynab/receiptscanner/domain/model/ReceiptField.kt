package com.ynab.receiptscanner.domain.model

/**
 * Represents a specific field extracted from a receipt via OCR
 * @property fieldType Type of the field extracted
 * @property value Extracted value as string
 * @property confidence OCR confidence score (0.0 to 1.0)
 * @property boundingBox Bounding box coordinates for the field in the image
 */
data class ReceiptField(
    val fieldType: FieldType,
    val value: String,
    val confidence: Float,
    val boundingBox: BoundingBox? = null
) {
    /**
     * Types of fields that can be extracted from receipts
     */
    enum class FieldType {
        MERCHANT_NAME,
        TRANSACTION_DATE,
        TRANSACTION_TIME,
        TOTAL_AMOUNT,
        SUBTOTAL,
        TAX,
        TIP,
        PAYMENT_METHOD,
        CARD_NUMBER,
        PHONE_NUMBER,
        ADDRESS,
        LINE_ITEM,
        OTHER
    }
    
    /**
     * Represents the bounding box of a field in the image
     * @property left Left coordinate
     * @property top Top coordinate
     * @property right Right coordinate
     * @property bottom Bottom coordinate
     */
    data class BoundingBox(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    ) {
        val width: Float
            get() = right - left
            
        val height: Float
            get() = bottom - top
    }
    
    /**
     * Returns true if the confidence level meets the threshold
     */
    fun isConfident(threshold: Float = 0.7f): Boolean {
        return confidence >= threshold
    }
}
