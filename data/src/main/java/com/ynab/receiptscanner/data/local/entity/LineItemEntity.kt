package com.ynab.receiptscanner.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a line item in the database
 * Has a foreign key relationship with ReceiptEntity
 */
@Entity(
    tableName = "line_items",
    foreignKeys = [
        ForeignKey(
            entity = ReceiptEntity::class,
            parentColumns = ["id"],
            childColumns = ["receipt_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["receipt_id"])
    ]
)
data class LineItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "receipt_id")
    val receiptId: String,
    
    @ColumnInfo(name = "description")
    val description: String,
    
    @ColumnInfo(name = "quantity")
    val quantity: Double,
    
    @ColumnInfo(name = "unit_price")
    val unitPrice: Double,
    
    @ColumnInfo(name = "total_price")
    val totalPrice: Double
)
