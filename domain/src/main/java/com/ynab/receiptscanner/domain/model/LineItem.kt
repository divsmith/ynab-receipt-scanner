package com.ynab.receiptscanner.domain.model

import java.util.UUID

/**
 * Represents an individual line item on a receipt
 * @property id Unique identifier for the line item
 * @property receiptId ID of the parent receipt
 * @property description Description or name of the item
 * @property quantity Quantity of the item
 * @property unitPrice Price per unit
 * @property totalPrice Total price for this line item (quantity × unitPrice)
 */
data class LineItem(
    val id: String = UUID.randomUUID().toString(),
    val receiptId: String,
    val description: String,
    val quantity: Double = 1.0,
    val unitPrice: Double,
    val totalPrice: Double = quantity * unitPrice
) {
    /**
     * Returns true if the line item has valid pricing information
     */
    fun isValid(): Boolean {
        return description.isNotBlank() && 
               quantity > 0 && 
               unitPrice >= 0 && 
               totalPrice >= 0
    }
}
