package com.ynab.receiptscanner.data.local.dao

import androidx.room.*
import com.ynab.receiptscanner.data.local.entity.ReceiptEntity
import com.ynab.receiptscanner.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Data Access Object for Receipt operations
 * Provides reactive queries using Flow and suspend functions for write operations
 */
@Dao
interface ReceiptDao {
    
    /**
     * Get all receipts ordered by creation date (newest first)
     */
    @Query("SELECT * FROM receipts ORDER BY created_at DESC")
    fun getAllReceipts(): Flow<List<ReceiptEntity>>
    
    /**
     * Get a receipt by ID
     */
    @Query("SELECT * FROM receipts WHERE id = :receiptId")
    fun getReceiptById(receiptId: String): Flow<ReceiptEntity?>
    
    /**
     * Get receipts by sync status
     */
    @Query("SELECT * FROM receipts WHERE sync_status = :status ORDER BY created_at DESC")
    fun getReceiptsByStatus(status: SyncStatus): Flow<List<ReceiptEntity>>
    
    /**
     * Get receipts within a date range
     */
    @Query("SELECT * FROM receipts WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getReceiptsByDateRange(startDate: Date, endDate: Date): Flow<List<ReceiptEntity>>
    
    /**
     * Search receipts by payee name
     */
    @Query("SELECT * FROM receipts WHERE payee LIKE '%' || :query || '%' ORDER BY created_at DESC")
    fun searchReceiptsByPayee(query: String): Flow<List<ReceiptEntity>>
    
    /**
     * Get count of receipts by status
     */
    @Query("SELECT COUNT(*) FROM receipts WHERE sync_status = :status")
    fun getReceiptCountByStatus(status: SyncStatus): Flow<Int>
    
    /**
     * Get receipts pending sync
     */
    @Query("SELECT * FROM receipts WHERE sync_status = 'PENDING' OR sync_status = 'FAILED' ORDER BY created_at ASC")
    fun getPendingSyncReceipts(): Flow<List<ReceiptEntity>>
    
    /**
     * Insert a receipt
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: ReceiptEntity)
    
    /**
     * Insert multiple receipts
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipts(receipts: List<ReceiptEntity>)
    
    /**
     * Update a receipt
     */
    @Update
    suspend fun updateReceipt(receipt: ReceiptEntity)
    
    /**
     * Delete a receipt
     */
    @Delete
    suspend fun deleteReceipt(receipt: ReceiptEntity)
    
    /**
     * Delete a receipt by ID
     */
    @Query("DELETE FROM receipts WHERE id = :receiptId")
    suspend fun deleteReceiptById(receiptId: String)
    
    /**
     * Update sync status for a receipt
     */
    @Query("UPDATE receipts SET sync_status = :status, updated_at = :updatedAt WHERE id = :receiptId")
    suspend fun updateSyncStatus(receiptId: String, status: SyncStatus, updatedAt: Date)
    
    /**
     * Delete all receipts
     */
    @Query("DELETE FROM receipts")
    suspend fun deleteAllReceipts()
}
