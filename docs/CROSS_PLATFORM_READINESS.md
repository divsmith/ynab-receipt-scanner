# Cross-Platform Readiness Guide

## Table of Contents
- [Overview](#overview)
- [Current Architecture Benefits](#current-architecture-benefits)
- [Kotlin Multiplatform Migration Path](#kotlin-multiplatform-migration-path)
- [Shared Components](#shared-components)
- [Platform-Specific Boundaries](#platform-specific-boundaries)
- [Migration Strategy](#migration-strategy)
- [iOS Implementation Guide](#ios-implementation-guide)
- [Web Platform Considerations](#web-platform-considerations)

## Overview

The YNAB Receipt Scanner is architected with **cross-platform readiness** in mind. The current Android implementation uses **Clean Architecture** with a **pure Kotlin domain layer**, making it straightforward to share business logic across platforms using **Kotlin Multiplatform** (KMP).

### Why Cross-Platform Ready?

✅ **Clean Architecture**: Business logic is separate from UI  
✅ **Pure Kotlin Domain**: No Android dependencies in domain module  
✅ **Repository Pattern**: Abstract platform differences  
✅ **Use Case Pattern**: Reusable business operations  
✅ **Modern Tech Stack**: Compatible with KMP

### Target Platforms
- ✅ **Android** (Current implementation)
- 🎯 **iOS** (SwiftUI + KMP shared business logic)
- 🎯 **Web** (React/Vue + WASM)
- 🎯 **Desktop** (Compose Multiplatform)

## Current Architecture Benefits

### 1. Domain Module is 100% Shareable
The `domain` module has **zero Android dependencies**:

```kotlin
// ✅ Pure Kotlin - Can run on any platform
data class Receipt(
    val id: String,
    val merchantName: String?,
    val amount: Double?,
    val date: String?
)

class CreateTransactionUseCase(
    private val ynabRepository: YnabRepository
) {
    suspend operator fun invoke(receipt: Receipt): Result<Transaction> {
        // Business logic - platform independent
    }
}
```

### 2. Repository Interfaces Define Contracts
Platform implementations provide the concrete behavior:

```kotlin
// Shared (domain module)
interface YnabRepository {
    suspend fun createTransaction(txn: Transaction): Result<Transaction>
}

// Android implementation (data module)
class YnabRepositoryAndroid : YnabRepository {
    override suspend fun createTransaction(...) = 
        // Android-specific: Retrofit, Room, etc.
}

// iOS implementation (future)
class YnabRepositoryIOS : YnabRepository {
    override suspend fun createTransaction(...) = 
        // iOS-specific: URLSession, CoreData, etc.
}
```

### 3. OCR Abstraction Already in Place
The OCR interface is already abstracted:

```kotlin
// Shared interface
interface OcrRepository {
    suspend fun extractText(imagePath: String): Result<OcrResult>
}

// Android: ML Kit
class MlKitOcrRepository : OcrRepository { ... }

// iOS: VisionKit
class VisionKitOcrRepository : OcrRepository { ... }
```

## Kotlin Multiplatform Migration Path

### Phase 1: Convert Domain Module to KMP

**Current structure:**
```
domain/
├── build.gradle.kts  (Android library)
└── src/
    └── main/
        └── java/
```

**KMP structure:**
```
domain/
├── build.gradle.kts  (Kotlin Multiplatform)
└── src/
    ├── commonMain/      # Shared code (current domain code moves here)
    │   └── kotlin/
    ├── androidMain/     # Android-specific (if any)
    │   └── kotlin/
    ├── iosMain/         # iOS-specific (if any)
    │   └── kotlin/
    └── commonTest/      # Shared tests
        └── kotlin/
```

**Updated build.gradle.kts:**
```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    androidTarget()
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Domain"
            isStatic = true
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        
        androidMain.dependencies {
            // Android-specific if needed
        }
        
        iosMain.dependencies {
            // iOS-specific if needed
        }
    }
}
```

### Phase 2: Create Shared Data Module

Extract platform-independent data logic:

```
data-shared/  (NEW KMP module)
├── src/
    ├── commonMain/
    │   ├── networking/      # HTTP client abstraction
    │   ├── storage/         # Key-value storage abstraction
    │   └── serialization/   # JSON parsing (shared)
    │
    ├── androidMain/
    │   ├── networking/      # OkHttp implementation
    │   └── storage/         # SharedPreferences/Room
    │
    └── iosMain/
        ├── networking/      # URLSession implementation
        └── storage/         # UserDefaults/CoreData
```

### Phase 3: Platform-Specific UI

Each platform keeps its own UI layer:

```
Android (existing):
app/src/main/java/ui/  → Fragments, ViewModels, Compose

iOS (new):
ios-app/               → SwiftUI, ViewModels
```

## Shared Components

### What Can Be Shared (90% of Code)

#### ✅ Domain Layer (100% Shareable)
```kotlin
// All use cases
- AuthenticateWithYnabUseCase
- CreateTransactionUseCase
- ParseReceiptUseCase
- SyncPendingTransactionsUseCase
- GetAccountsUseCase
- GetCategoriesUseCase
// ... all other use cases

// All domain models
- Receipt, Transaction, YnabAccount, YnabCategory
- SyncStatus, ConnectivityStatus, AuthStatus

// All validators
- ReceiptValidator, TransactionValidator

// Repository interfaces
- YnabRepository, ReceiptRepository, OcrRepository
```

#### ✅ Network Layer (Shareable with Ktor)
Use **Ktor** instead of Retrofit for cross-platform HTTP:

```kotlin
// Shared in commonMain
class YnabApiClient(private val httpClient: HttpClient) {
    suspend fun getAccounts(budgetId: String): AccountsResponse {
        return httpClient.get("$BASE_URL/budgets/$budgetId/accounts") {
            header("Authorization", "Bearer $token")
        }.body()
    }
}

// Platform-specific engine
// Android: httpClient(Android)
// iOS: httpClient(Darwin)
```

#### ✅ Business Logic
- Receipt parsing algorithms
- Transaction validation
- Duplicate detection
- Amount extraction logic
- Date parsing
- Currency handling

#### ✅ Data Models & Serialization
- JSON parsing (kotlinx.serialization)
- DTO models
- Mappers

### What Needs Platform Implementation

#### ❌ UI Layer
- **Android**: Jetpack Compose or XML layouts
- **iOS**: SwiftUI
- **Web**: React/Vue components

Each platform has its own UI framework and patterns.

#### ❌ Camera
- **Android**: CameraX
- **iOS**: AVFoundation or UIImagePickerController

Abstraction:
```kotlin
expect class CameraProvider {
    suspend fun captureImage(): ImageData
}

// Android implementation
actual class CameraProvider {
    actual suspend fun captureImage(): ImageData {
        // CameraX implementation
    }
}

// iOS implementation
actual class CameraProvider {
    actual suspend fun captureImage(): ImageData {
        // AVFoundation implementation
    }
}
```

#### ❌ OCR
- **Android**: ML Kit Text Recognition
- **iOS**: VisionKit Text Recognition

Already abstracted via `OcrRepository` interface.

#### ❌ Local Storage
- **Android**: Room (SQLite)
- **iOS**: CoreData or SQLite.swift

Abstraction:
```kotlin
expect class DatabaseDriver

// Android: Room
actual class DatabaseDriver {
    actual fun createDriver(): SqlDriver = 
        AndroidSqliteDriver(YnabDatabase.Schema, context, "ynab.db")
}

// iOS: SQLite
actual class DatabaseDriver {
    actual fun createDriver(): SqlDriver = 
        NativeSqliteDriver(YnabDatabase.Schema, "ynab.db")
}
```

Use **SQLDelight** for shared SQL across platforms.

#### ❌ Secure Storage
- **Android**: EncryptedSharedPreferences
- **iOS**: Keychain

Abstraction:
```kotlin
expect class SecureStorage {
    fun saveToken(token: String)
    fun getToken(): String?
    fun clearToken()
}
```

#### ❌ File System
- **Android**: Context.filesDir
- **iOS**: FileManager.default

Abstraction for image storage:
```kotlin
expect class FileStorage {
    suspend fun saveImage(data: ByteArray, name: String): String
    suspend fun loadImage(path: String): ByteArray?
    suspend fun deleteImage(path: String)
}
```

## Platform-Specific Boundaries

### Clear Separation

```
┌──────────────────────────────────────────────┐
│         Platform-Specific Layer              │
│  - UI (SwiftUI / Compose)                    │
│  - Platform APIs (Camera, OCR)               │
│  - Storage implementations                   │
│  - Dependency Injection setup                │
└──────────────────────────────────────────────┘
                    ↓ (interfaces)
┌──────────────────────────────────────────────┐
│           Shared Business Logic              │
│  - Use Cases (90% of code)                   │
│  - Domain Models                             │
│  - Validators                                │
│  - Repository Interfaces                     │
│  - Network layer (Ktor)                      │
│  - JSON serialization                        │
└──────────────────────────────────────────────┘
```

### Dependency Injection

#### Android (Current)
```kotlin
@HiltAndroidApp
class YnabReceiptApp : Application()

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    fun provideYnabRepository(...): YnabRepository = 
        YnabRepositoryAndroid(...)
}
```

#### iOS (Future)
```swift
class AppDependencies {
    let ynabRepository: YnabRepository
    
    init() {
        self.ynabRepository = YnabRepositoryIOS(...)
    }
}

// Or use a Swift DI library like Resolver or Swinject
```

## Migration Strategy

### Recommended Approach: Gradual Migration

#### Step 1: Setup KMP Project Structure (1-2 weeks)
- Add KMP plugin to project
- Create `commonMain`, `androidMain`, `iosMain` source sets
- Configure iOS framework generation

#### Step 2: Move Domain Module to KMP (1 week)
- Move all domain code to `commonMain`
- Update imports (remove Android-specific)
- Verify builds for both Android and iOS targets

#### Step 3: Share Network Layer (1 week)
- Replace Retrofit with Ktor
- Implement platform-specific HTTP engines
- Test network calls on both platforms

#### Step 4: Abstract Storage Layer (1-2 weeks)
- Use SQLDelight for shared database
- Implement platform-specific drivers
- Migrate existing data access logic

#### Step 5: Implement Platform-Specific Features (2-3 weeks)
- OCR implementations (ML Kit vs VisionKit)
- Camera providers
- Secure storage
- File management

#### Step 6: Build iOS UI (4-6 weeks)
- SwiftUI screens matching Android app
- ViewModels calling shared use cases
- Platform-specific styling

#### Step 7: Testing & Polish (2 weeks)
- Shared unit tests
- Platform-specific UI tests
- Bug fixes and refinements

**Total Estimate**: ~10-14 weeks for iOS app

## iOS Implementation Guide

### iOS Project Structure

```
ios-app/
├── YnabReceiptScanner/
│   ├── App/
│   │   ├── YnabReceiptScannerApp.swift  # App entry point
│   │   └── AppDependencies.swift        # DI container
│   │
│   ├── Views/                           # SwiftUI views
│   │   ├── Camera/
│   │   │   └── CameraView.swift
│   │   ├── Review/
│   │   │   └── ReviewView.swift
│   │   ├── Home/
│   │   │   └── HomeView.swift
│   │   └── Settings/
│   │       └── SettingsView.swift
│   │
│   ├── ViewModels/                      # Swift ViewModels
│   │   ├── CameraViewModel.swift
│   │   ├── ReviewViewModel.swift
│   │   ├── HomeViewModel.swift
│   │   └── SettingsViewModel.swift
│   │
│   ├── Platform/                        # Platform implementations
│   │   ├── CameraProvider.swift
│   │   ├── VisionKitOcrRepository.swift
│   │   ├── KeychainStorage.swift
│   │   └── FileStorageIOS.swift
│   │
│   └── Resources/
│       ├── Assets.xcassets
│       └── Localizable.strings
│
└── Frameworks/
    └── Domain.framework  # Generated from KMP domain module
```

### Example iOS ViewModel

```swift
import SwiftUI
import Domain  // Shared KMP framework

class ReviewViewModel: ObservableObject {
    @Published var receipt: Receipt
    @Published var accounts: [YnabAccount] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let createTransactionUseCase: CreateTransactionUseCase
    private let getAccountsUseCase: GetAccountsUseCase
    
    init(
        receipt: Receipt,
        createTransactionUseCase: CreateTransactionUseCase,
        getAccountsUseCase: GetAccountsUseCase
    ) {
        self.receipt = receipt
        self.createTransactionUseCase = createTransactionUseCase
        self.getAccountsUseCase = getAccountsUseCase
    }
    
    func loadAccounts(budgetId: String) {
        isLoading = true
        
        Task {
            let result = try await getAccountsUseCase.invoke(budgetId: budgetId)
            
            await MainActor.run {
                switch result {
                case let success as ResultSuccess<[YnabAccount]>:
                    self.accounts = success.data
                case let error as ResultError:
                    self.errorMessage = error.exception.message
                default:
                    break
                }
                self.isLoading = false
            }
        }
    }
    
    func createTransaction(accountId: String, categoryId: String?) {
        // Call shared use case
        Task {
            let result = try await createTransactionUseCase.invoke(
                receipt: receipt,
                accountId: accountId,
                categoryId: categoryId
            )
            // Handle result
        }
    }
}
```

### OCR Implementation (iOS)

```swift
import Vision

class VisionKitOcrRepository: OcrRepository {
    func extractText(imagePath: String) async -> Result<OcrResult> {
        guard let image = UIImage(contentsOfFile: imagePath),
              let cgImage = image.cgImage else {
            return ResultError(exception: ImageLoadException())
        }
        
        return await withCheckedContinuation { continuation in
            let request = VNRecognizeTextRequest { request, error in
                if let error = error {
                    continuation.resume(returning: ResultError(exception: error))
                    return
                }
                
                guard let observations = request.results as? [VNRecognizedTextObservation] else {
                    continuation.resume(returning: ResultError(exception: OcrException()))
                    return
                }
                
                let text = observations.compactMap { $0.topCandidates(1).first?.string }
                    .joined(separator: "\n")
                
                let result = OcrResult(text: text, confidence: 0.85)
                continuation.resume(returning: ResultSuccess(data: result))
            }
            
            request.recognitionLevel = .accurate
            
            let handler = VNImageRequestHandler(cgImage: cgImage)
            try? handler.perform([request])
        }
    }
}
```

## Web Platform Considerations

### Browser Limitations
- **No camera access** (use web file picker or webcam API)
- **No native OCR** (use Tesseract.js or cloud OCR)
- **Limited storage** (IndexedDB instead of SQLite)

### Kotlin/JS or WASM
- Compile shared Kotlin to JavaScript or WebAssembly
- Use Ktor JS client for networking
- Call from React/Vue components

### Example Architecture
```
React App
    ↓
JavaScript interop
    ↓
Kotlin/JS shared module (domain + data)
    ↓
YNAB API
```

## Testing Across Platforms

### Shared Tests
```kotlin
// commonTest/kotlin/
class CreateTransactionUseCaseTest {
    @Test
    fun `creates transaction successfully`() = runTest {
        // Test runs on Android, iOS, JS
        val useCase = CreateTransactionUseCase(mockRepository)
        val result = useCase(mockReceipt, "account-123", "cat-456")
        assertTrue(result is Result.Success)
    }
}
```

### Platform-Specific Tests
- **Android**: JUnit + Robolectric + Espresso
- **iOS**: XCTest + XCTestUI
- **Web**: Jest + Playwright

## Benefits of Cross-Platform Approach

### ✅ Faster Feature Development
- Write business logic once
- Implement UI twice (or more)
- Bugs fixed in one place

### ✅ Consistent Behavior
- Same business rules on all platforms
- Same YNAB API calls
- Same validation logic

### ✅ Easier Testing
- Test business logic once in Kotlin
- Only test UI separately

### ✅ Better Maintainability
- Single source of truth for logic
- Fewer codebases to maintain
- Easier to add new platforms

### ✅ Team Efficiency
- Backend/Kotlin devs contribute to all platforms
- Share knowledge and code reviews
- Smaller codebase overall

## Estimated Code Sharing

| Layer | Shareable | Platform-Specific |
|-------|-----------|-------------------|
| Domain (Use Cases, Models) | 100% | 0% |
| Data (Networking, Serialization) | 80% | 20% |
| Storage (Database, Preferences) | 60% | 40% |
| Platform APIs (Camera, OCR) | 0% | 100% |
| UI | 0% | 100% |
| **Overall** | **60-70%** | **30-40%** |

## Recommendations

### For iOS Development
1. **Use SwiftUI** for UI (modern, declarative like Compose)
2. **Keep ViewModels thin** (delegate to shared use cases)
3. **Match Android UX** (same flows, similar navigation)
4. **Platform-specific polish** (iOS HIG compliance)

### For Web Development
1. **Use React or Vue** (mature ecosystems)
2. **Progressive Web App** (offline support)
3. **Responsive design** (works on desktop and mobile browsers)
4. **Consider server-side rendering** for SEO

### General Tips
1. Start with domain module migration
2. Use Ktor for networking (not Retrofit)
3. Use SQLDelight for databases (not Room)
4. Use kotlinx.serialization (not Moshi)
5. Test shared code thoroughly before platform integration

---

## Related Documentation
- [Architecture](ARCHITECTURE.md) - Overall architecture
- [Module Structure](MODULE_STRUCTURE.md) - Current modules
- [Developer Guide](DEVELOPER_GUIDE.md) - Getting started
