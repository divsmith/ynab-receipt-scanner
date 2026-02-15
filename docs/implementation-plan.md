# Implementation Plan for YNAB Receipt Scanner

## Project Overview
Android-first mobile application using Kotlin for on-device receipt OCR and YNAB integration. The architecture is designed to be modular and ready for future cross-platform expansion.

---

## Phase 0: Project Foundation & Setup

### Step 0.1: Initial Project Creation & Build Configuration
**Files:**
- `settings.gradle.kts` (created)
- `build.gradle.kts` (root level, created)
- `app/build.gradle.kts` (created)
- `gradle/libs.versions.toml` (created)
- `.gitignore` (created)
- `gradle.properties` (created)
- `app/proguard-rules.pro` (created)

**Dependencies:** None

**Technical details:**
- Create Android project with minimum SDK 24 (Android 7.0), target SDK 34
- Enable Kotlin 1.9+, Coroutines, and ViewBinding
- Configure version catalog for dependency management
- Gradle build setup with:
  - Kotlin Android Plugin
  - Hilt for dependency injection
  - Room for local database
  - Retrofit for networking
  - Google ML Kit Text Recognition

### Step 0.2: Define Module Structure
**Files:**
- `settings.gradle.kts` (modified)
- `core/build.gradle.kts` (created)
- `data/build.gradle.kts` (created)
- `domain/build.gradle.kts` (created)

**Dependencies:** Step 0.1

**Technical details:**
- Multi-module architecture:
  - `:app` - UI layer (Activities, Fragments, ViewModels)
  - `:core` - Core utilities, extensions, constants
  - `:data` - Data layer (repositories, data sources, Room, network)
  - `:domain` - Business logic (use cases, models)
- Clean Architecture with clear separation of concerns
- Dependency graph: app → data → domain ← core

### Step 0.3: Core Package Structure & Base Classes
**Files:**
- `core/src/main/java/com/ynab/receiptscanner/core/di/CoreModule.kt` (created)
- `core/src/main/java/com/ynab/receiptscanner/core/util/Result.kt` (created)
- `core/src/main/java/com/ynab/receiptscanner/core/util/Extensions.kt` (created)
- `core/src/main/java/com/ynab/receiptscanner/core/Constants.kt` (created)
- `app/src/main/AndroidManifest.xml` (created)
- `app/src/main/java/com/ynab/receiptscanner/YnabReceiptApp.kt` (created)

**Dependencies:** Step 0.2

**Technical details:**
- Set up Hilt Application class
- Define sealed Result class for error handling
- Core utility extensions for common operations
- Application constants (API URLs, timeouts, etc.)

---

## Phase 1: Data Layer Foundation

### Step 1.1: Domain Models
**Files:**
- `domain/src/main/java/com/ynab/receiptscanner/domain/model/Receipt.kt` (created)
- `domain/src/main/java/com/ynab/receiptscanner/domain/model/ReceiptField.kt` (created)
- `domain/src/main/java/com/ynab/receiptscanner/domain/model/OcrResult.kt` (created)
- `domain/src/main/java/com/ynab/receiptscanner/domain/model/YnabTransaction.kt` (created)
- `domain/src/main/java/com/ynab/receiptscanner/domain/model/YnabAccount.kt` (created)
- `domain/src/main/java/com/ynab/receiptscanner/domain/model/YnabCategory.kt` (created)
- `domain/src/main/java/com/ynab/receiptscanner/domain/model/SyncStatus.kt` (created)

**Dependencies:** Step 0.3

**Technical details:**
- Define core domain entities (Receipt, Transaction, Account)
- Receipt model includes: id, payee, amount, date, currency, tax, lineItems, imagePath, ocrText
- SyncStatus enum: PENDING, SYNCING, SYNCED, FAILED
- All models are Kotlin data classes with proper nullability

### Step 1.2: Local Database Schema (Room)
**Files:**
- `data/src/main/java/com/ynab/receiptscanner/data/local/YnabDatabase.kt` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/local/entity/ReceiptEntity.kt` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/local/entity/LineItemEntity.kt` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/local/entity/PendingTransactionEntity.kt` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/local/dao/ReceiptDao.kt` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/local/dao/PendingTransactionDao.kt` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/local/converter/Converters.kt` (created)

