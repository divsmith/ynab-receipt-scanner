package com.ynab.receiptscanner.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data class for YNAB category from API
 */
@JsonClass(generateAdapter = true)
data class CategoryDto(
    @Json(name = "id")
    val id: String,
    @Json(name = "category_group_id")
    val categoryGroupId: String,
    @Json(name = "category_group_name")
    val categoryGroupName: String? = null,
    @Json(name = "name")
    val name: String,
    @Json(name = "hidden")
    val hidden: Boolean,
    @Json(name = "budgeted")
    val budgeted: Long,
    @Json(name = "activity")
    val activity: Long,
    @Json(name = "balance")
    val balance: Long,
    @Json(name = "deleted")
    val deleted: Boolean = false
)

/**
 * Wrapper for categories list response
 */
@JsonClass(generateAdapter = true)
data class CategoriesWrapper(
    @Json(name = "category_groups")
    val categoryGroups: List<CategoryGroupDto>,
    @Json(name = "server_knowledge")
    val serverKnowledge: Long
)

@JsonClass(generateAdapter = true)
data class CategoryGroupDto(
    @Json(name = "id")
    val id: String,
    @Json(name = "name")
    val name: String,
    @Json(name = "hidden")
    val hidden: Boolean,
    @Json(name = "deleted")
    val deleted: Boolean = false,
    @Json(name = "categories")
    val categories: List<CategoryDto>
)
