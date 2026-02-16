package com.receiptscanner

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application class for Receipt Scanner.
 * 
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation and
 * set up the application-level dependency injection container.
 */
@HiltAndroidApp
class ReceiptScannerApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