**Dependencies:** Step 1.1

**Technical details:**
- Room database with encrypted storage using SQLCipher
- Tables: receipts, line_items, pending_transactions
- DAOs with Flow-based queries for reactive updates
- Type converters for Date, Currency, enum types
- Migration strategy defined

### Step 1.3: Secure Storage for Auth & Encryption
**Files:**
- `data/src/main/java/com/ynab/receiptscanner/data/security/SecureStorage.kt` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/security/KeystoreManager.kt` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/security/EncryptionHelper.kt` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/local/preference/AuthPreferences.kt` (created)

**Dependencies:** Step 1.2

**Technical details:**
- Use Android Keystore for key generation and storage
- AES-256-GCM encryption for images and sensitive data
- EncryptedSharedPreferences for OAuth tokens
- Biometric authentication support for app access (optional)

---

## Phase 2: OCR & Image Processing

### Step 2.1: Camera Integration & Image Capture
**Files:**
- `app/src/main/java/com/ynab/receiptscanner/ui/camera/CameraActivity.kt` (created)
- `app/src/main/java/com/ynab/receiptscanner/ui/camera/CameraFragment.kt` (created)
- `app/src/main/java/com/ynab/receiptscanner/ui/camera/CameraViewModel.kt` (created)
- `app/src/main/res/layout/activity_camera.xml` (created)
- `app/src/main/res/layout/fragment_camera.xml` (created)
- `app/src/main/AndroidManifest.xml` (modified - add camera permissions)

**Dependencies:** Step 0.3, Phase 1

**Technical details:**
- CameraX library for modern camera API
- Guided framing overlay with receipt boundary detection
- Auto-capture when document edges detected
- Manual capture button fallback
- Preview with image quality assessment
- Permissions: CAMERA (runtime permission request)

### Step 2.2: Image Preprocessing Pipeline
**Files:**
- `data/src/main/java/com/ynab/receiptscanner/data/ocr/ImagePreprocessor.kt` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/ocr/EdgeDetector.kt` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/ocr/ImageEnhancer.kt` (created)
- `data/src/test/java/com/ynab/receiptscanner/data/ocr/ImagePreprocessorTest.kt` (created)

**Dependencies:** Step 2.1

**Technical details:**
- OpenCV Android SDK integration for image processing
- Pipeline: grayscale → noise reduction → edge detection → perspective correction → contrast enhancement
- Document boundary detection using Canny edge detection
- Adaptive thresholding for better OCR accuracy
- Quality scoring to validate image readiness

### Step 2.3: ML Kit OCR Integration
**Files:**
- `data/src/main/java/com/ynab/receiptscanner/data/ocr/OcrEngine.kt` (created - interface)
- `data/src/main/java/com/ynab/receiptscanner/data/ocr/MLKitOcrEngine.kt` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/ocr/OcrAdapter.kt` (created)
- `domain/src/main/java/com/ynab/receiptscanner/domain/usecase/ExtractTextFromImageUseCase.kt` (created)

**Dependencies:** Step 2.2

**Technical details:**
- Google ML Kit Text Recognition v2 (on-device)
- Abstract OcrEngine interface for iOS/other engine compatibility
- Extract Text, Block, Line, Element hierarchy
- Confidence scores for each recognized element
- Bounding box coordinates preserved for UI highlighting
- Supports Latin script (extensible to other scripts)

