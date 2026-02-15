package com.ynab.receiptscanner.data.ocr

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.PointF
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Edge detection for document boundaries
 * Implements simplified Canny-style edge detection
 */
@Singleton
class EdgeDetector @Inject constructor() {
    
    companion object {
        private const val GRADIENT_THRESHOLD = 50
        private const val MIN_EDGE_LENGTH = 100
    }
    
    /**
     * Detect document edges in an image
     * Returns list of edges representing document boundaries
     */
    fun detectDocumentEdges(bitmap: Bitmap): List<Edge> {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Convert to grayscale intensities
        val gray = pixels.map { pixel ->
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        }.toIntArray()
        
        // Compute gradients using Sobel operators
        val gradients = computeGradients(gray, width, height)
        
        // Find edge points
        val edgePoints = findEdgePoints(gradients, width, height)
        
        // Group into lines/edges
        return groupIntoEdges(edgePoints)
    }
    
    /**
     * Compute gradients using Sobel operator
     */
    private fun computeGradients(gray: IntArray, width: Int, height: Int): Array<Gradient> {
        val gradients = Array(width * height) { Gradient(0f, 0f) }
        
        // Sobel kernels
        val sobelX = arrayOf(
            intArrayOf(-1, 0, 1),
            intArrayOf(-2, 0, 2),
            intArrayOf(-1, 0, 1)
        )
        
        val sobelY = arrayOf(
            intArrayOf(-1, -2, -1),
            intArrayOf(0, 0, 0),
            intArrayOf(1, 2, 1)
        )
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var gx = 0f
                var gy = 0f
                
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixel = gray[(y + ky) * width + (x + kx)]
                        gx += pixel * sobelX[ky + 1][kx + 1]
                        gy += pixel * sobelY[ky + 1][kx + 1]
                    }
                }
                
                gradients[y * width + x] = Gradient(gx, gy)
            }
        }
        
        return gradients
    }
    
    /**
     * Find edge points based on gradient magnitude
     */
    private fun findEdgePoints(gradients: Array<Gradient>, width: Int, height: Int): List<Point> {
        val edgePoints = mutableListOf<Point>()
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val gradient = gradients[y * width + x]
                if (gradient.magnitude > GRADIENT_THRESHOLD) {
                    edgePoints.add(Point(x, y))
                }
            }
        }
        
        return edgePoints
    }
    
    /**
     * Group edge points into continuous edges
     * Simplified line detection
     */
    private fun groupIntoEdges(points: List<Point>): List<Edge> {
        if (points.isEmpty()) return emptyList()
        
        val edges = mutableListOf<Edge>()
        
        // Find corners (heuristic: points at extremes)
        val topLeft = points.minByOrNull { it.x + it.y }
        val topRight = points.maxByOrNull { it.x - it.y }
        val bottomLeft = points.minByOrNull { it.x - it.y }
        val bottomRight = points.maxByOrNull { it.x + it.y }
        
        // Create edges between corners if they exist
        if (topLeft != null && topRight != null && distance(topLeft, topRight) > MIN_EDGE_LENGTH) {
            edges.add(Edge(PointF(topLeft.x.toFloat(), topLeft.y.toFloat()), 
                          PointF(topRight.x.toFloat(), topRight.y.toFloat())))
        }
        
        if (topRight != null && bottomRight != null && distance(topRight, bottomRight) > MIN_EDGE_LENGTH) {
            edges.add(Edge(PointF(topRight.x.toFloat(), topRight.y.toFloat()), 
                          PointF(bottomRight.x.toFloat(), bottomRight.y.toFloat())))
        }
        
        if (bottomRight != null && bottomLeft != null && distance(bottomRight, bottomLeft) > MIN_EDGE_LENGTH) {
            edges.add(Edge(PointF(bottomRight.x.toFloat(), bottomRight.y.toFloat()), 
                          PointF(bottomLeft.x.toFloat(), bottomLeft.y.toFloat())))
        }
        
        if (bottomLeft != null && topLeft != null && distance(bottomLeft, topLeft) > MIN_EDGE_LENGTH) {
            edges.add(Edge(PointF(bottomLeft.x.toFloat(), bottomLeft.y.toFloat()), 
                          PointF(topLeft.x.toFloat(), topLeft.y.toFloat())))
        }
        
        return edges
    }
    
    private fun distance(p1: Point, p2: Point): Float {
        val dx = (p1.x - p2.x).toFloat()
        val dy = (p1.y - p2.y).toFloat()
        return sqrt(dx * dx + dy * dy)
    }
    
    /**
     * Represents a gradient at a pixel
     */
    data class Gradient(val gx: Float, val gy: Float) {
        val magnitude: Float = sqrt(gx * gx + gy * gy)
        val direction: Float = Math.atan2(gy.toDouble(), gx.toDouble()).toFloat()
    }
    
    /**
     * Represents an edge/line in the image
     */
    data class Edge(val start: PointF, val end: PointF) {
        val length: Float = sqrt(
            (end.x - start.x).pow(2) + (end.y - start.y).pow(2)
        )
    }
}
