# YNAB Receipt Scanner - Architecture Documentation

## Table of Contents
- [Overview](#overview)
- [Architecture Diagram](#architecture-diagram)
- [Clean Architecture Layers](#clean-architecture-layers)
- [Module Structure](#module-structure)
- [Data Flow](#data-flow)
- [Key Design Decisions](#key-design-decisions)
- [Technology Stack](#technology-stack)
- [Dependency Injection](#dependency-injection)
- [Threading Model](#threading-model)
- [Error Handling](#error-handling)

## Overview

The YNAB Receipt Scanner follows **Clean Architecture** principles combined with **MVVM** (Model-View-ViewModel) pattern for the presentation layer. The application is structured as a multi-module Android project to enforce separation of concerns, improve build times, and enable better testability.

### High-Level Goals
- **Maintainability**: Clear separation of concerns with well-defined boundaries
- **Testability**: Each layer can be tested independently
- **Scalability**: Easy to add new features without affecting existing code
- **Reusability**: Business logic is platform-independent and reusable

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        Presentation Layer                        │
│                           (app module)                           │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────────────┐ │
│  │  Activities  │  │  Fragments   │  │     ViewModels        │ │
│  │              │←→│              │←→│  (UI State Logic)     │ │
│  │  - MainActivity  - CameraFragment  - CameraViewModel      │ │
│  │  - OnboardingActivity ReviewFragment - ReviewViewModel    │ │
│  └──────────────┘  └──────────────┘  └───────────────────────┘ │
│                                              ↓                   │
│                                        Use Cases                 │
└──────────────────────────────────────────────────────────────────┘
                                        ↓
┌─────────────────────────────────────────────────────────────────┐
│                        Domain Layer                              │
│                        (domain module)                           │
│  ┌──────────────────────────┐  ┌──────────────────────────┐   │
│  │      Use Cases           │  │     Domain Models        │   │
│  │  - AuthenticateWithYnab  │  │  - Receipt               │   │
│  │  - ParseReceipt          │  │  - Transaction           │   │
│  │  - CreateTransaction     │  │  - YnabAccount           │   │
│  │  - SyncPendingTxns       │  │  - YnabCategory          │   │
│  └──────────────────────────┘  └──────────────────────────┘   │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │            Repository Interfaces                         │   │
│  │  - YnabRepository  - ReceiptRepository  - OcrRepository  │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                                        ↓
┌─────────────────────────────────────────────────────────────────┐
│                         Data Layer                               │
│                        (data module)                             │
│  ┌─────────────────────┐  ┌───────────────────────────────┐   │
│  │ Repository Impls    │  │     Data Sources              │   │
│  │ - YnabRepositoryImpl│  │ Remote:                       │   │
│  │ - ReceiptRepoImpl   │  │  - YnabApi (Retrofit)         │   │
│  │ - OcrRepositoryImpl │  │  - OAuth2Service              │   │
│  └─────────────────────┘  │ Local:                        │   │
│                            │  - YnabDatabase (Room)        │   │
│                            │  - AuthPreferences            │   │
│                            │  - ImageStorageManager        │   │
│                            │  - EncryptionHelper           │   │
│                            └───────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │               DTOs & Mappers                            │   │
│  │  - AccountDto ↔ YnabAccount                             │   │
│  │  - TransactionDto ↔ Transaction                          │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                                        ↓
┌─────────────────────────────────────────────────────────────────┐
│                     Core/Utilities Layer                         │
│                        (core module)                             │
│  ┌──────────────┐  ┌───────────┐  ┌────────────────────────┐  │
│  │  Result<T>   │  │ Constants │  │  Kotlin Extensions     │  │
│  │  - Success   │  │ - API_URL │  │  - String.toDate()     │  │
│  │  - Error     │  │ - Limits  │  │  - Context.showToast() │  │
│  └──────────────┘  └───────────┘  └────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘

External Dependencies:
┌──────────────┐  ┌──────────────┐  ┌─────────────┐
│  YNAB API    │  │  ML Kit OCR  │  │  CameraX    │
│  (REST API)  │  │  (Google)    │  │  (Camera)   │
└──────────────┘  └──────────────┘  └─────────────┘
```

## Clean Architecture Layers

### 1. Presentation Layer (app module)
**Purpose**: Handle all UI logic and user interactions

**Components**:
- **Activities**: Host fragments and handle global navigation
  - `MainActivity`: Main container with bottom navigation
  - `OnboardingActivity`: First-time user setup and OAuth
  
- **Fragments**: Individual screens
  - `CameraFragment`: Receipt capture with camera
  - `ReviewFragment`: Edit OCR results before submitting
  - `HomeFragment`: List of scanned receipts
  - `SettingsFragment`: App configuration
  
- **ViewModels**: Manage UI state and orchestrate use cases
  - `CameraViewModel`: Camera state, image capture
  - `ReviewViewModel`: Field editing, validation, submission
  - `HomeViewModel`: Receipt list, filtering, deletion
  - `SettingsViewModel`: Preferences, sync status
  
- **UI Components**: Reusable views
  - `SyncStatusBadge`: Shows sync state (synced/pending/failed)
  - `EmptyStateView`: Placeholder for empty lists
  - `LoadingDialog`: Progress indicator
  - `ErrorDialog`: User-friendly error messages

**Dependencies**:
- domain module (use cases, models)
- core module (utilities)

### 2. Domain Layer (domain module)
**Purpose**: Contains business logic, independent of Android framework

**Components**:
- **Use Cases**: Single-responsibility business operations
  ```
  Authentication:
  - AuthenticateWithYnabUseCase: OAuth 2.0 flow
  - GetAuthStatusUseCase: Check if user is logged in
  - SignOutUseCase: Clear credentials
  
  Receipt Processing:
  - ExtractTextFromImageUseCase: OCR extraction
  - ParseReceiptUseCase: Parse text into fields
  - SaveReceiptUseCase: Store receipt locally
  - SaveReceiptImageUseCase: Store receipt photo
  
  YNAB Integration:
  - GetBudgetsUseCase: Fetch available budgets
  - GetAccountsUseCase: Fetch accounts for budget
  - GetCategoriesUseCase: Fetch categories for budget
  - CreateTransactionUseCase: Create YNAB transaction
  - CheckDuplicateTransactionUseCase: Prevent duplicates
  
  Sync & Offline:
  - SyncPendingTransactionsUseCase: Upload pending transactions
  - GetSyncStatusUseCase: Get sync state
  - RetrySyncUseCase: Manual retry
  - ObserveConnectivityUseCase: Monitor network
  - TriggerSyncOnConnectivityUseCase: Auto-sync on reconnect
  
  Data Management:
  - GetAllReceiptsUseCase: Fetch user's receipts
  ```

- **Domain Models**: Core business entities
  - `Receipt`: Scanned receipt with fields
  - `Transaction`: YNAB transaction
  - `YnabAccount`: Bank/credit account
  - `YnabCategory`: Budget category
  - `YnabBudget`: Budget
  - `ReceiptField`: Individual receipt field (date, merchant, amount)
  - `SyncStatus`: Transaction sync state
  - `ConnectivityStatus`: Network state

- **Repository Interfaces**: Contracts for data access
  - `YnabRepository`: YNAB API operations
  - `ReceiptRepository`: Receipt storage/retrieval
  - `OcrRepository`: Text extraction from images
  - `ConnectivityRepository`: Network monitoring

- **Validators**: Business rules
  - `ReceiptValidator`: Validate receipt data
  - `TransactionValidator`: Validate transactions

**Dependencies**: 
- **None** (Pure Kotlin, no Android dependencies)

### 3. Data Layer (data module)
**Purpose**: Implement data access and external API communication

**Components**:
- **Repository Implementations**:
  - `YnabRepositoryImpl`: Implements `YnabRepository`
  - `ReceiptRepositoryImpl`: Implements `ReceiptRepository`
  - `OcrRepositoryImpl`: Implements `OcrRepository`
  - `ConnectivityRepositoryImpl`: Implements `ConnectivityRepository`

- **Remote Data Sources**:
  - `YnabApi`: Retrofit interface for YNAB REST API
  - `OAuth2Service`: Handle OAuth token exchange
  - **Interceptors**:
    - `AuthInterceptor`: Add Bearer token to requests
    - `ErrorInterceptor`: Parse API errors
    - `RetryInterceptor`: Retry failed requests
    - `CacheInterceptor`: Cache responses

- **Local Data Sources**:
  - `YnabDatabase`: Room database
    - `ReceiptDao`: Receipt CRUD operations
    - `PendingTransactionDao`: Offline transaction queue
    - `AccountDao`: Cache accounts
    - `CategoryDao`: Cache categories
  - `AuthPreferences`: Encrypted token storage
  - `AppPreferences`: App settings
  - `ImageStorageManager`: Receipt photo storage
  - `EncryptionHelper`: Encrypt sensitive data

- **DTOs & Mappers**: Convert between network/DB and domain models
  - `AccountDto` ↔ `YnabAccount`
  - `TransactionDto` ↔ `Transaction`
  - `CategoryDto` ↔ `YnabCategory`
  - `ReceiptEntity` ↔ `Receipt`

- **Background Sync**:
  - `TransactionSyncManager`: Orchestrate sync operations
  - `SyncStatusTracker`: Track sync progress
  - `ConnectivityMonitor`: Monitor network changes

- **Additional Services**:
  - `AnalyticsManager`: Track events (Firebase Analytics)
  - `CrashlyticsManager`: Error reporting
  - `MemoryCache`: In-memory caching

**Dependencies**:
- domain module (interfaces, models)
- core module (utilities)

### 4. Core Layer (core module)
**Purpose**: Shared utilities used across all modules

**Components**:
- `Result<T>`: Type-safe error handling wrapper
- `Constants`: App-wide constants (URLs, limits, keys)
- `Extensions`: Kotlin extension functions
  - String extensions (date parsing, validation)
  - Number extensions (currency formatting)
  - Context extensions (toast, dialog helpers)
- `Dispatchers`: Coroutine dispatcher injection
- `NetworkUtils`: Network helpers

**Dependencies**: 
- **None** (only Android standard library)

## Module Dependencies

```
        app
       /  |  \
      /   |   \
   domain core data
              /
           domain
```

**Dependency Rules**:
1. `app` → `domain`, `core`, `data`
2. `data` → `domain`, `core`
3. `domain` → **no dependencies** (pure Kotlin)
4. `core` → **no dependencies**

## Data Flow

### Example: Scanning a Receipt

```
1. User captures photo in CameraFragment
   ↓
2. CameraViewModel.captureImage()
   ↓
3. SaveReceiptImageUseCase.execute(bitmap)
      → ReceiptRepository.saveImage()
      → ImageStorageManager.saveImage()
   ↓
4. ExtractTextFromImageUseCase.execute(imagePath)
      → OcrRepository.extractText()
      → ML Kit Text Recognition
   ↓
5. ParseReceiptUseCase.execute(rawText)
      → ReceiptParser.parse()
      → Returns Receipt with fields
   ↓
6. Navigate to ReviewFragment with Receipt
   ↓
7. User edits fields and clicks "Create Transaction"
   ↓
8. ReviewViewModel.createTransaction()
   ↓
9. CreateTransactionUseCase.execute(receipt, account, category)
   ↓
   If Online:
      → YnabRepository.createTransaction()
      → YnabApi.createTransaction()
      → YNAB API
   If Offline:
      → ReceiptRepository.savePendingTransaction()
      → PendingTransactionDao.insert()
   ↓
10. Success: Show confirmation & return to home
```

### Offline-First Data Flow

```
User Action
    ↓
ViewModel + UseCase
    ↓
Repository
    ↓
┌───────────────┐
│ Try Network   │
└───────────────┘
    ↓
  Online? ─────No────→ Save to Local DB
    │                        ↓
   Yes                  Add to Sync Queue
    ↓                        ↓
  API Call             WorkManager schedules SyncWorker
    ↓                        ↓
  Success?              Network available? → Retry sync
    │
   No → Save locally + sync later
```

## Key Design Decisions

### 1. Multi-Module Structure
**Why**: 
- Enforces architectural boundaries at compile time
- Improves build times (parallel module builds)
- Enables feature module isolation
- Makes the domain layer truly framework-independent

### 2. Clean Architecture
**Why**:
- Business logic independent of frameworks/UI/DB
- High testability (domain layer is pure Kotlin)
- Easy to swap implementations (e.g., different OCR provider)
- Future-proof for cross-platform (Kotlin Multiplatform)

### 3. Sealed Result Type
**Why**:
- Type-safe error handling
- Forces exhaustive error handling at compile time
- No exceptions for business logic errors
- Easy to test success/failure paths

### 4. Use Case Pattern
**Why**:
- Single Responsibility Principle
- Easy to test individual operations
- Reusable across different UI flows
- Clear contract for what the app can do

### 5. Repository Pattern
**Why**:
- Abstract data source details
- Easy to mock for testing
- Can combine multiple data sources
- Single source of truth principle

### 6. MVVM for Presentation
**Why**:
- Lifecycle-aware (survives config changes)
- Testable (ViewModels are plain Kotlin)
- Clear separation of UI and logic
- Works well with Navigation Component

### 7. Offline-First Architecture
**Why**:
- Better user experience (works without internet)
- Resilient to network failures
- Reduces perceived latency
- Sync automatically when online

### 8. Dependency Injection with Hilt
**Why**:
- Compile-time safety
- Reduced boilerplate vs. manual DI
- Lifecycle-aware scoping
- Better testability with @TestInstallIn

## Technology Stack

### Core Technologies
| Technology | Version | Purpose |
|-----------|---------|---------|
| Kotlin | 1.9.22 | Primary language |
| Android Gradle Plugin | 8.2.2 | Build system |
| Gradle | 8.2+ | Build tool |
| Min SDK | 24 | Android 7.0+ |
| Target SDK | 34 | Android 14 |

### Android Jetpack
| Library | Version | Purpose |
|---------|---------|---------|
| AndroidX Core KTX | 1.12.0 | Core Android extensions |
| AppCompat | 1.6.1 | Backward compatibility |
| Activity KTX | 1.8.2 | Activity extensions |
| Fragment KTX | 1.6.2 | Fragment extensions |
| Lifecycle | 2.7.0 | Lifecycle-aware components |
| Navigation Component | 2.7.6 | Navigation handling |
| Room | 2.6.1 | Local database |
| WorkManager | 2.9.0 | Background tasks |

### Dependency Injection
| Library | Version | Purpose |
|---------|---------|---------|
| Hilt | 2.48 | Dependency injection |

### Networking
| Library | Version | Purpose |
|---------|---------|---------|
| Retrofit | 2.9.0 | REST API client |
| OkHttp | 4.12.0 | HTTP client |
| Moshi | 1.15.0 | JSON parsing |

### Camera & ML
| Library | Version | Purpose |
|---------|---------|---------|
| CameraX | 1.3.1 | Camera API |
| ML Kit Text Recognition | 16.0.0 | OCR |

### Concurrency
| Library | Version | Purpose |
|---------|---------|---------|
| Kotlin Coroutines | 1.7.3 | Async operations |
| Flow | (coroutines) | Reactive streams |

### UI
| Library | Version | Purpose |
|---------|---------|---------|
| Material Design 3 | 1.11.0 | UI components |
| ConstraintLayout | 2.1.4 | Layout |

### Security
| Library | Version | Purpose |
|---------|---------|---------|
| Security-Crypto | 1.1.0-alpha06 | Encrypted preferences |
| SQLCipher | 4.5.4 | Encrypted database |

### Testing
| Library | Version | Purpose |
|---------|---------|---------|
| JUnit | 4.13.2 | Unit testing |
| MockK | 1.13.8 | Mocking (Kotlin) |
| Truth | 1.1.5 | Assertions |
| Turbine | 1.0.0 | Flow testing |
| Espresso | 3.5.1 | UI testing |
| Robolectric | 4.11.1 | Android unit tests |

## Dependency Injection

### Hilt Structure

```
@HiltAndroidApp
YnabReceiptApp
    ↓
┌─────────────────────────────────────────────────┐
│              Application Scope                   │
│  @Singleton instances                            │
│  - Repository implementations                    │
│  - Network services (Retrofit, OkHttp)           │
│  - Database                                      │
│  - Preferences                                   │
│  - Dispatchers                                   │
└─────────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────────┐
│              Activity Scope                      │
│  @ActivityScoped                                 │
│  - Navigation controller                         │
│  - Activity utilities                            │
└─────────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────────┐
│              ViewModel Scope                     │
│  @ViewModelScoped                                │
│  - Use cases (auto-scoped per ViewModel)         │
└─────────────────────────────────────────────────┘
```

### Hilt Modules

**CoreModule** (core module):
- Provides: Coroutine dispatchers
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object CoreModule {
    @Provides
    @Singleton
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
```

**DatabaseModule** (data module):
- Provides: Room database, DAOs
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideYnabDatabase(@ApplicationContext context: Context): YnabDatabase
    
    @Provides
    fun provideReceiptDao(db: YnabDatabase): ReceiptDao
}
```

**NetworkModule** (data module):
- Provides: Retrofit, OkHttp, API services
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit
    
    @Provides
    @Singleton
    fun provideYnabApi(retrofit: Retrofit): YnabApi
}
```

**RepositoryModule** (data module):
- Binds: Repository interfaces to implementations
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindYnabRepository(impl: YnabRepositoryImpl): YnabRepository
}
```

**WorkerModule** (app module):
- Provides: WorkManager dependencies for HiltWorker

## Threading Model

### Coroutine Dispatchers

```kotlin
// Injected via Hilt
class SomeRepository @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher
) {
    suspend fun fetchData() = withContext(ioDispatcher) {
        // Network or DB operation
    }
}
```

### Dispatcher Usage:
- **Dispatchers.Main**: UI updates (ViewModels observing LiveData/Flow)
- **Dispatchers.IO**: Network requests, database operations, file I/O
- **Dispatchers.Default**: Heavy computation (parsing, encryption)

### Flow Usage:
```kotlin
// Repository returns Flow
fun observeReceipts(): Flow<List<Receipt>> = receiptDao.observeAll()
    .map { entities -> entities.map { it.toDomainModel() } }
    .flowOn(Dispatchers.IO)

// ViewModel collects
viewModelScope.launch {
    repository.observeReceipts()
        .collect { receipts ->
            _uiState.value = UiState.Success(receipts)
        }
}
```

## Error Handling

### Result Wrapper

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}

// Usage in Repository
override suspend fun getAccounts(budgetId: String): Result<List<YnabAccount>> {
    return try {
        val response = ynabApi.getAccounts(budgetId)
        Result.Success(response.data.accounts.map { it.toDomainModel() })
    } catch (e: Exception) {
        Result.Error(e)
    }
}

// Usage in ViewModel
viewModelScope.launch {
    when (val result = getAccountsUseCase(budgetId)) {
        is Result.Success -> _accounts.value = result.data
        is Result.Error -> _error.value = result.exception.message
    }
}
```

### Exception Hierarchy

```
AppException (base)
├── NetworkException
│   ├── NoInternetException
│   ├── TimeoutException
│   └── ApiException
├── DatabaseException
├── ValidationException
└── AuthException
    ├── InvalidTokenException
    └── TokenExpiredException
```

## Performance Considerations

### Database
- Use Room's `@Transaction` for atomic operations
- Index frequently queried columns
- Use `Flow` for reactive queries (efficient updates)
- Encrypt database with SQLCipher

### Network
- Cache responses with OkHttp cache
- Use conditional requests (ETags)
- Retry with exponential backoff
- Cancel requests when ViewModel is cleared

### Images
- Compress receipt images before saving
- Use proper image formats (JPEG for photos)
- Lazy load images in lists
- Clear image cache periodically

### Memory
- Use paging for large lists
- Avoid loading all receipts at once
- Use WeakReference where appropriate
- Profile with Memory Profiler

## Security

### Data Encryption
- **Tokens**: Stored in EncryptedSharedPreferences
- **Database**: Encrypted with SQLCipher
- **Images**: Stored in app-private directory

### Network Security
- HTTPS only (enforced by Network Security Config)
- Certificate pinning for YNAB API
- No sensitive data in logs (redacted in production)

### Code Obfuscation
- ProGuard rules configured
- R8 code shrinking enabled
- Keep rules for Retrofit, Moshi, Room

---

## Related Documentation
- [Module Structure](MODULE_STRUCTURE.md) - Detailed module breakdown
- [API Integration](API_INTEGRATION.md) - YNAB API usage
- [Developer Guide](DEVELOPER_GUIDE.md) - Getting started
- [Testing Guide](../TESTING_GUIDE.md) - Testing strategies
