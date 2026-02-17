package com.receiptscanner.domain.repository

import android.net.Uri

/**
 * Repository interface for managing receipt image storage operations.
 * Handles saving, retrieving, and deleting receipt images.
 */
interface ImageRepository {
    
    /**
     * Saves a receipt image to secure storage
     * @param imageData Raw image data in bytes
     * @return URI of the saved image, or null if save failed
     * @throws IOException if disk write fails
     */
    suspend fun saveImage(imageData: ByteArray): Uri?
    
    /**
     * Retrieves an image from storage
     * @param uri URI of the image to retrieve
     * @return Image data as bytes, or null if not found
     */
    suspend fun getImage(uri: Uri): ByteArray?
    
    /**
     * Deletes an image from storage
     * @param uri URI of the image to delete
     * @return true if deleted successfully, false otherwise
     */
    suspend fun deleteImage(uri: Uri): Boolean
    
    /**
     * Cleans up old images older than the specified number of days
     * @param olderThanDays Delete images older than this many days
     * @return Number of images deleted
     */
    suspend fun cleanupOldImages(olderThanDays: Int = 30): Int
}
