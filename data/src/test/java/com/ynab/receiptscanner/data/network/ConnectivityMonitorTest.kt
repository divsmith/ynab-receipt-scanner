package com.ynab.receiptscanner.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.test.core.app.ApplicationProvider
import com.ynab.receiptscanner.domain.model.ConnectivityStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetworkCapabilities

/**
 * Unit tests for ConnectivityMonitor
 */
@RunWith(RobolectricTestRunner::class)
class ConnectivityMonitorTest {
    
    private lateinit var context: Context
    private lateinit var connectivityMonitor: ConnectivityMonitor
    
    @Mock
    private lateinit var connectivityManager: ConnectivityManager
    
    @Mock
    private lateinit var network: Network
    
    @Mock
    private lateinit var networkCapabilities: NetworkCapabilities
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        connectivityMonitor = ConnectivityMonitor(context)
    }
    
    @Test
    fun `test connected to WiFi`() = runBlocking {
        // Given: WiFi network with internet and validation
        whenever(connectivityManager.activeNetwork).thenReturn(network)
        whenever(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities)
        whenever(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)).thenReturn(true)
        whenever(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)).thenReturn(true)
        whenever(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)).thenReturn(true)
        whenever(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(true)
        whenever(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).thenReturn(false)
        
        // When: Get current connectivity
        val status = connectivityMonitor.getCurrentConnectivity()
        
        // Then: Status is Connected and not metered
        assertTrue(status is ConnectivityStatus.Connected)
        assertFalse((status as ConnectivityStatus.Connected).isMetered)
    }
    
    @Test
    fun `test connected to cellular`() = runBlocking {
        // Given: Cellular network (metered)
        whenever(connectivityManager.activeNetwork).thenReturn(network)
        whenever(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities)
        whenever(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)).thenReturn(true)
        whenever(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)).thenReturn(true)
        whenever(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)).thenReturn(false)
        whenever(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(false)
        whenever(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).thenReturn(true)
        
        // When: Get current connectivity
        val status = connectivityMonitor.getCurrentConnectivity()
        
        // Then: Status is Connected and metered
        assertTrue(status is ConnectivityStatus.Connected)
        assertTrue((status as ConnectivityStatus.Connected).isMetered)
    }
    
    @Test
    fun `test no network`() = runBlocking {
        // Given: No active network
        whenever(connectivityManager.activeNetwork).thenReturn(null)
        
        // When: Get current connectivity
        val status = connectivityMonitor.getCurrentConnectivity()
        
        // Then: Status is Disconnected
        assertTrue(status is ConnectivityStatus.Disconnected)
    }
    
    @Test
    fun `test network without internet`() = runBlocking {
        // Given: Network without internet capability
        whenever(connectivityManager.activeNetwork).thenReturn(network)
        whenever(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities)
        whenever(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)).thenReturn(false)
        
        // When: Get current connectivity
        val status = connectivityMonitor.getCurrentConnectivity()
        
        // Then: Status is Disconnected
        assertTrue(status is ConnectivityStatus.Disconnected)
    }
    
    @Test
    fun `test network not validated`() = runBlocking {
        // Given: Network with internet but not validated
        whenever(connectivityManager.activeNetwork).thenReturn(network)
        whenever(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities)
        whenever(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)).thenReturn(true)
        whenever(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)).thenReturn(false)
        
        // When: Get current connectivity
        val status = connectivityMonitor.getCurrentConnectivity()
        
        // Then: Status is Disconnected
        assertTrue(status is ConnectivityStatus.Disconnected)
    }
    
    @Test
    fun `test connectivity status helpers`() {
        // Test Connected status helpers
        val connected = ConnectivityStatus.Connected(isMetered = false)
        assertTrue(connected.isConnected())
        assertFalse(connected.isMetered())
        
        val connectedMetered = ConnectivityStatus.Connected(isMetered = true)
        assertTrue(connectedMetered.isConnected())
        assertTrue(connectedMetered.isMetered())
        
        // Test Disconnected status helpers
        val disconnected = ConnectivityStatus.Disconnected
        assertFalse(disconnected.isConnected())
        assertFalse(disconnected.isMetered())
        
        // Test Unknown status helpers
        val unknown = ConnectivityStatus.Unknown
        assertFalse(unknown.isConnected())
        assertFalse(unknown.isMetered())
    }
}
