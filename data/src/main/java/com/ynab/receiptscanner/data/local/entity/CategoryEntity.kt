package com.ynab.receiptscanner.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Room entity for caching YNAB category data
 */
@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["budget_id"]),
        Index(value = ["category_group_id"]),
        Index(value = ["name"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = BudgetEntity::class,
            parentColumns = ["id"],
            childColumns = ["budget_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CategoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "budget_id")
    val budgetId: String,
    
    @ColumnInfo(name = "category_group_id")
    val categoryGroupId: String,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "budgeted")
    val budgeted: Long,
    
    @ColumnInfo(name = "activity")
    val activity: Long,
    
    @ColumnInfo(name = "balance")
    val balance: Long,
    
    @ColumnInfo(name = "cached_at")
    val cachedAt: Date = Date()
)
