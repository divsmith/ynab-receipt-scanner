# Troubleshooting Guide

## Table of Contents
- [Common Issues](#common-issues)
- [Build & Setup Issues](#build--setup-issues)
- [Runtime Errors](#runtime-errors)
- [OAuth & Authentication](#oauth--authentication)
- [OCR Issues](#ocr-issues)
- [Sync Failures](#sync-failures)
- [Performance Issues](#performance-issues)
- [Camera Problems](#camera-problems)
- [Database Issues](#database-issues)
- [Network Issues](#network-issues)
- [Diagnostic Tools](#diagnostic-tools)

## Common Issues

### App Crashes on Startup

**Symptoms**: App closes immediately after opening or shows white screen

**Possible Causes**:
1. Corrupted app data
2. Insufficient memory
3. Incompatible Android version
4. Missing Google Play Services

**Solutions**:

**Solution 1: Clear App Cache**
```
Device Settings > Apps > YNAB Receipt Scanner > Storage > Clear Cache
```

**Solution 2: Clear App Data** (⚠️ Loses local receipts)
```
Device Settings > Apps > YNAB Receipt Scanner > Storage > Clear Data
```

**Solution 3: Check Android Version**
```
Minimum required: Android 7.0 (API 24)
Check: Settings > About Phone > Android Version
```

**Solution 4: Update Google Play Services**
```
Play Store > Search "Google Play Services" > Update
```

**Solution 5: Reinstall**
```bash
# Uninstall
adb uninstall com.ynab.receiptscanner

# Reinstall from APK
adb install app-release.apk
```

**Check Logs**:
```bash
# View crash logs
adb logcat -b crash

# Filter for app logs
adb logcat | grep "ynab.receiptscanner"
```

### App Running Slowly

**Symptoms**: UI lags, animations stutter, slow response time

**Possible Causes**:
1. Low device memory
2. Too many cached receipts
3. Background sync running
4. Old Android version

**Solutions**:

**Solution 1: Clear Cache**
```
Settings > Privacy & Data > Clear Cache
```

**Solution 2: Delete Old Receipts**
```
Settings > Privacy & Data > Delete Synced Receipts
```

**Solution 3: Close Background Apps**
```
Recent apps button > Close all other apps
```

**Solution 4: Restart Device**
```
Power button > Restart
```

**Performance Monitoring**:
```bash
# Check memory usage
adb shell dumpsys meminfo com.ynab.receiptscanner

# Check CPU usage
adb shell top | grep ynab
```

### Features Not Working

**Symptoms**: Certain features don't respond or show errors

**General Solutions**:
1. Update to latest version from Play Store
2. Check permissions in Settings > Apps > Permissions
3. Restart app
4. Clear app cache
5. Reinstall if necessary

## Build & Setup Issues

### Gradle Sync Failed

**Error**: `Gradle sync failed: Could not resolve dependencies`

**Solutions**:

**Solution 1: Check Internet Connection**
```bash
# Test connectivity
ping google.com

# Test Gradle connectivity
./gradlew --version
```

**Solution 2: Invalidate Caches**
```
Android Studio > File > Invalidate Caches > Invalidate and Restart
```

**Solution 3: Clean and Rebuild**
```bash
./gradlew clean
./gradlew build --refresh-dependencies
```

**Solution 4: Update Gradle**
```bash
# In gradle/wrapper/gradle-wrapper.properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.4-bin.zip
```

### Build Failed: Compilation Error

**Error**: `Compilation failed; see the compiler error output for details.`

**Solutions**:

**Solution 1: Check JDK Version**
```bash
# Check Java version
java -version

# Should be JDK 17
# If not, set JAVA_HOME
export JAVA_HOME=/path/to/jdk17
```

**Solution 2: Check Kotlin Plugin**
```
Android Studio > Settings > Plugins > Kotlin > Update
```

**Solution 3: Clean Build**
```bash
./gradlew clean
rm -rf build .gradle
./gradlew build
```

**Solution 4: Update Dependencies**
```kotlin
// In gradle/libs.versions.toml
# Update all versions to latest compatible
```

### Missing SDK or Build Tools

**Error**: `Failed to find target with hash string 'android-34'`

**Solution**:
```
Android Studio > Settings > Appearance & Behavior > 
System Settings > Android SDK

Install:
- Android SDK Platform 34
- Android SDK Build-Tools 34.0.0
```

Or via command line:
```bash
sdkmanager "platforms;android-34"
sdkmanager "build-tools;34.0.0"
```

### ProGuard Errors in Release Build

**Error**: `Warning: can't find referenced class`

**Solution**: Update `proguard-rules.pro`:
```proguard
# Add keep rules for the missing classes
-keep class com.your.missing.Class { *; }

# Common rules for Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# For Moshi
-keep class com.squareup.moshi.** { *; }

# For Room
-keep class * extends androidx.room.RoomDatabase
```

## Runtime Errors

### NullPointerException

**Error**: `java.lang.NullPointerException: Attempt to invoke virtual method on a null object reference`

**Common Causes**:

**1. Uninitialized ViewModel**:
```kotlin
// ❌ Bad
class MyFragment : Fragment() {
    lateinit var viewModel: MyViewModel  // Not initialized!
}

// ✅ Good
@AndroidEntryPoint
class MyFragment : Fragment() {
    private val viewModel: MyViewModel by viewModels()
}
```

**2. Null Receipt Fields**:
```kotlin
// ❌ Bad
val amount = receipt.amount.toString()  // If amount is null: NPE!

// ✅ Good
val amount = receipt.amount?.toString() ?: "0.00"
```

**3. Missing Hilt Annotation**:
```kotlin
// ❌ Bad
class MyFragment : Fragment()

// ✅ Good
@AndroidEntryPoint
class MyFragment : Fragment()
```

### IllegalStateException

**Error**: `IllegalStateException: Cannot call this method before onCreate()`

**Solution**: Check fragment/activity lifecycle:
```kotlin
// ❌ Bad: Accessing view before created
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding.textView.text = "Hello"  // View not ready yet!
}

// ✅ Good: Access view in appropriate lifecycle method
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.textView.text = "Hello"  // View is ready
}
```

### Activity Not Found

**Error**: `android.content.ActivityNotFoundException: No Activity found to handle Intent`

**Solution**: Check `AndroidManifest.xml`:
```xml
<activity
    android:name=".ui.MainActivity"
    android:exported="true">  <!-- Add this for launcher activity -->
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

## OAuth & Authentication

### OAuth Redirect Not Working

**Symptoms**: After YNAB login, doesn't return to app

**Solutions**:

**Solution 1: Check Redirect URI**

In YNAB Developer Settings:
```
Redirect URI: ynab-receipt-scanner://oauth-callback
```

In `AndroidManifest.xml`:
```xml
<activity android:name=".ui.onboarding.OnboardingActivity">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="ynab-receipt-scanner"
            android:host="oauth-callback" />
    </intent-filter>
</activity>
```

**Solution 2: Test Deep Link**:
```bash
adb shell am start -W -a android.intent.action.VIEW \
  -d "ynab-receipt-scanner://oauth-callback?code=test123"
```

### 401 Unauthorized Error

**Error**: `HTTP 401 Unauthorized` when calling YNAB API

**Possible Causes**:
1. Token expired
2. Token revoked
3. Invalid token
4. Network time incorrect

**Solutions**:

**Solution 1: Sign Out and Sign In**
```
Settings > Account > Sign Out > Connect to YNAB
```

**Solution 2: Check Token Storage**:
```kotlin
// In AuthPreferences
val token = authPreferences.getAccessToken()
Log.d("Auth", "Token: ${token?.take(10)}...") // Check if exists
```

**Solution 3: Verify Token with YNAB**:
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  https://api.youneedabudget.com/v1/user
```

**Solution 4: Check Device Time**:
```
Settings > System > Date & Time > Use network-provided time
```

### OAuth Token Exchange Failed

**Error**: `Failed to exchange authorization code for token`

**Solutions**:

**Solution 1: Check Client Credentials**:
```kotlin
// In Constants.kt or local.properties
val clientId = "your_client_id"  // Must match YNAB app
val clientSecret = "your_client_secret"
```

**Solution 2: Check Network Request**:
```kotlin
// Enable OkHttp logging
val logging = HttpLoggingInterceptor()
logging.level = HttpLoggingInterceptor.Level.BODY

val client = OkHttpClient.Builder()
    .addInterceptor(logging)
    .build()
```

**Solution 3: Verify YNAB App Settings**:
- Go to YNAB Developer Settings
- Check if app is enabled
- Verify redirect URI matches exactly

## OCR Issues

### OCR Returns Empty Result

**Symptoms**: No text extracted from receipt image

**Solutions**:

**Solution 1: Check Image Quality**:
```kotlin
// Log image size and format
Log.d("OCR", "Image: ${bitmap.width}x${bitmap.height}, ${bitmap.config}")

// Minimum recommended: 800x600, ARGB_8888
```

**Solution 2: Preprocess Image**:
```kotlin
fun preprocessImage(bitmap: Bitmap): Bitmap {
    // Convert to grayscale
    val grayscale = toGrayscale(bitmap)
    
    // Increase contrast
    val enhanced = adjustContrast(grayscale, 1.5f)
    
    return enhanced
}
```

**Solution 3: Check ML Kit Initialization**:
```kotlin
val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

// Check if initialized
recognizer.process(InputImage.fromBitmap(bitmap, 0))
    .addOnSuccessListener { text ->
        Log.d("OCR", "Text blocks: ${text.textBlocks.size}")
    }
    .addOnFailureListener { e ->
        Log.e("OCR", "OCR failed", e)
    }
```

### OCR Extracting Wrong Data

**Symptoms**: Merchant name, amount, or date is incorrect

**Solutions**:

**Solution 1: Improve Photo Quality**:
- Better lighting
- Steady hands
- Sharp focus
- Fill frame with receipt

**Solution 2: Add Receipt Format Detector**:
```kotlin
class ReceiptFormatDetector {
    fun detectFormat(text: String): ReceiptFormat {
        return when {
            text.contains("WALMART", ignoreCase = true) -> ReceiptFormat.WALMART
            text.contains("TARGET", ignoreCase = true) -> ReceiptFormat.TARGET
            else -> ReceiptFormat.GENERIC
        }
    }
}
```

**Solution 3: Adjust Parsing Logic**:
```kotlin
// More lenient amount parsing
val amountPatterns = listOf(
    Regex("""\$\s*(\d+\.\d{2})"""),  // $12.34
    Regex("""(\d+\.\d{2})\s*USD"""),  // 12.34 USD
    Regex("""(?i)total.*?(\d+\.\d{2})""")  // TOTAL 12.34
)
```

### ML Kit Module Not Downloaded

**Error**: `MlKitException: Waiting for the text recognition model to be downloaded`

**Solutions**:

**Solution 1: Wait and Retry**:
```kotlin
recognizer.process(image)
    .addOnFailureListener { e ->
        if (e is MlKitException) {
            // Show message: "Downloading OCR model, please wait..."
            // Retry after 30 seconds
        }
    }
```

**Solution 2: Check Internet Connection**:
```kotlin
if (!networkMonitor.isOnline()) {
    showError("OCR model requires internet for initial download")
}
```

**Solution 3: Pre-download in Background**:
```kotlin
// In Application.onCreate()
TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    .close()  // Triggers download without processing
```

## Sync Failures

### Transactions Stuck in "Pending"

**Symptoms**: Transactions won't sync to YNAB

**Solutions**:

**Solution 1: Check Internet Connection**:
```kotlin
if (networkMonitor.isOnline()) {
    // Manually trigger sync
    syncScheduler.syncNow()
}
```

**Solution 2: Check Sync Status**:
```bash
# View sync logs
adb logcat | grep "SyncWorker"
```

**Solution 3: Clear Sync Queue and Retry**:
```
Settings > Sync > Clear Sync Queue
```

**Solution 4: Check YNAB API Status**:
- Visit: https://status.youneedabudget.com/
- If down, wait and retry

### "Duplicate Transaction" Error

**Error**: `409 Conflict: Duplicate transaction`

**Explanation**: YNAB detected a similar transaction already exists

**Solutions**:

**Solution 1: Check Existing Transactions**:
- Open YNAB
- Look for duplicate on same date with same amount
- Delete if actually duplicate

**Solution 2: Modify Transaction Slightly**:
- Change amount by $0.01
- Or modify memo
- Or try different date

**Solution 3: Implement Duplicate Detection**:
```kotlin
suspend fun checkDuplicate(
    accountId: String,
    date: String,
    amount: Long
): Boolean {
    return ynabApi.getTransactions(accountId)
        .data.transactions
        .any { it.date == date && it.amount == amount }
}
```

### "Invalid Account" or "Invalid Category"

**Error**: `400 Bad Request: Invalid account_id` or `Invalid category_id`

**Cause**: Account or category was deleted in YNAB

**Solutions**:

**Solution 1: Refresh Data**:
```
Settings > Sync > Refresh Accounts & Categories
```

**Solution 2: Reselect Account/Category**:
- Open transaction
- Select valid account
- Select valid category
- Retry sync

**Solution 3: Clear Local Cache**:
```kotlin
// Clear cached accounts/categories
accountDao.deleteAll()
categoryDao.deleteAll()

// Re-fetch from API
refreshUseCase.invoke()
```

## Performance Issues

### Slow App Startup

**Symptoms**: App takes 5+ seconds to open

**Solutions**:

**Solution 1**: Check for blocking operations in `Application.onCreate()`:
```kotlin
class YnabReceiptApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // ❌ Bad: Blocking work in onCreate()
        database.receiptDao().getAll()  // Don't do this!
        
        // ✅ Good: Defer or run in background
        lifecycleScope.launch {
            database.receiptDao().getAll()
        }
    }
}
```

**Solution 2: Enable R8 Full Mode**:
```gradle
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }
}
```

**Solution 3: Reduce Database Size**:
```
Settings > Privacy & Data > Delete Old Receipts
```

### High Memory Usage

**Symptoms**: App uses >200MB RAM, or OutOfMemoryError

**Solutions**:

**Solution 1: Scale Down Images**:
```kotlin
fun loadReceiptImage(path: String): Bitmap {
    return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply {
        inSampleSize = 2  // Load at 1/2 resolution
        inPreferredConfig = Bitmap.Config.RGB_565  // Use less memory
    })
}
```

**Solution 2: Limit Image Cache Size**:
```kotlin
val cacheSize = 10 * 1024 * 1024  // 10MB
val imageCache = LruCache<String, Bitmap>(cacheSize)
```

**Solution 3: Profile Memory**:
```bash
# Capture heap dump
adb shell am dumpheap com.ynab.receiptscanner /data/local/tmp/heap.hprof

# Pull and analyze
adb pull /data/local/tmp/heap.hprof
# Open in Android Studio Memory Profiler
```

### Slow OCR Processing

**Symptoms**: OCR takes >10 seconds

**Solutions**:

**Solution 1: Resize Image Before Processing**:
```kotlin
fun resizeForOcr(bitmap: Bitmap): Bitmap {
    val maxDimension = 1600
    val ratio = maxDimension.toFloat() / max(bitmap.width, bitmap.height)
    
    if (ratio < 1) {
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt(),
            (bitmap.height * ratio).toInt(),
            true
        )
    }
    return bitmap
}
```

**Solution 2: Use Lower Resolution Camera**:
```
Settings > Camera > Resolution > Standard (instead of High)
```

**Solution 3: Process in Background**:
```kotlin
viewModelScope.launch(Dispatchers.Default) {
    val result = ocrRepository.extractText(imagePath)
    withContext(Dispatchers.Main) {
        // Update UI
    }
}
```

## Camera Problems

### Camera Not Opening

**Error**: `Failed to open camera` or camera preview shows black screen

**Solutions**:

**Solution 1: Check Permissions**:
```bash
# Grant camera permission
adb shell pm grant com.ynab.receiptscanner android.permission.CAMERA
```

**Solution 2: Close Other Camera Apps**:
```bash
# List apps using camera
adb shell dumpsys media.camera | grep "client"

# Force stop
adb shell am force-stop com.other.camera.app
```

**Solution 3: Check CameraX Implementation**:
```kotlin
val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

cameraProviderFuture.addListener({
    try {
        val cameraProvider = cameraProviderFuture.get()
        bindCamera(cameraProvider)
    } catch (e: Exception) {
        Log.e("Camera", "Failed to init camera", e)
        showError("Camera error: ${e.message}")
    }
}, ContextCompat.getMainExecutor(context))
```

### Camera Preview Rotated or Stretched

**Symptoms**: Preview looks distorted or sideways

**Solution**: Set proper aspect ratio and rotation:
```kotlin
val preview = Preview.Builder()
    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
    .setTargetRotation(viewBinding.previewView.display.rotation)
    .build()

preview.setSurfaceProvider(viewBinding.previewView.surfaceProvider)
```

### Photo Quality Poor

**Symptoms**: Captured images are blurry or low resolution

**Solutions**:

**Solution 1: Use Higher Resolution**:
```kotlin
val imageCapture = ImageCapture.Builder()
    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
    .setTargetRotation(rotation)
    .build()
```

**Solution 2: Enable Auto-Focus**:
```kotlin
val cameraControl = camera.cameraControl
cameraControl.startFocusAndMetering(
    FocusMeteringAction.Builder(
        SurfaceOrientedMeteringPointFactory(1f, 1f).createPoint(.5f, .5f)
    ).build()
)
```

## Database Issues

### Database Corrupted

**Error**: `SQLiteException: database disk image is malformed`

**Solutions**:

**Solution 1: Clear App Data** (⚠️ Loses local receipts):
```
Settings > Apps > YNAB Receipt Scanner > Storage > Clear Data
```

**Solution 2: Export Data First** (if possible):
```kotlin
// In ReceiptDao
@Query("SELECT * FROM receipts")
suspend fun exportAll(): List<ReceiptEntity>
```

**Solution 3: Implement Database Recovery**:
```kotlin
class DatabaseRecovery {
    fun recoverDatabase(context: Context) {
        val dbPath = context.getDatabasePath("ynab_receipt_scanner.db")
        val backupPath = File(context.filesDir, "db_backup.db")
        
        try {
            dbPath.copyTo(backupPath, overwrite = true)
            // Try to repair or recreate
        } catch (e: Exception) {
            Log.e("DB", "Recovery failed", e)
        }
    }
}
```

### Migration Failed

**Error**: `IllegalStateException: A migration from X to Y was required but not found`

**Solution**: Add migration in `DatabaseModule`:
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE receipts ADD COLUMN new_field TEXT")
    }
}

Room.databaseBuilder(context, YnabDatabase::class.java, "ynab.db")
    .addMigrations(MIGRATION_1_2)
    .build()
```

## Network Issues

### Timeout Errors

**Error**: `SocketTimeoutException: timeout`

**Solutions**:

**Solution 1: Increase Timeout**:
```kotlin
val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()
```

**Solution 2: Retry with Exponential Backoff**:
```kotlin
class RetryInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)
        var tryCount = 0
        
        while (!response.isSuccessful && tryCount < 3) {
            tryCount++
            delay(1000L * tryCount)  // Exponential backoff
            response = chain.proceed(request)
        }
        
        return response
    }
}
```

### SSL/TLS Errors

**Error**: `SSLHandshakeException: Handshake failed`

**Solutions**:

**Solution 1: Update System WebView**:
```
Play Store > Search "Android System WebView" > Update
```

**Solution 2: Check Network Security Config**:
```xml
<!-- res/xml/network_security_config.xml -->
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">api.youneedabudget.com</domain>
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </domain-config>
</network-security-config>
```

## Diagnostic Tools

### Enable Debug Logging

```kotlin
// In build.gradle.kts
android {
    buildTypes {
        debug {
            buildConfigField("boolean", "DEBUG_MODE", "true")
        }
    }
}

// In code
if (BuildConfig.DEBUG_MODE) {
    Timber.plant(Timber.DebugTree())
}
```

### Collect Logs

```bash
# Clear logs
adb logcat -c

# Start collecting
adb logcat > app_logs.txt

# Or filter for app
adb logcat | grep "com.ynab.receiptscanner"
```

### Network Debugging

**Use Android Studio's Network Inspector**:
1. Run app in debug mode
2. View > Tool Windows > App Inspection
3. Network Inspector tab
4. Make API requests
5. View request/response details

**Or use Charles Proxy**:
1. Install Charles Proxy
2. Install SSL certificate on device
3. Configure proxy settings
4. View all HTTP/HTTPS traffic

### Database Inspection

```bash
# Pull database from device
adb pull /data/data/com.ynab.receiptscanner/databases/ynab_receipt_scanner.db

# Open with SQLite browser
sqlite3 ynab_receipt_scanner.db

# Or use Android Studio Database Inspector
# View > Tool Windows > App Inspection > Database Inspector
```

---

## Need More Help?

If none of these solutions work:

1. **Check Existing Issues**: [GitHub Issues](link)
2. **Create New Issue**: Include:
   - Device model and Android version
   - App version
   - Steps to reproduce
   - Error logs
   - Screenshots
3. **Email Support**: support@example.com

## Related Documentation

- [User Guide](USER_GUIDE.md) - How to use the app
- [FAQ](FAQ.md) - Frequently asked questions
- [Developer Guide](DEVELOPER_GUIDE.md) - Development setup
