package com.ynab.receiptscanner.data.repository

import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.data.local.dao.ReceiptDao
import com.ynab.receiptscanner.data.mapper.toDomain
import com.ynab.receiptscanner.data.mapper.toEntity
import com.ynab.receiptscanner.data.storage.ImageStorageManager
import com.ynab.receiptscanner.domain.model.Receipt
import com.ynab.receiptscanner.domain.model.SyncStatus
import com.ynab.receiptscanner.domain.repository.ReceiptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ReceiptRepository
 * Handles receipt data operations using Room database
 */
@Singleton
class ReceiptRepositoryImpl @Inject constructor(
    private val receiptDao: ReceiptDao,
    private val imageStorageManager: ImageStorageManager
) : ReceiptRepository {
    
    override fun getAllReceipts(): Flow<List<Receipt>> {
        return receiptDao.getAllReceipts().map { entities ->
            entities.toDomain()
        }
    }
    
    override fun getReceiptById(receiptId: String): Flow<Receipt?> {
        return receiptDao.getReceiptById(receiptId).map { entity ->
            entity?.toDomain()
        }
    }
    
    override fun getReceiptsByStatus(status: SyncStatus): Flow<List<Receipt>> {
        return receiptDao.getReceiptsByStatus(status).map { entities ->
            entities.toDomain()
        }
    }
    
    override fun getReceiptsByDateRange(startDate: Date, endDate: Date): Flow<List<Receipt>> {
        return receiptDao.getReceiptsByDateRange(startDate, endDate).map { entities ->
            entities.toDomain()
        }
    }
    
    override fun searchReceiptsByPayee(query: String): Flow<List<Receipt>> {
        return receiptDao.searchReceiptsByPayee(query).map { entities ->
            entities.toDomain()
        }
    }
    
    override fun getReceiptCountByStatus(status: SyncStatus): Flow<Int> {
        return receiptDao.getReceiptCountByStatus(status)
    }
    
    override fun getPendingSyncReceipts(): Flow<List<Receipt>> {
        return receiptDao.getPendingSyncReceipts().map { entities ->
            entities.toDomain()
        }
    }
    
    override suspend fun saveReceipt(receipt: Receipt): Result<Receipt> {
        return try {
            val updatedReceipt = receipt.copy(updatedAt = Date())
            receiptDao.insertReceipt(updatedReceipt.toEntity())
            Result.Success(updatedReceipt)
        } catch (e: Exception) {
            Result.Error(e, "Failed to save receipt: ${e.message}")
        }
    }
    
    override suspend fun deleteReceipt(receipt: Receipt): Result<Unit> {
        return try {
            // Delete associated image if exists
            receipt.imagePath?.let { imagePath ->
                imageStorageManager.deleteImage(imagePath)
            }
            
            receiptDao.deleteReceipt(receipt.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to delete receipt: ${e.message}")
        }
    }
    
    override suspend fun deleteReceiptById(receiptId: String): Result<Unit> {
        return try {
            receiptDao.deleteReceiptById(receiptId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to delete receipt: ${e.message}")
        }
    }
    
    override suspend fun updateSyncStatus(receiptId: String, status: SyncStatus): Result<Unit> {
        return try {
            receiptDao.updateSyncStatus(receiptId, status, Date())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to update sync status: ${e.message}")
        }
    }
    
    override suspend fun deleteAllReceipts(): Result<Unit> {
        return try {
            // Delete all images
            imageStorageManager.deleteAllImages()
            
            // Delete all receipts from database
            receiptDao.deleteAllReceipts()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to delete all receipts: ${e.message}")
        }
    }
}
