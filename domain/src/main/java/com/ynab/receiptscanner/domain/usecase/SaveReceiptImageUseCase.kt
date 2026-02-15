package com.ynab.receiptscanner.domain.usecase

import android.graphics.Bitmap
import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.data.storage.ImageStorageManager
import javax.inject.Inject

/**
 * Use case for saving receipt images to storage
 * Handles image persistence with encryption
 */
class SaveReceiptImageUseCase @Inject constructor(
    private val imageStorageManager: ImageStorageManager
) {
    
    /**
     * Save a receipt image
     * @param receiptId Receipt ID to associate with the image
     * @param bitmap Image to save
     * @return Result containing the file path or error
     */
    suspend operator fun invoke(receiptId: String, bitmap: Bitmap): Result<String> {
        // Validate inputs
        if (receiptId.isBlank()) {
            return Result.Error(
                IllegalArgumentException("Receipt ID is required"),
                "Cannot save image without receipt ID"
            )
        }
        
        if (bitmap.width == 0 || bitmap.height == 0) {
            return Result.Error(
                IllegalArgumentException("Invalid bitmap dimensions"),
                "Image has invalid dimensions"
            )
        }
        
        // Save image
        return imageStorageManager.saveImage(receiptId, bitmap)
    }
    
    /**
     * Delete a receipt image
     * @param imagePath Path to the image file
     * @return Result indicating success or error
     */
    suspend fun deleteImage(imagePath: String): Result<Unit> {
        return imageStorageManager.deleteImage(imagePath)
    }
}
