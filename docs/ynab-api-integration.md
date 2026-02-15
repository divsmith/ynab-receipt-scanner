# YNAB API Integration - Implementation Complete

## Overview

Complete implementation of YNAB API integration with OAuth 2.0 authentication for the YNAB Receipt Scanner Android app. This implementation follows Clean Architecture principles with multi-module structure.

## Features Implemented

### ✅ OAuth 2.0 Authentication
- **Chrome Custom Tabs** integration for secure OAuth flow
- **PKCE** (Proof Key for Code Exchange) for enhanced security
- Automatic token refresh with exponential backoff
- Personal Access Token support for testing
- Deep link handling for OAuth callback

### ✅ YNAB API Client
- Complete Retrofit API interface with all endpoints:
  - Budgets (list, get details)
  - Accounts (list, filter by budget)
  - Categories (list, grouped)
  - Transactions (list, create, update)
- DTOs for all API responses
- Type-safe Moshi JSON parsing
- Custom date adapter for YNAB date formats

### ✅ Network Layer
- **AuthInterceptor** - Adds Bearer token to all requests
- **ErrorInterceptor** - Handles API errors and rate limiting
- **TokenRefreshAuthenticator** - Auto-refresh on 401 responses
- OkHttp logging for debugging

### ✅ Repository Pattern
- `YnabRepository` interface in domain layer
- `YnabRepositoryImpl` with local caching using Room
- Offline-first architecture with queue support
- Bidirectional DTO ↔ Domain model mapping

### ✅ Local Caching
- Room entities for Budgets, Accounts, Categories
- DAOs with Flow support for reactive updates
- Cache invalidation and refresh logic
- Encrypted database using SQLCipher

### ✅ Transaction Sync
- `TransactionSyncManager` for background sync
- Exponential backoff retry logic
- Pending transaction queue
- Sync status tracking
- Failed transaction retry mechanism

### ✅ Duplicate Detection
- Fuzzy payee matching using Levenshtein distance
- Date tolerance (±2 days)
- Amount exact matching
- Deterministic import_id generation
- Confidence scoring for potential duplicates

### ✅ Use Cases
- `AuthenticateWithYnabUseCase` - Start OAuth flow
- `GetAuthStatusUseCase` - Check auth status
- `SignOutUseCase` - Revoke tokens and sign out
- `GetBudgetsUseCase` - Fetch user budgets
- `GetAccountsUseCase` - Fetch accounts for budget
- `GetCategoriesUseCase` - Fetch categories for budget
- `CreateTransactionUseCase` - Create transaction with deduplication
- `SyncPendingTransactionsUseCase` - Sync offline queue
- `CheckDuplicateTransactionUseCase` - Find potential duplicates

### ✅ UI Components
- **AuthActivity** - Sign in with OAuth or PAT
- **AuthViewModel** - Manage auth state
- **AuthState** - Sealed class for auth states
- Material Design UI with proper error handling

### ✅ Tests
- `YnabMapperTest` - DTO to domain model mapping
- `DuplicateDetectorTest` - Duplicate detection logic
- `YnabAuthManagerTest` - OAuth flow with MockWebServer

## Setup Instructions

### 1. Register YNAB Application

1. Go to https://app.youneedabudget.com/settings/developer
2. Create a new application
3. Set **Redirect URI** to: `ynabreceipt://oauth/callback`
4. Copy your **Client ID**

### 2. Configure Client ID

Update the client ID in [Constants.kt](../core/src/main/java/com/ynab/receiptscanner/core/Constants.kt):

```kotlin
const val OAUTH_CLIENT_ID = "YOUR_CLIENT_ID_HERE" // Replace with your client ID
```

### 3. Permissions

The following permissions are already configured:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 4. Deep Link Configuration

The OAuth callback deep link is configured in `AndroidManifest.xml`:

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    
    <data
        android:scheme="ynabreceipt"
        android:host="oauth"
        android:path="/callback" />
</intent-filter>
```

## Usage

### OAuth Authentication

```kotlin
// Start OAuth flow
val viewModel: AuthViewModel by viewModels()

// In Activity/Fragment
viewModel.authState.collect { state ->
    when (state) {
        is AuthState.SignedOut -> {
            // Show sign-in button
        }
        is AuthState.SigningIn -> {
            // Show loading
        }
        is AuthState.SignedIn -> {
            // Navigate to main screen
        }
        is AuthState.Error -> {
            // Show error message
        }
    }
}
```

### Fetch Budgets

```kotlin
class BudgetViewModel @Inject constructor(
    private val getBudgetsUseCase: GetBudgetsUseCase
) : ViewModel() {
    
    fun loadBudgets() {
        viewModelScope.launch {
            when (val result = getBudgetsUseCase(forceRefresh = false)) {
                is Result.Success -> {
                    val budgets = result.data
                    // Update UI
                }
                is Result.Error -> {
                    // Handle error
                }
            }
        }
    }
}
```

### Create Transaction

```kotlin
class TransactionViewModel @Inject constructor(
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val checkDuplicateUseCase: CheckDuplicateTransactionUseCase
) : ViewModel() {
    
    suspend fun createTransaction(transaction: YnabTransaction) {
        // Check for duplicates first
        when (val result = checkDuplicateUseCase(budgetId, transaction)) {
            is Result.Success -> {
                if (result.data.isNotEmpty()) {
                    // Show duplicate warning
                    showDuplicateDialog(result.data)
                } else {
                    submitTransaction(transaction)
                }
            }
            is Result.Error -> {
                // Proceed anyway
                submitTransaction(transaction)
            }
        }
    }
    
    private suspend fun submitTransaction(transaction: YnabTransaction) {
        when (val result = createTransactionUseCase(budgetId, transaction)) {
            is Result.Success -> {
                // Transaction created or queued
            }
            is Result.Error -> {
                // Handle error
            }
        }
    }
}
```

### Sync Pending Transactions

```kotlin
class SyncWorker @Inject constructor(
    private val syncUseCase: SyncPendingTransactionsUseCase
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return when (val result = syncUseCase()) {
            is com.ynab.receiptscanner.core.util.Result.Success -> {
                val count = result.data
                Log.d("SyncWorker", "Synced $count transactions")
                Result.success()
            }
            is com.ynab.receiptscanner.core.util.Result.Error -> {
                Result.retry()
            }
        }
    }
}
```

## Testing with Personal Access Token

For testing without OAuth:

1. Get your Personal Access Token from: https://app.youneedabudget.com/settings/developer
2. In the app, tap "Use Personal Access Token"
3. Paste your token
4. Sign in

**Note:** PAT is for testing only. OAuth is recommended for production.

## API Rate Limits

YNAB API has rate limits:
- **200 requests per hour** per access token
- Rate limit info in response headers: `X-Rate-Limit`

The `ErrorInterceptor` handles 429 responses and logs retry-after time.

## Architecture

```
┌─────────────────┐
│   Presentation  │  AuthActivity, AuthViewModel
│     (app)       │
└────────┬────────┘
         │
