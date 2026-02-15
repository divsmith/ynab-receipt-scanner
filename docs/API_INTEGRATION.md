# YNAB API Integration Guide

## Table of Contents
- [Overview](#overview)
- [Getting Started](#getting-started)
- [OAuth 2.0 Flow](#oauth-20-flow)
- [Personal Access Token (Testing)](#personal-access-token-testing)
- [API Endpoints Used](#api-endpoints-used)
- [Request/Response Examples](#requestresponse-examples)
- [Rate Limiting](#rate-limiting)
- [Error Handling](#error-handling)
- [Duplicate Detection](#duplicate-detection)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

## Overview

The YNAB Receipt Scanner integrates with the **YNAB (You Need A Budget) API** to create transactions from scanned receipts. The app uses OAuth 2.0 for user authentication and makes requests to the YNAB REST API v1.

### Key Features
- ✅ OAuth 2.0 authentication
- ✅ Fetch budgets, accounts, and categories
- ✅ Create transactions with memo support
- ✅ Duplicate transaction detection
- ✅ Offline queueing with automatic sync
- ✅ Rate limit handling
- ✅ Error retry logic

### Official Documentation
- **YNAB API Docs**: https://api.youneedabudget.com/
- **API Reference**: https://api.youneedabudget.com/v1
- **OAuth Guide**: https://api.youneedabudget.com/#oauth-applications

## Getting Started

### Step 1: Register Your Application

1. Go to [YNAB Account Settings - Developer](https://app.youneedabudget.com/settings/developer)
2. Click "New Application"
3. Fill in the details:
   - **Application Name**: `YNAB Receipt Scanner` (or your preferred name)
   - **Application Website**: (optional)
   - **Redirect URI(s)**: `ynab-receipt-scanner://oauth-callback`
     * This is the URI that YNAB will redirect to after authorization
     * Must match exactly in your app configuration

4. Save and note down:
   - **Client ID**: Your application identifier
   - **Client Secret**: Keep this secure!

### Step 2: Configure App

Add to your `local.properties` (not committed to git):
```properties
ynab.client.id=your_client_id_here
ynab.client.secret=your_client_secret_here
```

Or set in `Constants.kt`:
```kotlin
object Constants {
    const val OAUTH_CLIENT_ID = BuildConfig.YNAB_CLIENT_ID
    const val OAUTH_CLIENT_SECRET = BuildConfig.YNAB_CLIENT_SECRET
    const val OAUTH_REDIRECT_URI = "ynab-receipt-scanner://oauth-callback"
}
```

## OAuth 2.0 Flow

### Authorization Code Flow (Recommended)

The app uses the **Authorization Code flow** for secure authentication:

```
┌──────────┐                               ┌──────────┐
│   App    │                               │   YNAB   │
└─────┬────┘                               └────┬─────┘
      │                                         │
      │ 1. Open authorization URL               │
      │────────────────────────────────────────>│
      │                                         │
      │         2. User logs in & approves      │
      │                                         │
      │ 3. Redirect with authorization code     │
      │<────────────────────────────────────────│
      │                                         │
      │ 4. Exchange code for access token       │
      │────────────────────────────────────────>│
      │                                         │
      │ 5. Return access token                  │
      │<────────────────────────────────────────│
      │                                         │
      │ 6. Make API requests with token         │
      │────────────────────────────────────────>│
      │                                         │
```

### Step 1: Authorization Request

Open browser with this URL:
```
https://app.youneedabudget.com/oauth/authorize?
    client_id={YOUR_CLIENT_ID}
    &redirect_uri={YOUR_REDIRECT_URI}
    &response_type=code
```

**Implementation**:
```kotlin
fun buildAuthorizationUrl(): String {
    val params = mapOf(
        "client_id" to OAUTH_CLIENT_ID,
        "redirect_uri" to OAUTH_REDIRECT_URI,
        "response_type" to "code"
    )
    
    return buildString {
        append(OAUTH_AUTHORIZE_URL)
        append("?")
        append(params.entries.joinToString("&") { "${it.key}=${it.value.urlEncode()}" })
    }
}

// In Fragment/Activity
fun startOAuthFlow() {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(buildAuthorizationUrl()))
    startActivity(intent)
}
```

### Step 2: Handle Redirect

Configure deep link in `AndroidManifest.xml`:
```xml
<activity
    android:name=".ui.onboarding.OnboardingActivity"
    android:exported="true">
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

Extract authorization code from redirect:
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    val data: Uri? = intent?.data
    if (data != null && data.scheme == "ynab-receipt-scanner") {
        val code = data.getQueryParameter("code")
        if (code != null) {
            viewModel.exchangeCodeForToken(code)
        }
    }
}
```

### Step 3: Exchange Code for Token

POST to token endpoint:
```
POST https://app.youneedabudget.com/oauth/token
Content-Type: application/json

{
  "client_id": "{YOUR_CLIENT_ID}",
  "client_secret": "{YOUR_CLIENT_SECRET}",
  "redirect_uri": "{YOUR_REDIRECT_URI}",
  "grant_type": "authorization_code",
  "code": "{AUTHORIZATION_CODE}"
}
```

**Response**:
```json
{
  "access_token": "abc123...",
  "token_type": "Bearer",
  "expires_in": 7200,
  "refresh_token": "def456..."
}
```

**Implementation**:
```kotlin
interface OAuth2Service {
    @POST("/oauth/token")
    suspend fun exchangeCodeForToken(
        @Body request: TokenRequest
    ): TokenResponse
}

data class TokenRequest(
    val client_id: String,
    val client_secret: String,
    val redirect_uri: String,
    val grant_type: String,
    val code: String
)

data class TokenResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Long,
    val refresh_token: String?
)

// Usage
suspend fun exchangeCodeForToken(code: String): Result<String> {
    return try {
        val response = oauth2Service.exchangeCodeForToken(
            TokenRequest(
                client_id = OAUTH_CLIENT_ID,
                client_secret = OAUTH_CLIENT_SECRET,
                redirect_uri = OAUTH_REDIRECT_URI,
                grant_type = "authorization_code",
                code = code
            )
        )
        
        // Save token securely
        authPreferences.saveAccessToken(response.access_token)
        
        Result.Success(response.access_token)
    } catch (e: Exception) {
        Result.Error(e)
    }
}
```

### Step 4: Use Access Token

Add token to all API requests:
```kotlin
class AuthInterceptor @Inject constructor(
    private val authPreferences: AuthPreferences
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = authPreferences.getAccessToken()
        
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        
        return chain.proceed(request)
    }
}
```

## Personal Access Token (Testing)

For development and testing, you can use a **Personal Access Token** instead of OAuth.

### Generate Token

1. Go to [YNAB Account Settings - Developer](https://app.youneedabudget.com/settings/developer)
2. Click "New Token"
3. Give it a name (e.g., "Testing")
4. Copy the token (you won't see it again!)

### Use Token in App

In `SettingsFragment`, add an option to enter PAT manually:
```kotlin
fun setPersonalAccessToken(token: String) {
    authPreferences.saveAccessToken(token)
    authPreferences.setAuthMethod(AuthMethod.PERSONAL_TOKEN)
}
```

**⚠️ Warning**: Personal Access Tokens should only be used for testing. Production apps should use OAuth 2.0.

## API Endpoints Used

Base URL: `https://api.youneedabudget.com/v1/`

### 1. Get User Info
```
GET /user
Authorization: Bearer {token}
```

Response:
```json
{
  "data": {
    "user": {
      "id": "user-123"
    }
  }
}
```

### 2. Get Budgets
```
GET /budgets
Authorization: Bearer {token}
```

Response:
```json
{
  "data": {
    "budgets": [
      {
        "id": "budget-123",
        "name": "My Budget",
        "last_modified_on": "2026-02-14T10:00:00Z",
        "date_format": {
          "format": "MM/DD/YYYY"
        },
        "currency_format": {
          "iso_code": "USD",
          "example_format": "123,456.78",
          "decimal_digits": 2,
          "decimal_separator": ".",
          "symbol_first": true,
          "group_separator": ",",
          "currency_symbol": "$",
          "display_symbol": true
        }
      }
    ]
  }
}
```

### 3. Get Accounts
```
GET /budgets/{budget_id}/accounts
Authorization: Bearer {token}
```

Response:
```json
{
  "data": {
    "accounts": [
      {
        "id": "account-123",
        "name": "Checking Account",
        "type": "checking",
        "on_budget": true,
        "closed": false,
        "balance": 150000,
        "cleared_balance": 150000,
        "uncleared_balance": 0
      }
    ]
  }
}
```

### 4. Get Categories
```
GET /budgets/{budget_id}/categories
Authorization: Bearer {token}
```

Response:
```json
{
  "data": {
    "category_groups": [
      {
        "id": "group-123",
        "name": "Monthly Bills",
        "categories": [
          {
            "id": "category-123",
            "name": "Rent/Mortgage",
            "budgeted": 100000,
            "activity": -95000,
            "balance": 5000
          }
        ]
      }
    ]
  }
}
```

### 5. Create Transaction
```
POST /budgets/{budget_id}/transactions
Authorization: Bearer {token}
Content-Type: application/json

{
  "transaction": {
    "account_id": "account-123",
    "date": "2026-02-14",
    "amount": -45000,
    "payee_name": "Starbucks",
    "category_id": "category-456",
    "memo": "Coffee and pastry",
    "cleared": "cleared",
    "approved": true
  }
}
```

**Important Notes**:
- `amount` is in **milliunits**: $45.00 = -45000 (negative for outflow)
- `date` format: `YYYY-MM-DD`
- `cleared`: `cleared`, `uncleared`, or `reconciled`

Response:
```json
{
  "data": {
    "transaction": {
      "id": "txn-123",
      "date": "2026-02-14",
      "amount": -45000,
      "memo": "Coffee and pastry",
      "cleared": "cleared",
      "approved": true,
      "account_id": "account-123",
      "payee_name": "Starbucks",
      "category_id": "category-456"
    }
  }
}
```

## Request/Response Examples

### Complete Example: Create Transaction from Receipt

```kotlin
suspend fun createTransactionFromReceipt(
    receipt: Receipt,
    budgetId: String,
    accountId: String,
    categoryId: String?
): Result<Transaction> {
    
    // Convert amount to milliunits (dollars to milliunits)
    val amountInMilliunits = ((receipt.amount ?: 0.0) * -1000).toLong()
    
    // Format date
    val date = receipt.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.US)
        .format(Date(receipt.timestamp))
    
    // Build request
    val request = CreateTransactionRequest(
        transaction = TransactionDto(
            account_id = accountId,
            date = date,
            amount = amountInMilliunits,
            payee_name = receipt.merchantName,
            category_id = categoryId,
            memo = "Receipt scan: ${receipt.merchantName ?: "Unknown"}",
            cleared = "cleared",
            approved = true
        )
    )
    
    return try {
        val response = ynabApi.createTransaction(budgetId, request)
        Result.Success(response.data.transaction.toDomainModel())
    } catch (e: HttpException) {
        when (e.code()) {
            400 -> Result.Error(ValidationException("Invalid transaction data"))
            401 -> Result.Error(AuthException("Invalid access token"))
            409 -> Result.Error(DuplicateTransactionException())
            429 -> Result.Error(RateLimitException())
            else -> Result.Error(ApiException("Failed to create transaction"))
        }
    } catch (e: IOException) {
        // Network error - save to pending queue
        receiptRepository.savePendingTransaction(receipt, accountId, categoryId)
        Result.Error(NoInternetException())
    }
}
```

## Rate Limiting

### API Rate Limits

YNAB enforces rate limits to protect their servers:

- **200 requests per hour per access token**

### Rate Limit Headers

Response includes:
```
X-Rate-Limit: 200
X-Rate-Limit-Remaining: 185
X-Rate-Limit-Reset: 2026-02-14T11:00:00Z
```

### Handling Rate Limits

```kotlin
class RateLimitInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        
        if (response.code == 429) {
            val resetTime = response.header("X-Rate-Limit-Reset")
            val waitTime = calculateWaitTime(resetTime)
            
            // Log rate limit hit
            Log.w(TAG, "Rate limit exceeded. Reset at: $resetTime")
            
            // Optionally retry after wait time
            Thread.sleep(waitTime)
            return chain.proceed(chain.request())
        }
        
        return response
    }
}
```

### Best Practices
1. **Cache responses** - Reduce API calls by caching budgets/accounts/categories
2. **Batch operations** - Queue pending transactions and sync in batches
3. **Monitor usage** - Track rate limit headers
4. **Exponential backoff** - Wait longer between retries

## Error Handling

### HTTP Status Codes

| Code | Meaning | Action |
|------|---------|--------|
| 200 | Success | Process response |
| 201 | Created | Transaction created successfully |
| 400 | Bad Request | Fix request parameters |
| 401 | Unauthorized | Token is invalid or expired |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Duplicate transaction |
| 429 | Too Many Requests | Rate limited - wait and retry |
| 500 | Server Error | YNAB server issue - retry later |

### Error Response Format

```json
{
  "error": {
    "id": "401",
    "name": "unauthorized",
    "detail": "Unauthorized"
  }
}
```

### Implementation

```kotlin
class ErrorInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        
        if (!response.isSuccessful) {
            val errorBody = response.body?.string()
            val error = try {
                moshi.adapter(ErrorResponse::class.java).fromJson(errorBody)
            } catch (e: Exception) {
                null
            }
            
            throw when (response.code) {
                401 -> InvalidTokenException(error?.error?.detail)
                409 -> DuplicateTransactionException()
                429 -> RateLimitException()
                else -> ApiException(error?.error?.detail ?: "Unknown error")
            }
        }
        
        return response
    }
}
```

## Duplicate Detection

YNAB has built-in duplicate detection based on:
- Same account
- Same date
- Same amount
- Same payee (optional)

### HTTP 409 Response

When a duplicate is detected:
```json
{
  "error": {
    "id": "409.duplicate_transaction",
    "name": "conflict",
    "detail": "A transaction with the same account_id, date, amount, and payee already exists."
  }
}
```

### Handling Duplicates

```kotlin
suspend fun createTransaction(transaction: Transaction): Result<Transaction> {
    // First, check for potential duplicates
    val duplicates = checkDuplicateTransactionUseCase(
        accountId = transaction.accountId,
        date = transaction.date,
        amount = transaction.amount
    )
    
    if (duplicates.isNotEmpty()) {
        // Warn user
        return Result.Error(DuplicateTransactionException(duplicates))
    }
    
    // Proceed with creation
    return ynabRepository.createTransaction(transaction)
}
```

### User Confirmation

```kotlin
// Show dialog to user
"A similar transaction already exists on this date. Create anyway?"
[Cancel] [Create]
```

## Best Practices

### 1. Secure Token Storage
```kotlin
// Use EncryptedSharedPreferences
class AuthPreferences @Inject constructor(
    private val encryptedPrefs: SharedPreferences
) {
    fun saveAccessToken(token: String) {
        encryptedPrefs.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .apply()
    }
}
```

### 2. Token Refresh
OAuth tokens expire after 2 hours. Implement refresh:
```kotlin
suspend fun refreshToken(refreshToken: String): Result<String> {
    val request = TokenRequest(
        client_id = OAUTH_CLIENT_ID,
        client_secret = OAUTH_CLIENT_SECRET,
        grant_type = "refresh_token",
        refresh_token = refreshToken
    )
    
    val response = oauth2Service.refreshToken(request)
    authPreferences.saveAccessToken(response.access_token)
    return Result.Success(response.access_token)
}
```

### 3. Offline Support
```kotlin
// Queue transaction if offline
if (!networkMonitor.isOnline()) {
    receiptRepository.savePendingTransaction(transaction)
    // Will sync automatically when online
}
```

### 4. Cache Data
```kotlin
// Cache budgets/accounts/categories
suspend fun getAccounts(budgetId: String): Result<List<YnabAccount>> {
    return try {
        // Try network
        val accounts = ynabApi.getAccounts(budgetId)
        accountDao.insertAll(accounts)  // Cache
        Result.Success(accounts)
    } catch (e: IOException) {
        // Return cached
        val cached = accountDao.getAll()
        if (cached.isNotEmpty()) {
            Result.Success(cached)
        } else {
            Result.Error(e)
        }
    }
}
```

### 5. Validate Before Sending
```kotlin
fun validateTransaction(transaction: Transaction): Result<Unit> {
    if (transaction.amount == null || transaction.amount <= 0) {
        return Result.Error(ValidationException("Invalid amount"))
    }
    if (transaction.date.isNullOrBlank()) {
        return Result.Error(ValidationException("Date required"))
    }
    return Result.Success(Unit)
}
```

## Troubleshooting

### Issue: 401 Unauthorized
**Cause**: Invalid or expired token  
**Solution**: Re-authenticate with OAuth flow

### Issue: 409 Duplicate Transaction
**Cause**: Transaction with same details already exists  
**Solution**: Check for duplicates before creating, or ask user to confirm

### Issue: 429 Rate Limit Exceeded
**Cause**: Too many requests in short time  
**Solution**: Implement rate limit tracking and backoff

### Issue: Network timeout
**Cause**: Slow or unstable internet connection  
**Solution**: Increase timeout, implement retry logic, queue for later

### Issue: Invalid date format
**Cause**: Date not in YYYY-MM-DD format  
**Solution**: Use SimpleDateFormat to ensure correct format

### Issue: Amount is wrong
**Cause**: Forgot to convert to milliunits or apply negative for outflow  
**Solution**: `milliunits = dollars * 1000 * -1` (for expenses)

---

## Related Documentation
- [Architecture](ARCHITECTURE.md) - Overall system architecture
- [OCR Parser Guide](OCR_PARSER_GUIDE.md) - Receipt parsing
- [Developer Guide](DEVELOPER_GUIDE.md) - Getting started
- [Troubleshooting](TROUBLESHOOTING.md) - Common issues
