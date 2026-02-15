package com.ynab.receiptscanner.domain.model

/**
 * Represents a currency with its properties
 * @property code ISO 4217 currency code (e.g., USD, EUR, GBP)
 * @property symbol Currency symbol (e.g., $, €, £)
 * @property decimalPlaces Number of decimal places for the currency
 */
data class Currency(
    val code: String,
    val symbol: String,
    val decimalPlaces: Int = 2
) {
    companion object {
        val USD = Currency("USD", "$", 2)
        val EUR = Currency("EUR", "€", 2)
        val GBP = Currency("GBP", "£", 2)
        val CAD = Currency("CAD", "CA$", 2)
        val AUD = Currency("AUD", "A$", 2)
        val JPY = Currency("JPY", "¥", 0)
        val INR = Currency("INR", "₹", 2)
        
        /**
         * Get currency by code, defaults to USD if not found
         */
        fun fromCode(code: String): Currency {
            return when (code.uppercase()) {
                "USD" -> USD
                "EUR" -> EUR
                "GBP" -> GBP
                "CAD" -> CAD
                "AUD" -> AUD
                "JPY" -> JPY
                "INR" -> INR
                else -> Currency(code.uppercase(), code.uppercase(), 2)
            }
        }
    }
}
