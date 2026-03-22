package com.ynab.receipts.domain.model

import java.time.Instant

data class AuthState(
    val accessTokenRef: String,
    val refreshTokenRef: String,
    val expiresAt: Instant,
    val selectedBudgetId: String?
)
