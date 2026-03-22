package com.ynab.receipts.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.util.UUID

@Dao
interface QueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: QueuedTransactionEntity)

    @Query("SELECT * FROM queued_transactions WHERE remoteTransactionId IS NULL ORDER BY createdAt ASC LIMIT :limit")
    suspend fun listPending(limit: Int): List<QueuedTransactionEntity>

    @Query("UPDATE queued_transactions SET attemptCount = attemptCount + 1, lastError = :error, remoteTransactionId = :remoteTransactionId WHERE id = :queueId")
    suspend fun updateResult(queueId: UUID, remoteTransactionId: String?, error: String?)
}
