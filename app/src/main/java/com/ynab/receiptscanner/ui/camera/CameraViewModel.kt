package com.ynab.receiptscanner.ui.camera

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.domain.model.OcrResult
import com.ynab.receiptscanner.domain.model.Receipt
import com.ynab.receiptscanner.usecase.ExtractTextFromImageUseCase
import com.ynab.receiptscanner.usecase.ParseReceiptUseCase
import com.ynab.receiptscanner.usecase.SaveReceiptImageUseCase
import com.ynab.receiptscanner.usecase.SaveReceiptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for camera screen
 * Handles image capture, OCR processing, and receipt parsing
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val extractTextFromImageUseCase: ExtractTextFromImageUseCase,
    private val parseReceiptUseCase: ParseReceiptUseCase,
    private val saveReceiptUseCase: SaveReceiptUseCase,
    private val saveReceiptImageUseCase: SaveReceiptImageUseCase
) : ViewModel() {
    
    private val _captureState = MutableLiveData<CaptureState>()
    val captureState: LiveData<CaptureState> = _captureState
    
    private val _flashEnabled = MutableLiveData(false)
    val flashEnabled: LiveData<Boolean> = _flashEnabled
    
    private var currentBitmap: Bitmap? = null
    
    /**
     * Process captured image through OCR and parsing pipeline
     */
    fun processImage(bitmap: Bitmap) {
        currentBitmap = bitmap
        _captureState.value = CaptureState.Processing
        
        viewModelScope.launch {
            // Step 1: Extract text using OCR
            when (val ocrResult = extractTextFromImageUseCase(bitmap)) {
                is Result.Success -> {
                    processOcrResult(ocrResult.data, bitmap)
                }
                is Result.Error -> {
                    _captureState.value = CaptureState.Error(
                        ocrResult.message,
                        canRetry = true
                    )
                }
                is Result.Loading -> {
                    // Already in processing state
                }
            }
        }
    }
    
    private suspend fun processOcrResult(ocrResult: OcrResult, bitmap: Bitmap) {
        // Validate OCR result
        if (ocrResult.rawText.isBlank()) {
            _captureState.value = CaptureState.Error(
                "No text detected in image. Please ensure the receipt is clear and well-lit.",
                canRetry = true
            )
            return
        }
        
        if (ocrResult.confidence < 0.5f) {
            _captureState.value = CaptureState.Warning(
                "Low confidence OCR result. Review extracted data carefully.",
                ocrResult
            )
        }
        
        // Step 2: Parse receipt from OCR result
        when (val parseResult = parseReceiptUseCase(ocrResult)) {
            is Result.Success -> {
                val receipt = parseResult.data
                
                // Step 3: Save receipt to database
                when (val saveResult = saveReceiptUseCase(receipt)) {
                    is Result.Success -> {
                        val savedReceipt = saveResult.data
                        
                        // Step 4: Save receipt image
                        when (val imageResult = saveReceiptImageUseCase(savedReceipt.id, bitmap)) {
                            is Result.Success -> {
                                // Update receipt with image path
                                val updatedReceipt = savedReceipt.copy(
                                    imagePath = imageResult.data
                                )
                                saveReceiptUseCase(updatedReceipt)
                                
                                _captureState.value = CaptureState.Success(updatedReceipt)
                            }
                            is Result.Error -> {
                                // Receipt saved but image failed - still consider success
                                _captureState.value = CaptureState.Success(savedReceipt)
                            }
                            else -> {}
                        }
                    }
                    is Result.Error -> {
                        _captureState.value = CaptureState.Error(
                            "Failed to save receipt: ${saveResult.message}",
                            canRetry = false
                        )
                    }
                    else -> {}
                }
            }
            is Result.Error -> {
                _captureState.value = CaptureState.Error(
                    "Failed to parse receipt: ${parseResult.message}",
                    canRetry = false
                )
            }
            else -> {}
        }
    }
    
    /**
     * Retry processing the last captured image
     */
    fun retryProcessing() {
        currentBitmap?.let { bitmap ->
            processImage(bitmap)
        }
    }
    
    /**
     * Toggle flash on/off
     */
    fun toggleFlash() {
        _flashEnabled.value = !(_flashEnabled.value ?: false)
    }
    
    /**
     * Reset capture state
     */
    fun resetCaptureState() {
        _captureState.value = CaptureState.Idle
        currentBitmap?.recycle()
        currentBitmap = null
    }
    
    override fun onCleared() {
        super.onCleared()
        currentBitmap?.recycle()
        currentBitmap = null
    }
    
    /**
     * Represents the state of image capture and processing
     */
    sealed class CaptureState {
        object Idle : CaptureState()
        object Processing : CaptureState()
        data class Warning(
            val message: String,
            val ocrResult: OcrResult
        ) : CaptureState()
        data class Success(val receipt: Receipt) : CaptureState()
        data class Error(
            val message: String,
            val canRetry: Boolean
        ) : CaptureState()
    }
}