### Step 2.4: Receipt Parser - Field Extraction
**Files:**
- `data/src/main/java/com/ynab/receiptscanner/data/parser/ReceiptParser.kt` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/parser/AmountExtractor.kt` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/parser/DateExtractor.kt` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/parser/PayeeExtractor.kt` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/parser/LineItemExtractor.kt` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/parser/TaxExtractor.kt` (created)
- `domain/src/main/java/com/ynab/receiptscanner/domain/usecase/ParseReceiptUseCase.kt` (created)
- `data/src/test/java/com/ynab/receiptscanner/data/parser/ReceiptParserTest.kt` (created)

**Dependencies:** Step 2.3

**Technical details:**
- Rule-based extraction with regex patterns:
  - **Amount:** currency symbols, decimal formats, total keywords
  - **Date:** multiple formats (MM/DD/YYYY, DD-MM-YYYY, etc.)
  - **Payee:** top lines, merchant patterns, heuristics
  - **Tax:** tax keywords, percentage patterns
  - **Line items:** item-price pairs, quantity detection
- Confidence scoring for each extracted field
- Multiple candidate suggestions for ambiguous fields
- Normalization of extracted data (dates, amounts)

### Step 2.5: Image Storage Management
**Files:**
- `data/src/main/java/com/ynab/receiptscanner/data/storage/ImageStorageManager.kt` (created)
- `domain/src/main/java/com/ynab/receiptscanner/domain/usecase/SaveReceiptImageUseCase.kt` (created)
- `domain/src/main/java/com/ynab/receiptscanner/domain/usecase/DeleteReceiptImageUseCase.kt` (created)

**Dependencies:** Step 1.3, Step 2.1

**Technical details:**
- Save encrypted images to app-private storage
- Generate unique filename with UUID
- Compression with configurable quality
- Automatic cleanup after successful sync (user configurable)
- Option to retain images with user consent
- Total storage monitoring and limits

---

## Phase 3: YNAB API Integration

### Step 3.1: YNAB API Client Setup
**Files:**
- `data/src/main/java/com/ynab/receiptscanner/data/remote/YnabApi.kt` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/remote/dto/BudgetDto.kt` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/remote/dto/AccountDto.kt` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/remote/dto/CategoryDto.kt` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/remote/dto/TransactionDto.kt` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/remote/dto/CreateTransactionRequest.kt` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/remote/interceptor/AuthInterceptor.kt` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/di/NetworkModule.kt` (created)

**Dependencies:** Step 1.3

**Technical details:**
- Retrofit with OkHttp for HTTP client
- YNAB API base URL: `https://api.youneedabudget.com/v1/`
- Bearer token authentication via interceptor
- Request/response DTOs matching YNAB API schema
- Error handling with custom exceptions
- Logging interceptor for debugging (debug builds only)

### Step 3.2: OAuth 2.0 Implementation
**Files:**
- `app/src/main/java/com/ynab/receiptscanner/ui/auth/AuthActivity.kt` (created)
- `app/src/main/java/com/ynab/receiptscanner/ui/auth/AuthViewModel.kt` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/auth/YnabAuthManager.kt` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/auth/OAuthConfig.kt` (created)
- `domain/src/main/java/com/ynab/receiptscanner/domain/usecase/AuthenticateWithYnabUseCase.kt` (created)
- `app/src/main/res/layout/activity_auth.xml` (created)
- `app/src/main/AndroidManifest.xml` (modified - add deep link for OAuth callback)

**Dependencies:** Step 3.1

**Technical details:**
- Authorization Code flow with PKCE
- OAuth endpoints:
  - Authorize: `https://app.youneedabudget.com/oauth/authorize`
  - Token: `https://api.youneedabudget.com/v1/oauth/token`
- Redirect URI: `ynabreceipt://oauth/callback`
- Chrome Custom Tabs for OAuth web flow
- Token refresh logic with automatic retry
- Support for personal access token (testing mode)
- Secure token storage using EncryptedSharedPreferences

### Step 3.3: Repository Layer - YNAB Data
**Files:**
- `data/src/main/java/com/ynab/receiptscanner/data/repository/YnabRepositoryImpl.kt` (created)
- `domain/src/main/java/com/ynab/receiptscanner/domain/repository/YnabRepository.kt` (created - interface)
- `data/src/main/java/com/ynab/receiptscanner/data/mapper/YnabMapper.kt` (created)
- `domain/src/main/java/com/ynab/receiptscanner/domain/usecase/GetBudgetsUseCase.kt` (created)
- `domain/src/main/java/com/ynab/receiptscanner/domain/usecase/GetAccountsUseCase.kt` (created)
- `domain/src/main/java/com/ynab/receiptscanner/domain/usecase/GetCategoriesUseCase.kt` (created)

**Dependencies:** Step 3.1

