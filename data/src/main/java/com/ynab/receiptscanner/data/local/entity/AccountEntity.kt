package com.ynab.receiptscanner.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Room entity for caching YNAB account data
 */
@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["budget_id"]),
        Index(value = ["closed"]),
        Index(value = ["type"])
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
data class AccountEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "budget_id")
    val budgetId: String,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "type")
    val type: String,
    
    @ColumnInfo(name = "balance")
    val balance: Long,
    
    @ColumnInfo(name = "closed")
    val closed: Boolean,
    
    @ColumnInfo(name = "cached_at")
    val cachedAt: Date = Date()
)
