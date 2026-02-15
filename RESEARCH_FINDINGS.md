# Research Findings: Android Receipt Scanner with YNAB Integration

## 1. Android Project Structure

### Standard Kotlin Android App Structure
```
app/
├── src/
│   ├── main/
│   │   ├── java/com/yourpackage/ynabscanner/
│   │   │   ├── ui/
│   │   │   │   ├── camera/          # CameraActivity, CameraViewModel
│   │   │   │   ├── review/          # ReviewActivity, ReviewViewModel
│   │   │   │   ├── auth/            # YNABAuthActivity, AuthViewModel
│   │   │   │   └── common/          # Shared UI components
│   │   │   ├── data/
│   │   │   │   ├── local/           # Room database, DAOs, entities
│   │   │   │   ├── remote/          # YNAB API service, DTOs
│   │   │   │   ├── repository/      # Repository implementations
│   │   │   │   └── security/        # Keystore wrapper
│   │   │   ├── domain/
│   │   │   │   ├── model/           # Domain models (Receipt, Transaction)
│   │   │   │   ├── usecase/         # Business logic use cases
│   │   │   │   └── repository/      # Repository interfaces
│   │   │   ├── ml/
│   │   │   │   ├── ocr/             # ML Kit integration, text recognition
│   │   │   │   ├── preprocessing/   # Image enhancement, deskewing
│   │   │   │   └── parser/          # Field extraction logic
│   │   │   ├── sync/
│   │   │   │   ├── worker/          # WorkManager for background sync
│   │   │   │   └── queue/           # Offline queue management
│   │   │   └── di/                  # Dependency injection (Hilt modules)
│   │   ├── res/
│   │   │   ├── layout/              # XML layouts
│   │   │   ├── values/              # Strings, colors, themes
│   │   │   └── drawable/            # Icons, images
│   │   └── AndroidManifest.xml
│   ├── test/                        # Unit tests
│   │   └── java/com/yourpackage/ynabscanner/
│   │       ├── ml/parser/           # Parser unit tests
│   │       ├── domain/usecase/      # Use case tests
│   │       └── data/repository/     # Repository tests
│   └── androidTest/                 # Instrumented tests
│       └── java/com/yourpackage/ynabscanner/
│           ├── ui/                  # Espresso UI tests
│           └── data/local/          # Database tests
├── build.gradle.kts                 # Module-level Gradle config
└── proguard-rules.pro              # ProGuard rules for release builds

build.gradle.kts                     # Project-level Gradle config
settings.gradle.kts
gradle.properties
```

### Key Configuration Files

**settings.gradle.kts:**
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "YNAB Receipt Scanner"
include(":app")
```

**AndroidManifest.xml - Key Permissions:**
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-feature android:name="android.hardware.camera" android:required="true" />
<uses-feature android:name="android.hardware.camera.autofocus" android:required="false" />
```

---

## 2. ML Kit Text Recognition

### Integration Approach

**Dependencies (build.gradle.kts):**
```kotlin
dependencies {
    // ML Kit Text Recognition (on-device)
    implementation("com.google.mlkit:text-recognition:16.0.0")
    
    // For image processing
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    
    // OpenCV for advanced preprocessing (optional but recommended)
    implementation("org.opencv:opencv:4.8.0")
    // Alternatively, use built-in Android Bitmap operations
}
```

### Basic ML Kit Integration Pattern
```kotlin
class TextRecognizer(private val context: Context) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    suspend fun recognizeText(imageProxy: ImageProxy): RecognitionResult {
        return suspendCoroutine { continuation ->
            val mediaImage = imageProxy.image ?: return@suspendCoroutine
            val inputImage = InputImage.fromMediaImage(
                mediaImage, 
                imageProxy.imageInfo.rotationDegrees
            )
            
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    continuation.resume(RecognitionResult.Success(visionText))
                }
                .addOnFailureListener { e ->
                    continuation.resume(RecognitionResult.Error(e))
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }
}
```

### Image Preprocessing Libraries & Techniques

**1. Basic Android Bitmap Operations:**
```kotlin
class ImagePreprocessor {
    fun preprocessReceipt(bitmap: Bitmap): Bitmap {
        return bitmap
            .convertToGrayscale()
            .increaseContrast(1.2f)
            .sharpen()
            .cropToContent()
    }
    
    private fun Bitmap.convertToGrayscale(): Bitmap {
        val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(colorMatrix) }
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(result).drawBitmap(this, 0f, 0f, paint)
        return result
    }
    
    private fun Bitmap.increaseContrast(factor: Float): Bitmap {
        val colorMatrix = ColorMatrix(floatArrayOf(
            factor, 0f, 0f, 0f, 0f,
            0f, factor, 0f, 0f, 0f,
            0f, 0f, factor, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        // Apply matrix...
    }
}
```

**2. OpenCV for Advanced Preprocessing:**
```kotlin
class OpenCVPreprocessor {
    fun deskewImage(mat: Mat): Mat {
        // Convert to grayscale
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)
        
        // Apply threshold
        val thresh = Mat()
        Imgproc.threshold(gray, thresh, 0.0, 255.0, 
            Imgproc.THRESH_BINARY_INV or Imgproc.THRESH_OTSU)
        
        // Find contours and detect skew angle
        val contours = ArrayList<MatOfPoint>()
        Imgproc.findContours(thresh, contours, Mat(), 
            Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        
        // Get rotation matrix and apply
        val angle = detectSkewAngle(contours)
        return rotateImage(mat, angle)
    }
    
    fun enhanceText(mat: Mat): Mat {
        // Adaptive thresholding for better text clarity
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)
        
        val enhanced = Mat()
        Imgproc.adaptiveThreshold(gray, enhanced, 255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY, 11, 2.0)
        
        return enhanced
    }
    
    fun autoCrop(mat: Mat): Mat {
        // Detect receipt boundaries and crop
        val edges = Mat()
        Imgproc.Canny(mat, edges, 50.0, 150.0)
        
        // Find largest rectangle contour
        val contours = ArrayList<MatOfPoint>()
        Imgproc.findContours(edges, contours, Mat(),
            Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        
        val largestContour = contours.maxByOrNull { Imgproc.contourArea(it) }
        // Get bounding rect and crop
    }
}
```

