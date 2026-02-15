package com.ynab.receiptscanner.domain.model

import java.util.Date

/**
 * Domain model representing a YNAB transaction
 * @property id YNAB transaction ID
 * @property accountId YNAB account ID where this transaction belongs
 * @property categoryId YNAB category ID for this transaction
 * @property date Transaction date
 * @property amount Transaction amount in milliunits (e.g., $12.34 = 12340)
 * @property payee Payee name
 * @property memo Transaction memo/notes
 * @property cleared Transaction cleared status
 * @property approved Whether the transaction is approved
 * @property importId Import ID for deduplication
 */
data class YnabTransaction(
    val id: String,
    val accountId: String,
    val categoryId: String? = null,
    val date: Date,
    val amount: Long,
    val payee: String? = null,
    val memo: String? = null,
    val cleared: String = "uncleared",
    val approved: Boolean = false,
    val importId: String? = null
) {
    /**
     * Returns the amount in standard currency units
     * YNAB stores amounts in milliunits (1000 milliunits = 1 currency unit)
     */
    fun getAmountInCurrency(): Double {
        return amount / 1000.0
    }
    
    companion object {
        /**
         * Converts a standard currency amount to YNAB milliunits
         */
        fun toMilliunits(amount: Double): Long {
            return (amount * 1000).toLong()
        }
    }
}
