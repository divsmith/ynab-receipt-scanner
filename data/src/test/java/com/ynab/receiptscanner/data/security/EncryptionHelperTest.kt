package com.ynab.receiptscanner.data.security

import com.google.common.truth.Truth.assertThat
import com.ynab.receiptscanner.core.util.Result
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import javax.crypto.SecretKey

/**
 * Unit tests for EncryptionHelper
 * Tests encryption and decryption operations
 */
class EncryptionHelperTest {
    
    private lateinit var keystoreManager: KeystoreManager
    private lateinit var encryptionHelper: EncryptionHelper
    private lateinit var mockKey: SecretKey
    
    @Before
    fun setup() {
        keystoreManager = mockk(relaxed = true)
        mockKey = mockk(relaxed = true)
        encryptionHelper = EncryptionHelper(keystoreManager)
    }
    
    @Test
    fun encrypt_withValidData_returnsEncryptedBytes() {
        // Given
        val keyAlias = "test-key"
        val plainText = "Sensitive data to encrypt".toByteArray()
        
        every { keystoreManager.getOrCreateKey(keyAlias) } returns mockKey
        
        // When
        val result = encryptionHelper.encrypt(plainText, keyAlias)
        
        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val encrypted = (result as Result.Success).data
        assertThat(encrypted).isNotNull()
        assertThat(encrypted.size).isGreaterThan(plainText.size) // IV prepended
    }
    
    @Test
    fun decrypt_withEncryptedData_returnsOriginalData() {
        // Given
        val keyAlias = "test-key"
        val plainText = "Sensitive data to decrypt".toByteArray()
        
        every { keystoreManager.getOrCreateKey(keyAlias) } returns mockKey
        every { keystoreManager.getKey(keyAlias) } returns mockKey
        
        // When
        val encryptResult = encryptionHelper.encrypt(plainText, keyAlias)
        assertThat(encryptResult).isInstanceOf(Result.Success::class.java)
        
        val encrypted = (encryptResult as Result.Success).data
        val decryptResult = encryptionHelper.decrypt(encrypted, keyAlias)
        
        // Then
        assertThat(decryptResult).isInstanceOf(Result.Success::class.java)
        val decrypted = (decryptResult as Result.Success).data
        assertThat(decrypted).isEqualTo(plainText)
    }
    
    @Test
    fun encrypt_withEmptyData_succeeds() {
        // Given
        val keyAlias = "test-key"
        val emptyData = ByteArray(0)
        
        every { keystoreManager.getOrCreateKey(keyAlias) } returns mockKey
        
        // When
        val result = encryptionHelper.encrypt(emptyData, keyAlias)
        
        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
    }
    
    @Test
    fun decrypt_withInvalidKey_returnsError() {
        // Given
        val keyAlias = "test-key"
        val invalidData = "Invalid encrypted data".toByteArray()
        
        every { keystoreManager.getKey(keyAlias) } returns null
        
        // When
        val result = encryptionHelper.decrypt(invalidData, keyAlias)
        
        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        assertThat((result as Result.Error).message).contains("key not found")
    }
    
    @Test
    fun encrypt_withLargeData_succeeds() {
        // Given
        val keyAlias = "test-key"
        val largeData = ByteArray(1024 * 1024) { it.toByte() } // 1MB
        
        every { keystoreManager.getOrCreateKey(keyAlias) } returns mockKey
        
        // When
        val result = encryptionHelper.encrypt(largeData, keyAlias)
        
        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val encrypted = (result as Result.Success).data
        assertThat(encrypted.size).isGreaterThan(0)
    }
    
    @Test
    fun encryptDecryptCycle_withSpecialCharacters_preservesData() {
        // Given
        val keyAlias = "test-key"
        val specialData = "Test with émojis 🎉 and spëcial çharacters!".toByteArray(Charsets.UTF_8)
        
        every { keystoreManager.getOrCreateKey(keyAlias) } returns mockKey
        every { keystoreManager.getKey(keyAlias) } returns mockKey
        
        // When
        val encryptResult = encryptionHelper.encrypt(specialData, keyAlias)
        val encrypted = (encryptResult as Result.Success).data
        val decryptResult = encryptionHelper.decrypt(encrypted, keyAlias)
        
        // Then
        assertThat(decryptResult).isInstanceOf(Result.Success::class.java)
        val decrypted = (decryptResult as Result.Success).data
        assertThat(String(decrypted, Charsets.UTF_8)).isEqualTo(String(specialData, Charsets.UTF_8))
    }
    
    @Test
    fun encrypt_multipleTimes_producesUniqueOutput() {
        // Given
        val keyAlias = "test-key"
        val plainText = "Same data".toByteArray()
        
        every { keystoreManager.getOrCreateKey(keyAlias) } returns mockKey
        
        // When - Encrypt same data twice
        val result1 = encryptionHelper.encrypt(plainText, keyAlias)
        val result2 = encryptionHelper.encrypt(plainText, keyAlias)
        
        // Then - Should produce different ciphertext (different IV)
        assertThat(result1).isInstanceOf(Result.Success::class.java)
        assertThat(result2).isInstanceOf(Result.Success::class.java)
        
        val encrypted1 = (result1 as Result.Success).data
        val encrypted2 = (result2 as Result.Success).data
        
        // Different IV means different encrypted output
        assertThat(encrypted1).isNotEqualTo(encrypted2)
    }
    
    @Test
    fun decrypt_withCorruptedData_returnsError() {
        // Given
        val keyAlias = "test-key"
        val plainText = "Test data".toByteArray()
        
        every { keystoreManager.getOrCreateKey(keyAlias) } returns mockKey
        every { keystoreManager.getKey(keyAlias) } returns mockKey
        
        val encryptResult = encryptionHelper.encrypt(plainText, keyAlias)
        val encrypted = (encryptResult as Result.Success).data
        
        // Corrupt the data
        val corrupted = encrypted.copyOf()
        if (corrupted.size > 20) {
            corrupted[20] = (corrupted[20] + 1).toByte()
        }
        
        // When
        val decryptResult = encryptionHelper.decrypt(corrupted, keyAlias)
        
        // Then
        assertThat(decryptResult).isInstanceOf(Result.Error::class.java)
    }
    
    @Test
    fun encryptDecrypt_withBinaryData_preservesData() {
        // Given
        val keyAlias = "test-key"
        val binaryData = ByteArray(256) { it.toByte() }
        
        every { keystoreManager.getOrCreateKey(keyAlias) } returns mockKey
        every { keystoreManager.getKey(keyAlias) } returns mockKey
        
        // When
        val encryptResult = encryptionHelper.encrypt(binaryData, keyAlias)
        val encrypted = (encryptResult as Result.Success).data
        val decryptResult = encryptionHelper.decrypt(encrypted, keyAlias)
        
        // Then
        assertThat(decryptResult).isInstanceOf(Result.Success::class.java)
        val decrypted = (decryptResult as Result.Success).data
        assertThat(decrypted).isEqualTo(binaryData)
    }
}