**Technical details:**
- Repository pattern with interface in domain layer
- DTO to domain model mapping
- Caching strategy for budgets/accounts/categories
- Network-first with local fallback
- Error propagation with domain-specific exceptions

### Step 3.4: Transaction Submission
**Files:**
- `domain/src/main/java/com/ynab/receiptscanner/domain/usecase/CreateTransactionUseCase.kt` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/repository/YnabRepositoryImpl.kt` (modified)
- `data/src/main/java/com/ynab/receiptscanner/data/sync/TransactionSyncManager.kt` (created)
- `domain/src/main/java/com/ynab/receiptscanner/domain/usecase/SyncPendingTransactionsUseCase.kt` (created)

**Dependencies:** Step 3.3, Step 1.2

**Technical details:**
- Create transaction API call with proper mapping
- Queue failed transactions to pending_transactions table
- Retry logic with exponential backoff
- Duplicate detection by comparing import_id
- Batch transaction support for line items
- WorkManager for background sync

### Step 3.5: Duplicate Detection & Reconciliation
**Files:**
- `data/src/main/java/com/ynab/receiptscanner/data/repository/DuplicateDetector.kt` (created)
- `domain/src/main/java/com/ynab/receiptscanner/domain/usecase/CheckDuplicateTransactionUseCase.kt` (created)
- `data/src/test/java/com/ynab/receiptscanner/data/repository/DuplicateDetectorTest.kt` (created)

**Dependencies:** Step 3.4

**Technical details:**
- Compare by: payee + amount + date (within ±2 days)
- Generate deterministic import_id for YNAB deduplication
- Fuzzy matching for payee names (Levenshtein distance)
- Present potential duplicates to user before submission
- Store hash of (payee, amount, date) in local DB

---

## Phase 4: UI Layer - Main Application Flow

### Step 4.1: Navigation & App Structure
**Files:**
- `app/src/main/java/com/ynab/receiptscanner/ui/MainActivity.kt` (created)
- `app/src/main/res/layout/activity_main.xml` (created)
- `app/src/main/res/navigation/nav_graph.xml` (created)
- `app/src/main/res/menu/bottom_navigation.xml` (created)
- `app/src/main/res/values/strings.xml` (created)
- `app/src/main/res/values/themes.xml` (created)
- `app/src/main/res/values/colors.xml` (created)

**Dependencies:** Step 0.3

**Technical details:**
- Single Activity architecture with Navigation Component
- Bottom navigation: Home, Receipts, Settings
- Material Design 3 theming
- Dark mode support
- Navigation with Safe Args

### Step 4.2: Home Screen & Receipt List
**Files:**
- `app/src/main/java/com/ynab/receiptscanner/ui/home/HomeFragment.kt` (created)
- `app/src/main/java/com/ynab/receiptscanner/ui/home/HomeViewModel.kt` (created)
- `app/src/main/java/com/ynab/receiptscanner/ui/home/ReceiptAdapter.kt` (created)
- `app/src/main/res/layout/fragment_home.xml` (created)
- `app/src/main/res/layout/item_receipt.xml` (created)
- `domain/src/main/java/com/ynab/receiptscanner/domain/usecase/GetAllReceiptsUseCase.kt` (created)

**Dependencies:** Step 4.1, Phase 1

**Technical details:**
- RecyclerView with ListAdapter for receipt list
- Display: thumbnail, payee, amount, date, sync status
- Floating Action Button for "Scan Receipt"
- Pull-to-refresh for sync
- Empty state UI
- Filter by sync status (pending, synced, failed)

### Step 4.3: Receipt Review & Edit Screen
**Files:**
- `app/src/main/java/com/ynab/receiptscanner/ui/review/ReviewFragment.kt` (created)
- `app/src/main/java/com/ynab/receiptscanner/ui/review/ReviewViewModel.kt` (created)
- `app/src/main/res/layout/fragment_review.xml` (created)
- `app/src/main/java/com/ynab/receiptscanner/ui/review/FieldEditorView.kt` (created - custom view)
- `app/src/main/java/com/ynab/receiptscanner/ui/review/OcrHighlightView.kt` (created - custom view)

**Dependencies:** Step 4.2, Step 2.4

**Technical details:**
- Display preprocessed receipt image with OCR highlights
- Editable fields: payee, amount, date, tax
- Account picker (dropdown from YNAB accounts)
- Category picker (hierarchical from YNAB categories)
- Memo field (optional)
- Line items list (optional, expandable)
- Validation before submission
- Show confidence scores as visual indicators
- "Submit to YNAB" button

### Step 4.4: Account & Category Selection
**Files:**
- `app/src/main/java/com/ynab/receiptscanner/ui/review/AccountPickerDialog.kt` (created)
- `app/src/main/java/com/ynab/receiptscanner/ui/review/CategoryPickerDialog.kt` (created)
- `app/src/main/java/com/ynab/receiptscanner/ui/review/CategoryAdapter.kt` (created)
- `app/src/main/res/layout/dialog_account_picker.xml` (created)
- `app/src/main/res/layout/dialog_category_picker.xml` (created)

**Dependencies:** Step 4.3

**Technical details:**
- Material Dialog with search functionality
- Account list filtered by budget
- Category tree with subcategories
- Remember last used account/category
- Loading state while fetching from API

### Step 4.5: Settings & Preferences
**Files:**
- `app/src/main/java/com/ynab/receiptscanner/ui/settings/SettingsFragment.kt` (created)
- `app/src/main/res/xml/preferences.xml` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/local/preference/AppPreferences.kt` (created)

