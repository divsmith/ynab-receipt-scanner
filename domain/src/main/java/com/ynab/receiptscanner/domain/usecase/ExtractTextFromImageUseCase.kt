package com.ynab.receiptscanner.domain.usecase

import android.graphics.Bitmap
import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.data.ocr.OcrEngine
import com.ynab.receiptscanner.domain.model.OcrResult
import javax.inject.Inject

/**
 * Use case for extracting text from receipt images
 * Handles OCR processing
 */
class ExtractTextFromImageUseCase @Inject constructor(
    private val ocrEngine: OcrEngine
) {
    
    /**
     * Extract text from image using OCR
     * @param bitmap Receipt image
     * @return Result containing OCR result or error
     */
    suspend operator fun invoke(bitmap: Bitmap): Result<OcrResult> {
        // Validate input
        if (bitmap.width == 0 || bitmap.height == 0) {
            return Result.Error(
                IllegalArgumentException("Invalid bitmap dimensions"),
                "Image has invalid dimensions"
            )
        }
        
        // Check if OCR engine is available
        if (!ocrEngine.isAvailable()) {
            return Result.Error(
                Exception("OCR engine not available"),
                "OCR engine is not available or not initialized"
            )
        }
        
        // Perform OCR
        return ocrEngine.extractText(bitmap)
    }
}
