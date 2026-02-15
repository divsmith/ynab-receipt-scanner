package com.ynab.receiptscanner.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for Android Keystore operations
 * Handles generation, retrieval, and management of encryption keys
 */
@Singleton
class KeystoreManager @Inject constructor() {
    
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }
    
    /**
     * Generate a new AES key in the Android Keystore
     * @param alias Key alias for storage and retrieval
     * @return Generated SecretKey
     */
    fun generateKey(alias: String): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
            .setUserAuthenticationRequired(false)
            .setRandomizedEncryptionRequired(true)
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }
    
    /**
     * Retrieve an existing key from the Android Keystore
     * @param alias Key alias
     * @return SecretKey if exists, null otherwise
     */
    fun getKey(alias: String): SecretKey? {
        return try {
            val entry = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
            entry?.secretKey
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get or create a key with the given alias
     * @param alias Key alias
     * @return SecretKey (existing or newly generated)
     */
    fun getOrCreateKey(alias: String): SecretKey {
        return getKey(alias) ?: generateKey(alias)
    }
    
    /**
     * Check if a key exists in the Keystore
     * @param alias Key alias
     * @return True if key exists
     */
    fun hasKey(alias: String): Boolean {
        return keyStore.containsAlias(alias)
    }
    
    /**
     * Delete a key from the Keystore
     * @param alias Key alias to delete
     */
    fun deleteKey(alias: String) {
        if (hasKey(alias)) {
            keyStore.deleteEntry(alias)
        }
    }
    
    /**
     * Delete all keys managed by this application
     * Use with caution - this will make encrypted data unrecoverable
     */
    fun deleteAllKeys() {
        val aliases = keyStore.aliases()
        while (aliases.hasMoreElements()) {
            val alias = aliases.nextElement()
            keyStore.deleteEntry(alias)
        }
    }
    
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_SIZE = 256
        
        /**
         * Default key alias for database encryption
         */
        const val DATABASE_KEY_ALIAS = "ynab_database_key"
        
        /**
         * Default key alias for file encryption
         */
        const val FILE_ENCRYPTION_KEY_ALIAS = "ynab_file_encryption_key"
        
        /**
         * Default key alias for image encryption
         */
        const val IMAGE_ENCRYPTION_KEY_ALIAS = "ynab_image_encryption_key"
    }
}
