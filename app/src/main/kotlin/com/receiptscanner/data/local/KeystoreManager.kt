package com.receiptscanner.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.receiptscanner.domain.model.YnabToken
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages secure storage of YNAB OAuth tokens using Android Keystore
 * via EncryptedSharedPreferences.
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
