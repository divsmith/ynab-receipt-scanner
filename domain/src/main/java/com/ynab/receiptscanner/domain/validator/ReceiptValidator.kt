package com.ynab.receiptscanner.domain.validator

import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Receipt data validator
 * Validates receipt fields before processing
 */
@Singleton
class ReceiptValidator @Inject constructor() {
    
    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val errors: List<ValidationError>) : ValidationResult()
    }
    
    data class ValidationError(
        val field: String,
        val message: String
    )
    
    /**
     * Validate receipt fields
     */
    fun validate(
        amount: BigDecimal?,
        date: LocalDate?,
        payee: String?,
        currency: String?
    ): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        // Validate amount
        if (amount == null) {
            errors.add(ValidationError("amount", "Amount is required"))
        } else if (amount <= BigDecimal.ZERO) {
            errors.add(ValidationError("amount", "Amount must be greater than 0"))
        } else if (amount > BigDecimal("1000000")) {
            errors.add(ValidationError("amount", "Amount seems unreasonably high. Please verify."))
        }
        
        // Validate date
        if (date == null) {
            errors.add(ValidationError("date", "Date is required"))
        } else {
            val today = LocalDate.now()
            if (date.isAfter(today)) {
                errors.add(ValidationError("date", "Date cannot be in the future"))
            }
            
            // Warn if date is more than 1 year old
            if (date.isBefore(today.minusYears(1))) {
                errors.add(ValidationError("date", "Date is more than 1 year old. Please verify."))
            }
        }
        
        // Validate payee
        if (payee.isNullOrBlank()) {
            errors.add(ValidationError("payee", "Payee is required"))
        } else if (payee.length < 2) {
            errors.add(ValidationError("payee", "Payee name is too short"))
        } else if (payee.length > 100) {
            errors.add(ValidationError("payee", "Payee name is too long"))
        }
        
        // Validate currency
        if (!currency.isNullOrBlank() && !isValidCurrency(currency)) {
            errors.add(ValidationError("currency", "Invalid currency code"))
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    /**
     * Validate amount only
     */
    fun validateAmount(amount: BigDecimal?): Boolean {
        return amount != null && amount > BigDecimal.ZERO && amount <= BigDecimal("1000000")
    }
    
    /**
     * Validate date only
     */
    fun validateDate(date: LocalDate?): Boolean {
        return date != null && !date.isAfter(LocalDate.now())
    }
    
    /**
     * Validate payee only
     */
    fun validatePayee(payee: String?): Boolean {
        return !payee.isNullOrBlank() && payee.length in 2..100
    }
    
    /**
     * Check if currency code is valid ISO 4217
     */
    private fun isValidCurrency(currency: String): Boolean {
        val validCurrencies = setOf(
            "USD", "EUR", "GBP", "CAD", "AUD", "JPY", "CNY", "INR",
            "CHF", "SEK", "NZD", "MXN", "SGD", "HKD", "NOK", "KRW",
            "TRY", "RUB", "BRL", "ZAR", "DKK", "PLN", "THB", "IDR",
            "HUF", "CZK", "ILS", "CLP", "PHP", "AED", "COP", "SAR",
            "MYR", "RON", "ARS"
        )
        return validCurrencies.contains(currency.uppercase())
    }
}
