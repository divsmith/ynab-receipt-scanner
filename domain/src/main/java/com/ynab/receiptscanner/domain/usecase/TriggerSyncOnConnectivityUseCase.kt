package com.ynab.receiptscanner.domain.usecase

import android.util.Log
import com.ynab.receiptscanner.domain.model.ConnectivityStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case for triggering sync when connectivity is restored
 * Monitors connectivity changes and signals when sync should occur
 */
class TriggerSyncOnConnectivityUseCase @Inject constructor(
    private val observeConnectivityUseCase: ObserveConnectivityUseCase
) {
    companion object {
        private const val TAG = "TriggerSyncOnConnectivity"
    }
    
    /**
     * Returns a Flow that emits true when device comes online and ready for sync
     * Only emits when transitioning from disconnected to connected
     */
    operator fun invoke(allowMeteredNetworks: Boolean = true): Flow<Boolean> {
        return observeConnectivityUseCase()
            .distinctUntilChanged()
            .map { status ->
                when (status) {
                    is ConnectivityStatus.Connected -> {
                        // Check if we should sync on metered networks
                        if (!allowMeteredNetworks && status.isMetered) {
                            Log.d(TAG, "Connected to metered network, skipping auto-sync")
                            false
                        } else {
                            Log.d(TAG, "Connected to network, ready for sync")
                            true
                        }
                    }
                    is ConnectivityStatus.Disconnected -> {
                        Log.d(TAG, "Disconnected from network")
                        false
                    }
                    is ConnectivityStatus.Unknown -> false
                }
            }
            .filter { it } // Only emit true values
    }
}
