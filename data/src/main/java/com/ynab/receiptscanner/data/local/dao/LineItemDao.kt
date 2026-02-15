package com.ynab.receiptscanner.data.local.dao

import androidx.room.*
import com.ynab.receiptscanner.data.local.entity.LineItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for LineItem operations
 */
@Dao
interface LineItemDao {
    
    /**
     * Get all line items for a receipt
     */
    @Query("SELECT * FROM line_items WHERE receipt_id = :receiptId ORDER BY description ASC")
    fun getLineItemsForReceipt(receiptId: String): Flow<List<LineItemEntity>>
    
    /**
     * Get a line item by ID
     */
    @Query("SELECT * FROM line_items WHERE id = :lineItemId")
    fun getLineItemById(lineItemId: String): Flow<LineItemEntity?>
    
    /**
     * Get count of line items for a receipt
     */
    @Query("SELECT COUNT(*) FROM line_items WHERE receipt_id = :receiptId")
    fun getLineItemCount(receiptId: String): Flow<Int>
    
    /**
     * Get total amount of line items for a receipt
     */
    @Query("SELECT SUM(total_price) FROM line_items WHERE receipt_id = :receiptId")
    fun getTotalForReceipt(receiptId: String): Flow<Double?>
    
    /**
     * Insert a line item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineItem(lineItem: LineItemEntity)
    
    /**
     * Insert multiple line items
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineItems(lineItems: List<LineItemEntity>)
    
    /**
     * Update a line item
     */
    @Update
    suspend fun updateLineItem(lineItem: LineItemEntity)
    
    /**
     * Delete a line item
     */
    @Delete
    suspend fun deleteLineItem(lineItem: LineItemEntity)
    
    /**
     * Delete a line item by ID
     */
    @Query("DELETE FROM line_items WHERE id = :lineItemId")
    suspend fun deleteLineItemById(lineItemId: String)
    
    /**
     * Delete all line items for a receipt
     */
    @Query("DELETE FROM line_items WHERE receipt_id = :receiptId")
    suspend fun deleteLineItemsForReceipt(receiptId: String)
    
    /**
     * Delete all line items
     */
    @Query("DELETE FROM line_items")
    suspend fun deleteAllLineItems()
}
