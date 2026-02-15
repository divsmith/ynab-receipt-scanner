package com.ynab.receiptscanner.data.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.ynab.receiptscanner.BuildConfig
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runtime security checks
 * Detects potential security issues like rooted devices or debuggable builds
 */
@Singleton
class SecurityChecks @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "SecurityChecks"
    }
    
    data class SecurityStatus(
        val isRooted: Boolean,
        val isDebuggable: Boolean,
        val isEmulator: Boolean,
        val hasSuspiciousApps: Boolean,
        val warnings: List<String>
    ) {
        val isSecure: Boolean
            get() = !isRooted && !isDebuggable && warnings.isEmpty()
    }
    
    /**
     * Run all security checks
     */
    fun performSecurityChecks(): SecurityStatus {
        val warnings = mutableListOf<String>()
        
        val isRooted = checkIfRooted()
        if (isRooted) {
            warnings.add("Device appears to be rooted. This may pose security risks.")
        }
        
        val isDebuggable = checkIfDebuggable()
        if (isDebuggable && !BuildConfig.DEBUG) {
            warnings.add("App is debuggable in release mode. This should not happen.")
        }
        
        val isEmulator = checkIfEmulator()
        if (isEmulator) {
            Log.d(TAG, "Running on emulator")
        }
        
        val hasSuspiciousApps = checkForSuspiciousApps()
        if (hasSuspiciousApps) {
            warnings.add("Suspicious apps detected that may intercept data.")
        }
        
        return SecurityStatus(
            isRooted = isRooted,
            isDebuggable = isDebuggable,
            isEmulator = isEmulator,
            hasSuspiciousApps = hasSuspiciousApps,
            warnings = warnings
        )
    }
    
    /**
     * Check if device is rooted
     * WARNING: This is not foolproof and can be bypassed
     */
    private fun checkIfRooted(): Boolean {
        // Check for su binary
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        
        for (path in paths) {
            if (File(path).exists()) {
                Log.w(TAG, "Root binary found at: $path")
                return true
            }
        }
        
        // Check for common root management apps
        val rootApps = arrayOf(
            "com.noshufou.android.su",
            "com.thirdparty.superuser",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.zachspong.temprootremovejb",
            "com.ramdroid.appquarantine",
            "com.topjohnwu.magisk"
        )
        
        for (packageName in rootApps) {
            if (isPackageInstalled(packageName)) {
                Log.w(TAG, "Root management app found: $packageName")
                return true
            }
        }
        
        // Check build tags
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            Log.w(TAG, "Build contains test-keys")
            return true
        }
        
        return false
    }
    
    /**
     * Check if app is debuggable
     */
    private fun checkIfDebuggable(): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    
    /**
     * Check if running on emulator
     */
    private fun checkIfEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
                "google_sdk" == Build.PRODUCT)
    }
    
    /**
     * Check for suspicious apps that might intercept data
     */
    private fun checkForSuspiciousApps(): Boolean {
        val suspiciousApps = arrayOf(
            "com.chelpus.lackypatch",
            "com.dimonvideo.luckypatcher",
            "com.forpda.lp",
            "com.android.vending.billing.InAppBillingService.LOCK"
        )
        
        for (packageName in suspiciousApps) {
            if (isPackageInstalled(packageName)) {
                Log.w(TAG, "Suspicious app found: $packageName")
                return true
            }
        }
        
        return false
    }
    
    /**
     * Check if package is installed
     */
    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    /**
     * Verify app signature (basic tamper detection)
     */
    fun verifyAppSignature(): Boolean {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }
            
            // In production, compare with known signature hash
            // For now, just check that signature exists
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners?.isNotEmpty() == true
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures?.isNotEmpty() == true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify app signature", e)
            false
        }
    }
    
    /**
     * Log security status
     */
    fun logSecurityStatus() {
        val status = performSecurityChecks()
        Log.i(TAG, "Security Status:")
        Log.i(TAG, "  Rooted: ${status.isRooted}")
        Log.i(TAG, "  Debuggable: ${status.isDebuggable}")
        Log.i(TAG, "  Emulator: ${status.isEmulator}")
        Log.i(TAG, "  Suspicious Apps: ${status.hasSuspiciousApps}")
        Log.i(TAG, "  Secure: ${status.isSecure}")
        
        if (status.warnings.isNotEmpty()) {
            Log.w(TAG, "Security Warnings:")
            status.warnings.forEach { warning ->
                Log.w(TAG, "  - $warning")
            }
        }
    }
}
