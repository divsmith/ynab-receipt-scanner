package com.ynab.receiptscanner.data.security

/**
 * Interface for secure key-value storage operations
 * Provides encrypted storage for sensitive data
 */
interface SecureStorage {
    
    /**
     * Store a string value securely
     * @param key Storage key
     * @param value Value to store
     */
    suspend fun putString(key: String, value: String)
    
    /**
     * Retrieve a string value
     * @param key Storage key
     * @param defaultValue Default value if key doesn't exist
     * @return Stored value or default
     */
    suspend fun getString(key: String, defaultValue: String? = null): String?
    
    /**
     * Store an integer value securely
     * @param key Storage key
     * @param value Value to store
     */
    suspend fun putInt(key: String, value: Int)
    
    /**
     * Retrieve an integer value
     * @param key Storage key
     * @param defaultValue Default value if key doesn't exist
     * @return Stored value or default
     */
    suspend fun getInt(key: String, defaultValue: Int = 0): Int
    
    /**
     * Store a long value securely
     * @param key Storage key
     * @param value Value to store
     */
    suspend fun putLong(key: String, value: Long)
    
    /**
     * Retrieve a long value
     * @param key Storage key
     * @param defaultValue Default value if key doesn't exist
     * @return Stored value or default
     */
    suspend fun getLong(key: String, defaultValue: Long = 0L): Long
    
    /**
     * Store a boolean value securely
     * @param key Storage key
     * @param value Value to store
     */
    suspend fun putBoolean(key: String, value: Boolean)
    
    /**
     * Retrieve a boolean value
     * @param key Storage key
     * @param defaultValue Default value if key doesn't exist
     * @return Stored value or default
     */
    suspend fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    
    /**
     * Check if a key exists
     * @param key Storage key
     * @return True if key exists
     */
    suspend fun contains(key: String): Boolean
    
    /**
     * Remove a key-value pair
     * @param key Storage key to remove
     */
    suspend fun remove(key: String)
    
    /**
     * Clear all stored data
     */
    suspend fun clear()
}
