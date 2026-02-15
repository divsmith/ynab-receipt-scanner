package com.ynab.receiptscanner.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Room entity for caching YNAB budget data
 */
@Entity(
    tableName = "budgets",
    indices = [Index(value = ["last_modified_on"])]
)
data class BudgetEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "last_modified_on")
    val lastModifiedOn: Date,
    
    @ColumnInfo(name = "cached_at")
    val cachedAt: Date = Date()
)
