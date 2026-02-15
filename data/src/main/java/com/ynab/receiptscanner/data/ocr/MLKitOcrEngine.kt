package com.ynab.receiptscanner.data.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.domain.model.OcrResult
import com.ynab.receiptscanner.domain.model.ReceiptField
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ML Kit implementation of OCR engine
 * Uses Google ML Kit Text Recognition v2 for on-device OCR
 */
@Singleton
class MLKitOcrEngine @Inject constructor(
    private val imagePreprocessor: ImagePreprocessor
) : OcrEngine {
    
    private val recognizer = TextRecognition.getClient()
    
    override suspend fun extractText(bitmap: Bitmap): Result<OcrResult> {
        return try {
            // Preprocess image for better OCR
            val preprocessed = imagePreprocessor.preprocess(bitmap)
            
            // Process with ML Kit
            extractTextFromPreprocessed(preprocessed)
        } catch (e: Exception) {
            Result.Error(e, "OCR preprocessing failed: ${e.message}")
        }
    }
    
    override suspend fun extractTextFromPreprocessed(bitmap: Bitmap): Result<OcrResult> {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val visionText = recognizer.process(inputImage).await()
            
            if (visionText.text.isEmpty()) {
                return Result.Error(
                    Exception("No text detected"),
                    "No text could be detected in the image"
                )
            }
            
            // Build OCR result from ML Kit response
            val textBlocks = visionText.textBlocks.map { block ->
                OcrResult.TextBlock(
                    text = block.text,
                    boundingBox = block.boundingBox?.let {
                        ReceiptField.BoundingBox(
                            left = it.left.toFloat(),
                            top = it.top.toFloat(),
                            right = it.right.toFloat(),
                            bottom = it.bottom.toFloat()
                        )
                    } ?: ReceiptField.BoundingBox(0f, 0f, 0f, 0f),
                    confidence = 0.8f // ML Kit doesn't provide confidence per block
                )
            }
            
            val textLines = visionText.textBlocks.flatMap { block ->
                block.lines.map { line ->
                    OcrResult.TextLine(
                        text = line.text,
                        boundingBox = line.boundingBox?.let {
                            ReceiptField.BoundingBox(
                                left = it.left.toFloat(),
                                top = it.top.toFloat(),
                                right = it.right.toFloat(),
                                bottom = it.bottom.toFloat()
                            )
                        } ?: ReceiptField.BoundingBox(0f, 0f, 0f, 0f),
                        confidence = 0.8f // ML Kit doesn't provide confidence per line
                    )
                }
            }
            
            val textElements = visionText.textBlocks.flatMap { block ->
                block.lines.flatMap { line ->
                    line.elements.map { element ->
                        OcrResult.TextElement(
                            text = element.text,
                            boundingBox = element.boundingBox?.let {
                                ReceiptField.BoundingBox(
                                    left = it.left.toFloat(),
                                    top = it.top.toFloat(),
                                    right = it.right.toFloat(),
                                    bottom = it.bottom.toFloat()
                                )
                            } ?: ReceiptField.BoundingBox(0f, 0f, 0f, 0f),
                            confidence = 0.8f // ML Kit doesn't provide confidence per element
                        )
                    }
                }
            }
            
            // Calculate overall confidence
            val overallConfidence = if (textBlocks.isNotEmpty()) {
                textBlocks.map { it.confidence }.average().toFloat()
            } else {
                0f
            }
            
            val ocrResult = OcrResult(
                rawText = visionText.text,
                blocks = textBlocks,
                lines = textLines,
                elements = textElements,
                confidence = overallConfidence,
                timestamp = Date()
            )
            
            Result.Success(ocrResult)
            
        } catch (e: Exception) {
            Result.Error(e, "ML Kit OCR failed: ${e.message}")
        }
    }
    
    override fun isAvailable(): Boolean {
        return try {
            recognizer != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Release resources
     * Should be called when OCR engine is no longer needed
     */
    fun close() {
        recognizer.close()
    }
}
