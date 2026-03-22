package com.ynab.receipts.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.time.LocalDate
import java.util.UUID

@Dao
interface ReceiptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ReceiptEntity)

    @Query("SELECT * FROM receipt_scans WHERE id = :id LIMIT 1")
    suspend fun findById(id: UUID): ReceiptEntity?

    @Query("UPDATE receipt_scans SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: UUID, status: String)

    @Query("SELECT * FROM receipt_scans WHERE status = 'Synced' AND date BETWEEN :fromInclusive AND :toInclusive")
    suspend fun findSyncedInDateWindow(fromInclusive: LocalDate, toInclusive: LocalDate): List<ReceiptEntity>
}