**Dependencies:** Step 4.1

**Technical details:**
- PreferenceFragmentCompat for settings UI
- Preferences:
  - Connected YNAB account info
  - Sign out button
  - Image retention policy (delete after sync / keep)
  - Auto-sync toggle
  - Default account selection
  - Theme preference (light/dark/system)
  - About & version info
- Clear cache option
- Export logs for debugging (debug builds)

### Step 4.6: Onboarding Flow
**Files:**
- `app/src/main/java/com/ynab/receiptscanner/ui/onboarding/OnboardingActivity.kt` (created)
- `app/src/main/java/com/ynab/receiptscanner/ui/onboarding/OnboardingViewModel.kt` (created)
- `app/src/main/res/layout/activity_onboarding.xml` (created)
- `app/src/main/res/layout/onboarding_page.xml` (created)

**Dependencies:** Step 3.2, Step 4.1

**Technical details:**
- ViewPager2 for onboarding screens
- Screens: Welcome → Features → Privacy → Connect YNAB
- Skip button to proceed directly to OAuth
- Show only on first launch (SharedPreferences flag)
- Smooth transitions with animations

---

## Phase 5: Offline Support & Sync

### Step 5.1: Sync Status Management
**Files:**
- `data/src/main/java/com/ynab/receiptscanner/data/sync/SyncStatusTracker.kt` (created)
- `domain/src/main/java/com/ynab/receiptscanner/domain/usecase/GetSyncStatusUseCase.kt` (created)
- `app/src/main/java/com/ynab/receiptscanner/ui/common/SyncStatusView.kt` (created - custom view)

**Dependencies:** Step 1.2, Step 3.4

**Technical details:**
- Track sync status per receipt
- Status indicators: pending, syncing, synced, failed
- Last sync timestamp
- Error messages for failed syncs
- Retry mechanism

### Step 5.2: Background Sync with WorkManager
**Files:**
- `app/src/main/java/com/ynab/receiptscanner/worker/SyncWorker.kt` (created)
- `app/src/main/java/com/ynab/receiptscanner/worker/WorkerModule.kt` (created)
- `app/src/main/AndroidManifest.xml` (modified - WorkManager initialization)

**Dependencies:** Step 5.1, Step 3.4

**Technical details:**
- WorkManager for reliable background sync
- Periodic sync work (configurable interval, default 1 hour)
- Constraint: requires network connectivity
- Exponential backoff for failures
- Notification for sync completion/errors
- Battery optimization handling

### Step 5.3: Connectivity Monitoring
**Files:**
- `data/src/main/java/com/ynab/receiptscanner/data/network/ConnectivityMonitor.kt` (created)
- `domain/src/main/java/com/ynab/receiptscanner/domain/usecase/ObserveConnectivityUseCase.kt` (created)