**3. Recommended Preprocessing Pipeline:**
1. **Auto-crop** to receipt boundaries (OpenCV contour detection)
2. **Deskew** to correct rotation (Hough transform or minAreaRect)
3. **Convert to grayscale**
4. **Adaptive thresholding** or CLAHE for contrast enhancement
5. **Noise reduction** (Gaussian blur + bilateral filter)
6. **Sharpening** for text clarity

---

## 3. YNAB API Integration

### OAuth 2.0 Flow

**1. Register Application:**
- Register at: https://app.ynab.com/settings/developer
- Get Client ID and Client Secret
- Set redirect URI: `ynabscanner://oauth-callback`

**2. Authorization Code Flow Implementation:**

```kotlin
class YNABAuthManager(
    private val context: Context,
    private val clientId: String,
    private val clientSecret: String
) {
    private val authorizationEndpoint = "https://app.ynab.com/oauth/authorize"
    private val tokenEndpoint = "https://app.ynab.com/oauth/token"
    private val redirectUri = "ynabscanner://oauth-callback"
    
    fun initiateAuth() {
        val state = generateRandomState() // For CSRF protection
        val authUrl = Uri.parse(authorizationEndpoint)
            .buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("state", state)
            .build()
        
        // Store state for verification
        saveState(state)
        
        // Open browser
        val intent = Intent(Intent.ACTION_VIEW, authUrl)
        context.startActivity(intent)
    }
    
    suspend fun handleCallback(uri: Uri): TokenResult {
        val code = uri.getQueryParameter("code") ?: return TokenResult.Error("No code")
        val state = uri.getQueryParameter("state") ?: return TokenResult.Error("No state")
        
        // Verify state
        if (!verifyState(state)) {
            return TokenResult.Error("Invalid state")
        }
        
        // Exchange code for token
        return exchangeCodeForToken(code)
    }
    
    private suspend fun exchangeCodeForToken(code: String): TokenResult {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val requestBody = FormBody.Builder()
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("redirect_uri", redirectUri)
                .add("grant_type", "authorization_code")
                .add("code", code)
                .build()
            
            val request = Request.Builder()
                .url(tokenEndpoint)
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "")
            
            TokenResult.Success(
                accessToken = json.getString("access_token"),
                refreshToken = json.getString("refresh_token"),
                expiresIn = json.getLong("expires_in")
            )
        }
    }
}
```

**3. Deep Link Configuration (AndroidManifest.xml):**
```xml
<activity android:name=".ui.auth.OAuthCallbackActivity">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="ynabscanner"
            android:host="oauth-callback" />
    </intent-filter>
</activity>
```

### YNAB API Endpoints

**Base URL:** `https://api.ynab.com/v1`

**Key Endpoints:**

1. **Get Budgets:**
   - `GET /budgets`
   - Returns list of budgets user has access to

2. **Get Accounts:**
   - `GET /budgets/{budget_id}/accounts`
   - Returns accounts for a budget

3. **Get Categories:**
   - `GET /budgets/{budget_id}/categories`
   - Returns category groups and categories

4. **Create Transaction:**
   - `POST /budgets/{budget_id}/transactions`
   - Body:
   ```json
   {
     "transaction": {
       "account_id": "uuid",
       "date": "2026-02-15",
       "amount": -15420,  // In milliunits (e.g., $15.42 = -15420)
       "payee_name": "Coffee Shop",
       "category_id": "uuid",
       "memo": "Scanned from receipt",
       "cleared": "uncleared"
     }
   }
   ```

5. **Get Transactions:**
   - `GET /budgets/{budget_id}/transactions?since_date=YYYY-MM-DD`
   - For duplicate detection

**Retrofit Service Interface:**
```kotlin
interface YNABApiService {
    @GET("budgets")
    suspend fun getBudgets(): BudgetsResponse
    
    @GET("budgets/{budget_id}/accounts")
    suspend fun getAccounts(@Path("budget_id") budgetId: String): AccountsResponse
    
    @GET("budgets/{budget_id}/categories")
    suspend fun getCategories(@Path("budget_id") budgetId: String): CategoriesResponse
    
    @POST("budgets/{budget_id}/transactions")
    suspend fun createTransaction(
        @Path("budget_id") budgetId: String,
        @Body request: CreateTransactionRequest
    ): TransactionResponse
    
    @GET("budgets/{budget_id}/transactions")
    suspend fun getTransactions(
        @Path("budget_id") budgetId: String,
        @Query("since_date") sinceDate: String? = null
    ): TransactionsResponse
}
```

### Token Storage in Android Keystore

```kotlin
class SecureTokenStorage(private val context: Context) {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val prefs = context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
    
    private val keyAlias = "ynab_token_key"
    
    init {
        createKeyIfNeeded()
    }
    
    private fun createKeyIfNeeded() {
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .build()
            
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }
    
    fun saveToken(token: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = keyStore.getKey(keyAlias, null) as SecretKey
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        
        val iv = cipher.iv
        val encrypted = cipher.doFinal(token.toByteArray())
        
        prefs.edit()
            .putString("token_encrypted", Base64.encodeToString(encrypted, Base64.DEFAULT))
            .putString("token_iv", Base64.encodeToString(iv, Base64.DEFAULT))
            .apply()
    }
    
    fun getToken(): String? {
        val encryptedToken = prefs.getString("token_encrypted", null) ?: return null
        val iv = prefs.getString("token_iv", null) ?: return null
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = keyStore.getKey(keyAlias, null) as SecretKey
        val ivSpec = GCMParameterSpec(128, Base64.decode(iv, Base64.DEFAULT))
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        
        val decrypted = cipher.doFinal(Base64.decode(encryptedToken, Base64.DEFAULT))
        return String(decrypted)
    }
    
    fun clearToken() {
        prefs.edit()
            .remove("token_encrypted")
            .remove("token_iv")
            .apply()
    }
}
```

---

## 4. OCR Field Extraction

### Parsing Strategy: Rule-Based + Pattern Matching

**Receipt Structure Analysis:**
```
Typical Receipt Layout:
┌─────────────────────────┐
│   STORE NAME/LOGO       │ ← Payee (top 25% of text)
│   Address, Phone        │
│   ─────────────────     │
│   Item 1        $X.XX   │ ← Line items (middle)
│   Item 2        $X.XX   │
│   Tax           $X.XX   │ ← Tax (before total)
│   ─────────────────     │
│   TOTAL        $XX.XX   │ ← Amount (keywords: total, balance, amount due)
│   Date: MM/DD/YYYY      │ ← Date (bottom 30%, various formats)
│   Time: HH:MM           │
└─────────────────────────┘
```

