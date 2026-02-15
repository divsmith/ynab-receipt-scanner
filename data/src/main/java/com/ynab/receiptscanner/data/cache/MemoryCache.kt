package com.ynab.receiptscanner.data.cache

import android.graphics.Bitmap
import android.util.LruCache
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LRU memory cache for bitmaps
 * Caches preprocessed images and thumbnails with memory awareness
 */
@Singleton
class MemoryCache @Inject constructor() {
    
    companion object {
        // Use 1/8th of available memory for cache
        private val MAX_MEMORY = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        private val CACHE_SIZE = MAX_MEMORY / 8
        private const val TAG = "MemoryCache"
    }
    
    private val bitmapCache = object : LruCache<String, Bitmap>(CACHE_SIZE) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            // Size in kilobytes
            return bitmap.byteCount / 1024
        }
        
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: Bitmap,
            newValue: Bitmap?
        ) {
            // Don't recycle - bitmap might still be in use
            android.util.Log.d(TAG, "Evicted: $key (evicted=$evicted)")
        }
    }
    
    /**
     * Get bitmap from cache
     */
    fun get(key: String): Bitmap? {
        return bitmapCache.get(key)
    }
    
    /**
     * Put bitmap in cache
     */
    fun put(key: String, bitmap: Bitmap) {
        if (get(key) == null) {
            bitmapCache.put(key, bitmap)
        }
    }
    
    /**
     * Remove bitmap from cache
     */
    fun remove(key: String) {
        bitmapCache.remove(key)
    }
    
    /**
     * Clear all cached bitmaps
     */
    fun clear() {
        bitmapCache.evictAll()
    }
    
    /**
     * Get cache statistics
     */
    fun getStats(): CacheStats {
        return CacheStats(
            size = bitmapCache.size(),
            maxSize = bitmapCache.maxSize(),
            hitCount = bitmapCache.hitCount(),
            missCount = bitmapCache.missCount(),
            evictionCount = bitmapCache.evictionCount()
        )
    }
    
    /**
     * Trim cache to specified size (percentage)
     */
    fun trimToSize(percentage: Int) {
        val targetSize = (CACHE_SIZE * percentage) / 100
        bitmapCache.trimToSize(targetSize)
    }
    
    /**
     * Generate cache key for receipt image
     */
    fun generateReceiptKey(receiptId: String): String {
        return "receipt_$receiptId"
    }
    
    /**
     * Generate cache key for preprocessed image
     */
    fun generatePreprocessedKey(receiptId: String): String {
        return "preprocessed_$receiptId"
    }
    
    /**
     * Generate cache key for thumbnail
     */
    fun generateThumbnailKey(receiptId: String): String {
        return "thumbnail_$receiptId"
    }
    
    /**
     * Handle low memory situation
     */
    fun onLowMemory() {
        android.util.Log.w(TAG, "Low memory - clearing cache")
        clear()
    }
    
    /**
     * Handle memory trim
     */
    fun onTrimMemory(level: Int) {
        when (level) {
            // Running low on memory
            android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                android.util.Log.w(TAG, "Trim memory level $level - trimming cache to 50%")
                trimToSize(50)
            }
            // Going into background
            android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                android.util.Log.d(TAG, "UI hidden - trimming cache to 75%")
                trimToSize(75)
            }
            // Critical memory situations
            android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                android.util.Log.w(TAG, "Trim memory complete - clearing cache")
                clear()
            }
        }
    }
    
    data class CacheStats(
        val size: Int,
        val maxSize: Int,
        val hitCount: Int,
        val missCount: Int,
        val evictionCount: Int
    ) {
        val hitRate: Float
            get() = if (hitCount + missCount > 0) {
                hitCount.toFloat() / (hitCount + missCount)
            } else {
                0f
            }
        
        override fun toString(): String {
            return "CacheStats(size=$size, maxSize=$maxSize, hitRate=${String.format("%.2f", hitRate * 100)}%, " +
                    "hits=$hitCount, misses=$missCount, evictions=$evictionCount)"
        }
    }
}
