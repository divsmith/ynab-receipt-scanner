package com.receiptscanner.domain.usecase

import android.net.Uri
import com.receiptscanner.domain.repository.ImageRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case for capturing and saving receipt images.
 * Handles image validation and storage operations.
 */
class CaptureReceiptUseCase @Inject constructor(
    private val imageRepository: ImageRepository
) {
    
    companion object {
        private const val MIN_IMAGE_SIZE_BYTES = 1024 // 1KB
        private const val MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024 // 10MB
    }
    
    /**
     * Captures and saves a receipt image
     * @param imageData Raw image data to save
     * @return Result containing the URI of the saved image or error
     */
    suspend operator fun invoke(imageData: ByteArray): Result<Uri> {
        return try {
            // Validate image data is not empty
            if (imageData.isEmpty()) {
                Timber.w("Image data is empty")
                return Result.failure(IllegalArgumentException("Image data cannot be empty"))
            }
            
            // Validate image size is within acceptable range
            if (imageData.size < MIN_IMAGE_SIZE_BYTES) {
                Timber.w("Image size too small: ${imageData.size} bytes")
                return Result.failure(
                    IllegalArgumentException("Image size too small. Minimum: $MIN_IMAGE_SIZE_BYTES bytes")
                )
            }
            
            if (imageData.size > MAX_IMAGE_SIZE_BYTES) {
                Timber.w("Image size too large: ${imageData.size} bytes")
                return Result.failure(
                    IllegalArgumentException("Image size too large. Maximum: $MAX_IMAGE_SIZE_BYTES bytes")
                )
            }
            
            // Save image using repository
            val uri = imageRepository.saveImage(imageData)
            
            if (uri == null) {
                Timber.e("Repository returned null URI when saving image")
                return Result.failure(
                    IllegalStateException("Failed to save image: repository returned null")
                )
            }
            
            Timber.d("Successfully saved image: $uri")
            Result.success(uri)
            
        } catch (e: Exception) {
            Timber.e(e, "Error capturing receipt image")
            Result.failure(e)
        }
    }
}
