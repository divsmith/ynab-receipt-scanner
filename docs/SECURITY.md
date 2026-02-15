# Security Documentation

## Table of Contents
- [Security Architecture](#security-architecture)
- [Data Encryption](#data-encryption)
- [Authentication & Authorization](#authentication--authorization)
- [Network Security](#network-security)
- [Data Storage](#data-storage)
- [Image Handling](#image-handling)
- [API Security](#api-security)
- [Vulnerability Reporting](#vulnerability-reporting)
- [Security Best Practices](#security-best-practices)
- [Threat Model](#threat-model)
- [Security Audit Checklist](#security-audit-checklist)

## Security Architecture

### Security Principles

The YNAB Receipt Scanner follows these core security principles:

1. **Defense in Depth**: Multiple layers of security controls
2. **Least Privilege**: Minimal permissions requested
3. **Secure by Default**: Security features enabled by default
4. **Data Minimization**: Collect only necessary data
5. **Transparency**: Open source code for audit
6. **Privacy First**: No tracking or analytics

### Security Layers

```
┌─────────────────────────────────────────────┐
│        Application Security Layer           │
│  - Input validation                         │
│  - Error handling                           │
│  - Safe intent handling                     │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│         Authentication Layer                │
│  - OAuth 2.0                               │
│  - Secure token storage                    │
│  - Token refresh                           │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│           Network Security Layer            │
│  - HTTPS only                              │
│  - Certificate pinning                     │
│  - TLS 1.2+                               │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│          Data Storage Layer                 │
│  - Encrypted SharedPreferences             │
│  - SQLCipher database encryption           │
│  - Secure file storage                     │
└─────────────────────────────────────────────┘
```

## Data Encryption

### Encryption at Rest

**EncryptedSharedPreferences** for sensitive data:
```kotlin
private fun createEncryptedPreferences(context: Context): SharedPreferences {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    return EncryptedSharedPreferences.create(
        context,
        "encrypted_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}
```

**Stored securely:**
- OAuth access tokens
- OAuth refresh tokens
- YNAB client secrets
- User preferences (optional)

**SQLCipher** for database encryption:
```kotlin
val passphrase = getOrCreatePassphrase()
val factory = PassphraseSQLiteOpenHelperFactory(passphrase)

Room.databaseBuilder(context, YnabDatabase::class.java, "ynab.db")
    .openHelperFactory(factory)
    .build()
```

**Encrypted:**
- Receipt data
- Pending transactions
- Cached accounts/categories

### Encryption in Transit

**All network communication uses HTTPS (TLS 1.2+)**:

```kotlin
val client = OkHttpClient.Builder()
    .connectionSpecs(listOf(
        ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)
            .build()
    ))
    .build()
```

**Network Security Config:**
```xml
<!-- res/xml/network_security_config.xml -->
<network-security-config>
    <!-- Enforce HTTPS -->
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
    
    <!-- YNAB API domain -->
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">api.youneedabudget.com</domain>
        <pin-set>
            <!-- Certificate pinning -->
            <pin digest="SHA-256">AAAAAAAAAAAAA...=</pin>
            <pin digest="SHA-256">BBBBBBBBBBBBB...=</pin>
        </pin-set>
    </domain-config>
    
    <!-- Localhost for development only -->
    <domain-config cleartextTrafficPermitted="true">
        <domain>localhost</domain>
        <domain>10.0.2.2</domain>
    </domain-config>
</network-security-config>
```

### Key Management

**MasterKey Generation:**
```kotlin
class KeyManager @Inject constructor(
    private val context: Context
) {
    private val masterKeyAlias = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    fun encryptData(data: String): ByteArray {
        val encryptedFile = EncryptedFile.Builder(
            context,
            File(context.filesDir, "encrypted_data"),
            masterKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
        
        encryptedFile.openFileOutput().use {
            it.write(data.toByteArray())
        }
    }
}
```

**Database Passphrase:**
```kotlin
private fun getOrCreatePassphrase(): ByteArray {
    val prefs = context.encryptedSharedPreferences
    
    return prefs.getString(KEY_DB_PASSPHRASE, null)?.let {
        Base64.decode(it, Base64.DEFAULT)
    } ?: run {
        // Generate new passphrase
        val passphrase = ByteArray(32)
        SecureRandom().nextBytes(passphrase)
        
        // Store securely
        prefs.edit()
            .putString(KEY_DB_PASSPHRASE, Base64.encodeToString(passphrase, Base64.DEFAULT))
            .apply()
        
        passphrase
    }
}
```

## Authentication & Authorization

### OAuth 2.0 Flow

**Security Features:**
- ✅ Authorization Code Flow (not Implicit Flow)
- ✅ PKCE (Proof Key for Code Exchange) - optional enhancement
- ✅ State parameter for CSRF protection
- ✅ Secure token storage
- ✅ Automatic token refresh

**Implementation:**
```kotlin
class OAuth2Manager @Inject constructor(
    private val authService: OAuth2Service,
    private val authPreferences: AuthPreferences
) {
    suspend fun authenticate(): Result<String> {
        // Generate state for CSRF protection
        val state = generateRandomState()
        authPreferences.saveOAuthState(state)
        
        // Open authorization URL
        val authUrl = buildAuthorizationUrl(state)
        openBrowser(authUrl)
        
        // When redirected back, verify state
        val returnedState = intent.getQueryParameter("state")
        val savedState = authPreferences.getOAuthState()
        
        if (returnedState != savedState) {
            return Result.Error(SecurityException("Invalid state parameter"))
        }
        
        // Exchange code for token
        val code = intent.getQueryParameter("code")
        return exchangeCodeForToken(code)
    }
    
    private fun generateRandomState(): String {
        val random = ByteArray(16)
        SecureRandom().nextBytes(random)
        return Base64.encodeToString(random, Base64.URL_SAFE or Base64.NO_WRAP)
    }
}
```

### Token Storage

**Secure storage with EncryptedSharedPreferences:**
```kotlin
class AuthPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val encryptedPrefs by lazy {
        createEncryptedPreferences(context)
    }
    
    fun saveAccessToken(token: String) {
        encryptedPrefs.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .putLong(KEY_TOKEN_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }
    
    fun getAccessToken(): String? {
        return encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)
    }
    
    fun clearTokens() {
        encryptedPrefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_TOKEN_TIMESTAMP)
            .apply()
    }
}
```

**Token Expiration Handling:**
```kotlin
class AuthInterceptor @Inject constructor(
    private val authPreferences: AuthPreferences,
    private val tokenRefreshService: TokenRefreshService
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        // Check if token is expired
        if (isTokenExpired()) {
            // Refresh token
            runBlocking {
                refreshToken()
            }
        }
        
        // Add Bearer token
        val token = authPreferences.getAccessToken()
        val authenticatedRequest = request.newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        
        val response = chain.proceed(authenticatedRequest)
        
        // Handle 401 Unauthorized
        if (response.code == 401) {
            response.close()
            
            // Try refreshing token
            runBlocking {
                refreshToken()
            }
            
            // Retry request
            return chain.proceed(authenticatedRequest)
        }
        
        return response
    }
}
```

## Network Security

### HTTPS Enforcement

**Network Security Config enforces HTTPS:**
```xml
<network-security-config>
    <!-- No cleartext traffic allowed -->
    <base-config cleartextTrafficPermitted="false" />
</network-security-config>
```

**Manifest configuration:**
```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    android:usesCleartextTraffic="false">
```

### Certificate Pinning

**Pin YNAB API certificates:**
```kotlin
val certificatePinner = CertificatePinner.Builder()
    .add("api.youneedabudget.com", "sha256/AAAAAAA...")
    .add("api.youneedabudget.com", "sha256/BBBBBBB...")  // Backup pin
    .build()

val client = OkHttpClient.Builder()
    .certificatePinner(certificatePinner)
    .build()
```

**Generate certificate pin:**
```bash
# Get certificate
openssl s_client -connect api.youneedabudget.com:443 | \
  openssl x509 -pubkey -noout | \
  openssl rsa -pubin -outform der | \
  openssl dgst -sha256 -binary | \
  openssl enc -base64
```

### Request Signing (Future Enhancement)

For additional security, consider request signing:
```kotlin
class RequestSigningInterceptor(
    private val secret: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        // Generate signature
        val timestamp = System.currentTimeMillis()
        val signature = generateHMAC(request.url.toString(), timestamp, secret)
        
        val signedRequest = request.newBuilder()
            .addHeader("X-Timestamp", timestamp.toString())
            .addHeader("X-Signature", signature)
            .build()
        
        return chain.proceed(signedRequest)
    }
    
    private fun generateHMAC(data: String, timestamp: Long, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
        mac.init(secretKey)
        
        val signData = "$data:$timestamp"
        val hmac = mac.doFinal(signData.toByteArray())
        return Base64.encodeToString(hmac, Base64.NO_WRAP)
    }
}
```

## Data Storage

### Secure File Storage

**Store files in app-private directory:**
```kotlin
class ImageStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val storageDir = File(context.filesDir, "receipts")
    
    init {
        // Ensure directory exists and is private
        if (!storageDir.exists()) {
            storageDir.mkdirs()
            // Set permissions: owner only
            storageDir.setReadable(false, false)
            storageDir.setReadable(true, true)
        }
    }
    
    fun saveImage(bitmap: Bitmap, receiptId: String): String {
        val file = File(storageDir, "$receiptId.jpg")
        
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        
        // Set file permissions: owner only
        file.setReadable(false, false)
        file.setReadable(true, true)
        file.setWritable(false, false)
        file.setWritable(true, true)
        
        return file.absolutePath
    }
}
```

### Data Sanitization

**Sanitize user input:**
```kotlin
class InputSanitizer {
    fun sanitizeMerchantName(name: String): String {
        return name
            .trim()
            .take(100)  // Limit length
            .replace(Regex("[^a-zA-Z0-9\\s\\-\\.]"), "")  // Remove special chars
    }
    
    fun sanitizeAmount(amount: String): Double? {
        return amount
            .replace(Regex("[^0-9.]"), "")
            .toDoubleOrNull()
            ?.takeIf { it > 0 && it < 1_000_000.00 }
    }
    
    fun sanitizeMemo(memo: String): String {
        return memo
            .trim()
            .take(200)
            .replace(Regex("<[^>]*>"), "")  // Remove HTML tags
    }
}
```

### SQL Injection Prevention

**Use Room for safe queries:**
```kotlin
@Dao
interface ReceiptDao {
    // ✅ Safe: Room uses parameterized queries
    @Query("SELECT * FROM receipts WHERE merchant_name = :merchant")
    suspend fun findByMerchant(merchant: String): List<ReceiptEntity>
    
    // ❌ Never do this (if using raw SQL)
    // SELECT * FROM receipts WHERE merchant_name = '$merchant'
}
```

## Image Handling

### Image Security

**Validate image files:**
```kotlin
class ImageValidator {
    fun validateImage(uri: Uri): Boolean {
        // Check file size
        val size = getFileSize(uri)
        if (size > MAX_IMAGE_SIZE) {
            return false
        }
        
        // Check MIME type
        val mimeType = getMimeType(uri)
        if (mimeType !in ALLOWED_MIME_TYPES) {
            return false
        }
        
        // Validate image header
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(uri.path, options)
            options.outWidth > 0 && options.outHeight > 0
        } catch (e: Exception) {
            false
        }
    }
    
    companion object {
        private const val MAX_IMAGE_SIZE = 10 * 1024 * 1024  // 10MB
        private val ALLOWED_MIME_TYPES = setOf("image/jpeg", "image/png")
    }
}
```

### Image Exif Data Removal

**Strip metadata for privacy:**
```kotlin
fun removeExifData(bitmap: Bitmap): Bitmap {
    // Create new bitmap without EXIF data
    return bitmap.copy(bitmap.config, true)
}

fun stripExifFromFile(file: File) {
    try {
        val exif = ExifInterface(file.absolutePath)
        
        // Remove sensitive tags
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, null)
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, null)
        exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, null)
        exif.setAttribute(ExifInterface.TAG_DATETIME, null)
        
        exif.saveAttributes()
    } catch (e: IOException) {
        Log.e("Security", "Failed to strip EXIF", e)
    }
}
```

## API Security

### Input Validation

**Validate all API inputs:**
```kotlin
class TransactionValidator {
    fun validate(transaction: CreateTransactionRequest): Result<Unit> {
        // Validate amount
        if (transaction.amount == 0L) {
            return Result.Error(ValidationException("Amount cannot be zero"))
        }
        
        if (transaction.amount < MIN_AMOUNT_MILLIUNITS || 
            transaction.amount > MAX_AMOUNT_MILLIUNITS) {
            return Result.Error(ValidationException("Amount out of range"))
        }
        
        // Validate date
        if (!isValidDate(transaction.date)) {
            return Result.Error(ValidationException("Invalid date format"))
        }
        
        // Validate account ID format
        if (!isValidUUID(transaction.accountId)) {
            return Result.Error(ValidationException("Invalid account ID"))
        }
        
        return Result.Success(Unit)
    }
    
    private fun isValidDate(date: String): Boolean {
        val pattern = Regex("""^\d{4}-\d{2}-\d{2}$""")
        if (!pattern.matches(date)) return false
        
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)
            true
        } catch (e: ParseException) {
            false
        }
    }
}
```

### Rate Limiting

**Implement client-side rate limiting:**
```kotlin
class RateLimiter(
    private val maxRequests: Int = 100,
    private val timeWindow: Long = TimeUnit.HOURS.toMillis(1)
) {
    private val requests = mutableListOf<Long>()
    
    @Synchronized
    fun allowRequest(): Boolean {
        val now = System.currentTimeMillis()
        
        // Remove old requests
        requests.removeAll { it < now - timeWindow }
        
        // Check limit
        if (requests.size >= maxRequests) {
            return false
        }
        
        // Add current request
        requests.add(now)
        return true
    }
}
```

### Error Handling

**Don't leak sensitive info in errors:**
```kotlin
class ErrorHandler {
    fun handleApiError(exception: Exception): String {
        return when (exception) {
            is HttpException -> {
                when (exception.code()) {
                    401 -> "Authentication failed. Please sign in again."
                    403 -> "Access denied."
                    404 -> "Resource not found."
                    429 -> "Too many requests. Please try again later."
                    500 -> "Server error. Please try again."
                    else -> "An error occurred. Please try again."
                }
            }
            is IOException -> "Network error. Check your connection."
            else -> {
                // ❌ Don't do this: "Error: ${exception.message}"
                // ✅ Generic message:
                "An unexpected error occurred."
            }
        }
    }
}
```

## Vulnerability Reporting

### Responsible Disclosure

If you discover a security vulnerability:

**DO:**
1. ✅ Report privately via email: security@example.com
2. ✅ Include details: steps to reproduce, impact, suggested fix
3. ✅ Allow reasonable time for fix (90 days)
4. ✅ Work with maintainers on fix

**DON'T:**
1. ❌ Publicly disclose before fix
2. ❌ Exploit vulnerability
3. ❌ Test on production users

### Security Advisory Process

1. **Report received**: Acknowledged within 48 hours
2. **Assessment**: Severity evaluated (Critical/High/Medium/Low)
3. **Fix development**: Patch created and tested
4. **Patch release**: Update published
5. **Public disclosure**: Security advisory published after users updated

### Bug Bounty

Currently no formal bug bounty program, but security researchers are credited in:
- SECURITY.md
- Release notes
- Hall of Fame

## Security Best Practices

### For Developers

**Code Review Checklist:**
- [ ] No hardcoded secrets or API keys
- [ ] Input validation on all user inputs
- [ ] Parameterized queries (no SQL injection)
- [ ] HTTPS for all network calls
- [ ] Sensitive data encrypted at rest
- [ ] Proper error handling (no info leakage)
- [ ] ProGuard rules configured
- [ ] Security headers validated

**Secure Coding:**
```kotlin
// ❌ Bad: Hardcoded secret
const val API_KEY = "abc123secret"

// ✅ Good: From BuildConfig or secure storage
val apiKey = BuildConfig.API_KEY

// ❌ Bad: SQL injection risk
database.rawQuery("SELECT * FROM users WHERE name = '$name'")

// ✅ Good: Parameterized query
database.query("users", null, "name = ?", arrayOf(name), null, null, null)

// ❌ Bad: Logging sensitive data
Log.d("Auth", "Token: $accessToken")

// ✅ Good: Redacted logging
Log.d("Auth", "Token: ${accessToken.take(4)}...")
```

### For Users

**Security Tips:**
1. ✅ Keep app updated
2. ✅ Use device lock screen
3. ✅ Don't share screenshots with tokens
4. ✅ Revoke access if device stolen
5. ✅ Review app permissions regularly

## Threat Model

### Threat Actors

**1. Malicious App**
- **Threat**: Another app tries to steal data
- **Mitigation**: App-private storage, encrypted database

**2. Network Attacker**
- **Threat**: Man-in-the-middle attack
- **Mitigation**: HTTPS, certificate pinning

**3. Physical Access**
- **Threat**: Unauthorized device access
- **Mitigation**: Encrypted storage, requires device unlock

**4. Malware**
- **Threat**: Device compromised by malware
- **Mitigation**: Android sandbox, minimal permissions

### Attack Scenarios

**Scenario 1**: Token Theft
- **Attack**: Attacker extracts OAuth token
- **Impact**: Unauthorized YNAB access
- **Mitigation**: Encrypted storage, token expiration

**Scenario 2**: Receipt Image Interception
- **Attack**: Attacker intercepts receipt photos
- **Impact**: PII disclosure
- **Mitigation**: No network transmission, local storage only

**Scenario 3**: API Key Extraction
- **Attack**: Reverse engineer APK for keys
- **Impact**: Impersonate app
- **Mitigation**: ProGuard, no critical secrets in APK

## Security Audit Checklist

### Authentication & Authorization
- [ ] OAuth 2.0 implemented correctly
- [ ] State parameter for CSRF protection
- [ ] Tokens stored encrypted
- [ ] Token refresh implemented
- [ ] Automatic logout on token expiration

### Data Protection
- [ ] EncryptedSharedPreferences for tokens
- [ ] SQLCipher for database
- [ ] File permissions set correctly
- [ ] Sensitive logs redacted

### Network Security
- [ ] HTTPS enforced
- [ ] Certificate pinning configured
- [ ] TLS 1.2+ required
- [ ] Network Security Config set

### Input Validation
- [ ] All user inputs validated
- [ ] Parameterized queries used
- [ ] Image files validated
- [ ] API inputs sanitized

### Code Security
- [ ] ProGuard enabled in release
- [ ] No hardcoded secrets
- [ ] Dependencies up to date
- [ ] No known vulnerabilities

### Privacy
- [ ] Minimal permissions requested
- [ ] No analytics/tracking
- [ ] Privacy policy published
- [ ] GDPR compliant

---

## Security Updates

Check for security updates:
- **GitHub Security Advisories**: [Link]
- **Release Notes**: Check CHANGELOG for security fixes
- **Email Notifications**: security@example.com

**Report security issues**: security@example.com

**PGP Key**: [If applicable]

---

## Related Documentation

- [Architecture](ARCHITECTURE.md) - System design
- [Privacy Policy](PRIVACY_POLICY.md) - Privacy practices
- [Developer Guide](DEVELOPER_GUIDE.md) - Development practices
