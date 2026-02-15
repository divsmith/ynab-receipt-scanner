package com.ynab.receiptscanner.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data class for YNAB account from API
 */
@JsonClass(generateAdapter = true)
data class AccountDto(
    @Json(name = "id")
    val id: String,
    @Json(name = "name")
    val name: String,
    @Json(name = "type")
    val type: String,
    @Json(name = "on_budget")
    val onBudget: Boolean,
    @Json(name = "closed")
    val closed: Boolean,
    @Json(name = "balance")
    val balance: Long,
    @Json(name = "cleared_balance")
    val clearedBalance: Long,
    @Json(name = "uncleared_balance")
    val unclearedBalance: Long,
    @Json(name = "transfer_payee_id")
    val transferPayeeId: String? = null,
    @Json(name = "deleted")
    val deleted: Boolean = false
)

/**
 * Wrapper for accounts list response
 */
@JsonClass(generateAdapter = true)
data class AccountsWrapper(
    @Json(name = "accounts")
    val accounts: List<AccountDto>,
    @Json(name = "server_knowledge")
    val serverKnowledge: Long
)
