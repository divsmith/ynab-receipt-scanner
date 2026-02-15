package com.ynab.receiptscanner.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Smart image compression utility
 * Compresses images while maintaining quality and aspect ratio
 */
@Singleton
class ImageCompressor @Inject constructor() {
    
    companion object {
        private const val DEFAULT_QUALITY = 85
        private const val MAX_DIMENSION = 2048
        private const val TARGET_SIZE_BYTES = 500 * 1024 // 500 KB
        private const val MIN_QUALITY = 50
    }
    
    data class CompressionResult(
        val bitmap: Bitmap,
        val sizeBytes: Long,
        val quality: Int
    )
    
    /**
     * Compress image file to target size
     * @param inputFile Source image file
     * @param outputFile Destination file for compressed image
     * @param maxDimension Maximum width or height (default 2048)
     * @param targetQuality Target compression quality 0-100 (default 85)
     * @return Compression result with final size and quality
     */
    fun compressImage(
        inputFile: File,
        outputFile: File,
        maxDimension: Int = MAX_DIMENSION,
        targetQuality: Int = DEFAULT_QUALITY
    ): CompressionResult {
        // Load bitmap with proper orientation
        var bitmap = loadBitmapWithOrientation(inputFile, maxDimension)
        
        // Apply compression
        var quality = targetQuality
        var outputBytes: ByteArray
        
        do {
            outputBytes = compressBitmap(bitmap, quality)
            
            if (outputBytes.size <= TARGET_SIZE_BYTES || quality <= MIN_QUALITY) {
                break
            }
            
            // Reduce quality progressively
            quality -= 5
        } while (quality > MIN_QUALITY)
        
        // Write to output file
        FileOutputStream(outputFile).use { fos ->
            fos.write(outputBytes)
        }
        
        return CompressionResult(
            bitmap = bitmap,
            sizeBytes = outputBytes.size.toLong(),
            quality = quality
        )
    }
    
    /**
     * Compress bitmap to target size
     */
    fun compressBitmap(
        bitmap: Bitmap,
        maxDimension: Int = MAX_DIMENSION,
        targetQuality: Int = DEFAULT_QUALITY
    ): CompressionResult {
        // Resize if needed
        val resized = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            resizeBitmap(bitmap, maxDimension)
        } else {
            bitmap
        }
        
        // Compress
        var quality = targetQuality
        var outputBytes: ByteArray
        
        do {
            outputBytes = compressBitmap(resized, quality)
            
            if (outputBytes.size <= TARGET_SIZE_BYTES || quality <= MIN_QUALITY) {
                break
            }
            
            quality -= 5
        } while (quality > MIN_QUALITY)
        
        return CompressionResult(
            bitmap = resized,
            sizeBytes = outputBytes.size.toLong(),
            quality = quality
        )
    }
    
    /**
     * Compress bitmap to byte array
     */
    private fun compressBitmap(bitmap: Bitmap, quality: Int): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }
    
    /**
     * Resize bitmap maintaining aspect ratio
     */
    private fun resizeBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        val scale = min(
            maxDimension.toFloat() / width,
            maxDimension.toFloat() / height
        )
        
        if (scale >= 1f) {
            return bitmap
        }
        
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * Load bitmap with correct orientation from EXIF data
     */
    private fun loadBitmapWithOrientation(file: File, maxDimension: Int): Bitmap {
        // First decode with inJustDecodeBounds to get dimensions
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)
        
        // Calculate sample size
        val sampleSize = calculateSampleSize(options.outWidth, options.outHeight, maxDimension)
        
        // Decode actual bitmap
        options.inJustDecodeBounds = false
        options.inSampleSize = sampleSize
        var bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
        
        // Handle orientation
        val exif = ExifInterface(file.absolutePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        
        bitmap = rotateBitmapIfNeeded(bitmap, orientation)
        
        return bitmap
    }
    
    /**
     * Calculate sample size for efficient loading
     */
    private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        val maxDim = max(width, height)
        
        if (maxDim > maxDimension) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while ((halfHeight / sampleSize) >= maxDimension &&
                (halfWidth / sampleSize) >= maxDimension) {
                sampleSize *= 2
            }
        }
        
        return sampleSize
    }
    
    /**
     * Rotate bitmap based on EXIF orientation
     */
    private fun rotateBitmapIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    /**
     * Get file size in bytes
     */
    fun getFileSize(file: File): Long {
        return file.length()
    }
    
    /**
     * Get bitmap size in bytes
     */
    fun getBitmapSize(bitmap: Bitmap): Long {
        return (bitmap.rowBytes * bitmap.height).toLong()
    }
}
