package com.receiptscanner.presentation.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.receiptscanner.domain.usecase.CaptureReceiptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for camera capture screen.
 * Manages camera state, permissions, and image capture operations.
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val captureReceiptUseCase: CaptureReceiptUseCase
) : ViewModel() {

    private val _cameraState = MutableStateFlow<CameraState>(CameraState.Idle)
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    private val _isFlashEnabled = MutableStateFlow(false)
    val isFlashEnabled: StateFlow<Boolean> = _isFlashEnabled.asStateFlow()

    /**
     * Called when camera permission is granted by the user
     */
    fun onPermissionGranted() {
        Timber.d("Camera permission granted")
        _cameraState.value = CameraState.CameraInitializing
    }

    /**
     * Called when camera permission is denied by the user
     */
    fun onPermissionDenied() {
        Timber.d("Camera permission denied")
        _cameraState.value = CameraState.PermissionRequired
    }

    /**
     * Called when camera has been successfully initialized
     */
    fun onCameraInitialized() {
        Timber.d("Camera initialized successfully")
        _cameraState.value = CameraState.CameraReady
    }

    /**
     * Called when camera initialization fails
     * @param message Error message describing the failure
     */
    fun onCameraInitializationFailed(message: String) {
        Timber.e("Camera initialization failed: $message")
        _cameraState.value = CameraState.Error(message)
    }

    /**
     * Captures an image and saves it using the use case
     * @param imageData Raw image data to be saved
     */
    fun captureImage(imageData: ByteArray) {
        viewModelScope.launch {
            try {
                Timber.d("Capturing image, size: ${imageData.size} bytes")
                _cameraState.value = CameraState.Capturing
                
                val result = captureReceiptUseCase.invoke(imageData)
                
                result.fold(
                    onSuccess = { uri ->
                        Timber.d("Image captured successfully: $uri")
                        _cameraState.value = CameraState.ImageCaptured(uri)
                    },
                    onFailure = { exception ->
                        Timber.e(exception, "Failed to capture image")
                        _cameraState.value = CameraState.Error(
                            exception.message ?: "Failed to capture image"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during image capture")
                _cameraState.value = CameraState.Error(
                    e.message ?: "Unexpected error during capture"
                )
            }
        }
    }

    /**
     * Resets camera state back to ready for next capture
     */
    fun resetToReady() {
        Timber.d("Resetting camera to ready state")
        _cameraState.value = CameraState.CameraReady
    }

    /**
     * Toggles flash on/off
     */
    fun toggleFlash() {
        _isFlashEnabled.value = !_isFlashEnabled.value
        Timber.d("Flash toggled: ${_isFlashEnabled.value}")
    }
}