**Dependencies:** Step 5.2

**Technical details:**
- ConnectivityManager.NetworkCallback for real-time monitoring
- Flow-based connectivity state
- Trigger sync when connectivity restored
- Show connection status in UI
- Handle metered connections appropriately

---

## Phase 6: Testing Strategy

### Step 6.1: Unit Tests - Core Logic
**Files:**
- `data/src/test/java/com/ynab/receiptscanner/data/parser/*Test.kt` (created - all parser tests)
- `domain/src/test/java/com/ynab/receiptscanner/domain/usecase/*Test.kt` (created)
- `data/src/test/java/com/ynab/receiptscanner/data/mapper/YnabMapperTest.kt` (created)

**Dependencies:** All corresponding implementation files

**Technical details:**
- JUnit 5 for unit tests
- MockK for mocking
- Test parsers with known receipt samples
- Target: 80%+ code coverage for parser & mapper logic
- Test datasets with edge cases (missing fields, multi-currency, etc.)

### Step 6.2: Integration Tests - Database & Repository
**Files:**
- `data/src/androidTest/java/com/ynab/receiptscanner/data/local/ReceiptDaoTest.kt` (created)
- `data/src/androidTest/java/com/ynab/receiptscanner/data/repository/YnabRepositoryTest.kt` (created)
- `data/src/test/java/com/ynab/receiptscanner/data/auth/YnabAuthManagerTest.kt` (created)

**Dependencies:** Phase 1, Phase 3

**Technical details:**
- Room in-memory database for DAO tests
- MockWebServer for API tests
- Test CRUD operations
- Test transaction sync queue
- Verify data integrity and constraints

### Step 6.3: UI Tests - Critical Flows
**Files:**
- `app/src/androidTest/java/com/ynab/receiptscanner/ui/camera/CameraFlowTest.kt` (created)
- `app/src/androidTest/java/com/ynab/receiptscanner/ui/review/ReviewFlowTest.kt` (created)
- `app/src/androidTest/java/com/ynab/receiptscanner/ui/auth/AuthFlowTest.kt` (created)
- `app/src/androidTest/java/com/ynab/receiptscanner/MainActivityTest.kt` (created)

**Dependencies:** Phase 4

**Technical details:**
- Espresso for UI testing
- Test critical user journeys:
  - Onboarding → OAuth → Home
  - Camera → Review → Submit
  - View receipts → Edit → Resubmit
- Mock camera and OCR for consistent testing
- Hilt test dependencies

### Step 6.4: Accuracy Evaluation
**Files:**
- `app/src/androidTest/assets/test_receipts/` (directory with sample images)
- `app/src/androidTest/assets/test_receipts/ground_truth.json` (created)
- `app/src/androidTest/java/com/ynab/receiptscanner/evaluation/AccuracyTest.kt` (created)
- `docs/accuracy-evaluation-report.md` (created)

**Dependencies:** Step 2.4, Step 6.1

**Technical details:**
- Curated test dataset of 50+ receipts with ground truth
- Automated accuracy calculation:
  - Payee accuracy: exact match / 1-char-off match
  - Amount accuracy: exact match (decimal precision)
  - Date accuracy: exact match
- Generate report with per-field metrics
- Identify problematic receipt types for improvement
- Target metrics: Payee ≥92%, Amount ≥99%, Date ≥98%

---

## Phase 7: Polish & Production Readiness

### Step 7.1: Error Handling & User Feedback
**Files:**
- `app/src/main/java/com/ynab/receiptscanner/ui/common/ErrorHandler.kt` (created)
- `app/src/main/java/com/ynab/receiptscanner/ui/common/SnackbarManager.kt` (created)
- `app/src/main/java/com/ynab/receiptscanner/ui/common/LoadingDialog.kt` (created)
- `app/src/main/res/values/error_messages.xml` (created)

**Dependencies:** Phase 4

**Technical details:**
- Centralized error handling
- User-friendly error messages
- Retry actions for network errors
- Loading states for all async operations
- Success confirmations with Snackbar/Toast
- Dialog for critical errors

