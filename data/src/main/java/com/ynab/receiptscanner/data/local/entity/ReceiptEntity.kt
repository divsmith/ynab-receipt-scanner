package com.ynab.receiptscanner.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ynab.receiptscanner.domain.model.Currency
import com.ynab.receiptscanner.domain.model.SyncStatus
import java.util.Date

/**
 * Room entity representing a receipt in the database
 * Uses indexes on commonly queried fields for performance
 */
@Entity(
    tableName = "receipts",
    indices = [
        Index(value = ["sync_status"]),
        Index(value = ["created_at"]),
        Index(value = ["date"])
    ]
)
data class ReceiptEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "payee")
    val payee: String?,
    
    @ColumnInfo(name = "amount")
    val amount: Double?,
    
    @ColumnInfo(name = "date")
    val date: Date?,
    
    @ColumnInfo(name = "currency")
    val currency: Currency,
    
    @ColumnInfo(name = "tax")
    val tax: Double?,
    
    @ColumnInfo(name = "image_path")
    val imagePath: String?,
    
    @ColumnInfo(name = "ocr_text")
    val ocrText: String?,
    
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Date,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Date
)
