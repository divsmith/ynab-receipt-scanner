package com.ynab.receiptscanner.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.data.security.EncryptionHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages storage and retrieval of receipt images
 * Handles encryption, compression, and cleanup
 */
@Singleton
class ImageStorageManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionHelper: EncryptionHelper
) {
    
    companion object {
        private const val IMAGES_DIR = "receipt_images"
        private const val ENCRYPTION_KEY_ALIAS = "receipt_image_key"
        private const val IMAGE_QUALITY = 85 // JPEG compression quality (0-100)
        private const val MAX_IMAGE_DIMENSION = 2048 // Max width/height in pixels
    }
    
    private val imagesDirectory: File by lazy {
        File(context.filesDir, IMAGES_DIR).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    /**
     * Save a receipt image
     * @param receiptId Unique identifier for the receipt
     * @param bitmap Image to save
     * @return Result containing the file path or error
     */
    fun saveImage(receiptId: String, bitmap: Bitmap): Result<String> {
        return try {
            // Resize image if too large
            val resized = resizeIfNeeded(bitmap)
            
            // Compress to JPEG
            val compressed = compressBitmap(resized)
            
            // Encrypt
            val encryptionResult = encryptionHelper.encrypt(compressed, ENCRYPTION_KEY_ALIAS)
            val encrypted = when (encryptionResult) {
                is Result.Success -> encryptionResult.data
                is Result.Error -> return Result.Error(
                    encryptionResult.exception,
                    "Failed to encrypt image: ${encryptionResult.message}"
                )
                else -> return Result.Error(
                    Exception("Unknown error"),
                    "Failed to encrypt image"
                )
            }
            
            // Generate filename
            val filename = generateFilename(receiptId)
            val file = File(imagesDirectory, filename)
            
            // Write to file
            FileOutputStream(file).use { fos ->
                fos.write(encrypted)
            }
            
            Result.Success(file.absolutePath)
            
        } catch (e: Exception) {
            Result.Error(e, "Failed to save image: ${e.message}")
        }
    }
    
    /**
     * Load a receipt image
     * @param imagePath Path to the encrypted image file
     * @return Result containing the bitmap or error
     */
    fun loadImage(imagePath: String): Result<Bitmap> {
        return try {
            val file = File(imagePath)
            
            if (!file.exists()) {
                return Result.Error(
                    Exception("File not found"),
                    "Image file does not exist: $imagePath"
                )
            }
            
            // Read encrypted data
            val encrypted = FileInputStream(file).use { fis ->
                fis.readBytes()
            }
            
            // Decrypt
            val decryptionResult = encryptionHelper.decrypt(encrypted, ENCRYPTION_KEY_ALIAS)
            val decrypted = when (decryptionResult) {
                is Result.Success -> decryptionResult.data
                is Result.Error -> return Result.Error(
                    decryptionResult.exception,
                    "Failed to decrypt image: ${decryptionResult.message}"
                )
                else -> return Result.Error(
                    Exception("Unknown error"),
                    "Failed to decrypt image"
                )
            }
            
            // Decode bitmap
            val bitmap = BitmapFactory.decodeByteArray(decrypted, 0, decrypted.size)
            
            if (bitmap == null) {
                return Result.Error(
                    Exception("Failed to decode bitmap"),
                    "Could not decode image data"
                )
            }
            
            Result.Success(bitmap)
            
        } catch (e: Exception) {
            Result.Error(e, "Failed to load image: ${e.message}")
        }
    }
    
    /**
     * Delete a receipt image
     * @param imagePath Path to the image file
     * @return Result indicating success or error
     */
    fun deleteImage(imagePath: String): Result<Unit> {
        return try {
            val file = File(imagePath)
            
            if (file.exists()) {
                val deleted = file.delete()
                if (deleted) {
                    Result.Success(Unit)
                } else {
                    Result.Error(
                        Exception("Failed to delete file"),
                        "Could not delete image file: $imagePath"
                    )
                }
            } else {
                // File doesn't exist, consider it a success
                Result.Success(Unit)
            }
            
        } catch (e: Exception) {
            Result.Error(e, "Failed to delete image: ${e.message}")
        }
    }
    
    /**
     * Get the size of an image file in bytes
     */
    fun getImageSize(imagePath: String): Long {
        val file = File(imagePath)
        return if (file.exists()) file.length() else 0L
    }
    
    /**
     * Clean up orphaned images (images without corresponding receipts)
     * Should be called periodically
     */
    fun cleanupOrphanedImages(validReceiptIds: List<String>): Result<Int> {
        return try {
            var deletedCount = 0
            
            imagesDirectory.listFiles()?.forEach { file ->
                val receiptId = extractReceiptIdFromFilename(file.name)
                
                if (receiptId != null && !validReceiptIds.contains(receiptId)) {
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }
            
            Result.Success(deletedCount)
            
        } catch (e: Exception) {
            Result.Error(e, "Failed to cleanup images: ${e.message}")
        }
    }
    
    /**
     * Get total size of all stored images in bytes
     */
    fun getTotalStorageSize(): Long {
        return try {
            imagesDirectory.listFiles()?.sumOf { it.length() } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Delete all images (use with caution!)
     */
    fun deleteAllImages(): Result<Int> {
        return try {
            var deletedCount = 0
            
            imagesDirectory.listFiles()?.forEach { file ->
                if (file.delete()) {
                    deletedCount++
                }
            }
            
            Result.Success(deletedCount)
            
        } catch (e: Exception) {
            Result.Error(e, "Failed to delete all images: ${e.message}")
        }
    }
    
    /**
     * Resize bitmap if it exceeds maximum dimensions
     */
    private fun resizeIfNeeded(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= MAX_IMAGE_DIMENSION && height <= MAX_IMAGE_DIMENSION) {
            return bitmap
        }
        
        val ratio = Math.min(
            MAX_IMAGE_DIMENSION.toFloat() / width,
            MAX_IMAGE_DIMENSION.toFloat() / height
        )
        
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * Compress bitmap to JPEG byte array
     */
    private fun compressBitmap(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, outputStream)
        return outputStream.toByteArray()
    }
    
    /**
     * Generate filename for receipt image
     */
    private fun generateFilename(receiptId: String): String {
        return "receipt_${receiptId}.enc"
    }
    
    /**
     * Extract receipt ID from filename
     */
    private fun extractReceiptIdFromFilename(filename: String): String? {
        val pattern = Regex("""receipt_(.+)\.enc""")
        val match = pattern.find(filename)
        return match?.groupValues?.getOrNull(1)
    }
}
