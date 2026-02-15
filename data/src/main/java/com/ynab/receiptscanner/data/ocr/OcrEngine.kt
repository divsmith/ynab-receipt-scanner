package com.ynab.receiptscanner.data.ocr

import android.graphics.Bitmap
import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.domain.model.OcrResult

/**
 * Interface for OCR engines
 * Allows for different OCR implementations (ML Kit, Tesseract, cloud services, etc.)
 */
interface OcrEngine {
    
    /**
     * Extract text from an image
     * @param bitmap Image to process
     * @return Result containing OCR result or error
     */
    suspend fun extractText(bitmap: Bitmap): Result<OcrResult>
    
    /**
     * Extract text from a preprocessed image
     * Skips preprocessing step for already processed images
     * @param bitmap Preprocessed image
     * @return Result containing OCR result or error
     */
    suspend fun extractTextFromPreprocessed(bitmap: Bitmap): Result<OcrResult>
    
    /**
     * Check if OCR engine is available and ready
     */
    fun isAvailable(): Boolean
}