### Field Extraction Implementation

```kotlin
class ReceiptParser {
    companion object {
        // Amount patterns
        private val AMOUNT_PATTERN = Regex(
            """(?:total|amount|balance|due|grand\s*total|subtotal)\s*:?\s*\$?\s*(\d+[.,]\d{2})""",
            RegexOption.IGNORE_CASE
        )
        
        // Date patterns (MM/DD/YYYY, DD/MM/YYYY, YYYY-MM-DD, etc.)
        private val DATE_PATTERNS = listOf(
            Regex("""\b(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{2,4})\b"""),
            Regex("""\b(\d{4})[/\-.](\d{1,2})[/\-.](\d{1,2})\b"""),
            Regex("""\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+(\d{1,2}),?\s+(\d{4})\b""", 
                RegexOption.IGNORE_CASE)
        )
        
        // Currency symbols
        private val CURRENCY_PATTERN = Regex("""\$|€|£|¥|₹""")
        
        // Tax patterns
        private val TAX_PATTERN = Regex(
            """(?:tax|vat|gst|sales\s*tax)\s*:?\s*\$?\s*(\d+[.,]\d{2})""",
            RegexOption.IGNORE_CASE
        )
        
        // Total keywords for contextual search
        private val TOTAL_KEYWORDS = setOf(
            "total", "grand total", "amount due", "balance", "balance due"
        )
    }
    
    data class ParsedReceipt(
        val payee: String?,
        val amount: Double?,
        val date: LocalDate?,
        val currency: String = "USD",
        val tax: Double?,
        val lineItems: List<LineItem>,
        val confidence: ConfidenceScores
    )
    
    data class LineItem(
        val description: String,
        val price: Double,
        val quantity: Int = 1
    )
    
    data class ConfidenceScores(
        val payee: Float,
        val amount: Float,
        val date: Float,
        val overall: Float
    )
    
    fun parse(ocrText: Text): ParsedReceipt {
        val lines = ocrText.textBlocks.flatMap { it.lines }
        val textContent = ocrText.text
        
        // Extract fields
        val payee = extractPayee(lines)
        val amount = extractAmount(textContent, lines)
        val date = extractDate(textContent)
        val tax = extractTax(textContent)
        val lineItems = extractLineItems(lines)
        val currency = detectCurrency(textContent)
        
        // Calculate confidence scores
        val confidence = calculateConfidence(payee, amount, date)
        
        return ParsedReceipt(
            payee = payee,
            amount = amount,
            date = date,
            currency = currency,
            tax = tax,
            lineItems = lineItems,
            confidence = confidence
        )
    }
    
    private fun extractPayee(lines: List<Text.Line>): String? {
        // Heuristic: payee is typically in the first few lines
        // Look for the largest text in the top 25% AND not a number
        val topLines = lines.take((lines.size * 0.25).toInt().coerceAtLeast(3))
        
        return topLines
            .filter { line ->
                val text = line.text.trim()
                // Filter out addresses, phone numbers, URLs
                !text.matches(Regex(""".*\d{3}[-.]?\d{3}[-.]?\d{4}.*""")) && // Phone
                !text.matches(Regex(""".*\d+\s+\w+\s+(st|ave|rd|blvd|street|avenue).*""", 
                    RegexOption.IGNORE_CASE)) && // Address
                !text.contains("www", ignoreCase = true) &&
                text.length > 3
            }
            .maxByOrNull { it.boundingBox?.height() ?: 0 }
            ?.text
            ?.trim()
    }
    
    private fun extractAmount(text: String, lines: List<Text.Line>): Double? {
        // Strategy: Look for "total" keyword context first
        val totalMatch = AMOUNT_PATTERN.find(text)
        if (totalMatch != null) {
            return parseAmount(totalMatch.groupValues[1])
        }
        
        // Fallback: Find largest monetary value in bottom 40% of receipt
        val bottomLines = lines.takeLast((lines.size * 0.4).toInt())
        val amounts = bottomLines
            .flatMap { line ->
                Regex("""\$?\s*(\d+[.,]\d{2})""").findAll(line.text)
                    .map { parseAmount(it.groupValues[1]) }
                    .filterNotNull()
            }
        
        return amounts.maxOrNull()
    }
    
    private fun extractDate(text: String): LocalDate? {
        for (pattern in DATE_PATTERNS) {
            val match = pattern.find(text) ?: continue
            
            return try {
                when (pattern) {
                    DATE_PATTERNS[0] -> { // MM/DD/YYYY or DD/MM/YYYY
                        val (_, p1, p2, year) = match.groupValues
                        val y = if (year.length == 2) "20$year" else year
                        // Heuristic: if first number > 12, it's DD/MM
                        if (p1.toInt() > 12) {
                            LocalDate.of(y.toInt(), p2.toInt(), p1.toInt())
                        } else {
                            LocalDate.of(y.toInt(), p1.toInt(), p2.toInt())
                        }
                    }
                    DATE_PATTERNS[1] -> { // YYYY-MM-DD
                        val (_, year, month, day) = match.groupValues
                        LocalDate.of(year.toInt(), month.toInt(), day.toInt())
                    }
                    DATE_PATTERNS[2] -> { // Month DD, YYYY
                        val (_, month, day, year) = match.groupValues
                        val monthNum = parseMonthName(month)
                        LocalDate.of(year.toInt(), monthNum, day.toInt())
                    }
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }
        return null
    }
    
    private fun extractTax(text: String): Double? {
        val taxMatch = TAX_PATTERN.find(text) ?: return null
        return parseAmount(taxMatch.groupValues[1])
    }
    
    private fun extractLineItems(lines: List<Text.Line>): List<LineItem> {
        // Extract lines that have: description + price pattern
        val itemPattern = Regex("""^(.+?)\s+\$?\s*(\d+[.,]\d{2})$""")
        
        return lines
            .mapNotNull { line ->
                val match = itemPattern.find(line.text.trim()) ?: return@mapNotNull null
                val description = match.groupValues[1].trim()
                val price = parseAmount(match.groupValues[2]) ?: return@mapNotNull null
                
                // Filter out total/tax lines
                if (description.lowercase().matches(Regex(".*(total|tax|subtotal).*"))) {
                    return@mapNotNull null
                }
                
                LineItem(description, price)
            }
    }
    
    private fun detectCurrency(text: String): String {
        return when {
            text.contains("$") -> "USD"
            text.contains("€") -> "EUR"
            text.contains("£") -> "GBP"
            text.contains("¥") -> "JPY"
            text.contains("₹") -> "INR"
            else -> "USD" // Default
        }
    }
    
    private fun parseAmount(amountStr: String): Double? {
        return try {
            amountStr.replace(",", ".").toDouble()
        } catch (e: NumberFormatException) {
            null
        }
    }
    
    private fun parseMonthName(month: String): Int {
        val months = mapOf(
            "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4,
            "may" to 5, "jun" to 6, "jul" to 7, "aug" to 8,
            "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12
        )
        return months[month.take(3).lowercase()] ?: 1
    }
    
    private fun calculateConfidence(
        payee: String?,
        amount: Double?,
        date: LocalDate?
    ): ConfidenceScores {
        val payeeConf = if (payee != null && payee.length > 3) 0.9f else 0.3f
        val amountConf = if (amount != null && amount > 0) 0.95f else 0.2f
        val dateConf = if (date != null) 0.95f else 0.4f
        val overall = (payeeConf + amountConf + dateConf) / 3
        
        return ConfidenceScores(payeeConf, amountConf, dateConf, overall)
    }
}
```

