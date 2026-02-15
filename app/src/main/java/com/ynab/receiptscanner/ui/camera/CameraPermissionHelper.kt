package com.ynab.receiptscanner.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * Helper class for handling camera permission requests
 * Simplifies camera permission workflow for CameraFragment
 */
class CameraPermissionHelper(private val fragment: Fragment) {
    
    companion object {
        const val CAMERA_PERMISSION = Manifest.permission.CAMERA
        
        /**
         * Check if camera permission is granted
         */
        fun hasPermission(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                CAMERA_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private var permissionLauncher: ActivityResultLauncher<String>? = null
    private var onPermissionResult: ((Boolean) -> Unit)? = null
    
    /**
     * Register permission launcher with callback
     * Call this in Fragment.onCreate() or Fragment.onViewCreated()
     */
    fun registerPermissionLauncher(
        launcher: ActivityResultLauncher<String>,
        onResult: (Boolean) -> Unit
    ) {
        permissionLauncher = launcher
        onPermissionResult = onResult
    }
    
    /**
     * Request camera permission
     * Triggers the registered callback with the result
     */
    fun requestPermission() {
        permissionLauncher?.launch(CAMERA_PERMISSION)
            ?: throw IllegalStateException("Permission launcher not registered")
    }
    
    /**
     * Check if permission should show rationale
     */
    fun shouldShowRationale(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            fragment.shouldShowRequestPermissionRationale(CAMERA_PERMISSION)
        } else {
            false
        }
    }
    
    /**
     * Clean up references
     * Call this in Fragment.onDestroyView()
     */
    fun cleanup() {
        permissionLauncher = null
        onPermissionResult = null
    }
}
