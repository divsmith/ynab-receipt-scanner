package com.receiptscanner.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.receiptscanner.data.local.KeystoreManager
import com.receiptscanner.domain.repository.ImageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ImageRepository for managing receipt images.
 * Handles encrypted storage, retrieval, and cleanup of receipt images.
 */
@Singleton
class ImageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keystoreManager: KeystoreManager
) : ImageRepository {

    companion object {
        private const val RECEIPTS_DIR = "receipts"
        private const val DATE_FORMAT = "yyyyMMdd_HHmmss"
        private const val FILE_PREFIX = "receipt_"
        private const val FILE_EXTENSION = ".jpg"
    }

    private val receiptDirectory: File by lazy {
        File(context.filesDir, RECEIPTS_DIR).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    override suspend fun saveImage(imageData: ByteArray): Uri? = withContext(Dispatchers.IO) {
        try {
            // Generate unique filename with timestamp and UUID
            val timestamp = SimpleDateFormat(DATE_FORMAT, Locale.US).format(Date())
            val uuid = UUID.randomUUID().toString().substring(0, 8)
            val filename = "$FILE_PREFIX${timestamp}_$uuid$FILE_EXTENSION"
            val file = File(receiptDirectory, filename)

            Timber.d("Saving image to: ${file.absolutePath}")

            // Encrypt data before saving if possible
            val dataToWrite = try {
                keystoreManager.encrypt(imageData) ?: run {
                    Timber.w("Encryption failed or returned null, saving unencrypted")
                    imageData
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during encryption, saving unencrypted")
                imageData
            }

            // Write to file
            file.writeBytes(dataToWrite)
            
            Timber.i("Image saved successfully: ${file.absolutePath}, size: ${dataToWrite.size} bytes")
            file.toUri()

        } catch (e: IOException) {
            Timber.e(e, "Failed to save image")
            null
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error saving image")
            null
        }
    }

    override suspend fun getImage(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val file = File(uri.path ?: return@withContext null)
            
            if (!file.exists()) {
                Timber.w("File does not exist: ${file.absolutePath}")
                return@withContext null
            }

            Timber.d("Reading image from: ${file.absolutePath}")
            
            val encryptedData = file.readBytes()

            // Attempt to decrypt
            try {
                val decryptedData = keystoreManager.decrypt(encryptedData)
                if (decryptedData != null) {
                    Timber.d("Image decrypted successfully")
                    return@withContext decryptedData
                } else {
                    Timber.w("Decryption returned null, returning encrypted data")
                    return@withContext encryptedData
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during decryption, returning raw data")
                return@withContext encryptedData
            }

        } catch (e: IOException) {
            Timber.e(e, "Failed to read image")
            null
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error reading image")
            null
        }
    }

    override suspend fun deleteImage(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(uri.path ?: return@withContext false)
            
            if (!file.exists()) {
                Timber.w("File does not exist, cannot delete: ${file.absolutePath}")
                return@withContext false
            }

            val deleted = file.delete()
            if (deleted) {
                Timber.i("Image deleted successfully: ${file.absolutePath}")
            } else {
                Timber.w("Failed to delete image: ${file.absolutePath}")
            }
            
            deleted

        } catch (e: Exception) {
            Timber.e(e, "Error deleting image")
            false
        }
    }

    override suspend fun cleanupOldImages(olderThanDays: Int): Int = withContext(Dispatchers.IO) {
        try {
            if (!receiptDirectory.exists()) {
                Timber.d("Receipt directory doesn't exist, no cleanup needed")
                return@withContext 0
            }

            val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
            val files = receiptDirectory.listFiles() ?: return@withContext 0
            
            var deletedCount = 0
            
            files.forEach { file ->
                if (file.isFile && file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        deletedCount++
                        Timber.d("Deleted old image: ${file.name}")
                    } else {
                        Timber.w("Failed to delete old image: ${file.name}")
                    }
                }
            }

            Timber.i("Cleanup completed: deleted $deletedCount old images")
            deletedCount

        } catch (e: Exception) {
            Timber.e(e, "Error during cleanup")
            0
        }
    }
}