### Step 7.2: Analytics & Crash Reporting
**Files:**
- `data/src/main/java/com/ynab/receiptscanner/data/analytics/AnalyticsManager.kt` (created)
- `app/src/main/java/com/ynab/receiptscanner/YnabReceiptApp.kt` (modified)
- `app/build.gradle.kts` (modified - add Firebase/analytics)

**Dependencies:** Step 0.3

**Technical details:**
- Firebase Analytics for usage tracking
- Firebase Crashlytics for crash reporting
- Privacy-compliant event tracking:
  - Receipt scanned (no PII)
  - Transaction submitted (success/failure)
  - Feature usage
- User opt-out option in settings
- No tracking of OCR text or transaction details

### Step 7.3: Performance Optimization
**Files:**
- `app/src/main/java/com/ynab/receiptscanner/util/ImageCompressor.kt` (created)
- `data/src/main/java/com/ynab/receiptscanner/data/ocr/ImagePreprocessor.kt` (modified)
- `app/build.gradle.kts` (modified - ProGuard/R8 optimization)

**Dependencies:** Phase 2, Step 7.1

**Technical details:**
- Image compression before storage (JPEG quality 85%)
- Optimize OCR preprocessing (reduce resolution if needed)
- RecyclerView optimizations (view pooling, stable IDs)
- LazyLoading for receipts list
- R8 code shrinking and obfuscation
- Benchmark tests for critical paths

### Step 7.4: Accessibility & Localization
**Files:**
- `app/src/main/res/values/strings.xml` (modified)
- `app/src/main/res/values/content_descriptions.xml` (created)
- All layout XML files (modified - add contentDescription)
- `app/src/main/res/values-es/strings.xml` (created - Spanish)
- `app/src/main/res/values-fr/strings.xml` (created - French)

**Dependencies:** Phase 4

**Technical details:**
- Content descriptions for all interactive elements
- TalkBack testing
- Minimum touch target size (48dp)
- Color contrast compliance (WCAG AA)
- Support for system font scaling
- Localization for at least English + 2 languages

### Step 7.5: App Icon, Branding & Store Assets
**Files:**
- `app/src/main/res/mipmap-*/ic_launcher.png` (created - adaptive icon)
- `app/src/main/res/mipmap-*/ic_launcher_foreground.png` (created)
- `app/src/main/res/mipmap-*/ic_launcher_background.png` (created)
- `app/src/main/res/drawable/splash_screen.xml` (created)
- `docs/play-store-listing.md` (created)
- `app/proguard-rules.pro` (modified)

**Dependencies:** Step 7.4

**Technical details:**
- Adaptive icon with foreground/background layers
- Splash screen with app logo
- Play Store assets: screenshots, feature graphic, promo
- Privacy policy document
- App description and keywords

### Step 7.6: Release Build Configuration
**Files:**
- `app/build.gradle.kts` (modified)
- `app/release-keystore.properties` (created - template, not in repo)
- `.github/workflows/release.yml` (created - optional CI/CD)
- `app/proguard-rules.pro` (modified)
- `README.md` (created)
- `docs/deployment-guide.md` (created)

**Dependencies:** All previous steps

**Technical details:**
- Release build type with signing config
- ProGuard rules for Retrofit, Room, Hilt
- Versioning strategy (semantic versioning)
- Build variants: debug, staging, release
- GitHub Actions for automated builds (optional)
- README with build instructions
- Deployment guide for Play Store submission

---

## Phase 8: Documentation & Architecture

### Step 8.1: Architecture Documentation
**Files:**
- `docs/architecture.md` (created)
- `docs/module-structure.md` (created)
- `docs/cross-platform-readiness.md` (created)
- `docs/diagrams/architecture.png` (created)
- `docs/diagrams/data-flow.png` (created)

**Dependencies:** All implementation phases

**Technical details:**
- Document Clean Architecture approach
- Module dependency graph
- Data flow diagrams (camera → OCR → UI → YNAB)
- Explain abstraction layers for iOS portability
- Kotlin Multiplatform migration path
- Design decision rationale

### Step 8.2: API Documentation & Developer Guide
**Files:**
- `docs/api-integration.md` (created)
- `docs/ocr-parser-guide.md` (created)
- `docs/testing-guide.md` (created)
- `docs/contributing.md` (created)