### Advanced NER Approach (Optional Enhancement)

For better accuracy, consider training a Named Entity Recognition model:

```kotlin
// Using TensorFlow Lite for custom NER model
class NERReceiptParser {
    private lateinit var interpreter: Interpreter
    
    fun loadModel(context: Context) {
        val model = loadModelFile(context, "receipt_ner_model.tflite")
        interpreter = Interpreter(model)
    }
    
    fun extractEntities(text: String): Map<String, String> {
        // Tokenize text
        val tokens = tokenize(text)
        
        // Convert to input tensor
        val inputTensor = prepareInput(tokens)
        
        // Run inference
        val outputTensor = Array(1) { FloatArray(tokens.size * NUM_LABELS) }
        interpreter.run(inputTensor, outputTensor)
        
        // Decode predictions to entities
        return decodeEntities(tokens, outputTensor[0])
    }
    
    // Entity labels: PAYEE, AMOUNT, DATE, TAX, ITEM, OTHER
}
```

---

## 5. Testing Strategy

### Testing Frameworks & Libraries

**Dependencies:**
```kotlin
dependencies {
    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("com.google.truth:truth:1.1.5")
    testImplementation("androidx.arch.core:core-testing:2.2.0") // InstantTaskExecutorRule
    
    // Instrumented Testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("io.mockk:mockk-android:1.13.8")
    
    // Room testing
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    
    // Hilt testing
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48.1")
    kaptAndroidTest("com.google.dagger:hilt-android-compiler:2.48.1")
    
    // Testing utilities
    testImplementation("app.cash.turbine:turbine:1.0.0") // Flow testing
    androidTestImplementation("androidx.work:work-testing:2.9.0") // WorkManager testing
}
```

### Test Types & Coverage Strategy

#### 1. Unit Tests (src/test/)

**Parser Tests:**
```kotlin
class ReceiptParserTest {
    private lateinit var parser: ReceiptParser
    
    @Before
    fun setup() {
        parser = ReceiptParser()
    }
    
    @Test
    fun `extractAmount detects total with dollar sign`() {
        val ocrText = mockOcrText("""
            Store Name
            Item 1    $5.99
            Item 2    $10.00
            Tax       $0.80
            Total     $16.79
        """.trimIndent())
        
        val result = parser.parse(ocrText)
        
        assertThat(result.amount).isEqualTo(16.79)
    }
    
    @Test
    fun `extractPayee returns first large text not a phone number`() {
        val ocrText = mockOcrText("""
            COFFEE SHOP
            123 Main St
            Tel: 555-123-4567
            Order #: 12345
        """.trimIndent())
        
        val result = parser.parse(ocrText)
        
        assertThat(result.payee).isEqualTo("COFFEE SHOP")
    }
    
    @Test
    fun `extractDate parses MM-DD-YYYY format`() {
        val ocrText = mockOcrText("Date: 02/15/2026\nTotal: $10.00")
        
        val result = parser.parse(ocrText)
        
        assertThat(result.date).isEqualTo(LocalDate.of(2026, 2, 15))
    }
    
    @Test
    fun `parse returns low confidence when fields missing`() {
        val ocrText = mockOcrText("Unreadable text ###")
        
        val result = parser.parse(ocrText)
        
        assertThat(result.confidence.overall).isLessThan(0.5f)
    }
}
```

**Repository Tests:**
```kotlin
@ExperimentalCoroutinesTest
class TransactionRepositoryTest {
    private lateinit var repository: TransactionRepository
    private lateinit var localDataSource: TransactionDao
    private lateinit var remoteDataSource: YNABApiService
    
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    @Before
    fun setup() {
        localDataSource = mockk()
        remoteDataSource = mockk()
        repository = TransactionRepositoryImpl(localDataSource, remoteDataSource)
    }
    
    @Test
    fun `queueTransaction stores locally when offline`() = runTest {
        val transaction = createTestTransaction()
        coEvery { localDataSource.insert(any()) } returns 1L
        
        repository.queueTransaction(transaction)
        
        coVerify { localDataSource.insert(transaction) }
    }
    
    @Test
    fun `syncPendingTransactions uploads queued items when online`() = runTest {
        val queuedTransactions = listOf(createTestTransaction(), createTestTransaction())
        coEvery { localDataSource.getPendingTransactions() } returns flowOf(queuedTransactions)
        coEvery { remoteDataSource.createTransaction(any(), any()) } returns mockk()
        coEvery { localDataSource.markAsSynced(any()) } just Runs
        
        repository.syncPendingTransactions()
        
        coVerify(exactly = 2) { remoteDataSource.createTransaction(any(), any()) }
        coVerify(exactly = 2) { localDataSource.markAsSynced(any()) }
    }
}
```

