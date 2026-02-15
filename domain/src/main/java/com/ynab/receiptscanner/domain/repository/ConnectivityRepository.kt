package com.ynab.receiptscanner.domain.repository

import com.ynab.receiptscanner.domain.model.ConnectivityStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for network connectivity monitoring
 */
interface ConnectivityRepository {
    /**
     * Observe connectivity changes as a Flow
     */
    fun observeConnectivity(): Flow<ConnectivityStatus>
    
    /**
     * Get current connectivity status
     */
    suspend fun getCurrentConnectivity(): ConnectivityStatus
}
