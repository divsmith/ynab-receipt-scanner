package com.ynab.receiptscanner.data.ocr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Image preprocessing pipeline for receipt images
 * Improves OCR accuracy through grayscale conversion, denoising, and contrast enhancement
 */
@Singleton
class ImagePreprocessor @Inject constructor(
    private val edgeDetector: EdgeDetector,
    private val imageEnhancer: ImageEnhancer
) {
    
    companion object {
        private const val MAX_DIMENSION = 2048
    }
    
    /**
     * Apply complete preprocessing pipeline to an image
     * @param bitmap Input image
     * @return Preprocessed image optimized for OCR
     */
    fun preprocess(bitmap: Bitmap): Bitmap {
        var processed = bitmap
        
        // Resize if image is too large (optimization)
        if (bitmap.width > MAX_DIMENSION || bitmap.height > MAX_DIMENSION) {
            processed = resize(processed, MAX_DIMENSION, MAX_DIMENSION)
        }
        
        // Step 1: Convert to grayscale
        processed = toGrayscale(processed)
        
        // Step 2: Detect and correct perspective if needed
        val edges = edgeDetector.detectDocumentEdges(processed)
        if (edges.isNotEmpty()) {
            processed = correctPerspective(processed, edges)
        }
        
        // Step 3: Enhance contrast and brightness
        processed = imageEnhancer.enhance(processed)
        
        // Step 4: Denoise
        processed = denoise(processed)
        
        return processed
    }
    
    /**
     * Convert image to grayscale
     */
    fun toGrayscale(bitmap: Bitmap): Bitmap {
        val grayscale = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height,
            Bitmap.Config.ARGB_8888
        )
        
        val canvas = Canvas(grayscale)
        val paint = Paint()
        
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return grayscale
    }
    
    /**
     * Apply denoising to reduce noise in the image
     * Uses a simple box blur filter
     */
    fun denoise(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val denoised = IntArray(width * height)
        val kernelSize = 3
        val offset = kernelSize / 2
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sumR = 0
                var sumG = 0
                var sumB = 0
                var count = 0
                
                for (ky in -offset..offset) {
                    for (kx in -offset..offset) {
                        val px = x + kx
                        val py = y + ky
                        
                        if (px in 0 until width && py in 0 until height) {
                            val pixel = pixels[py * width + px]
                            sumR += (pixel shr 16) and 0xFF
                            sumG += (pixel shr 8) and 0xFF
                            sumB += pixel and 0xFF
                            count++
                        }
                    }
                }
                
                val r = sumR / count
                val g = sumG / count
                val b = sumB / count
                denoised[y * width + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(denoised, 0, width, 0, 0, width, height)
        return result
    }
    
    /**
     * Correct perspective distortion based on detected edges
     * Simplified implementation using crop and scale
     */
    private fun correctPerspective(bitmap: Bitmap, edges: List<EdgeDetector.Edge>): Bitmap {
        // Find bounding box of detected edges
        if (edges.isEmpty()) return bitmap
        
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        
        edges.forEach { edge ->
            minX = min(minX, min(edge.start.x, edge.end.x))
            minY = min(minY, min(edge.start.y, edge.end.y))
            maxX = max(maxX, max(edge.start.x, edge.end.x))
            maxY = max(maxY, max(edge.start.y, edge.end.y))
        }
        
        // Add some padding
        val padding = 20
        val x = max(0, (minX - padding).toInt())
        val y = max(0, (minY - padding).toInt())
        val width = min(bitmap.width - x, (maxX - minX + 2 * padding).toInt())
        val height = min(bitmap.height - y, (maxY - minY + 2 * padding).toInt())
        
        // Crop to detected region
        return try {
            if (width > 0 && height > 0) {
                Bitmap.createBitmap(bitmap, x, y, width, height)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            bitmap
        }
    }
    
    /**
     * Resize image while maintaining aspect ratio
     * Useful for reducing processing time for large images
     */
    fun resize(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }
        
        val ratio = min(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
