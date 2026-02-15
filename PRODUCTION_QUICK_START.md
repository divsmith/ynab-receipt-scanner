# Production Features - Quick Start Guide

This guide explains how to use the new production-ready features in the YNAB Receipt Scanner app.

## 🚀 Quick Start

### 1. Building a Release APK

Before building, ensure you have:
1. Created a keystore (see below)
2. Configured `keystore.properties` (copy from template)

```bash
# Clean build
./gradlew clean

# Build release APK
./gradlew assembleRelease

# Or build AAB (for Play Store)
./gradlew bundleRelease

# Output locations:
# APK: app/build/outputs/apk/release/app-release.apk
# AAB: app/build/outputs/bundle/release/app-release.aab
```

### 2. Creating a Signing Key

```bash
keytool -genkey -v -keystore ynab-receipt-scanner.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias ynab-scanner-key
```

Then:
1. Copy `keystore.properties.template` to `keystore.properties`
2. Fill in your keystore details
3. **NEVER commit `keystore.properties` or the `.jks` file to git!**

---

## 📱 Using New Features

### Error Handling

```kotlin
// In ViewModels or Repositories
try {
    performOperation()
} catch (e: Exception) {
    errorHandler.handleError(e, "operation_context")
    val userMessage = errorHandler.getErrorMessage(e)
    // Show to user via SnackbarManager or UI state
}
```

### Snackbar Manager

```kotlin
// In Fragments
@Inject lateinit var snackbarManager: SnackbarManager

// Show simple message
snackbarManager.showMessage("Transaction saved")

// Show error with retry
snackbarManager.showError("Sync failed") {
    retrySync()
}

// Show with custom action
snackbarManager.showWithAction("Offline mode enabled", "Settings") {
    navigateToSettings()
}

// In Fragment's view lifecycle
viewLifecycleOwner.lifecycleScope.launch {
    snackbarManager.currentMessage.collect { message ->
        message?.let {
            snackbarManager.showSnackbar(requireView())
        }
    }
}
```

### Analytics

```kotlin
// Track events
analyticsManager.trackEvent(
    AnalyticsEvent.ReceiptScanned(
        processingTimeMs = 1234,
        imageSize = 512000,
        success = true
    )
)

// Track errors
analyticsManager.trackEvent(
    AnalyticsEvent.ErrorOccurred(
        errorType = "OCR_ERROR",
        errorMessage = "Failed to extract text",
        screen = "ScanFragment",
        canRetry = true
    )
)

// Set user properties (non-PII only!)
analyticsManager.setUserProperty("theme", "dark")
```

### Performance Monitoring

```kotlin
// Measure operation time
@Inject lateinit var performanceMonitor: PerformanceMonitor

// Automatic measurement
val result = performanceMonitor.measure("ocr_extraction") {
    performOCR(image)
}

// Manual timing
val timer = performanceMonitor.start("database_query")
// ... perform operation ...
timer.stop()

// Track specific metrics
performanceMonitor.trackOcrPerformance(durationMs = 2000, success = true)
performanceMonitor.trackApiCall("budgets", durationMs = 500, success = true)
```

### Data Validation

```kotlin
// Validate receipt
@Inject lateinit var receiptValidator: ReceiptValidator

val result = receiptValidator.validate(
    amount = BigDecimal("12.99"),
    date = LocalDate.now(),
    payee = "Coffee Shop",
    currency = "USD"
)

when (result) {
    is ReceiptValidator.ValidationResult.Valid -> {
        // Proceed with save
    }
    is ReceiptValidator.ValidationResult.Invalid -> {
        result.errors.forEach { error ->
            // Show error: "${error.field}: ${error.message}"
        }
    }
}

// Validate transaction before submission
@Inject lateinit var transactionValidator: TransactionValidator

val result = transactionValidator.validate(
    accountId = "account-123",
    amount = amount,
    date = date,
    payee = payee
)
```

### Security Checks

```kotlin
// Perform security checks on app start
@Inject lateinit var securityChecks: SecurityChecks

val status = securityChecks.performSecurityChecks()

if (!status.isSecure) {
    // Show warnings to user
    status.warnings.forEach { warning ->
        Log.w(TAG, warning)
        // Optionally show dialog
    }
}

// Check specific conditions
if (status.isRooted) {
    // Show warning about security risks
}
```

### Image Compression

```kotlin
// Compress image before processing
@Inject lateinit var imageCompressor: ImageCompressor

// Compress file
val result = imageCompressor.compressImage(
    inputFile = originalFile,
    outputFile = compressedFile,
    maxDimension = 2048,
    targetQuality = 85
)

Log.d(TAG, "Compressed to ${result.sizeBytes} bytes at quality ${result.quality}")

// Compress bitmap
val result = imageCompressor.compressBitmap(
    bitmap = originalBitmap,
    maxDimension = 2048,
    targetQuality = 85
)
```

### Memory Cache

```kotlin
// Cache preprocessed images
@Inject lateinit var memoryCache: MemoryCache

// Put in cache
val key = memoryCache.generatePreprocessedKey(receiptId)
memoryCache.put(key, processedBitmap)

// Get from cache
val cached = memoryCache.get(key)
if (cached != null) {
    // Use cached bitmap
} else {
    // Process and cache
}

// Get cache stats
val stats = memoryCache.getStats()
Log.d(TAG, "Cache hit rate: ${stats.hitRate * 100}%")

// Handle low memory
override fun onLowMemory() {
    super.onLowMemory()
    memoryCache.onLowMemory()
}
```

