package com.ynab.receiptscanner.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data class for YNAB budget from API
 */
@JsonClass(generateAdapter = true)
data class BudgetDto(
    @Json(name = "id")
    val id: String,
    @Json(name = "name")
    val name: String,
    @Json(name = "last_modified_on")
    val lastModifiedOn: String,
    @Json(name = "first_month")
    val firstMonth: String? = null,
    @Json(name = "last_month")
    val lastMonth: String? = null,
    @Json(name = "date_format")
    val dateFormat: DateFormatDto? = null,
    @Json(name = "currency_format")
    val currencyFormat: CurrencyFormatDto? = null,
    @Json(name = "accounts")
    val accounts: List<AccountDto>? = null,
    @Json(name = "categories")
    val categories: List<CategoryDto>? = null
)

/**
 * Wrapper for budgets list response
 */
@JsonClass(generateAdapter = true)
data class BudgetsWrapper(
    @Json(name = "budgets")
    val budgets: List<BudgetDto>,
    @Json(name = "default_budget")
    val defaultBudget: BudgetDto? = null
)

@JsonClass(generateAdapter = true)
data class DateFormatDto(
    @Json(name = "format")
    val format: String
)

@JsonClass(generateAdapter = true)
data class CurrencyFormatDto(
    @Json(name = "iso_code")
    val isoCode: String,
    @Json(name = "example_format")
    val exampleFormat: String,
    @Json(name = "decimal_digits")
    val decimalDigits: Int,
    @Json(name = "decimal_separator")
    val decimalSeparator: String,
    @Json(name = "symbol_first")
    val symbolFirst: Boolean,
    @Json(name = "group_separator")
    val groupSeparator: String,
    @Json(name = "currency_symbol")
    val currencySymbol: String,
    @Json(name = "display_symbol")
    val displaySymbol: Boolean
)
