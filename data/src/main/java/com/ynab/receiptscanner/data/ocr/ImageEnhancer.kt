package com.ynab.receiptscanner.data.ocr

import android.graphics.Bitmap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Image enhancement for improving OCR accuracy
 * Implements adaptive brightness and contrast adjustment
 */
@Singleton
class ImageEnhancer @Inject constructor() {
    
    companion object {
        private const val TARGET_BRIGHTNESS = 128
        private const val CONTRAST_FACTOR = 1.2f
    }
    
    /**
     * Enhance an image for better OCR results
     * Applies adaptive brightness and contrast adjustment
     */
    fun enhance(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Calculate current brightness statistics
        val stats = calculateBrightnessStats(pixels)
        
        // Apply adaptive contrast enhancement
        val enhanced = adaptiveContrastEnhancement(pixels, stats)
        
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(enhanced, 0, width, 0, 0, width, height)
        return result
    }
    
    /**
     * Calculate brightness statistics for adaptive enhancement
     */
    private fun calculateBrightnessStats(pixels: IntArray): BrightnessStats {
        var sum = 0L
        var min = 255
        var max = 0
        
        pixels.forEach { pixel ->
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val brightness = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            
            sum += brightness
            min = min(min, brightness)
            max = max(max, brightness)
        }
        
        val mean = sum.toFloat() / pixels.size
        return BrightnessStats(mean, min.toFloat(), max.toFloat())
    }
    
    /**
     * Apply adaptive contrast enhancement
     * Uses histogram equalization technique
     */
    private fun adaptiveContrastEnhancement(
        pixels: IntArray,
        stats: BrightnessStats
    ): IntArray {
        val enhanced = IntArray(pixels.size)
        
        // Calculate brightness adjustment
        val brightnessAdjust = TARGET_BRIGHTNESS - stats.mean
        
        // Calculate stretch factor for contrast
        val inputRange = stats.max - stats.min
        val stretchFactor = if (inputRange > 0) 255f / inputRange else 1f
        
        pixels.forEachIndexed { index, pixel ->
            val a = (pixel shr 24) and 0xFF
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            
            // Enhance each channel
            val newR = enhanceChannel(r.toFloat(), stats, brightnessAdjust, stretchFactor)
            val newG = enhanceChannel(g.toFloat(), stats, brightnessAdjust, stretchFactor)
            val newB = enhanceChannel(b.toFloat(), stats, brightnessAdjust, stretchFactor)
            
            enhanced[index] = (a shl 24) or (newR shl 16) or (newG shl 8) or newB
        }
        
        return enhanced
    }
    
    /**
     * Enhance a single color channel
     */
    private fun enhanceChannel(
        value: Float,
        stats: BrightnessStats,
        brightnessAdjust: Float,
        stretchFactor: Float
    ): Int {
        // Apply contrast stretch
        var enhanced = (value - stats.min) * stretchFactor
        
        // Apply brightness adjustment
        enhanced += brightnessAdjust
        
        // Apply additional contrast boost
        enhanced = ((enhanced - 128) * CONTRAST_FACTOR + 128)
        
        // Clamp to valid range
        return enhanced.toInt().coerceIn(0, 255)
    }
    
    /**
     * Apply sharpening filter to enhance edges
     */
    fun sharpen(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val sharpened = IntArray(width * height)
        
        // Sharpening kernel
        val kernel = arrayOf(
            floatArrayOf(0f, -1f, 0f),
            floatArrayOf(-1f, 5f, -1f),
            floatArrayOf(0f, -1f, 0f)
        )
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var sumR = 0f
                var sumG = 0f
                var sumB = 0f
                
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixel = pixels[(y + ky) * width + (x + kx)]
                        val weight = kernel[ky + 1][kx + 1]
                        
                        sumR += ((pixel shr 16) and 0xFF) * weight
                        sumG += ((pixel shr 8) and 0xFF) * weight
                        sumB += (pixel and 0xFF) * weight
                    }
                }
                
                val r = sumR.toInt().coerceIn(0, 255)
                val g = sumG.toInt().coerceIn(0, 255)
                val b = sumB.toInt().coerceIn(0, 255)
                
                sharpened[y * width + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        
        // Copy edges from original
        for (y in 0 until height) {
            sharpened[y * width] = pixels[y * width]
            sharpened[y * width + width - 1] = pixels[y * width + width - 1]
        }
        for (x in 0 until width) {
            sharpened[x] = pixels[x]
            sharpened[(height - 1) * width + x] = pixels[(height - 1) * width + x]
        }
        
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(sharpened, 0, width, 0, 0, width, height)
        return result
    }
    
    /**
     * Statistics about image brightness
     */
    private data class BrightnessStats(
        val mean: Float,
        val min: Float,
        val max: Float
    )
}
