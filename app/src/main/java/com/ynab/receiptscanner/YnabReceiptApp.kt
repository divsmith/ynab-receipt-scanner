package com.ynab.receiptscanner

import android.app.Application
import android.os.StrictMode
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.ynab.receiptscanner.data.analytics.AnalyticsEvent
import com.ynab.receiptscanner.data.analytics.AnalyticsManager
import com.ynab.receiptscanner.data.analytics.CrashlyticsManager
import com.ynab.receiptscanner.data.security.SecurityChecks
import com.ynab.receiptscanner.performance.LaunchTimeTracker
import com.ynab.receiptscanner.util.VersionManager
import com.ynab.receiptscanner.worker.CleanupWorker
import com.ynab.receiptscanner.worker.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class for YNAB Receipt Scanner
 * Entry point for Hilt dependency injection, WorkManager configuration,
 * and app-level initialization
 */
@HiltAndroidApp
class YnabReceiptApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var syncScheduler: SyncScheduler
    
    @Inject
    lateinit var analyticsManager: AnalyticsManager
    
    @Inject
    lateinit var crashlyticsManager: CrashlyticsManager
    
    @Inject
    lateinit var launchTimeTracker: LaunchTimeTracker
    
    @Inject
    lateinit var versionManager: VersionManager
    
    @Inject
    lateinit var securityChecks: SecurityChecks

    override fun onCreate() {
        // Track launch time
        launchTimeTracker.onApplicationCreate()
        
        super.onCreate()
        
        // Enable StrictMode in debug builds
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
        
        // Initialize Crashlytics
        initializeCrashlytics()
        
        // Set up global exception handler
        setupGlobalExceptionHandler()
        
        // Perform security checks
        performSecurityChecks()
        
        // Log app version
        logAppVersion()
        
        // Track app launch
        trackAppLaunch()
        
        // Initialize WorkManager with Hilt Worker Factory
        WorkManager.initialize(this, workManagerConfiguration)
        
        // Schedule periodic sync on app startup
        syncScheduler.schedulePeriodicSync()
        
        // Schedule periodic cleanup
        CleanupWorker.schedule(this)
        
        Log.i(TAG, "YNAB Receipt Scanner initialized successfully")
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.INFO)
            .build()
    
    /**
     * Enable StrictMode for debug builds
     */
    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
        
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
        
        Log.d(TAG, "StrictMode enabled")
    }
    
    /**
     * Initialize Crashlytics
     */
    private fun initializeCrashlytics() {
        try {
            crashlyticsManager.setCustomKey("app_version", versionManager.getVersionName())
            crashlyticsManager.setCustomKey("version_code", versionManager.getVersionCode().toInt())
            crashlyticsManager.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            crashlyticsManager.log("App initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Crashlytics", e)
        }
    }
    
    /**
     * Set up global exception handler
     */
    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)
                crashlyticsManager.logException(throwable)
                crashlyticsManager.setCustomKey("thread_name", thread.name)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log uncaught exception", e)
            } finally {
                // Call default handler
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
    
    /**
     * Perform security checks
     */
    private fun performSecurityChecks() {
        try {
            val securityStatus = securityChecks.performSecurityChecks()
            
            if (!securityStatus.isSecure) {
                Log.w(TAG, "Security warnings detected:")
                securityStatus.warnings.forEach { warning ->
                    Log.w(TAG, "  - $warning")
                    crashlyticsManager.log("Security warning: $warning")
                }
            }
            
            // Log security status to analytics
            analyticsManager.setUserProperty("device_rooted", securityStatus.isRooted.toString())
            analyticsManager.setUserProperty("is_emulator", securityStatus.isEmulator.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Security checks failed", e)
            crashlyticsManager.logException(e)
        }
    }
    
    /**
     * Log app version
     */
    private fun logAppVersion() {
        val version = versionManager.getFormattedVersion()
        Log.i(TAG, "App version: $version")
        crashlyticsManager.log("App version: $version")
        
        if (versionManager.isFirstLaunchEver()) {
            Log.i(TAG, "First launch ever")
            analyticsManager.setUserProperty("first_launch_version", versionManager.getVersionName())
        }
        
        if (versionManager.isFirstLaunchOfVersion()) {
            Log.i(TAG, "First launch of version ${versionManager.getVersionName()}")
        }
    }
    
    /**
     * Track app launch analytics
     */
    private fun trackAppLaunch() {
        try {
            analyticsManager.trackEvent(
                AnalyticsEvent.AppLaunched(
                    isFirstLaunch = versionManager.isFirstLaunchEver(),
                    coldStart = true
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track app launch", e)
        }
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "Low memory warning received")
        crashlyticsManager.log("Low memory warning")
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.d(TAG, "Trim memory level: $level")
        
        when (level) {
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                Log.w(TAG, "Memory critically low")
                crashlyticsManager.log("Memory critically low")
            }
        }
    }
    
    companion object {
        private const val TAG = "YnabReceiptApp"
    }
}