┌────────▼────────┐
│     Domain      │  Use Cases, Repository Interface, Models
│                 │
└────────┬────────┘
         │
┌────────▼────────┐
│      Data       │  Repository Impl, API, DAOs, Mappers
│                 │  Auth Manager, Sync Manager
└────────┬────────┘
         │
┌────────▼────────┐
│   Remote/Local  │  Retrofit, Room, OkHttp, SecureStorage
│                 │
└─────────────────┘
```

## Security Considerations

✅ **PKCE** - Prevents authorization code interception  
✅ **Encrypted Storage** - Tokens stored in EncryptedSharedPreferences  
✅ **Certificate Pinning** - Can be added via OkHttp  
✅ **ProGuard Rules** - Keep API models and Hilt annotations  
✅ **Network Security Config** - Enforces HTTPS  

## Dependencies Added

### Gradle (libs.versions.toml)
```toml
browser = "1.7.0"  # Chrome Custom Tabs
mockito = "5.5.0"  # Testing
mockwebserver = "4.12.0"  # API mocking
```

### Already Included
- Retrofit 2.9.0
- OkHttp 4.12.0
- Moshi 1.15.0
- Room 2.6.1
- Hilt (Dagger) 2.48
- Coroutines 1.7.3

## Files Created

### Data Layer
- `data/remote/YnabApi.kt` - Retrofit interface
- `data/remote/dto/*.kt` - DTO models (7 files)
- `data/remote/interceptor/*.kt` - Auth & Error interceptors
- `data/auth/*.kt` - OAuth manager, config, authenticator
- `data/repository/*.kt` - Repository impl, duplicate detector
- `data/mapper/YnabMapper.kt` - DTO to domain mapping
- `data/sync/TransactionSyncManager.kt` - Background sync
- `data/local/entity/*.kt` - Room entities (Budget, Account, Category)
- `data/local/dao/*.kt` - Room DAOs (3 files)
- `data/di/NetworkModule.kt` - DI for network layer

### Domain Layer
- `domain/repository/YnabRepository.kt` - Repository interface
- `domain/usecase/*.kt` - 9 use cases

### App Layer
- `app/ui/auth/AuthActivity.kt` - OAuth UI
- `app/ui/auth/AuthViewModel.kt` - Auth state management
- `app/ui/auth/AuthState.kt` - Sealed class for states
- `app/res/layout/activity_auth.xml` - Auth UI layout

### Tests
- `data/test/.../YnabMapperTest.kt`
- `data/test/.../DuplicateDetectorTest.kt`
- `data/test/.../YnabAuthManagerTest.kt`

## Next Steps

### Phase 3: OCR & Receipt Processing
- Integrate ML Kit for OCR
- Receipt image preprocessing
- Field extraction and parsing
- Confidence scoring

### Phase 4: UI & UX
- Camera integration
- Receipt list screen
- Transaction review screen
- Budget/account selection

### Phase 5: Advanced Features
- Multi-receipt scanning
- CSV export
- Background sync worker
- Notifications

## Troubleshooting

### OAuth Redirect Not Working
- Verify redirect URI matches exactly: `ynabreceipt://oauth/callback`
- Check `AndroidManifest.xml` intent filter
- Ensure `android:launchMode="singleTask"` on AuthActivity

### Token Expired Errors
- Tokens automatically refresh via `TokenRefreshAuthenticator`
- Check `AuthPreferences.isTokenExpired()`
- Force refresh with `YnabAuthManager.refreshToken()`

### Network Errors
- Check internet permission in manifest
- Check `network_security_config.xml` for HTTPS enforcement
- Enable OkHttp logging to debug requests

### Duplicate Transactions
- YNAB uses `import_id` for deduplication
- Use `CheckDuplicateTransactionUseCase` before creating
- Adjust similarity threshold in `Constants.kt`

## Support

- **YNAB API Docs**: https://api.youneedabudget.com/
- **GitHub Issues**: Create issue in repository
- **YNAB Community**: https://support.youneedabudget.com/

---

**Implementation Status**: ✅ **COMPLETE**  
**Version**: 1.0.0  
**Last Updated**: February 2026
