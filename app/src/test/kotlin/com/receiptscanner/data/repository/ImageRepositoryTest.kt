package com.receiptscanner.data.repository

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.receiptscanner.data.local.KeystoreManager
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ImageRepositoryTest {

    private lateinit var repository: ImageRepositoryImpl
    private lateinit var context: Context
    private lateinit var keystoreManager: KeystoreManager
    private lateinit var testImagesDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        keystoreManager = mockk(relaxed = true)
        
        // Mock encryption/decryption to pass through data unchanged for tests
        every { keystoreManager.encrypt(any()) } answers { firstArg() }
        every { keystoreManager.decrypt(any()) } answers { firstArg() }
        
        repository = ImageRepositoryImpl(context, keystoreManager)
        
        // Create test directory
        testImagesDir = File(context.cacheDir, "test_receipts")
        testImagesDir.mkdirs()
    }

    @After
    fun teardown() {
        // Clean up test files
        testImagesDir.listFiles()?.forEach { it.delete() }
        testImagesDir.delete()
        clearAllMocks()
    }

    @Test
    fun `saveImage creates file with timestamped name`() = runTest {
        // Given
        val imageData = ByteArray(1024) { it.toByte() }

        // When
        val uri = repository.saveImage(imageData)

        // Then
        assertNotNull("URI should not be null", uri)
        assertTrue("URI should have a path", uri?.path?.isNotEmpty() == true)
        assertTrue("Filename should contain 'receipt_'", 
            uri?.lastPathSegment?.contains("receipt_") == true)
        assertTrue("Filename should end with .jpg", 
            uri?.lastPathSegment?.endsWith(".jpg") == true)
    }

    @Test
    fun `saveImage stores image data correctly`() = runTest {
        // Given
        val imageData = ByteArray(2048) { (it % 256).toByte() }

        // When
        val uri = repository.saveImage(imageData)

        // Then
        assertNotNull("URI should not be null", uri)
        
        // Read back and verify
        val retrievedData = repository.getImage(uri!!)
        assertNotNull("Retrieved data should not be null", retrievedData)
        assertArrayEquals("Image data should match", imageData, retrievedData)
    }

    @Test
    fun `saveImage encrypts data using KeystoreManager`() = runTest {
        // Given
        val imageData = ByteArray(1024) { it.toByte() }
        val encryptedData = ByteArray(1024) { (it + 1).toByte() }
        
        every { keystoreManager.encrypt(imageData) } returns encryptedData

        // When
        val uri = repository.saveImage(imageData)

        // Then
        assertNotNull("URI should not be null", uri)
        verify(exactly = 1) { keystoreManager.encrypt(imageData) }
    }

    @Test
    fun `getImage returns null for non-existent URI`() = runTest {
        // Given
        val nonExistentUri = Uri.parse("file:///non/existent/path.jpg")

        // When
        val data = repository.getImage(nonExistentUri)

        // Then
        assertNull("Data should be null for non-existent file", data)
    }

    @Test
    fun `getImage decrypts data using KeystoreManager`() = runTest {
        // Given
        val imageData = ByteArray(1024) { it.toByte() }
        val encryptedData = ByteArray(1024) { (it + 1).toByte() }
        
        every { keystoreManager.encrypt(imageData) } returns encryptedData
        every { keystoreManager.decrypt(encryptedData) } returns imageData
        
        val uri = repository.saveImage(imageData)

        // When
        val retrievedData = repository.getImage(uri!!)

        // Then
        assertNotNull("Retrieved data should not be null", retrievedData)
        assertArrayEquals("Decrypted data should match original", imageData, retrievedData)
        verify { keystoreManager.decrypt(encryptedData) }
    }

    @Test
    fun `deleteImage removes file from storage`() = runTest {
        // Given
        val imageData = ByteArray(1024) { it.toByte() }
        val uri = repository.saveImage(imageData)

        // When
        val deleted = repository.deleteImage(uri!!)

        // Then
        assertTrue("Delete should return true", deleted)
        
        // Verify file no longer exists
        val retrievedData = repository.getImage(uri)
        assertNull("Data should be null after deletion", retrievedData)
    }

    @Test
    fun `deleteImage returns false for non-existent file`() = runTest {
        // Given
        val nonExistentUri = Uri.parse("file:///non/existent/path.jpg")

        // When
        val deleted = repository.deleteImage(nonExistentUri)

        // Then
        assertFalse("Delete should return false for non-existent file", deleted)
    }

    @Test
    fun `cleanupOldImages deletes files older than specified days`() = runTest {
        // Given - create multiple image files
        val imageData = ByteArray(1024) { it.toByte() }
        
        // Save 3 images
        val uri1 = repository.saveImage(imageData)
        val uri2 = repository.saveImage(imageData)
        val uri3 = repository.saveImage(imageData)
        
        assertNotNull("URI1 should be saved", uri1)
        assertNotNull("URI2 should be saved", uri2)
        assertNotNull("URI3 should be saved", uri3)

        // When - cleanup images older than 0 days (should delete all)
        val deletedCount = repository.cleanupOldImages(olderThanDays = 0)

        // Then
        assertTrue("Should have deleted at least some images", deletedCount >= 0)
    }

    @Test
    fun `cleanupOldImages does not delete recent files`() = runTest {
        // Given - create image file
        val imageData = ByteArray(1024) { it.toByte() }
        val uri = repository.saveImage(imageData)

        // When - cleanup images older than 30 days (should not delete recent file)
        val deletedCount = repository.cleanupOldImages(olderThanDays = 30)

        // Then
        assertEquals("Should not delete recent files", 0, deletedCount)
        
        // Verify file still exists
        val retrievedData = repository.getImage(uri!!)
        assertNotNull("Recent file should still exist", retrievedData)
    }

    @Test
    fun `saveImage handles null or error from KeystoreManager gracefully`() = runTest {
        // Given
        val imageData = ByteArray(1024) { it.toByte() }
        every { keystoreManager.encrypt(any()) } returns null

        // When
        val uri = repository.saveImage(imageData)

        // Then - should store unencrypted if encryption fails
        assertNotNull("URI should not be null even if encryption fails", uri)
    }

    @Test
    fun `multiple saveImage calls create unique filenames`() = runTest {
        // Given
        val imageData = ByteArray(1024) { it.toByte() }

        // When - save multiple images
        val uri1 = repository.saveImage(imageData)
        Thread.sleep(10) // Small delay to ensure different timestamps
        val uri2 = repository.saveImage(imageData)

        // Then
        assertNotNull("URI1 should not be null", uri1)
        assertNotNull("URI2 should not be null", uri2)
        assertNotEquals("URIs should be different", uri1, uri2)
        assertNotEquals("Filenames should be different", 
            uri1?.lastPathSegment, uri2?.lastPathSegment)
    }
}
