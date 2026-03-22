package com.ynab.receipts.data.ocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import com.ynab.receipts.domain.service.ReceiptImagePreprocessor
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class DefaultReceiptImagePreprocessor @Inject constructor() : ReceiptImagePreprocessor {
    override fun buildCandidates(imageBytes: ByteArray): List<ByteArray> {
        val sourceBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: return listOf(imageBytes)

        val enhanced = sourceBitmap.enhanceForText()

        return listOf(
            imageBytes,
            enhanced.toJpegBytes(),
            enhanced.rotate(90f).toJpegBytes(),
            enhanced.rotate(270f).toJpegBytes()
        ).distinctBy { candidate -> candidate.contentHashCode() }
    }

    private fun Bitmap.enhanceForText(): Bitmap {
        val grayscale = ColorMatrix().apply { setSaturation(0f) }
        val contrast = ColorMatrix(
            floatArrayOf(
                CONTRAST_FACTOR,
                0f,
                0f,
                0f,
                CONTRAST_TRANSLATION,
                0f,
                CONTRAST_FACTOR,
                0f,
                0f,
                CONTRAST_TRANSLATION,
                0f,
                0f,
                CONTRAST_FACTOR,
                0f,
                CONTRAST_TRANSLATION,
                0f,
                0f,
                0f,
                1f,
                0f
            )
        )
        grayscale.postConcat(contrast)

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(grayscale)
        }
        canvas.drawBitmap(this, 0f, 0f, paint)
        return output
    }

    private fun Bitmap.rotate(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private fun Bitmap.toJpegBytes(): ByteArray {
        val output = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        return output.toByteArray()
    }

    private companion object {
        const val JPEG_QUALITY = 90
        const val CONTRAST_FACTOR = 1.35f
        const val CONTRAST_TRANSLATION = 128f * (1f - CONTRAST_FACTOR)
    }
}
