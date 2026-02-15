# Module Structure Documentation

## Table of Contents
- [Overview](#overview)
- [Module Dependency Graph](#module-dependency-graph)
- [App Module](#app-module)
- [Domain Module](#domain-module)
- [Data Module](#data-module)
- [Core Module](#core-module)
- [Adding New Modules](#adding-new-modules)

## Overview

The YNAB Receipt Scanner project is organized into **4 distinct modules**, each with a specific purpose and clearly defined dependencies. This modular architecture provides several benefits:

✅ **Separation of Concerns**: Each module has a single, well-defined responsibility  
✅ **Faster Build Times**: Gradle can build modules in parallel  
✅ **Better Testability**: Modules can be tested in isolation  
✅ **Reusability**: Domain module is pure Kotlin and can be shared  
✅ **Enforced Boundaries**: Dependencies are enforced at compile-time

## Module Dependency Graph

```
┌─────────────────────────────────────────┐
│           app (Application)             │
│    Android UI Layer (MVVM Pattern)      │
│                                          │
│  Dependencies:                           │
│  - domain (use cases, models)            │
│  - data (repository implementations)     │
│  - core (utilities)                      │
│  - Android Jetpack (Fragments, etc)      │
│  - Material Design 3                     │
│  - CameraX, Navigation                   │
└────────┬────────────┬───────────────────┘
         │            │
         ▼            ▼
┌──────────────┐  ┌─────────────────────────┐
│    domain    │  │         data            │
│ (Pure Kotlin)│◀─│  (Data & API Layer)     │
│              │  │                          │
│ No Android   │  │  Dependencies:           │
│ dependencies │  │  - domain (interfaces)   │
│              │  │  - core (utilities)      │
│              │  │  - Retrofit, Room, etc   │
└──────┬───────┘  └────────┬────────────────┘
       │                   │
       └───────┬───────────┘
               ▼
      ┌─────────────────┐
      │      core       │
      │   (Utilities)   │
      │                 │
      │  Dependencies:  │
      │  - Kotlin stdlib│
      │  - Minimal      │
      └─────────────────┘
```

**Key Principle**: Dependencies only flow downward and inward. The `domain` module has no dependencies on other modules.

---

## App Module

### Purpose
The main application module containing all UI code, ViewModels, navigation, and Android-specific components.

### Location
`/app`

### Package Structure

```
com.ynab.receiptscanner/
├── YnabReceiptApp.kt                    # Application class (@HiltAndroidApp)
│
├── ui/                                  # All UI components
│   ├── base/
│   │   └── ViewBindingDelegate.kt      # View binding helper
│   │
│   ├── camera/
│   │   ├── CameraFragment.kt           # Receipt capture screen
│   │   ├── CameraViewModel.kt          # Camera state & logic
│   │   └── CameraPermissionHelper.kt   # Permission handling
│   │
│   ├── review/
│   │   ├── ReviewFragment.kt           # Edit OCR results
│   │   ├── ReviewViewModel.kt          # Field editing logic
│   │   ├── AccountPickerDialog.kt      # Account selection dialog
│   │   ├── CategoryPickerDialog.kt     # Category selection dialog
│   │   ├── AccountAdapter.kt           # Account list adapter
│   │   └── CategoryAdapter.kt          # Category list adapter
│   │
│   ├── home/
│   │   ├── HomeFragment.kt             # Receipt list screen
│   │   ├── HomeViewModel.kt            # Receipt list logic
│   │   ├── ReceiptAdapter.kt           # RecyclerView adapter
│   │   └── ReceiptDiffCallback.kt      # DiffUtil for efficient updates
│   │
│   ├── settings/
│   │   ├── SettingsFragment.kt         # Settings screen
│   │   └── SettingsViewModel.kt        # Settings logic
│   │
│   ├── onboarding/
│   │   ├── OnboardingActivity.kt       # First-time setup
│   │   ├── OnboardingViewModel.kt      # Onboarding logic
│   │   └── OnboardingPagerAdapter.kt   # ViewPager adapter
│   │
│   ├── about/
│   │   └── AboutFragment.kt            # About/version info
│   │
│   ├── sync/
│   │   └── SyncStatusView.kt           # Sync status display
│   │
│   └── common/                          # Reusable UI components
│       ├── EmptyStateView.kt           # Empty list placeholder
│       ├── LoadingDialog.kt            # Progress dialog
│       ├── ErrorDialog.kt              # Error message dialog
│       ├── ConfirmationDialog.kt       # Confirmation prompt
│       ├── SyncStatusBadge.kt          # Sync badge widget
│       ├── ErrorHandler.kt             # Error handling utilities
│       ├── SnackbarManager.kt          # Centralized snackbar
│       └── AnimationUtils.kt           # UI animations
│
├── worker/                              # Background work
│   ├── SyncWorker.kt                   # WorkManager sync job
│   ├── SyncScheduler.kt                # Schedule sync jobs
│   ├── CleanupWorker.kt                # Clean old data
│   └── WorkerModule.kt                 # Hilt DI for workers
│
├── notification/
│   └── NotificationHelper.kt           # Push notifications
│
├── performance/
│   ├── PerformanceMonitor.kt           # Performance tracking
│   └── LaunchTimeTracker.kt            # App launch metrics
│
└── util/
    ├── ImageCompressor.kt               # Compress receipt images
    └── VersionManager.kt                # App version handling
```

### Key Classes

#### YnabReceiptApp
- Entry point for dependency injection
- Initializes Hilt, WorkManager, Analytics
- Sets up crash reporting
- Configures app-level behavior

#### ViewModels
All ViewModels extend `ViewModel` and use `viewModelScope` for coroutines:
```kotlin
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val saveReceiptImageUseCase: SaveReceiptImageUseCase,
    private val extractTextUseCase: ExtractTextFromImageUseCase,
    private val parseReceiptUseCase: ParseReceiptUseCase
) : ViewModel() {
    // ViewModel logic
}
```

#### Workers
Background tasks using WorkManager:
- **SyncWorker**: Syncs pending transactions to YNAB
- **CleanupWorker**: Removes old receipt data
- Both use Hilt for dependency injection via HiltWorker

### Dependencies (build.gradle.kts)

```kotlin
dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":data"))
    
    // Android Jetpack
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    
    // UI
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    
    // Camera
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    
    // Dependency Injection
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.work)
    kapt(libs.androidx.hilt.compiler)
}
```

---

## Domain Module

### Purpose
Pure Kotlin module containing business logic, use cases, domain models, and repository interfaces. **No Android dependencies.**

### Location
`/domain`

### Package Structure

```
com.ynab.receiptscanner.domain/
├── model/                               # Domain models
│   ├── Receipt.kt                       # Scanned receipt
│   ├── ReceiptField.kt                  # Individual field (date, merchant, etc)
│   ├── Transaction.kt                   # YNAB transaction
│   ├── YnabAccount.kt                   # Bank/credit account
│   ├── YnabCategory.kt                  # Budget category
│   ├── YnabBudget.kt                    # Budget
│   ├── YnabCategoryGroup.kt             # Category group
│   ├── SyncStatus.kt                    # Transaction sync state
│   ├── ConnectivityStatus.kt            # Network state
│   ├── AuthStatus.kt                    # Authentication state
│   └── OcrResult.kt                     # OCR extraction result
│
├── usecase/                             # Business operations
│   ├── auth/
│   │   ├── AuthenticateWithYnabUseCase.kt
│   │   ├── GetAuthStatusUseCase.kt
│   │   └── SignOutUseCase.kt
│   │
│   ├── receipt/
│   │   ├── SaveReceiptUseCase.kt
│   │   ├── SaveReceiptImageUseCase.kt
│   │   ├── GetAllReceiptsUseCase.kt
│   │   ├── ExtractTextFromImageUseCase.kt
│   │   └── ParseReceiptUseCase.kt
│   │
│   ├── transaction/
│   │   ├── CreateTransactionUseCase.kt
│   │   ├── CheckDuplicateTransactionUseCase.kt
│   │   └── SyncPendingTransactionsUseCase.kt
│   │
│   ├── ynab/
│   │   ├── GetBudgetsUseCase.kt
│   │   ├── GetAccountsUseCase.kt
│   │   └── GetCategoriesUseCase.kt
│   │
│   └── sync/
│       ├── GetSyncStatusUseCase.kt
│       ├── RetrySyncUseCase.kt
│       ├── ObserveConnectivityUseCase.kt
│       └── TriggerSyncOnConnectivityUseCase.kt
│
├── repository/                          # Repository interfaces
│   ├── YnabRepository.kt               # YNAB API operations
│   ├── ReceiptRepository.kt            # Receipt storage
│   ├── OcrRepository.kt                # Text extraction
│   └── ConnectivityRepository.kt       # Network monitoring
│
└── validator/                           # Business rules validation
    ├── ReceiptValidator.kt             # Validate receipt data
    └── TransactionValidator.kt         # Validate transactions
```

### Key Patterns

#### Use Case Pattern
All use cases follow this structure:
```kotlin
class CreateTransactionUseCase @Inject constructor(
    private val ynabRepository: YnabRepository,
    private val receiptRepository: ReceiptRepository
) {
    suspend operator fun invoke(
        receipt: Receipt,
        accountId: String,
        categoryId: String?
    ): Result<Transaction> {
        // Business logic here
    }
}
```

#### Repository Interface
```kotlin
interface YnabRepository {
    suspend fun getAccounts(budgetId: String): Result<List<YnabAccount>>
    suspend fun createTransaction(transaction: Transaction): Result<Transaction>
    // ... other methods
}
```

### Domain Models

All models are **data classes** with:
- Immutability (val properties)
- No Android types (Parcelable, etc.)
- Business logic methods when needed
- Validation logic

Example:
```kotlin
data class Receipt(
    val id: String,
    val timestamp: Long,
    val merchantName: String?,
    val amount: Double?,
    val date: String?,
    val imagePath: String,
    val rawOcrText: String,
    val fields: List<ReceiptField>
) {
    fun isValid(): Boolean = 
        amount != null && amount > 0 && !merchantName.isNullOrBlank()
}
```

### Dependencies (build.gradle.kts)

```kotlin
dependencies {
    // Pure Kotlin - No Android dependencies
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    
    // Dependency Injection (annotations only)
    implementation(libs.javax.inject)
}
```

---

## Data Module

### Purpose
Implements data access layer with repository implementations, API clients, database, and data sources.

### Location
`/data`

### Package Structure

```
com.ynab.receiptscanner.data/
├── repository/                          # Repository implementations
│   ├── YnabRepositoryImpl.kt
│   ├── ReceiptRepositoryImpl.kt
│   ├── OcrRepositoryImpl.kt
│   └── ConnectivityRepositoryImpl.kt
│
├── remote/                              # Network layer
│   ├── YnabApi.kt                      # Retrofit interface
│   ├── OAuth2Service.kt                # OAuth flow
│   │
│   ├── dto/                            # Data Transfer Objects
│   │   ├── AccountDto.kt
│   │   ├── TransactionDto.kt
│   │   ├── CategoryDto.kt
│   │   ├── BudgetDto.kt
│   │   ├── YnabResponse.kt             # Generic API response
│   │   ├── CreateTransactionRequest.kt
│   │   └── ErrorResponse.kt
│   │
│   ├── interceptor/                    # OkHttp interceptors
│   │   ├── AuthInterceptor.kt         # Add Bearer token
│   │   ├── ErrorInterceptor.kt        # Parse errors
│   │   ├── RetryInterceptor.kt        # Retry logic
│   │   └── CacheInterceptor.kt        # Cache responses
│   │
│   └── mapper/                         # DTO ↔ Domain mappers
│       ├── AccountMapper.kt
│       ├── TransactionMapper.kt
│       └── CategoryMapper.kt
│
├── local/                               # Database layer
│   ├── YnabDatabase.kt                 # Room database
│   │
│   ├── dao/                            # Data Access Objects
│   │   ├── ReceiptDao.kt
│   │   ├── PendingTransactionDao.kt
│   │   ├── AccountDao.kt
│   │   └── CategoryDao.kt
│   │
│   ├── entity/                         # Database entities
│   │   ├── ReceiptEntity.kt
│   │   ├── PendingTransactionEntity.kt
│   │   ├── AccountEntity.kt
│   │   └── CategoryEntity.kt
│   │
│   ├── converter/                      # Room type converters
│   │   └── Converters.kt
│   │
│   ├── migration/
│   │   └── Migrations.kt               # Database migrations
│   │
│   └── preference/                     # SharedPreferences
│       ├── AuthPreferences.kt          # Token storage (encrypted)
│       └── AppPreferences.kt           # App settings
│
├── storage/                             # File storage
│   └── ImageStorageManager.kt          # Receipt image management
│
├── security/                            # Security utilities
│   ├── EncryptionHelper.kt             # Encrypt sensitive data
│   └── SecurityChecks.kt               # Security validations
│
├── sync/                                # Sync logic
│   ├── TransactionSyncManager.kt       # Orchestrate sync
│   └── SyncStatusTracker.kt            # Track sync progress
│
├── network/                             # Network utilities
│   └── ConnectivityMonitor.kt          # Monitor network changes
│
├── cache/                               # Caching
│   └── MemoryCache.kt                  # In-memory cache
│
├── analytics/                           # Analytics
│   ├── AnalyticsManager.kt             # Interface
│   ├── AnalyticsEvent.kt               # Event definitions
│   ├── FirebaseAnalyticsImpl.kt        # Firebase implementation
│   └── CrashlyticsManager.kt           # Crash reporting
│
└── di/                                  # Dependency injection
    ├── DatabaseModule.kt
    ├── NetworkModule.kt
    ├── RepositoryModule.kt
    └── StorageModule.kt
```

### Key Components

#### Repository Implementation
```kotlin
class YnabRepositoryImpl @Inject constructor(
    private val ynabApi: YnabApi,
    private val accountDao: AccountDao,
    private val authPreferences: AuthPreferences,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : YnabRepository {
    
    override suspend fun getAccounts(budgetId: String): Result<List<YnabAccount>> = 
        withContext(ioDispatcher) {
            try {
                // Try network first
                val response = ynabApi.getAccounts(budgetId)
                val accounts = response.data.accounts.map { it.toDomainModel() }
                
                // Cache in database
                accountDao.insertAll(accounts.map { it.toEntity() })
                
                Result.Success(accounts)
            } catch (e: IOException) {
                // Fall back to cache
                val cached = accountDao.getAll()
                if (cached.isNotEmpty()) {
                    Result.Success(cached.map { it.toDomainModel() })
                } else {
                    Result.Error(e)
                }
            }
        }
}
```

#### Room Database
```kotlin
@Database(
    entities = [
        ReceiptEntity::class,
        PendingTransactionEntity::class,
        AccountEntity::class,
        CategoryEntity::class
    ],
    version = 1
)
@TypeConverters(Converters::class)
abstract class YnabDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao
    abstract fun pendingTransactionDao(): PendingTransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
}
```

### Dependencies (build.gradle.kts)

```kotlin
dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    
    // Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)
    implementation(libs.sqlcipher)
    
    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi)
    kapt(libs.moshi.codegen)
    
    // Security
    implementation(libs.androidx.security.crypto)
    
    // Dependency Injection
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}
```

---

## Core Module

### Purpose
Shared utilities, extensions, and base classes used across all modules.

### Location
`/core`

### Package Structure

```
com.ynab.receiptscanner.core/
├── Constants.kt                        # App-wide constants
│
├── util/
│   ├── Result.kt                       # Result wrapper sealed class
│   ├── Extensions.kt                   # Kotlin extensions
│   └── NetworkUtils.kt                 # Network helpers
│
└── di/
    └── CoreModule.kt                   # Hilt core module (dispatchers)
```

### Key Components

#### Result Wrapper
```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
    
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
    }
}
```

#### Constants
```kotlin
object Constants {
    // API
    const val YNAB_API_BASE_URL = "https://api.youneedabudget.com/v1/"
    const val OAUTH_AUTHORIZE_URL = "https://app.youneedabudget.com/oauth/authorize"
    const val OAUTH_TOKEN_URL = "https://app.youneedabudget.com/oauth/token"
    
    // Database
    const val DATABASE_NAME = "ynab_receipt_scanner.db"
    
    // Validation
    const val MAX_RECEIPT_AGE_DAYS = 90
    const val MIN_AMOUNT = 0.01
    const val MAX_AMOUNT = 1_000_000.00
    
    // Sync
    const val SYNC_INTERVAL_MINUTES = 15L
    const val MAX_RETRY_ATTEMPTS = 3
}
```

#### Extensions
```kotlin
// String extensions
fun String.toDate(pattern: String = "yyyy-MM-dd"): Date? = ...
fun String.isValidEmail(): Boolean = ...

// Number extensions
fun Double.toCurrency(): String = NumberFormat.getCurrencyInstance().format(this)

// Context extensions
fun Context.showToast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
```

### Dependencies (build.gradle.kts)

```kotlin
dependencies {
    // Minimal dependencies
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.core.ktx)
    
    // Dependency Injection
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}
```

---

## Adding New Modules

### Step 1: Create Module Directory
```bash
mkdir -p new-module/src/main/java/com/ynab/receiptscanner/newmodule
```

### Step 2: Create build.gradle.kts
```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.ynab.receiptscanner.newmodule"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Add dependencies
}
```

### Step 3: Update settings.gradle.kts
```kotlin
include(":app")
include(":core")
include(":domain")
include(":data")
include(":new-module")  // Add this line
```

### Step 4: Add AndroidManifest.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest package="com.ynab.receiptscanner.newmodule" />
```

### Step 5: Depend on Module
In `app/build.gradle.kts`:
```kotlin
dependencies {
    implementation(project(":new-module"))
}
```

---

## Module Best Practices

### 1. **Keep Domain Pure**
- No Android imports in domain module
- Only pure Kotlin and coroutines
- Makes code more testable and reusable

### 2. **Use Interfaces**
- Define repository interfaces in domain
- Implement in data module
- Easy to mock for testing

### 3. **Single Responsibility**
- Each module has one clear purpose
- Don't mix concerns across modules
- Easier to understand and maintain

### 4. **Minimize Dependencies**
- Only add necessary dependencies
- Avoid circular dependencies
- Keep module graph simple

### 5. **Version Catalog**
- Use `libs.versions.toml` for all dependencies
- Consistent versions across modules
- Easy to update

---

## Related Documentation
- [Architecture](ARCHITECTURE.md) - Overall architecture
- [Developer Guide](DEVELOPER_GUIDE.md) - Getting started
- [Testing Guide](../TESTING_GUIDE.md) - Testing each module
