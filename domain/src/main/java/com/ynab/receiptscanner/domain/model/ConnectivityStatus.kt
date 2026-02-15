package com.ynab.receiptscanner.domain.model

/**
 * Represents the network connectivity status of the device
 */
sealed class ConnectivityStatus {
    /**
     * Device is connected to the internet
     */
    data class Connected(val isMetered: Boolean) : ConnectivityStatus()
    
    /**
     * Device is not connected to the internet
     */
    object Disconnected : ConnectivityStatus()
    
    /**
     * Connectivity status is unknown (e.g., permission denied)
     */
    object Unknown : ConnectivityStatus()
    
    /**
     * Check if device is connected to any network
     */
    fun isConnected(): Boolean = this is Connected
    
    /**
     * Check if device is connected to a metered network (cellular data)
     */
    fun isMetered(): Boolean = this is Connected && this.isMetered
}
