package com.receiptscanner.presentation.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import timber.log.Timber

/**
 * Custom view that draws a framing overlay to guide users
 * in positioning their receipt for capture.
 */
class FramingOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val OVERLAY_ALPHA = 140 // Semi-transparent overlay
        private const val FRAME_STROKE_WIDTH = 8f
        private const val CORNER_RADIUS = 24f
        private const val FRAME_MARGIN_RATIO = 0.1f // 10% margin from edges
        private const val TEXT_SIZE = 48f
        private const val TEXT_MARGIN = 80f
    }

    // Paint for the semi-transparent overlay
    private val overlayPaint = Paint().apply {
        color = Color.BLACK
        alpha = OVERLAY_ALPHA
        style = Paint.Style.FILL
    }

    // Paint for the frame border
    private val framePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = FRAME_STROKE_WIDTH
        isAntiAlias = true
    }

    // Paint for guide text
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = TEXT_SIZE
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    // Frame rectangle calculated based on view dimensions
    private val frameRect = RectF()

    // Guide text to display
    private var guideText: String = "Align receipt within frame"

    /**
     * Sets the guide text to display below the frame
     */
    fun setGuideText(text: String) {
        guideText = text
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateFrameRect(w, h)
    }

    /**
     * Calculates the frame rectangle based on view dimensions
     */
    private fun calculateFrameRect(width: Int, height: Int) {
        if (width == 0 || height == 0) {
            Timber.w("Invalid dimensions for frame calculation")
            return
        }

        // Calculate margins
        val horizontalMargin = width * FRAME_MARGIN_RATIO
        val verticalMargin = height * FRAME_MARGIN_RATIO

        // Calculate frame bounds (portrait-oriented rectangle for receipts)
        val left = horizontalMargin
        val right = width - horizontalMargin
        val top = verticalMargin
        val bottom = height - verticalMargin

        frameRect.set(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
        
        Timber.d("Frame calculated: $frameRect")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()

        if (width <= 0 || height <= 0 || frameRect.isEmpty) {
            Timber.w("Invalid canvas dimensions or empty frame rect")
            return
        }

        // Draw semi-transparent overlay outside the frame
        // Top overlay
        canvas.drawRect(0f, 0f, width, frameRect.top, overlayPaint)
        
        // Bottom overlay
        canvas.drawRect(0f, frameRect.bottom, width, height, overlayPaint)
        
        // Left overlay
        canvas.drawRect(0f, frameRect.top, frameRect.left, frameRect.bottom, overlayPaint)
        
        // Right overlay
        canvas.drawRect(frameRect.right, frameRect.top, width, frameRect.bottom, overlayPaint)

        // Draw the frame border with rounded corners
        canvas.drawRoundRect(frameRect, CORNER_RADIUS, CORNER_RADIUS, framePaint)

        // Draw corner indicators for better visual guidance
        drawCornerIndicators(canvas)

        // Draw guide text below the frame
        val textX = width / 2
        val textY = frameRect.bottom + TEXT_MARGIN
        canvas.drawText(guideText, textX, textY, textPaint)
    }

    /**
     * Draws corner indicators for better visual guidance
     */
    private fun drawCornerIndicators(canvas: Canvas) {
        val cornerLength = 60f
        val cornerPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = FRAME_STROKE_WIDTH * 1.5f
            isAntiAlias = true
        }

        // Top-left corner
        canvas.drawLine(frameRect.left, frameRect.top, frameRect.left + cornerLength, frameRect.top, cornerPaint)
        canvas.drawLine(frameRect.left, frameRect.top, frameRect.left, frameRect.top + cornerLength, cornerPaint)

        // Top-right corner
        canvas.drawLine(frameRect.right - cornerLength, frameRect.top, frameRect.right, frameRect.top, cornerPaint)
        canvas.drawLine(frameRect.right, frameRect.top, frameRect.right, frameRect.top + cornerLength, cornerPaint)

        // Bottom-left corner
        canvas.drawLine(frameRect.left, frameRect.bottom - cornerLength, frameRect.left, frameRect.bottom, cornerPaint)
        canvas.drawLine(frameRect.left, frameRect.bottom, frameRect.left + cornerLength, frameRect.bottom, cornerPaint)

        // Bottom-right corner
        canvas.drawLine(frameRect.right - cornerLength, frameRect.bottom, frameRect.right, frameRect.bottom, cornerPaint)
        canvas.drawLine(frameRect.right, frameRect.bottom - cornerLength, frameRect.right, frameRect.bottom, cornerPaint)
    }
}