**ViewModel Tests:**
```kotlin
@ExperimentalCoroutinesTest
class ReviewViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var viewModel: ReviewViewModel
    private lateinit var repository: TransactionRepository
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        viewModel = ReviewViewModel(repository)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `submitTransaction shows success when upload succeeds`() = runTest {
        val transaction = createTestTransaction()
        coEvery { repository.createTransaction(any()) } returns Result.success(Unit)
        
        viewModel.submitTransaction(transaction)
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertThat(viewModel.uiState.value).isInstanceOf(UiState.Success::class.java)
    }
    
    @Test
    fun `submitTransaction queues locally when offline`() = runTest {
        val transaction = createTestTransaction()
        coEvery { repository.createTransaction(any()) } returns Result.failure(
            IOException("No network")
        )
        coEvery { repository.queueTransaction(any()) } just Runs
        
        viewModel.submitTransaction(transaction)
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { repository.queueTransaction(transaction) }
        assertThat(viewModel.uiState.value).isInstanceOf(UiState.QueuedOffline::class.java)
    }
}
```

#### 2. Instrumented Tests (src/androidTest/)

**Database Tests:**
```kotlin
@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: TransactionDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.transactionDao()
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun insertAndRetrieveTransaction() = runTest {
        val transaction = TransactionEntity(
            id = "test-1",
            accountId = "account-1",
            amount = -1500,
            payee = "Test Store",
            date = "2026-02-15",
            isSynced = false
        )
        
        dao.insert(transaction)
        
        val retrieved = dao.getById("test-1")
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.payee).isEqualTo("Test Store")
    }
    
    @Test
    fun getPendingTransactions_returnOnlyUnsynced() = runTest {
        dao.insert(createTransaction(id = "1", isSynced = false))
        dao.insert(createTransaction(id = "2", isSynced = true))
        dao.insert(createTransaction(id = "3", isSynced = false))
        
        val pending = dao.getPendingTransactions().first()
        
        assertThat(pending).hasSize(2)
        assertThat(pending.map { it.id }).containsExactly("1", "3")
    }
}
```

**UI Tests (Espresso):**
```kotlin
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class CameraFlowTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)
    
    @Before
    fun setup() {
        hiltRule.inject()
        // Grant camera permission
        grantCameraPermission()
    }
    
    @Test
    fun captureReceipt_navigatesToReviewScreen() {
        // Open camera
        onView(withId(R.id.btn_scan_receipt)).perform(click())
        
        // Wait for camera preview
        onView(withId(R.id.camera_preview))
            .check(matches(isDisplayed()))
        
        // Capture image
        onView(withId(R.id.btn_capture)).perform(click())
        
        // Verify navigation to review screen
        onView(withId(R.id.review_screen))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun reviewScreen_displaysExtractedFields() {
        // Mock OCR result
        val mockReceipt = ParsedReceipt(
            payee = "Test Store",
            amount = 25.99,
            date = LocalDate.now(),
            currency = "USD",
            tax = 2.00,
            lineItems = emptyList(),
            confidence = ConfidenceScores(0.9f, 0.95f, 0.95f, 0.93f)
        )
        
        // Navigate to review with mock data
        launchReviewActivity(mockReceipt)
        
        // Verify fields displayed
        onView(withId(R.id.edit_payee))
            .check(matches(withText("Test Store")))
        onView(withId(R.id.edit_amount))
            .check(matches(withText("25.99")))
    }
    
    @Test
    fun submitTransaction_showsSuccessMessage() {
        launchReviewActivity(createValidReceipt())
        
        // Edit fields if needed
        onView(withId(R.id.edit_payee))
            .perform(replaceText("Updated Payee"))
        
        // Select category
        onView(withId(R.id.spinner_category)).perform(click())
        onData(anything()).atPosition(0).perform(click())
        
        // Submit
        onView(withId(R.id.btn_submit)).perform(click())
        
        // Verify success message
        onView(withText(R.string.transaction_created_success))
            .inRoot(withDecorView(not(activityRule.scenario.result.resultData)))
            .check(matches(isDisplayed()))
    }
}
```

#### 3. Integration Tests

**OAuth Flow Test:**
```kotlin
@RunWith(AndroidJUnit4::class)
class YNABAuthIntegrationTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(AuthActivity::class.java)
    
    @Test
    fun completeOAuthFlow_savesTokenAndNavigatesToMain() {
        // Start OAuth
        onView(withId(R.id.btn_connect_ynab)).perform(click())
        
        // Simulate callback with auth code
        val callbackIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("ynabscanner://oauth-callback?code=test-code&state=valid-state")
        }
        activityRule.scenario.onActivity { activity ->
            activity.startActivity(callbackIntent)
        }
        
        // Verify token saved and navigated
        // (This requires mocking the API response)
    }
}
```

### Test Coverage Goals

- **Parser logic:** 95%+ coverage (critical for accuracy)
- **Repository/ViewModel:** 85%+ coverage
- **UI components:** 70%+ coverage (focus on critical paths)
- **Overall:** 80%+ coverage

### Continuous Testing

```kotlin
// In build.gradle.kts
android {
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }
}

tasks.register("testAll") {
    dependsOn("test", "connectedAndroidTest")
}
```

---

## 6. Architecture Decisions

### Recommended: Clean Architecture + MVVM

**Architecture Layers:**

```
┌─────────────────────────────────────────┐
│          Presentation Layer             │
│  (Activities, Fragments, ViewModels)    │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│           Domain Layer                  │
│  (Use Cases, Domain Models, Repos)      │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│            Data Layer                   │
│  (Repositories, Data Sources, DTOs)     │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│        External Services                │
│  (YNAB API, ML Kit, Room Database)      │
└─────────────────────────────────────────┘
```

### Module Structure

```kotlin
// Domain Layer - Pure Kotlin, no Android dependencies
module: domain
  - model/
      - Receipt.kt
      - Transaction.kt
      - Account.kt
  - repository/
      - TransactionRepository (interface)
      - AuthRepository (interface)
  - usecase/
      - CreateTransactionUseCase
      - ScanReceiptUseCase
      - AuthenticateUserUseCase
```

