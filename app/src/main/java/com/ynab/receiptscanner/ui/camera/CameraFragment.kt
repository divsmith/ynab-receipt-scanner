package com.ynab.receiptscanner.ui.camera

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.ynab.receiptscanner.R
import com.ynab.receiptscanner.databinding.FragmentCameraBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Fragment for capturing receipt images using CameraX
 * Provides camera preview, flash control, and image capture
 */
@AndroidEntryPoint
class CameraFragment : Fragment() {
    
    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: CameraViewModel by viewModels()
    
    private lateinit var cameraPermissionHelper: CameraPermissionHelper
    private lateinit var cameraExecutor: ExecutorService
    
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            showPermissionDeniedDialog()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraPermissionHelper = CameraPermissionHelper(this)
        
        cameraPermissionHelper.registerPermissionLauncher(permissionLauncher) { isGranted ->
            if (isGranted) {
                startCamera()
            } else {
                showPermissionDeniedDialog()
            }
        }
        
        setupUI()
        observeViewModel()
        
        // Check permission and start camera
        if (CameraPermissionHelper.hasPermission(requireContext())) {
            startCamera()
        } else {
            cameraPermissionHelper.requestPermission()
        }
    }
    
    private fun setupUI() {
        binding.captureButton.setOnClickListener {
            captureImage()
        }
        
        binding.flashButton.setOnClickListener {
            viewModel.toggleFlash()
        }
        
        binding.galleryButton.setOnClickListener {
            // TODO: Navigate to gallery when implemented
            Toast.makeText(context, "Gallery coming soon", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun observeViewModel() {
        viewModel.flashEnabled.observe(viewLifecycleOwner) { enabled ->
            updateFlashMode(enabled)
            binding.flashButton.setImageResource(
                if (enabled) R.drawable.ic_launcher_foreground // TODO: Use proper flash icons
                else R.drawable.ic_launcher_foreground
            )
        }
        
        viewModel.captureState.observe(viewLifecycleOwner) { state ->
            handleCaptureState(state)
        }
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }
    
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return
        
        // Preview use case
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
        
        // ImageCapture use case
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetRotation(binding.previewView.display.rotation)
            .build()
        
        // ImageAnalysis use case (for edge detection - optional)
        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        
        // Select back camera
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        
        try {
            // Unbind all use cases before rebinding
            cameraProvider.unbindAll()
            
            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalyzer
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Camera binding failed", e)
            showError("Failed to start camera: ${e.message}")
        }
    }
    
    private fun captureImage() {
        val imageCapture = imageCapture ?: return
        
        // Disable capture button while processing
        binding.captureButton.isEnabled = false
        binding.progressIndicator.visibility = View.VISIBLE
        
        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()
                    
                    requireActivity().runOnUiThread {
                        bitmap?.let {
                            viewModel.processImage(it)
                        } ?: run {
                            showError("Failed to process image")
                            binding.captureButton.isEnabled = true
                            binding.progressIndicator.visibility = View.GONE
                        }
                    }
                }
                
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Image capture failed", exception)
                    requireActivity().runOnUiThread {
                        showError("Failed to capture image: ${exception.message}")
                        binding.captureButton.isEnabled = true
                        binding.progressIndicator.visibility = View.GONE
                    }
                }
            }
        )
    }
    
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        
        // Rotate bitmap based on image rotation
        val matrix = Matrix()
        matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
        
        return Bitmap.createBitmap(
            bitmap,
            0, 0,
            bitmap.width, bitmap.height,
            matrix,
            true
        )
    }
    
    private fun updateFlashMode(enabled: Boolean) {
        camera?.cameraControl?.enableTorch(enabled)
    }
    
    private fun handleCaptureState(state: CameraViewModel.CaptureState) {
        when (state) {
            is CameraViewModel.CaptureState.Idle -> {
                binding.captureButton.isEnabled = true
                binding.progressIndicator.visibility = View.GONE
            }
            is CameraViewModel.CaptureState.Processing -> {
                binding.captureButton.isEnabled = false
                binding.progressIndicator.visibility = View.VISIBLE
            }
            is CameraViewModel.CaptureState.Success -> {
                binding.captureButton.isEnabled = true
                binding.progressIndicator.visibility = View.GONE
                
                // Navigate to review screen with receipt ID
                val action = CameraFragmentDirections.actionCameraToReview(state.receipt.id)
                findNavController().navigate(action)
            }
            is CameraViewModel.CaptureState.Warning -> {
                binding.captureButton.isEnabled = true
                binding.progressIndicator.visibility = View.GONE
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
            is CameraViewModel.CaptureState.Error -> {
                binding.captureButton.isEnabled = true
                binding.progressIndicator.visibility = View.GONE
                
                if (state.canRetry) {
                    showRetryDialog(state.message)
                } else {
                    showError(state.message)
                }
            }
        }
    }
    
    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Camera Permission Required")
            .setMessage("This app needs camera access to scan receipts. Please grant camera permission in settings.")
            .setPositiveButton("OK") { _, _ ->
                findNavController().navigateUp()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showRetryDialog(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Capture Failed")
            .setMessage(message)
            .setPositiveButton("Retry") { _, _ ->
                viewModel.retryProcessing()
            }
            .setNegativeButton("Cancel") { _, _ ->
                viewModel.resetCaptureState()
            }
            .show()
    }
    
    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
    
    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        cameraPermissionHelper.cleanup()
        _binding = null
    }
    
    companion object {
        private const val TAG = "CameraFragment"
    }
}
