package com.receiptscanner.presentation.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.receiptscanner.R
import com.receiptscanner.databinding.FragmentCameraBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Fragment for capturing receipt images using CameraX.
 * Provides preview, framing overlay, and manual capture functionality.
 */
@AndroidEntryPoint
class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CameraViewModel by viewModels()

    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private lateinit var cameraExecutor: ExecutorService

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onPermissionGranted()
        } else {
            viewModel.onPermissionDenied()
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

        setupUI()
        observeViewModel()
        checkCameraPermission()
    }

    private fun setupUI() {
        // Close button
        binding.btnClose.setOnClickListener {
            requireActivity().finish()
        }

        // Flash toggle
        binding.btnFlash.setOnClickListener {
            viewModel.toggleFlash()
        }

        // Capture button
        binding.btnCapture.setOnClickListener {
            captureImage()
        }

        // Set initial guide text
        binding.framingOverlay.setGuideText(getString(R.string.ready_to_capture))
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.cameraState.collect { state ->
                        handleCameraState(state)
                    }
                }

                launch {
                    viewModel.isFlashEnabled.collect { enabled ->
                        updateFlashMode(enabled)
                    }
                }
            }
        }
    }

    private fun handleCameraState(state: CameraState) {
        Timber.d("Camera state: $state")

        when (state) {
            is CameraState.Idle -> {
                // Initial state, do nothing
            }

            is CameraState.PermissionRequired -> {
                showError(getString(R.string.camera_permission_required))
                binding.tvStatus.text = getString(R.string.camera_permission_rationale)
            }

            is CameraState.CameraInitializing -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.tvError.visibility = View.GONE
                binding.tvStatus.text = getString(R.string.camera_initializing)
                startCamera()
            }

            is CameraState.CameraReady -> {
                binding.progressBar.visibility = View.GONE
                binding.tvError.visibility = View.GONE
                binding.btnCapture.isEnabled = true
                binding.tvStatus.text = getString(R.string.ready_to_capture)
            }

            is CameraState.Capturing -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.btnCapture.isEnabled = false
                binding.tvStatus.text = getString(R.string.camera_capturing)
            }

            is CameraState.ImageCaptured -> {
                binding.progressBar.visibility = View.GONE
                binding.btnCapture.isEnabled = true
                Toast.makeText(
                    requireContext(),
                    getString(R.string.camera_success),
                    Toast.LENGTH_SHORT
                ).show()
                // TODO: Navigate to next screen with captured image URI
                requireActivity().finish()
            }

            is CameraState.Error -> {
                binding.progressBar.visibility = View.GONE
                binding.btnCapture.isEnabled = true
                showError(getString(R.string.camera_error, state.message))
            }
        }
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.onPermissionGranted()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Show rationale and request permission
                showError(getString(R.string.camera_permission_rationale))
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Preview use case
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.previewView.surfaceProvider)
                    }

                // Image capture use case
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()

                // Select back camera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                viewModel.onCameraInitialized()

            } catch (e: Exception) {
                Timber.e(e, "Camera initialization failed")
                viewModel.onCameraInitializationFailed(e.message ?: "Unknown error")
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun captureImage() {
        val imageCapture = imageCapture ?: run {
            Timber.e("ImageCapture is null")
            viewModel.onCameraInitializationFailed("Camera not ready")
            return
        }

        // Capture image to memory
        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val buffer: ByteBuffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)

                        // Pass image data to ViewModel
                        viewModel.captureImage(bytes)

                    } catch (e: Exception) {
                        Timber.e(e, "Error processing captured image")
                        viewModel.onCameraInitializationFailed(e.message ?: "Capture failed")
                    } finally {
                        image.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Timber.e(exception, "Image capture failed")
                    viewModel.onCameraInitializationFailed(
                        exception.message ?: "Capture failed"
                    )
                }
            }
        )
    }

    private fun updateFlashMode(enabled: Boolean) {
        camera?.cameraControl?.enableTorch(enabled)
        
        // Update flash button icon based on state
        val iconRes = if (enabled) {
            android.R.drawable.ic_menu_camera // TODO: Use proper flash on/off icons
        } else {
            android.R.drawable.ic_menu_camera
        }
        binding.btnFlash.setImageResource(iconRes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}