```kotlin
// Data Layer
module: data
  - repository/
      - TransactionRepositoryImpl
      - AuthRepositoryImpl
  - local/
      - database/
          - AppDatabase
          - TransactionDao
          - AccountDao
      - entity/
          - TransactionEntity
  - remote/
      - api/
          - YNABApiService
          - AuthInterceptor
      - dto/
          - TransactionDto
          - BudgetDto
  - mapper/
      - TransactionMapper (Entity ↔ Domain)
```

```kotlin
// Presentation Layer
module: app
  - ui/
      - camera/
          - CameraFragment
          - CameraViewModel
      - review/
          - ReviewFragment
          - ReviewViewModel
      - auth/
          - AuthActivity
          - AuthViewModel
  - ml/
      - ocr/
          - TextRecognizer
      - preprocessing/
          - ImagePreprocessor
      - parser/
          - ReceiptParser
```

### Data Flow Pattern

**MVVM with Repository Pattern:**

```kotlin
// Domain Model
data class Transaction(
    val id: String,
    val accountId: String,
    val date: LocalDate,
    val amount: Double,
    val payee: String,
    val categoryId: String?,
    val memo: String?,
    val cleared: ClearedStatus
)

// Use Case
class CreateTransactionUseCase(
    private val repository: TransactionRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend operator fun invoke(transaction: Transaction): Result<Unit> {
        return withContext(dispatcher) {
            try {
                repository.createTransaction(transaction)
                Result.success(Unit)
            } catch (e: IOException) {
                // Network error, queue for later
                repository.queueTransaction(transaction)
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

// ViewModel
class ReviewViewModel @Inject constructor(
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ReviewUiState>(ReviewUiState.Loading)
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()
    
    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()
    
    init {
        loadAccounts()
        loadCategories()
    }
    
    fun submitTransaction(transaction: Transaction) {
        viewModelScope.launch {
            _uiState.value = ReviewUiState.Submitting
            
            createTransactionUseCase(transaction)
                .onSuccess {
                    _uiState.value = ReviewUiState.Success
                }
                .onFailure { error ->
                    if (error is IOException) {
                        _uiState.value = ReviewUiState.QueuedOffline
                    } else {
                        _uiState.value = ReviewUiState.Error(error.message)
                    }
                }
        }
    }
    
    private fun loadAccounts() {
        viewModelScope.launch {
            getAccountsUseCase()
                .onSuccess { _accounts.value = it }
                .onFailure { /* handle error */ }
        }
    }
}

// Fragment
class ReviewFragment : Fragment() {
    private val viewModel: ReviewViewModel by viewModels()
    private lateinit var binding: FragmentReviewBinding
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentReviewBinding.bind(view)
        
        observeUiState()
        setupListeners()
    }
    
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is ReviewUiState.Loading -> showLoading()
                    is ReviewUiState.Success -> showSuccess()
                    is ReviewUiState.Error -> showError(state.message)
                    is ReviewUiState.QueuedOffline -> showQueuedMessage()
                }
            }
        }
    }
}
```

### Dependency Injection with Hilt

```kotlin
// App Module
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ynab_scanner.db"
        )
            .addMigrations(/* migrations */)
            .build()
    }
    
    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
    
    @Provides
    @Singleton
    fun provideOkHttpClient(tokenManager: TokenManager): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = tokenManager.getAccessToken()
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                chain.proceed(request)
            }
            .build()
    }
    
    @Provides
    @Singleton
    fun provideYNABApi(okHttpClient: OkHttpClient): YNABApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.ynab.com/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YNABApiService::class.java)
    }
}

// Repository Module
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl
    ): TransactionRepository
    
    @Binds
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository
}
```

---

## 7. Key Dependencies

### Complete build.gradle.kts

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
}

android {
    namespace = "com.yourpackage.ynabscanner"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.yourpackage.ynabscanner"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // YNAB API configuration
        buildConfigField("String", "YNAB_CLIENT_ID", "\"${project.findProperty("YNAB_CLIENT_ID")}\"")
        buildConfigField("String", "YNAB_CLIENT_SECRET", "\"${project.findProperty("YNAB_CLIENT_SECRET")}\"")
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // Android Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    
    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Dependency Injection - Hilt
    implementation("com.google.dagger:hilt-android:2.48.1")
    kapt("com.google.dagger:hilt-android-compiler:2.48.1")
    
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // ML Kit Text Recognition (on-device)
    implementation("com.google.mlkit:text-recognition:16.0.0")
    
    // CameraX
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("androidx.camera:camera-extensions:1.3.1")
    
    // Image Processing - OpenCV (optional but recommended)
    implementation("org.opencv:opencv:4.8.0")
    
    // Networking - Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // JSON Parsing
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Security - Encrypted SharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // WorkManager for background sync
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // DataStore (alternative to SharedPreferences)
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Image loading - Coil
    implementation("io.coil-kt:coil:2.5.0")
    
    // Timber for logging
    implementation("com.jakewharton.timber:timber:5.0.1")
    
    // Testing - Unit Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("com.google.truth:truth:1.1.5")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    
    // Testing - Instrumented Tests
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("io.mockk:mockk-android:1.13.8")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("androidx.work:work-testing:2.9.0")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48.1")
    kaptAndroidTest("com.google.dagger:hilt-android-compiler:2.48.1")
}

kapt {
    correctErrorTypes = true
}
```

### gradle.properties Configuration

```properties
# YNAB API credentials (store securely, not in VCS)
YNAB_CLIENT_ID=your_client_id_here
YNAB_CLIENT_SECRET=your_client_secret_here

# Android optimization
android.useAndroidX=true
android.enableJetifier=false
kotlin.code.style=official

