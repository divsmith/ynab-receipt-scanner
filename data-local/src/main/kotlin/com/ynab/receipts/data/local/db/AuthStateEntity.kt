package com.ynab.receipts.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "auth_state")
data class AuthStateEntity(
    @PrimaryKey val id: String = "default",
    val accessTokenRef: String,
    val refreshTokenRef: String,
    val expiresAt: Instant,
    val selectedBudgetId: String?
)
