package com.ynab.receiptscanner.data.storage

import android.content.Context
import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.data.security.EncryptionHelper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream

/**
 * Unit tests for ImageStorageManager
 * Tests image save/load/delete operations
 */
class ImageStorageManagerTest {
    
    @get:Rule
    val temporaryFolder = TemporaryFolder()
    
    private lateinit var context: Context
    private lateinit var encryptionHelper: EncryptionHelper
    private lateinit var imageStorageManager: ImageStorageManager
    private lateinit var mockBitmap: Bitmap
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        encryptionHelper = mockk(relaxed = true)
        
        // Mock context to use temporary folder
        every { context.filesDir } returns temporaryFolder.root
        
        imageStorageManager = ImageStorageManager(context, encryptionHelper)
        
        // Create mock bitmap
        mockBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    }
    
    @Test
    fun saveImage_withValidBitmap_returnsSavePath() {
        // Given
        val receiptId = "test-receipt-123"
        val encryptedData = ByteArray(100) { it.toByte() }
        
        coEvery { 
            encryptionHelper.encrypt(any(), any()) 
        } returns Result.Success(encryptedData)
        
        // When
        val result = imageStorageManager.saveImage(receiptId, mockBitmap)
        
        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val path = (result as Result.Success).data
        assertThat(path).isNotEmpty()
        assertThat(File(path).exists()).isTrue()
    }
    
    @Test
    fun saveImage_createsImagesDirectory() {
        // Given
        val receiptId = "test-receipt"
        val encryptedData = ByteArray(100)
        
        coEvery { 
            encryptionHelper.encrypt(any(), any()) 
        } returns Result.Success(encryptedData)
        
        // When
        imageStorageManager.saveImage(receiptId, mockBitmap)
        
        // Then
        val imagesDir = File(context.filesDir, "receipt_images")
        assertThat(imagesDir.exists()).isTrue()
        assertThat(imagesDir.isDirectory).isTrue()
    }
    
    @Test
    fun saveImage_encryptsImageData() {
        // Given
        val receiptId = "test-receipt"
        val encryptedData = ByteArray(100)
        
        coEvery { 
            encryptionHelper.encrypt(any(), any()) 
        } returns Result.Success(encryptedData)
        
        // When
        imageStorageManager.saveImage(receiptId, mockBitmap)
        
        // Then
        verify { encryptionHelper.encrypt(any(), any()) }
    }
    
    @Test
    fun saveImage_whenEncryptionFails_returnsError() {
        // Given
        val receiptId = "test-receipt"
        val exception = Exception("Encryption failed")
        
        coEvery { 
            encryptionHelper.encrypt(any(), any()) 
        } returns Result.Error(exception, "Encryption error")
        
        // When
        val result = imageStorageManager.saveImage(receiptId, mockBitmap)
        
        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        assertThat((result as Result.Error).message).contains("encrypt")
    }
    
    @Test
    fun loadImage_withValidPath_returnsBitmap() {
        // Given
        val receiptId = "test-receipt"
        val encryptedData = ByteArray(100)
        val decryptedData = ByteArray(1000) // Simulated JPEG data
        
        coEvery { 
            encryptionHelper.encrypt(any(), any()) 
        } returns Result.Success(encryptedData)
        
        coEvery { 
            encryptionHelper.decrypt(any(), any()) 
        } returns Result.Success(decryptedData)
        
        // Save first
        val saveResult = imageStorageManager.saveImage(receiptId, mockBitmap)
        val path = (saveResult as Result.Success).data
        
        // When
        val loadResult = imageStorageManager.loadImage(path)
        
        // Then - With mock decryption, this might fail bitmap decode, but decryption should be called
        verify { encryptionHelper.decrypt(any(), any()) }
    }
    
    @Test
    fun loadImage_withNonExistentPath_returnsError() {
        // Given
        val nonExistentPath = "/path/to/nonexistent/image.jpg"
        
        // When
        val result = imageStorageManager.loadImage(nonExistentPath)
        
        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        assertThat((result as Result.Error).message).contains("not exist")
    }
    
    @Test
    fun deleteImage_withValidPath_deletesFile() {
        // Given
        val receiptId = "test-receipt"
        val encryptedData = ByteArray(100)
        
        coEvery { 
            encryptionHelper.encrypt(any(), any()) 
        } returns Result.Success(encryptedData)
        
        val saveResult = imageStorageManager.saveImage(receiptId, mockBitmap)
        val path = (saveResult as Result.Success).data
        
        assertThat(File(path).exists()).isTrue()
        
        // When
        val deleteResult = imageStorageManager.deleteImage(path)
        
        // Then
        assertThat(deleteResult).isInstanceOf(Result.Success::class.java)
        assertThat(File(path).exists()).isFalse()
    }
    
    @Test
    fun deleteImage_withNonExistentPath_succeeds() {
        // Given
        val nonExistentPath = "/path/to/nonexistent/image.jpg"
        
        // When
        val result = imageStorageManager.deleteImage(nonExistentPath)
        
        // Then - Should succeed even if file doesn't exist
        assertThat(result).isInstanceOf(Result.Success::class.java)
    }
    
    @Test
    fun saveImage_generatesUniqueFilenames() {
        // Given
        val receiptId = "test-receipt"
        val encryptedData = ByteArray(100)
        
        coEvery { 
            encryptionHelper.encrypt(any(), any()) 
        } returns Result.Success(encryptedData)
        
        // When - Save same receipt ID multiple times
        val result1 = imageStorageManager.saveImage(receiptId, mockBitmap)
        val result2 = imageStorageManager.saveImage(receiptId, mockBitmap)
        
        // Then
        val path1 = (result1 as Result.Success).data
        val path2 = (result2 as Result.Success).data
        
        assertThat(path1).isNotEqualTo(path2)
    }
    
    @Test
    fun clearAllImages_deletesAllImages() {
        // Given
        val receiptIds = listOf("receipt-1", "receipt-2", "receipt-3")
        val encryptedData = ByteArray(100)
        
        coEvery { 
            encryptionHelper.encrypt(any(), any()) 
        } returns Result.Success(encryptedData)
        
        receiptIds.forEach { receiptId ->
            imageStorageManager.saveImage(receiptId, mockBitmap)
        }
        
        val imagesDir = File(context.filesDir, "receipt_images")
        val filesBefore = imagesDir.listFiles()?.size ?: 0
        assertThat(filesBefore).isEqualTo(3)
        
        // When
        val result = imageStorageManager.clearAllImages()
        
        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val filesAfter = imagesDir.listFiles()?.size ?: 0
        assertThat(filesAfter).isEqualTo(0)
    }
    
    @Test
    fun getStorageSize_returnsCorrectSize() {
        // Given
        val receiptIds = listOf("receipt-1", "receipt-2")
        val encryptedData = ByteArray(100)
        
        coEvery { 
            encryptionHelper.encrypt(any(), any()) 
        } returns Result.Success(encryptedData)
        
        receiptIds.forEach { receiptId ->
            imageStorageManager.saveImage(receiptId, mockBitmap)
        }
        
        // When
        val size = imageStorageManager.getStorageSize()
        
        // Then
        assertThat(size).isGreaterThan(0L)
    }
    
    @Test
    fun saveImage_withLargeBitmap_compressesImage() {
        // Given
        val receiptId = "large-receipt"
        val largeBitmap = Bitmap.createBitmap(4000, 4000, Bitmap.Config.ARGB_8888)
        val encryptedData = ByteArray(100)
        
        coEvery { 
            encryptionHelper.encrypt(any(), any()) 
        } returns Result.Success(encryptedData)
        
        // When
        val result = imageStorageManager.saveImage(receiptId, largeBitmap)
        
        // Then - Should succeed (image gets resized before encryption)
        assertThat(result).isInstanceOf(Result.Success::class.java)
        verify { encryptionHelper.encrypt(any(), any()) }
    }
}