# Build optimizations
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
org.gradle.parallel=true
org.gradle.caching=true
android.enableBuildCache=true
```

---

## 8. Implementation Considerations

### Challenges & Technical Risks

#### 1. OCR Accuracy Challenges

**Issue:** Receipt text can be:
- Low contrast (faded ink)
- Skewed or curved
- Poor lighting conditions
- Damaged or crumpled paper
- Small font sizes
- Mixed languages

**Mitigations:**
- **Preprocessing pipeline:** Implement deskewing, contrast enhancement, noise reduction
- **Multiple OCR passes:** Try different preprocessing configurations if confidence is low
- **User guidance:** Provide camera overlay guides, recommend good lighting
- **Manual correction:** Always allow user to edit extracted fields
- **Confidence scoring:** Show confidence level to user, flag low-confidence fields
- **Image quality check:** Validate image quality before OCR (blur detection, brightness check)

```kotlin
class ImageQualityChecker {
    fun assessQuality(bitmap: Bitmap): QualityAssessment {
        val blurScore = detectBlur(bitmap)
        val brightnessScore = assessBrightness(bitmap)
        val contrastScore = assessContrast(bitmap)
        
        return QualityAssessment(
            isAcceptable = blurScore > 0.7 && brightnessScore in 0.3..0.8,
            blurScore = blurScore,
            brightnessScore = brightnessScore,
            suggestions = buildList {
                if (blurScore < 0.7) add("Image appears blurry. Hold phone steady.")
                if (brightnessScore < 0.3) add("Image too dark. Try better lighting.")
                if (brightnessScore > 0.8) add("Image overexposed. Avoid bright light.")
            }
        )
    }
}
```

#### 2. Receipt Format Variability

**Issue:** Receipts vary widely:
- Different layouts (merchant-specific)
- Multiple languages
- Currency variations
- Different date formats
- Itemized vs. summary-only
- Handwritten notes

**Mitigations:**
- **Rule-based parser with fallbacks:** Try multiple extraction strategies
- **Machine learning:** Train custom NER model on diverse receipt dataset
- **User selection:** Allow user to tap/select text regions for specific fields
- **Template matching:** Learn common formats from successful parses
- **Confidence thresholds:** Only auto-fill high-confidence fields

```kotlin
class AdaptiveReceiptParser(
    private val ruleBasedParser: RuleBasedParser,
    private val mlParser: MLParser? = null
) {
    suspend fun parse(ocrText: Text): ParsedReceipt {
        // Try ML parser first if available
        val mlResult = mlParser?.parse(ocrText)
        if (mlResult != null && mlResult.confidence.overall > 0.85) {
            return mlResult
        }
        
        // Fallback to rule-based
        val ruleResult = ruleBasedParser.parse(ocrText)
        
        // Merge results, taking highest confidence fields
        return mergeResults(mlResult, ruleResult)
    }
}
```

#### 3. YNAB API Rate Limiting

**Issue:** YNAB API has rate limits (200 requests per hour per token)

**Mitigations:**
- **Batch operations:** Use bulk transaction creation when possible
- **Local caching:** Cache budgets, accounts, categories locally
- **Request throttling:** Implement exponential backoff
- **Queue management:** Batch offline transactions before syncing
- **Rate limit monitoring:** Track usage, show user when approaching limit

```kotlin
class RateLimitedYNABClient(
    private val apiService: YNABApiService,
    private val rateLimiter: RateLimiter
) {
    suspend fun createTransaction(
        budgetId: String,
        transaction: Transaction
    ): Result<Unit> {
        return rateLimiter.acquire {
            try {
                apiService.createTransaction(budgetId, transaction)
                Result.success(Unit)
            } catch (e: HttpException) {
                if (e.code() == 429) { // Rate limited
                    Result.failure(RateLimitException("Rate limit exceeded"))
                } else {
                    Result.failure(e)
                }
            }
        }
    }
}

class RateLimiter(private val requestsPerHour: Int = 200) {
    private val requestTimestamps = mutableListOf<Long>()
    
    suspend fun <T> acquire(block: suspend () -> T): T {
        waitIfNeeded()
        requestTimestamps.add(System.currentTimeMillis())
        return block()
    }
    