**Dependencies:** Step 8.1

**Technical details:**
- YNAB API usage documentation
- OAuth flow diagram
- Parser customization guide
- Adding new languages/locales
- Testing setup and execution
- Contribution guidelines

### Step 8.3: User Manual & Help
**Files:**
- `docs/user-guide.md` (created)
- `docs/privacy-policy.md` (created)
- `docs/faq.md` (created)
- `app/src/main/assets/help.html` (created)

**Dependencies:** Step 8.2

**Technical details:**
- Step-by-step user guide with screenshots
- Privacy policy explaining data handling
- FAQ for common issues
- In-app help accessible from settings
- Contact/support information

---

## Dependency Summary

**Critical Path:**
1. Phase 0 → Phase 1 → Phase 2 → Phase 4 → Phase 6 (MVP without YNAB)
2. Phase 3 is parallel to Phase 2 (can develop separately)
3. Phase 5 requires Phase 3 & 4
4. Phase 7 & 8 are final polish and can be done in parallel

**Parallel Work Opportunities:**
- UI Layer (Phase 4) can start once domain models (Step 1.1) are defined
- YNAB integration (Phase 3) can be built parallel to OCR (Phase 2)
- Testing (Phase 6) should happen continuously alongside implementation
- Documentation (Phase 8) can be written as features are completed

---

## Risk Mitigation

### High-Risk Areas:

1. **OCR Accuracy**
   - Risk: Not meeting target accuracy (92%/99%/98%)
   - Mitigation: Test with diverse receipts early, iterate on parser rules, fallback to manual entry
   
2. **YNAB API Changes**
   - Risk: API deprecation or breaking changes
   - Mitigation: Abstract API behind repository, monitor YNAB developer updates, version API client
   
3. **OAuth Complexity**
   - Risk: Complex implementation, security issues
   - Mitigation: Use tested libraries (AppAuth), thorough testing, security audit
   
4. **Performance on Low-End Devices**
   - Risk: OCR/image processing too slow
   - Mitigation: Benchmark on min-spec devices, optimize image size, background processing

5. **Data Privacy**
   - Risk: User data exposure
   - Mitigation: Encryption at rest, minimal data retention, security audit, clear privacy policy

---

## Timeline Estimate

**Week 1-2:** Phase 0-1 (Foundation & Data Layer) - 10 days  
**Week 3-4:** Phase 2 (OCR & Image Processing) - 10 days  
**Week 5:** Phase 3.1-3.3 (YNAB API & OAuth) - 5 days  
**Week 6:** Phase 4.1-4.4 (Core UI) - 5 days  
**Week 7:** Phase 3.4-3.5 + 4.5-4.6 (Sync & Onboarding) - 5 days  
**Week 8:** Phase 5 (Offline Support) - 5 days  
**Week 9:** Phase 6 (Testing & Accuracy) - 5 days  
**Week 10:** Phase 7 (Polish & Production) - 5 days  
**Week 11:** Phase 8 + Final Testing - 5 days  

**Total: ~11 weeks (2.75 months)**

*Note: Timeline assumes 1 full-time developer. With 2 developers working in parallel, this could be reduced to ~7-8 weeks.*

---

## Success Metrics

**Technical Metrics:**
- OCR accuracy: Payee ≥92%, Amount ≥99%, Date ≥98%
- End-to-end transaction posting: <10 seconds
- App crash rate: <0.1%
- Unit test coverage: >80%
- Build time: <2 minutes

**User Metrics:**
- Successful OAuth connection rate: >95%
- Receipt scan to submission completion rate: >85%
- Manual field corrections per receipt: <30%
- User retention (7-day): >50%

---

## Post-MVP Enhancements

1. **Multi-transaction receipts** (split bills)
2. **Multi-currency detection and conversion**
3. **ML-based auto-categorization**
4. **Attach images to YNAB transactions** (if API supports)
5. **iOS app** (reuse domain/data modules via KMP)
6. **Receipt search and filtering**
7. **Export receipts to PDF/CSV**
8. **Recurring receipt templates**
9. **Budget insights based on scanning history**
10. **Widget for quick scan access**
