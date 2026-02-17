package com.receiptscanner.presentation.camera

import android.net.Uri

/**
 * Represents the different states of the camera capture feature.
 */
sealed class CameraState {
    /**
     * Initial state before any camera operations
     */
    object Idle : CameraState()
    
    /**
     * Camera permission is required from the user
     */
    object PermissionRequired : CameraState()
    
    /**
     * Camera is being initialized
     */
    object CameraInitializing : CameraState()
    
    /**
     * Camera is ready to capture images
     */
    object CameraReady : CameraState()
    
    /**
     * Camera is currently capturing an image
     */
    object Capturing : CameraState()
    
    /**
     * Image has been successfully captured
     * @param imageUri URI of the captured image
     */
    data class ImageCaptured(val imageUri: Uri) : CameraState()
    
    /**
     * An error occurred during camera operations
     * @param message Error message describing what went wrong
     */
    data class Error(val message: String) : CameraState()
}
