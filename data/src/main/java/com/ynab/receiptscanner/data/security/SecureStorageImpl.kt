package com.ynab.receiptscanner.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SecureStorage using EncryptedSharedPreferences
 * Provides AES256-GCM encrypted storage for sensitive key-value pairs
 */
@Singleton
class SecureStorageImpl @Inject constructor(
    private val context: Context
) : SecureStorage {
    
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val sharedPreferences: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    override suspend fun putString(key: String, value: String) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putString(key, value).apply()
    }
    
    override suspend fun getString(key: String, defaultValue: String?): String? = withContext(Dispatchers.IO) {
        sharedPreferences.getString(key, defaultValue)
    }
    
    override suspend fun putInt(key: String, value: Int) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putInt(key, value).apply()
    }
    
    override suspend fun getInt(key: String, defaultValue: Int): Int = withContext(Dispatchers.IO) {
        sharedPreferences.getInt(key, defaultValue)
    }
    
    override suspend fun putLong(key: String, value: Long) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putLong(key, value).apply()
    }
    
    override suspend fun getLong(key: String, defaultValue: Long): Long = withContext(Dispatchers.IO) {
        sharedPreferences.getLong(key, defaultValue)
    }
    
    override suspend fun putBoolean(key: String, value: Boolean) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }
    
    override suspend fun getBoolean(key: String, defaultValue: Boolean): Boolean = withContext(Dispatchers.IO) {
        sharedPreferences.getBoolean(key, defaultValue)
    }
    
    override suspend fun contains(key: String): Boolean = withContext(Dispatchers.IO) {
        sharedPreferences.contains(key)
    }
    
    override suspend fun remove(key: String) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().remove(key).apply()
    }
    
    override suspend fun clear() = withContext(Dispatchers.IO) {
        sharedPreferences.edit().clear().apply()
    }
    
    companion object {
        private const val PREFS_FILE_NAME = "ynab_secure_prefs"
    }
}
