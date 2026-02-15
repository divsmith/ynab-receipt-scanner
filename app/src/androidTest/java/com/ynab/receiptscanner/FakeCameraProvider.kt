package com.ynab.receiptscanner

import android.graphics.Bitmap
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.util.concurrent.Executor

/**
 * Fake CameraProvider for testing camera functionality
 * Simulates camera operations without requiring actual camera hardware
 */
class FakeCameraProvider {
    
    var isCameraBound = false
    var captureCallback: ImageCapture.OnImageSavedCallback? = null
    var lastCapturedFile: File? = null
    
    /**
     * Simulate camera binding
     */
    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        cameraSelector: CameraSelector,
        preview: Preview,
        imageCapture: ImageCapture
    ) {
        isCameraBound = true
    }
    
    /**
     * Simulate image capture
     */
    fun captureImage(
        outputFileOptions: ImageCapture.OutputFileOptions,
        executor: Executor,
        callback: ImageCapture.OnImageSavedCallback
    ) {
        captureCallback = callback
        
        // Simulate successful capture after short delay
        executor.execute {
            Thread.sleep(100)
            
            // Create a test image file
            val testFile = File.createTempFile("test_capture", ".jpg")
            lastCapturedFile = testFile
            
            // Write a simple bitmap to file
            val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            testFile.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            
            // Notify success
            callback.onImageSaved(
                ImageCapture.OutputFileResults(android.net.Uri.fromFile(testFile))
            )
        }
    }
    
    /**
     * Simulate capture failure
     */
    fun simulateCaptureFailure(error: ImageCapture.ImageCaptureException) {
        captureCallback?.onError(error)
    }
    
    /**
     * Check if camera is available
     */
    fun isCameraAvailable(): Boolean = true
    
    /**
     * Unbind camera
     */
    fun unbindAll() {
        isCameraBound = false
    }
    
    /**
     * Reset state
     */
    fun reset() {
        isCameraBound = false
        captureCallback = null
        lastCapturedFile?.delete()
        lastCapturedFile = null
    }
}
