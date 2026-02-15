package com.ynab.receiptscanner.domain.repository

import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.domain.model.Receipt
import com.ynab.receiptscanner.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Repository interface for receipt operations
 * Defines contract for receipt data access
 */
interface ReceiptRepository {
    
    /**
     * Get all receipts
     * @return Flow of list of receipts
     */
    fun getAllReceipts(): Flow<List<Receipt>>
    
    /**
     * Get a receipt by ID
     * @param receiptId Unique identifier
     * @return Flow of receipt or null
     */
    fun getReceiptById(receiptId: String): Flow<Receipt?>
    
    /**
     * Get receipts by sync status
     * @param status Sync status to filter by
     * @return Flow of list of receipts
     */
    fun getReceiptsByStatus(status: SyncStatus): Flow<List<Receipt>>
    
    /**
     * Get receipts within a date range
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Flow of list of receipts
     */
    fun getReceiptsByDateRange(startDate: Date, endDate: Date): Flow<List<Receipt>>
    
    /**
     * Search receipts by payee name
     * @param query Search query
     * @return Flow of list of receipts
     */
    fun searchReceiptsByPayee(query: String): Flow<List<Receipt>>
    
    /**
     * Get count of receipts by status
     * @param status Sync status
     * @return Flow of count
     */
    fun getReceiptCountByStatus(status: SyncStatus): Flow<Int>
    
    /**
     * Get receipts pending sync
     * @return Flow of list of receipts
     */
    fun getPendingSyncReceipts(): Flow<List<Receipt>>
    
    /**
     * Insert or update a receipt
     * @param receipt Receipt to save
     * @return Result with saved receipt
     */
    suspend fun saveReceipt(receipt: Receipt): Result<Receipt>
    
    /**
     * Delete a receipt
     * @param receipt Receipt to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteReceipt(receipt: Receipt): Result<Unit>
    
    /**
     * Delete a receipt by ID
     * @param receiptId Unique identifier
     * @return Result indicating success or failure
     */
    suspend fun deleteReceiptById(receiptId: String): Result<Unit>
    
    /**
     * Update sync status for a receipt
     * @param receiptId Receipt ID
     * @param status New sync status
     * @return Result indicating success or failure
     */
    suspend fun updateSyncStatus(receiptId: String, status: SyncStatus): Result<Unit>
    
    /**
     * Delete all receipts
     * @return Result indicating success or failure
     */
    suspend fun deleteAllReceipts(): Result<Unit>
}
