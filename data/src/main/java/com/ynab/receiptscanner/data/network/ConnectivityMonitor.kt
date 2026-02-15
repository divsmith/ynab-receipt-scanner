package com.ynab.receiptscanner.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import com.ynab.receiptscanner.domain.model.ConnectivityStatus
import com.ynab.receiptscanner.domain.repository.ConnectivityRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors network connectivity changes using ConnectivityManager
 * Provides real-time updates via Flow
 */
@Singleton
class ConnectivityMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) : ConnectivityRepository {
    
    companion object {
        private const val TAG = "ConnectivityMonitor"
    }
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    /**
     * Observe connectivity changes as a Flow
     * Uses NetworkCallback for real-time updates
     */
    override fun observeConnectivity(): Flow<ConnectivityStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            private val networks = mutableSetOf<Network>()
            
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network available: $network")
                networks.add(network)
                trySend(getCurrentConnectivityInternal())
            }
            
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                Log.d(TAG, "Network capabilities changed: $network")
                trySend(getCurrentConnectivityInternal())
            }
            
            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost: $network")
                networks.remove(network)
                trySend(getCurrentConnectivityInternal())
            }
        }
        
        // Register callback
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()
        
        connectivityManager.registerNetworkCallback(request, callback)
        
        // Emit initial state
        trySend(getCurrentConnectivityInternal())
        
        // Unregister callback when flow is cancelled
        awaitClose {
            Log.d(TAG, "Unregistering network callback")
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
    
    /**
     * Get current connectivity status synchronously
     */
    override suspend fun getCurrentConnectivity(): ConnectivityStatus {
        return getCurrentConnectivityInternal()
    }
    
    private fun getCurrentConnectivityInternal(): ConnectivityStatus {
        val network = connectivityManager.activeNetwork
        if (network == null) {
            Log.d(TAG, "No active network")
            return ConnectivityStatus.Disconnected
        }
        
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        if (capabilities == null) {
            Log.d(TAG, "No network capabilities")
            return ConnectivityStatus.Disconnected
        }
        
        // Check if network has internet capability
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val hasValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        
        if (!hasInternet || !hasValidated) {
            Log.d(TAG, "Network not validated or no internet")
            return ConnectivityStatus.Disconnected
        }
        
        // Check if network is metered (cellular data)
        val isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        
        val transportType = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Unknown"
        }
        
        Log.d(TAG, "Connected via $transportType, metered: $isMetered")
        return ConnectivityStatus.Connected(isMetered = isMetered)
    }
}
