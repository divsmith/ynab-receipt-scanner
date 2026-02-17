package com.receiptscanner.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.receiptscanner.domain.model.YnabToken
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import timber.log.Timber
import java.security.KeyStore
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages secure storage of YNAB OAuth tokens and encryption/decryption
 * of receipt images using Android Keystore.
 */
@Singleton
class KeystoreManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val PREFS_FILE_NAME = "ynab_secure_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_IN = "expires_in"
        private const val KEY_TOKEN_TYPE = "token_type"
        private const val KEY_CREATED_AT = "created_at"
        
        private const val KEYSTORE_ALIAS = "ReceiptImageKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val IV_SIZE = 12
    }

    private val moshi = Moshi.Builder().build()
    
    private val sharedPreferences: SharedPreferences by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            
            EncryptedSharedPreferences.create(
                PREFS_FILE_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Timber.e(e, "Error creating encrypted shared preferences, falling back to regular")
            // Fallback to regular SharedPreferences for testing/dev
            context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
        }
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
    }

    /**
     * Gets or creates the encryption key
     */
    private fun getOrCreateKey(): SecretKey {
        // Check if key exists
        if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
            return keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
        }

        // Create new key
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts data using AES-GCM
     * @param data Data to encrypt
     * @return Encrypted data with IV prepended, or null if encryption fails
     */
    fun encrypt(data: ByteArray): ByteArray? {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            
            val iv = cipher.iv
            val encryptedData = cipher.doFinal(data)
            
            // Prepend IV to encrypted data
            iv + encryptedData
        } catch (e: Exception) {
            Timber.e(e, "Error encrypting data")
            null
        }
    }

    /**
     * Decrypts data using AES-GCM
     * @param encryptedData Encrypted data with IV prepended
     * @return Decrypted data, or null if decryption fails
     */
    fun decrypt(encryptedData: ByteArray): ByteArray? {
        return try {
            if (encryptedData.size < IV_SIZE) {
                Timber.e("Encrypted data too small to contain IV")
                return null
            }
            
            // Extract IV and encrypted data
            val iv = encryptedData.copyOfRange(0, IV_SIZE)
            val actualEncryptedData = encryptedData.copyOfRange(IV_SIZE, encryptedData.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
            
            cipher.doFinal(actualEncryptedData)
        } catch (e: Exception) {
            Timber.e(e, "Error decrypting data")
            null
        }
    }

    /**
     * Saves a YNAB token securely
     * @param token The token to save
     * @return true if saved successfully, false otherwise
     */
    fun saveToken(token: YnabToken): Boolean {
        return try {
            sharedPreferences.edit().apply {
                putString(KEY_ACCESS_TOKEN, token.accessToken)
                putString(KEY_REFRESH_TOKEN, token.refreshToken)
                putLong(KEY_EXPIRES_IN, token.expiresIn)
                putString(KEY_TOKEN_TYPE, token.tokenType)
                putLong(KEY_CREATED_AT, token.createdAt.epochSecond)
                apply()
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "Error saving token")
            false
        }
    }

    /**
     * Retrieves the stored YNAB token
     * @return The stored token or null if none exists
     */
    fun getToken(): YnabToken? {
        return try {
            val accessToken = sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
            val refreshToken = sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
            val expiresIn = sharedPreferences.getLong(KEY_EXPIRES_IN, -1L)
            val tokenType = sharedPreferences.getString(KEY_TOKEN_TYPE, null)
            val createdAtEpoch = sharedPreferences.getLong(KEY_CREATED_AT, -1L)

            if (accessToken != null && expiresIn != -1L && tokenType != null && createdAtEpoch != -1L) {
                YnabToken(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresIn = expiresIn,
                    tokenType = tokenType,
                    createdAt = Instant.ofEpochSecond(createdAtEpoch)
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error retrieving token")
            null
        }
    }

    /**
     * Deletes the stored token
     */
    fun deleteToken() {
        try {
            sharedPreferences.edit().clear().apply()
        } catch (e: Exception) {
            Timber.e(e, "Error deleting token")
        }
    }

    /**
     * Checks if a token exists in storage
     * @return true if a token exists, false otherwise
     */
    fun hasToken(): Boolean {
        return sharedPreferences.contains(KEY_ACCESS_TOKEN)
    }
}
