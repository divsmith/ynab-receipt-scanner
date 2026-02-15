package com.ynab.receiptscanner.data.security

import com.ynab.receiptscanner.core.util.Result
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for AES-256-GCM encryption and decryption operations
 * Provides secure encryption for files and sensitive data
 */
@Singleton
class EncryptionHelper @Inject constructor(
    private val keystoreManager: KeystoreManager
) {
    
    /**
     * Encrypt a byte array
     * @param data Data to encrypt
     * @param keyAlias Keystore alias for encryption key
     * @return Result containing encrypted data with IV prepended
     */
    fun encrypt(data: ByteArray, keyAlias: String): Result<ByteArray> {
        return try {
            val key = keystoreManager.getOrCreateKey(keyAlias)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            
            val iv = cipher.iv
            val encryptedData = cipher.doFinal(data)
            
            // Prepend IV to encrypted data
            val combined = ByteArray(iv.size + encryptedData.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedData, 0, combined, iv.size, encryptedData.size)
            
            Result.Success(combined)
        } catch (e: Exception) {
            Result.Error(e, "Encryption failed: ${e.message}")
        }
    }
    
    /**
     * Decrypt a byte array
     * @param encryptedData Encrypted data with IV prepended
     * @param keyAlias Keystore alias for decryption key
     * @return Result containing decrypted data
     */
    fun decrypt(encryptedData: ByteArray, keyAlias: String): Result<ByteArray> {
        return try {
            val key = keystoreManager.getKey(keyAlias)
                ?: return Result.Error(Exception("Key not found"), "Decryption key not found")
            
            // Extract IV from the beginning
            val iv = ByteArray(IV_SIZE)
            System.arraycopy(encryptedData, 0, iv, 0, IV_SIZE)
            
            // Extract encrypted data
            val data = ByteArray(encryptedData.size - IV_SIZE)
            System.arraycopy(encryptedData, IV_SIZE, data, 0, data.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val gcmParameterSpec = GCMParameterSpec(TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec)
            
            val decryptedData = cipher.doFinal(data)
            Result.Success(decryptedData)
        } catch (e: Exception) {
            Result.Error(e, "Decryption failed: ${e.message}")
        }
    }
    
    /**
     * Encrypt a file
     * @param inputFile File to encrypt
     * @param outputFile Destination for encrypted file
     * @param keyAlias Keystore alias for encryption key
     * @return Result indicating success or failure
     */
    fun encryptFile(inputFile: File, outputFile: File, keyAlias: String): Result<Unit> {
        return try {
            val inputData = FileInputStream(inputFile).use { it.readBytes() }
            
            when (val result = encrypt(inputData, keyAlias)) {
                is Result.Success -> {
                    FileOutputStream(outputFile).use { it.write(result.data) }
                    Result.Success(Unit)
                }
                is Result.Error -> result
                is Result.Loading -> Result.Error(Exception("Unexpected loading state"))
            }
        } catch (e: Exception) {
            Result.Error(e, "File encryption failed: ${e.message}")
        }
    }
    
    /**
     * Decrypt a file
     * @param inputFile Encrypted file
     * @param outputFile Destination for decrypted file
     * @param keyAlias Keystore alias for decryption key
     * @return Result indicating success or failure
     */
    fun decryptFile(inputFile: File, outputFile: File, keyAlias: String): Result<Unit> {
        return try {
            val encryptedData = FileInputStream(inputFile).use { it.readBytes() }
            
            when (val result = decrypt(encryptedData, keyAlias)) {
                is Result.Success -> {
                    FileOutputStream(outputFile).use { it.write(result.data) }
                    Result.Success(Unit)
                }
                is Result.Error -> result
                is Result.Loading -> Result.Error(Exception("Unexpected loading state"))
            }
        } catch (e: Exception) {
            Result.Error(e, "File decryption failed: ${e.message}")
        }
    }
    
    /**
     * Encrypt a string
     * @param text Text to encrypt
     * @param keyAlias Keystore alias for encryption key
     * @return Result containing encrypted text as Base64 string
     */
    fun encryptString(text: String, keyAlias: String): Result<String> {
        return when (val result = encrypt(text.toByteArray(), keyAlias)) {
            is Result.Success -> Result.Success(android.util.Base64.encodeToString(
                result.data, 
                android.util.Base64.NO_WRAP
            ))
            is Result.Error -> result
            is Result.Loading -> Result.Error(Exception("Unexpected loading state"))
        }
    }
    
    /**
     * Decrypt a string
     * @param encryptedText Encrypted text as Base64 string
     * @param keyAlias Keystore alias for decryption key
     * @return Result containing decrypted text
     */
    fun decryptString(encryptedText: String, keyAlias: String): Result<String> {
        return try {
            val encryptedData = android.util.Base64.decode(encryptedText, android.util.Base64.NO_WRAP)
            when (val result = decrypt(encryptedData, keyAlias)) {
                is Result.Success -> Result.Success(String(result.data))
                is Result.Error -> result
                is Result.Loading -> Result.Error(Exception("Unexpected loading state"))
            }
        } catch (e: Exception) {
            Result.Error(e, "String decryption failed: ${e.message}")
        }
    }
    
    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12 // 96 bits
        private const val TAG_LENGTH = 128 // 128 bits
    }
}
