package com.ynab.receiptscanner.data.remote.interceptor

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cache interceptor for offline support and performance
 * Caches GET requests and provides offline fallback
 */
@Singleton
class CacheInterceptor @Inject constructor(
    @ApplicationContext private val context: Context
) : Interceptor {
    
    companion object {
        // Cache durations
        private val CACHE_CONTROL_ONLINE = CacheControl.Builder()
            .maxAge(5, TimeUnit.MINUTES)
            .build()
        
        private val CACHE_CONTROL_OFFLINE = CacheControl.Builder()
            .maxStale(7, TimeUnit.DAYS)
            .onlyIfCached()
            .build()
        
        // Cacheable endpoints (budgets, accounts, categories rarely change)
        private val CACHEABLE_PATHS = listOf(
            "/budgets",
            "/accounts",
            "/categories",
            "/payees"
        )
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        
        // Only cache GET requests
        if (request.method != "GET") {
            return chain.proceed(request)
        }
        
        // Only cache specific endpoints
        val path = request.url.encodedPath
        val isCacheable = CACHEABLE_PATHS.any { path.contains(it) }
        
        if (!isCacheable) {
            return chain.proceed(request)
        }
        
        // Check network availability
        val isOnline = isNetworkAvailable()
        
        // Modify request cache control based on network status
        val cacheControl = if (isOnline) {
            CACHE_CONTROL_ONLINE
        } else {
            CACHE_CONTROL_OFFLINE
        }
        
        request = request.newBuilder()
            .cacheControl(cacheControl)
            .build()
        
        val response = try {
            chain.proceed(request)
        } catch (e: Exception) {
            // If request fails and we're offline, try to serve from cache
            if (!isOnline) {
                request = request.newBuilder()
                    .cacheControl(CACHE_CONTROL_OFFLINE)
                    .build()
                chain.proceed(request)
            } else {
                throw e
            }
        }
        
        // Add cache headers to response if successful
        return if (response.isSuccessful && isOnline) {
            response.newBuilder()
                .header("Cache-Control", "public, max-age=${5 * 60}")
                .removeHeader("Pragma")
                .build()
        } else {
            response
        }
    }
    
    /**
     * Check if network is available
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }
}
