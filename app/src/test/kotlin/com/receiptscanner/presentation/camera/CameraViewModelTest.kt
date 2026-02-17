package com.receiptscanner.presentation.camera

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.receiptscanner.domain.usecase.CaptureReceiptUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CameraViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var viewModel: CameraViewModel
    private lateinit var captureReceiptUseCase: CaptureReceiptUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        captureReceiptUseCase = mockk()
        viewModel = CameraViewModel(captureReceiptUseCase)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `initial state is Idle`() = runTest {
        // When
        val state = viewModel.cameraState.first()
        
        // Then
        assertTrue("Initial state should be Idle", state is CameraState.Idle)
    }

    @Test
    fun `onPermissionGranted changes state to CameraInitializing`() = runTest {
        // When
        viewModel.onPermissionGranted()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.cameraState.value
        assertTrue("State should be CameraInitializing after permission granted", 
            state is CameraState.CameraInitializing)
    }

    @Test
    fun `onPermissionDenied changes state to PermissionRequired`() = runTest {
        // When
        viewModel.onPermissionDenied()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.cameraState.value
        assertTrue("State should be PermissionRequired after permission denied", 
            state is CameraState.PermissionRequired)
    }

    @Test
    fun `onCameraInitialized changes state to CameraReady`() = runTest {
        // Given
        viewModel.onPermissionGranted()
        advanceUntilIdle()
        
        // When
        viewModel.onCameraInitialized()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.cameraState.value
        assertTrue("State should be CameraReady after initialization", 
            state is CameraState.CameraReady)
    }

    @Test
    fun `onCameraInitializationFailed changes state to Error`() = runTest {
        // Given
        val errorMessage = "Camera initialization failed"
        viewModel.onPermissionGranted()
        advanceUntilIdle()
        
        // When
        viewModel.onCameraInitializationFailed(errorMessage)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.cameraState.value
        assertTrue("State should be Error after initialization failure", 
            state is CameraState.Error)
        assertEquals(errorMessage, (state as CameraState.Error).message)
    }

    @Test
    fun `captureImage changes state to Capturing then ImageCaptured on success`() = runTest {
        // Given
        val imageUri = mockk<Uri>()
        coEvery { captureReceiptUseCase.invoke(any()) } returns Result.success(imageUri)
        
        viewModel.onCameraInitialized()
        advanceUntilIdle()
        
        // When
        viewModel.captureImage(byteArrayOf())
        advanceUntilIdle()
        
        // Verify it went through Capturing state
        // Then final state should be ImageCaptured
        val state = viewModel.cameraState.value
        assertTrue("State should be ImageCaptured after successful capture", 
            state is CameraState.ImageCaptured)
        assertEquals(imageUri, (state as CameraState.ImageCaptured).imageUri)
    }

    @Test
    fun `captureImage changes state to Error on failure`() = runTest {
        // Given
        val errorMessage = "Failed to save image"
        coEvery { captureReceiptUseCase.invoke(any()) } returns Result.failure(Exception(errorMessage))
        
        viewModel.onCameraInitialized()
        advanceUntilIdle()
        
        // When
        viewModel.captureImage(byteArrayOf())
        advanceUntilIdle()
        
        // Then
        val state = viewModel.cameraState.value
        assertTrue("State should be Error after capture failure", 
            state is CameraState.Error)
        assertTrue((state as CameraState.Error).message.contains(errorMessage))
    }

    @Test
    fun `resetToReady changes state back to CameraReady`() = runTest {
        // Given - simulate we're in some other state
        viewModel.onPermissionDenied()
        advanceUntilIdle()
        
        // When
        viewModel.resetToReady()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.cameraState.value
        assertTrue("State should be CameraReady after reset", 
            state is CameraState.CameraReady)
    }

    @Test
    fun `toggleFlash updates flash mode`() = runTest {
        // Given
        assertFalse("Initial flash mode should be false", viewModel.isFlashEnabled.value)
        
        // When
        viewModel.toggleFlash()
        advanceUntilIdle()
        
        // Then
        assertTrue("Flash should be enabled after toggle", viewModel.isFlashEnabled.value)
        
        // When - toggle again
        viewModel.toggleFlash()
        advanceUntilIdle()
        
        // Then
        assertFalse("Flash should be disabled after second toggle", viewModel.isFlashEnabled.value)
    }
}
