package com.ynab.receiptscanner.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Generic wrapper for YNAB API responses
 */
@JsonClass(generateAdapter = true)
data class YnabResponse<T>(
    @Json(name = "data")
    val data: T
)

/**
 * Error response from YNAB API
 */
@JsonClass(generateAdapter = true)
data class YnabErrorResponse(
    @Json(name = "error")
    val error: YnabError
)

@JsonClass(generateAdapter = true)
data class YnabError(
    @Json(name = "id")
    val id: String,
    @Json(name = "name")
    val name: String,
    @Json(name = "detail")
    val detail: String
)
