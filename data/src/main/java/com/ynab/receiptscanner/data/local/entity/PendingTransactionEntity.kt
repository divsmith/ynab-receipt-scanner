package com.ynab.receiptscanner.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Room entity representing a pending YNAB transaction in the offline queue
 * Used for offline-first architecture to queue transactions when network is unavailable
 */
@Entity(
    tableName = "pending_transactions",
    indices = [
        Index(value = ["status"]),
        Index(value = ["created_at"]),
        Index(value = ["retry_count"])
    ]
)
data class PendingTransactionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "receipt_id")
    val receiptId: String?,
    
    @ColumnInfo(name = "account_id")
    val accountId: String,
    
    @ColumnInfo(name = "category_id")
    val categoryId: String?,
    
    @ColumnInfo(name = "date")
    val date: Date,
    
    @ColumnInfo(name = "amount")
    val amount: Long,
    
    @ColumnInfo(name = "payee")
    val payee: String?,
    
    @ColumnInfo(name = "memo")
    val memo: String?,
    
    @ColumnInfo(name = "cleared")
    val cleared: String,
    
    @ColumnInfo(name = "approved")
    val approved: Boolean,
    
    @ColumnInfo(name = "import_id")
    val importId: String?,
    
    @ColumnInfo(name = "status")
    val status: String, // PENDING, PROCESSING, COMPLETED, FAILED
    
    @ColumnInfo(name = "retry_count")
    val retryCount: Int,
    
    @ColumnInfo(name = "last_error")
    val lastError: String?,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Date,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Date
)
