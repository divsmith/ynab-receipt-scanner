package com.ynab.receiptscanner.validator

import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Transaction validator
 * Validates transaction data before submission to YNAB
 */
@Singleton
class TransactionValidator @Inject constructor() {
    
    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val errors: List<ValidationError>) : ValidationResult()
    }
    
    data class ValidationError(
        val field: String,
        val message: String
    )
    
    /**
     * Validate transaction before submission
     */
    fun validate(
        accountId: String?,
        amount: BigDecimal?,
        date: LocalDate?,
        payee: String?,
        categoryId: String? = null,
        memo: String? = null
    ): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        // Validate account
        if (accountId.isNullOrBlank()) {
            errors.add(ValidationError("account", "Please select an account"))
        }
        
        // Validate amount
        if (amount == null) {
            errors.add(ValidationError("amount", "Amount is required"))
        } else if (amount == BigDecimal.ZERO) {
            errors.add(ValidationError("amount", "Amount cannot be zero"))
        } else if (amount.abs() > BigDecimal("1000000")) {
            errors.add(ValidationError("amount", "Amount exceeds maximum allowed value"))
        }
        
        // Validate date
        if (date == null) {
            errors.add(ValidationError("date", "Date is required"))
        } else {
            val today = LocalDate.now()
            if (date.isAfter(today)) {
                errors.add(ValidationError("date", "Date cannot be in the future"))
            }
            
            // Check if date is within reasonable range (e.g., within 5 years)
            if (date.isBefore(today.minusYears(5))) {
                errors.add(ValidationError("date", "Date is too far in the past"))
            }
        }
        
        // Validate payee
        if (payee.isNullOrBlank()) {
            errors.add(ValidationError("payee", "Payee is required"))
        } else if (payee.length > 100) {
            errors.add(ValidationError("payee", "Payee name is too long (max 100 characters)"))
        }
        
        // Validate memo (optional but has limits)
        if (memo != null && memo.length > 200) {
            errors.add(ValidationError("memo", "Memo is too long (max 200 characters)"))
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    /**
     * Validate account selection
     */
    fun validateAccount(accountId: String?): Boolean {
        return !accountId.isNullOrBlank()
    }
    
    /**
     * Validate amount for transaction
     */
    fun validateAmount(amount: BigDecimal?): Boolean {
        return amount != null && amount != BigDecimal.ZERO && amount.abs() <= BigDecimal("1000000")
    }
    
    /**
     * Validate date for transaction
     */
    fun validateDate(date: LocalDate?): Boolean {
        if (date == null) return false
        val today = LocalDate.now()
        return !date.isAfter(today) && !date.isBefore(today.minusYears(5))
    }
    
    /**
     * Check if transaction is within YNAB budget period
     * YNAB typically allows transactions within the current month and future months
     */
    fun isWithinBudgetPeriod(date: LocalDate): Boolean {
        val today = LocalDate.now()
        val earliestAllowed = today.minusMonths(3) // Allow 3 months back
        val latestAllowed = today.plusYears(1) // Allow 1 year forward
        
        return date.isAfter(earliestAllowed) && date.isBefore(latestAllowed)
    }
    
    /**
     * Get user-friendly error message
     */
    fun getErrorMessage(result: ValidationResult): String? {
        return when (result) {
            is ValidationResult.Valid -> null
            is ValidationResult.Invalid -> {
                result.errors.joinToString("\n") { "${it.field}: ${it.message}" }
            }
        }
    }
}