### Version Management

```kotlin
@Inject lateinit var versionManager: VersionManager

// Get version info
val version = versionManager.getFormattedVersion() // "v1.0.0 (1)"

// Check for first launch
if (versionManager.isFirstLaunchEver()) {
    // Show onboarding
}

if (versionManager.isFirstLaunchOfVersion()) {
    // Show what's new dialog
    val message = versionManager.getWhatsNewMessage()
    // Show to user
}
```

---

## 🔧 Configuration

### Analytics Opt-Out

Users can opt out of analytics in Settings. The preference is stored in `analytics_config.xml`:

```kotlin
// Check if analytics is enabled
if (analyticsManager.isAnalyticsEnabled()) {
    analyticsManager.trackEvent(event)
}

// Enable/disable
analyticsManager.setAnalyticsEnabled(enabled)
```

### Enable Firebase (Optional)

1. Add Firebase to your project:
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Add Android app
   - Download `google-services.json`
   - Place in `app/` directory

2. Uncomment in `app/build.gradle.kts`:
   ```kotlin
   // Firebase
   implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
   implementation("com.google.firebase:firebase-analytics-ktx")
   implementation("com.google.firebase:firebase-crashlytics-ktx")
   ```

3. Add plugin:
   ```kotlin
   plugins {
       // ...
       id("com.google.gms.google-services")
       id("com.google.firebase.crashlytics")
   }
   ```

4. Uncomment Firebase implementations in:
   - `FirebaseAnalyticsImpl.kt`
   - `CrashlyticsManager.kt`

### Cleanup Worker Configuration

The cleanup worker runs weekly when device is charging. To configure:

```kotlin
// Change retention period (default 30 days)
val cleanupRequest = OneTimeWorkRequestBuilder<CleanupWorker>()
    .setInputData(workDataOf("retention_days" to 60))
    .build()
```

---

## 📊 Monitoring

### Performance Metrics

Performance metrics are automatically tracked for:
- OCR processing (threshold: 5 seconds)
- Image preprocessing (threshold: 3 seconds)
- API calls (threshold: 10 seconds)
- Database queries (threshold: 1 second)

Slow operations are logged and sent to analytics.

### Crash Reports

Non-fatal exceptions are logged to Crashlytics:

```kotlin
@Inject lateinit var crashlyticsManager: CrashlyticsManager

try {
    riskyOperation()
} catch (e: Exception) {
    crashlyticsManager.logException(e)
    crashlyticsManager.setCustomKey("operation", "risky_op")
    // Handle error
}
```

### Launch Time Tracking

App launch times are automatically tracked:
- Cold start: < 2 seconds (target)
- Warm start: < 1 second (target)

Metrics are sent to analytics on each launch.

---

## 🧪 Testing

### Test Release Build

```bash
# Build release
./gradlew assembleRelease

# Install on device
adb install app/build/outputs/apk/release/app-release.apk

# Test thoroughly:
# - All main flows (scan, review, submit)
# - Error handling
# - Offline mode
# - Performance
# - ProGuard obfuscation (check logs)
```

### Test ProGuard Rules

```bash
# Build release and check for missing rules
./gradlew assembleRelease

# If you see errors like:
# "Missing classes detected while running R8"
# Add appropriate rules to proguard-rules.pro
```

### Debug LeakCanary (Debug builds only)

LeakCanary is automatically enabled in debug builds to detect memory leaks.

---

## 📝 Release Checklist

Before releasing, complete the **release-checklist.md**:

- [ ] Version bumped
- [ ] Tests passing
- [ ] ProGuard tested
- [ ] Signing configured
- [ ] Store listing ready
- [ ] Privacy policy updated

---

## 🐛 Troubleshooting

### "Signing key not configured"
- Ensure `keystore.properties` exists and has correct values
- Check that keystore file path is correct

### "Firebase not configured" warnings in logs
- This is normal if Firebase is not set up
- App works fine without Firebase (stub implementation)
- To enable, follow Firebase setup steps above

### ProGuard issues
- Check `proguard-rules.pro` for missing rules
- Test release build thoroughly
- Use `./gradlew assembleRelease --stacktrace` for detailed errors

### High memory usage
- Memory cache is limited to 1/8 of available memory
- Use `memoryCache.onTrimMemory()` in low memory situations
- Check for bitmap leaks with LeakCanary (debug build)

---

## 📚 Additional Resources

- [PRODUCTION_FEATURES.md](PRODUCTION_FEATURES.md) - Complete feature documentation
- [release-checklist.md](release-checklist.md) - Pre-release checklist
- [ProGuard documentation](https://www.guardsquare.com/manual)
- [Firebase documentation](https://firebase.google.com/docs/android/setup)

---

## 🎯 Next Steps

1. ✅ Build and test release APK
2. ✅ Complete release checklist
3. ✅ Configure Firebase (optional)
4. ✅ Test on multiple devices
5. ✅ Submit to Play Store

**Your app is production-ready! 🚀**