    private suspend fun waitIfNeeded() {
        val now = System.currentTimeMillis()
        val oneHourAgo = now - 3600_000
        
        // Remove old requests
        requestTimestamps.removeAll { it < oneHourAgo }
        
        if (requestTimestamps.size >= requestsPerHour) {
            val oldestRequest = requestTimestamps.first()
            val waitTime = (oldestRequest + 3600_000) - now
            if (waitTime > 0) {
                delay(waitTime)
            }
        }
    }
}
```

#### 4. Offline Sync Complexity

**Issue:** Managing transaction queue when offline, avoiding duplicates

**Mitigations:**
- **Unique client-side IDs:** Generate UUIDs for each scanned receipt
- **Sync status tracking:** Mark transactions as pending/synced/failed
- **Conflict resolution:** Check for duplicates before creating (by amount, date, payee)
- **WorkManager:** Use for reliable background sync
- **Retry logic:** Exponential backoff for failed syncs

```kotlin
class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val repository: TransactionRepository
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val pendingTransactions = repository.getPendingTransactions()
            
            pendingTransactions.forEach { transaction ->
                try {
                    repository.syncTransaction(transaction)
                    repository.markAsSynced(transaction.id)
                } catch (e: Exception) {
                    // Log error, continue with next
                    Timber.e(e, "Failed to sync transaction ${transaction.id}")
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}

// Schedule periodic sync
class SyncScheduler(private val workManager: WorkManager) {
    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            "transaction_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
```

#### 5. Privacy & Data Security

**Issue:** Receipts contain sensitive information (PII, financial data)

**Mitigations:**
- **Encrypted storage:** Use EncryptedSharedPreferences for tokens, SQLCipher for database
- **Minimal retention:** Delete images after successful upload (or user opt-in)
- **Android Keystore:** Store encryption keys securely
- **TLS/HTTPS:** All network traffic encrypted
- **Permissions:** Request minimal permissions (camera, network only)
- **User control:** Clear settings for data retention, ability to delete all data

```kotlin
// Encrypted Room Database
@Database(entities = [TransactionEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    
    companion object {
        fun build(context: Context): AppDatabase {
            val passphrase = getOrCreateDatabasePassphrase(context)
            val factory = SupportFactory(passphrase)
            
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "ynab_scanner.db"
            )
                .openHelperFactory(factory) // SQLCipher encryption
                .build()
        }
        
        private fun getOrCreateDatabasePassphrase(context: Context): ByteArray {
            // Store passphrase in Android Keystore
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            
            if (!keyStore.containsAlias("db_passphrase_key")) {
                // Generate new passphrase
                generatePassphrase()
            }
            
            return retrievePassphrase()
        }
    }
}

// Image retention policy
class ImageRetentionManager(
    private val prefs: SharedPreferences,
    private val fileManager: FileManager
) {
    fun handleCapturedImage(imageFile: File, transaction: Transaction) {
        val retentionPolicy = getRetentionPolicy()
        
        when (retentionPolicy) {
            RetentionPolicy.DELETE_IMMEDIATELY -> {
                // Delete after successful sync
                fileManager.scheduleDelete(imageFile, transaction.id)
            }
            RetentionPolicy.KEEP_FOR_30_DAYS -> {
                val deleteDate = LocalDate.now().plusDays(30)
                fileManager.scheduleDelete(imageFile, deleteDate)
            }
            RetentionPolicy.KEEP_FOREVER -> {
                // Encrypt and store
                fileManager.encryptAndStore(imageFile)
            }
        }
    }
}
```

#### 6. Testing OCR Accuracy

**Issue:** Difficult to test OCR accuracy comprehensively

**Mitigations:**
- **Test dataset:** Collect diverse receipt samples (50+ receipts)
- **Automated evaluation:** Run parser on test set, measure accuracy metrics
- **Ground truth labels:** Manually label test receipts with correct values
- **Continuous monitoring:** Track field extraction accuracy in production (anonymized)
- **A/B testing:** Compare different preprocessing/parsing strategies

```kotlin
class AccuracyEvaluator {
    data class EvaluationResult(
        val payeeAccuracy: Float,
        val amountAccuracy: Float,
        val dateAccuracy: Float,
        val overallAccuracy: Float,
        val detailedResults: List<ReceiptResult>
    )
    
    suspend fun evaluate(
        testDataset: List<LabeledReceipt>,
        parser: ReceiptParser
    ): EvaluationResult {
        val results = testDataset.map { labeled ->
            val ocrText = performOCR(labeled.imageFile)
            val parsed = parser.parse(ocrText)
            
            ReceiptResult(
                filename = labeled.imageFile.name,
                payeeMatch = parsed.payee == labeled.groundTruth.payee,
                amountMatch = abs(parsed.amount - labeled.groundTruth.amount) < 0.01,
                dateMatch = parsed.date == labeled.groundTruth.date,
                parsed = parsed,
                groundTruth = labeled.groundTruth
            )
        }
        
        return EvaluationResult(
            payeeAccuracy = results.count { it.payeeMatch }.toFloat() / results.size,
            amountAccuracy = results.count { it.amountMatch }.toFloat() / results.size,
            dateAccuracy = results.count { it.dateMatch }.toFloat() / results.size,
            overallAccuracy = results.count { 
                it.payeeMatch && it.amountMatch && it.dateMatch 
            }.toFloat() / results.size,
            detailedResults = results
        )
    }
}
```

#### 7. Camera UX Challenges

**Issue:** Users struggle with capturing clear receipt images

**Mitigations:**
- **Guided framing:** Overlay rectangle guides for receipt positioning
- **Auto-focus assistance:** Detect receipt edges, provide visual feedback
- **Auto-capture:** Automatically capture when receipt is properly framed
- **Image quality feedback:** Real-time blur/brightness warnings
- **Flash control:** Smart flash recommendations based on lighting
- **Multi-shot:** Option to take multiple photos, select best

```kotlin
class SmartCameraController(
    private val cameraProvider: ProcessCameraProvider,
    private val qualityChecker: ImageQualityChecker
) {
    fun setupAutoCapture(
        previewView: PreviewView,
        onCaptured: (Bitmap) -> Unit
    ) {
        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        
        imageAnalyzer.setAnalyzer(executor) { imageProxy ->
            val quality = qualityChecker.assessQuality(imageProxy.toBitmap())
            
            // Auto-capture when quality is good and receipt is centered
            if (quality.isAcceptable && isReceiptCentered(imageProxy)) {
                captureImage(onCaptured)
            }
            
            // Provide real-time feedback
            updateOverlay(quality)
            
            imageProxy.close()
        }
    }
    
    private fun isReceiptCentered(imageProxy: ImageProxy): Boolean {
        // Detect receipt edges using edge detection
        // Check if receipt fills frame adequately (60-90% of frame)
        // Return true if well-positioned
    }
}
```

### Performance Optimization

**ML Kit optimization:**
- Run OCR on background thread
- Resize large images before processing (max 1920x1080)
- Use low-latency mode for preview analysis

**Database optimization:**
- Use indices on frequently queried columns (date, isSynced)
- Implement pagination for transaction lists
- Use Flow for reactive updates

**Network optimization:**
- Cache API responses (budgets, accounts)
- Use conditional requests (If-Modified-Since)
- Compress request bodies

---

## Summary: Implementation Roadmap

### Phase 1: Foundation (Week 1)
- Set up Android project structure with Hilt
- Implement basic MVVM architecture
- Integrate Room database
- Set up CameraX for capture

### Phase 2: OCR & Parsing (Week 2-3)
- Integrate ML Kit Text Recognition
- Implement image preprocessing pipeline
- Build rule-based receipt parser
- Create test dataset and evaluation framework
- Achieve ≥92% payee, ≥99% amount accuracy

### Phase 3: YNAB Integration (Week 4)
- Implement OAuth 2.0 flow
- Build YNAB API client (Retrofit)
- Implement secure token storage (Keystore)
- Create transaction creation logic
- Build review/edit UI

### Phase 4: Offline & Sync (Week 5)
- Implement offline transaction queue
- Build WorkManager sync worker
- Add duplicate detection
- Handle sync conflicts

### Phase 5: Testing & Polish (Week 6)
- Write comprehensive unit tests (80%+ coverage)
- Create UI tests (Espresso)
- Performance optimization
- Security audit
- Documentation

### Critical Success Factors
1. **OCR accuracy:** Rigorous testing with diverse receipts
2. **User experience:** Frictionless camera capture and review flow
3. **Data security:** Proper encryption and minimal retention
4. **Offline reliability:** Robust queue management and sync
5. **Testing coverage:** Comprehensive tests for all critical paths

---

## Additional Resources

- **ML Kit Docs:** https://developers.google.com/ml-kit/vision/text-recognition
- **YNAB API Docs:** https://api.ynab.com/
- **CameraX Guide:** https://developer.android.com/training/camerax
- **Room Database:** https://developer.android.com/training/data-storage/room
- **Hilt Documentation:** https://developer.android.com/training/dependency-injection/hilt-android
- **Android App Architecture:** https://developer.android.com/topic/architecture
- **OpenCV Android:** https://opencv.org/android/
- **Testing Guide:** https://developer.android.com/training/testing

---

**End of Research Findings**
