package com.receiptscanner.presentation.camera

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.receiptscanner.databinding.ActivityCameraBinding
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Host activity for camera capture functionality.
 * Displays CameraFragment in fullscreen mode.
 */
@AndroidEntryPoint
class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar for fullscreen camera
        supportActionBar?.hide()

        Timber.d("CameraActivity created")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("CameraActivity destroyed")
    }
}
