package com.ynab.receiptscanner.domain.usecase

import com.ynab.receiptscanner.domain.model.ConnectivityStatus
import com.ynab.receiptscanner.domain.repository.ConnectivityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for observing network connectivity changes
 * Provides a stream of connectivity status updates
 */
class ObserveConnectivityUseCase @Inject constructor(
    private val connectivityRepository: ConnectivityRepository
) {
    /**
     * Observe connectivity changes
     */
    operator fun invoke(): Flow<ConnectivityStatus> {
        return connectivityRepository.observeConnectivity()
    }
    
    /**
     * Get current connectivity status
     */
    suspend fun getCurrentStatus(): ConnectivityStatus {
        return connectivityRepository.getCurrentConnectivity()
    }
}
