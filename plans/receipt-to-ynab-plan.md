## Plan: Receipt to YNAB Scanner

Building an Android-first receipt scanner app using Kotlin, ML Kit OCR, and YNAB API integration. The app captures receipts via camera, extracts transaction details on-device, and uploads to YNAB with OAuth authentication. Implementation follows strict TDD with Clean Architecture pattern.

**Scope Clarifications:**
- No receipt image attachment to YNAB (transaction data only)
- English receipts only for MVP
- Single transaction per receipt (no splitting in v1)
- Manual category selection (no ML auto-categorization in v1)
- Target Android 8.0+ (API 26+) for 85%+ device coverage
- Will use publicly available labeled receipt dataset for testing/training

**Phases: 10**

---

### 1. **Phase 1: Project Setup & Core Architecture**
- **Objective:** Initialize Android project with Clean Architecture, dependency injection (Hilt), and baseline testing infrastructure
- **Files/Functions to Create:**
  - `build.gradle.kts` (project & app modules)
  - `app/build.gradle.kts` with all dependencies
  - `app/src/main/AndroidManifest.xml`
  - `app/src/main/kotlin/com/receiptscanner/ReceiptScannerApp.kt` (Application class)
  - `app/src/main/kotlin/com/receiptscanner/di/AppModule.kt`
  - Package structure: `data/`, `domain/`, `presentation/`
  - `app/src/test/kotlin/com/receiptscanner/architecture/ArchitectureTest.kt`
- **Tests to Write:**
  - `ArchitectureTest.kt` - Verify module dependencies follow Clean Architecture
  - `HiltModuleTest.kt` - Verify DI graph construction
  - `BuildConfigTest.kt` - Verify build configuration and API level
- **Steps:**
  1. Write architecture validation tests (should fail - no code exists)
  2. Create Gradle build files with all dependencies (ML Kit, CameraX, Retrofit, Room, Hilt, WorkManager)
  3. Set up Android manifest with required permissions (CAMERA, INTERNET) and minSdk 26
  4. Create package structure (`data`, `domain`, `presentation` layers)
  5. Create Hilt application class and base DI modules
  6. Run tests to verify setup (should pass)
  7. Run `./gradlew assembleDebug` to verify build (should succeed)

---

### 2. **Phase 2: YNAB Authentication Module**
- **Objective:** Implement OAuth 2.0 Authorization Code flow for YNAB, secure token storage in Android Keystore
- **Files/Functions to Modify/Create:**
  - [domain/model/YnabToken.kt](domain/model/YnabToken.kt) - Token data model
  - [domain/repository/YnabAuthRepository.kt](domain/repository/YnabAuthRepository.kt) - Auth repository interface
  - [data/repository/YnabAuthRepositoryImpl.kt](data/repository/YnabAuthRepositoryImpl.kt) - Implementation with Keystore
  - [data/remote/YnabAuthService.kt](data/remote/YnabAuthService.kt) - Retrofit service for OAuth
  - [data/local/KeystoreManager.kt](data/local/KeystoreManager.kt) - Android Keystore wrapper
  - [presentation/auth/YnabAuthViewModel.kt](presentation/auth/YnabAuthViewModel.kt) - ViewModel for auth flow
  - [presentation/auth/YnabAuthActivity.kt](presentation/auth/YnabAuthActivity.kt) - OAuth web flow handler
- **Tests to Write:**
  - `YnabTokenTest.kt` - Token model validation
  - `YnabAuthRepositoryTest.kt` - Repository OAuth flow logic (mocked network)
  - `KeystoreManagerTest.kt` - Secure token storage/retrieval
  - `YnabAuthViewModelTest.kt` - ViewModel state management
- **Steps:**
  1. Write unit tests for token model and validation (fail - no model)
  2. Create token data model with expiration logic
  3. Write repository interface tests with mocked responses (fail)
  4. Implement KeystoreManager for secure storage
  5. Implement repository with Retrofit + Keystore integration
  6. Write ViewModel tests for auth state machine (fail)
  7. Implement ViewModel with StateFlow
  8. Run all tests (should pass)
  9. Run lint and format (`./gradlew lintDebug ktlintCheck`)

---

### 3. **Phase 3: Camera Integration & Capture**
- **Objective:** Implement CameraX preview, guided framing overlay, and manual/auto-capture for receipts
- **Files/Functions to Modify/Create:**
  - [presentation/camera/CameraFragment.kt](presentation/camera/CameraFragment.kt) - CameraX preview & capture
  - [presentation/camera/CameraViewModel.kt](presentation/camera/CameraViewModel.kt) - Camera state management
  - [presentation/camera/FramingOverlayView.kt](presentation/camera/FramingOverlayView.kt) - Custom view for receipt border guide
  - [domain/usecase/CaptureReceiptUseCase.kt](domain/usecase/CaptureReceiptUseCase.kt) - Business logic for capture
  - [data/repository/ImageRepository.kt](data/repository/ImageRepository.kt) - Image file management
  - [data/repository/ImageRepositoryImpl.kt](data/repository/ImageRepositoryImpl.kt) - Implementation
- **Tests to Write:**
  - `CameraViewModelTest.kt` - Camera permission, state transitions
  - `CaptureReceiptUseCaseTest.kt` - Image capture validation logic
  - `ImageRepositoryTest.kt` - Image save/retrieve/delete operations
  - `FramingOverlayViewTest.kt` - UI component rendering tests
- **Steps:**
  1. Write ViewModel tests for camera states (fail - no ViewModel)
  2. Create CameraViewModel with permission handling
  3. Write UseCase tests for capture validation (fail)
  4. Implement CaptureReceiptUseCase with image quality checks
  5. Write ImageRepository tests (fail)
  6. Implement ImageRepositoryImpl with encrypted file storage
  7. Create CameraFragment with CameraX (manual integration test)
  8. Run tests (should pass)

---

### 4. **Phase 4: Image Preprocessing Pipeline**
- **Objective:** Implement image preprocessing (deskew, crop, grayscale, contrast enhancement) to improve OCR accuracy
- **Files/Functions to Modify/Create:**
  - [domain/usecase/PreprocessImageUseCase.kt](domain/usecase/PreprocessImageUseCase.kt) - Preprocessing orchestration
  - [data/image/ImagePreprocessor.kt](data/image/ImagePreprocessor.kt) - Core preprocessing algorithms
  - [data/image/EdgeDetector.kt](data/image/EdgeDetector.kt) - Receipt edge detection
  - [data/image/PerspectiveTransformer.kt](data/image/PerspectiveTransformer.kt) - Deskew/perspective correction
- **Tests to Write:**
  - `PreprocessImageUseCaseTest.kt` - End-to-end preprocessing flow
  - `ImagePreprocessorTest.kt` - Individual preprocessing operations (test with sample images)
  - `EdgeDetectorTest.kt` - Edge detection accuracy with fixtures
  - `PerspectiveTransformerTest.kt` - Transformation correctness
- **Steps:**
  1. Add test fixtures (sample receipt images in `androidTest/assets/`)
  2. Write preprocessing tests with expected outputs (fail - no implementation)
  3. Implement EdgeDetector using Android Bitmap APIs
  4. Implement PerspectiveTransformer for deskewing
  5. Implement ImagePreprocessor with grayscale, adaptive threshold, noise reduction
  6. Implement PreprocessImageUseCase orchestrating all steps
  7. Run tests with sample images (should pass with >90% edge detection)
  8. Visual validation with test app screen

---

### 5. **Phase 5: ML Kit OCR Integration**
- **Objective:** Integrate Google ML Kit Text Recognition to extract raw text and bounding boxes from preprocessed receipt images
- **Files/Functions to Modify/Create:**
  - [data/ocr/MlKitTextRecognizer.kt](data/ocr/MlKitTextRecognizer.kt) - ML Kit wrapper
  - [data/ocr/TextRecognizerAdapter.kt](data/ocr/TextRecognizerAdapter.kt) - Abstract interface (future iOS compatibility)
  - [domain/model/OcrResult.kt](domain/model/OcrResult.kt) - OCR output model (text blocks, confidence)
  - [domain/usecase/RecognizeTextUseCase.kt](domain/usecase/RecognizeTextUseCase.kt) - OCR orchestration
- **Tests to Write:**
  - `MlKitTextRecognizerTest.kt` - ML Kit integration with mock InputImage
  - `RecognizeTextUseCaseTest.kt` - UseCase with preprocessed images
  - `OcrResultTest.kt` - Model validation and confidence filtering
  - `OcrAccuracyTest.kt` - Accuracy benchmarks with test dataset (≥92% payee, ≥99% amount)
- **Steps:**
  1. Write OcrResult model tests (fail)
  2. Create OcrResult data model with confidence scores
  3. Write TextRecognizerAdapter interface tests (fail)
  4. Implement MlKitTextRecognizer wrapping ML Kit APIs
  5. Write RecognizeTextUseCase tests with sample receipts (fail)
  6. Implement RecognizeTextUseCase with error handling
  7. Run accuracy tests with test dataset (should meet thresholds)
  8. Log accuracy metrics for validation

---

### 6. **Phase 6: Receipt Parser & Field Extraction**
- **Objective:** Parse OCR text to extract payee, amount, date, currency, tax, and line items using rule-based patterns (English receipts only)
- **Files/Functions to Modify/Create:**
  - [domain/model/ParsedReceipt.kt](domain/model/ParsedReceipt.kt) - Extracted fields model
  - [data/parser/ReceiptParser.kt](data/parser/ReceiptParser.kt) - Main parser orchestrator
  - [data/parser/AmountExtractor.kt](data/parser/AmountExtractor.kt) - Amount + currency extraction
  - [data/parser/DateExtractor.kt](data/parser/DateExtractor.kt) - Date parsing (multiple formats)
  - [data/parser/PayeeExtractor.kt](data/parser/PayeeExtractor.kt) - Merchant name identification
  - [data/parser/LineItemExtractor.kt](data/parser/LineItemExtractor.kt) - Optional line items
  - [domain/usecase/ParseReceiptUseCase.kt](domain/usecase/ParseReceiptUseCase.kt) - Parsing business logic
- **Tests to Write:**
  - `AmountExtractorTest.kt` - Test decimal parsing, currency symbols (≥99% accuracy)
  - `DateExtractorTest.kt` - Test date format variations (≥98% accuracy)
  - `PayeeExtractorTest.kt` - Test merchant name extraction (≥92% accuracy)
  - `LineItemExtractorTest.kt` - Test item parsing
  - `ReceiptParserTest.kt` - Integration test with full OCR output
  - `ParseReceiptUseCaseTest.kt` - UseCase validation
- **Steps:**
  1. Create test fixtures with diverse receipt OCR texts
  2. Write AmountExtractor tests (fail - no implementation)
  3. Implement AmountExtractor with regex for amounts, decimals, currency
  4. Write DateExtractor tests with various formats (fail)
  5. Implement DateExtractor with common date patterns
  6. Write PayeeExtractor tests (fail)
  7. Implement PayeeExtractor with heuristics (top of receipt, larger font)
  8. Write LineItemExtractor tests (fail)
  9. Implement LineItemExtractor
  10. Write ReceiptParser integration tests (fail)
  11. Implement ReceiptParser orchestrating all extractors
  12. Implement ParseReceiptUseCase
  13. Run all parser tests (should meet accuracy targets)

---

### 7. **Phase 7: Review UI & Data Flow**
- **Objective:** Create editable review screen where users confirm/edit extracted fields before upload (manual category selection)
- **Files/Functions to Modify/Create:**
  - [presentation/review/ReviewFragment.kt](presentation/review/ReviewFragment.kt) - Review UI with editable fields
  - [presentation/review/ReviewViewModel.kt](presentation/review/ReviewViewModel.kt) - ViewModel managing parsed data
  - [presentation/review/AccountCategorySelector.kt](presentation/review/AccountCategorySelector.kt) - YNAB account/category picker
  - [domain/usecase/ValidateTransactionUseCase.kt](domain/usecase/ValidateTransactionUseCase.kt) - Field validation
  - [domain/usecase/GetYnabBudgetDataUseCase.kt](domain/usecase/GetYnabBudgetDataUseCase.kt) - Fetch accounts/categories
- **Tests to Write:**
  - `ReviewViewModelTest.kt` - State management, field updates, validation
  - `ValidateTransactionUseCaseTest.kt` - Required field validation
  - `ReviewFragmentTest.kt` - UI rendering tests (Espresso)
  - `AccountCategorySelectorTest.kt` - Dropdown population tests
  - `GetYnabBudgetDataUseCaseTest.kt` - Budget data fetching
- **Steps:**
  1. Write ViewModel tests for review state (fail)
  2. Create ReviewViewModel with StateFlow for parsed receipt
  3. Write validation tests (fail)
  4. Implement ValidateTransactionUseCase (required: payee, amount, date)
  5. Write budget data UseCase tests (fail)
  6. Implement GetYnabBudgetDataUseCase
  7. Write UI component tests (fail)
  8. Create ReviewFragment with Material Design components
  9. Implement two-way data binding for editable fields
  10. Create AccountCategorySelector with YNAB budget data
  11. Run UI tests (Espresso - should pass)
  12. Manual UI validation on emulator

---

### 8. **Phase 8: YNAB Transaction Upload**
- **Objective:** Create transactions in YNAB via API (data only, no image attachment), handle errors, show success confirmation
- **Files/Functions to Modify/Create:**
  - [data/remote/YnabApiService.kt](data/remote/YnabApiService.kt) - Retrofit service for transactions
  - [data/remote/dto/TransactionDto.kt](data/remote/dto/TransactionDto.kt) - YNAB API DTOs
  - [data/repository/YnabTransactionRepository.kt](data/repository/YnabTransactionRepository.kt) - Transaction operations interface
  - [data/repository/YnabTransactionRepositoryImpl.kt](data/repository/YnabTransactionRepositoryImpl.kt) - Implementation
  - [domain/usecase/CreateYnabTransactionUseCase.kt](domain/usecase/CreateYnabTransactionUseCase.kt) - Upload business logic
  - [presentation/result/UploadResultFragment.kt](presentation/result/UploadResultFragment.kt) - Success/error screen
  - [presentation/result/UploadResultViewModel.kt](presentation/result/UploadResultViewModel.kt) - Result state
- **Tests to Write:**
  - `YnabApiServiceTest.kt` - API endpoint tests with MockWebServer
  - `TransactionDtoTest.kt` - DTO serialization/deserialization
  - `YnabTransactionRepositoryTest.kt` - Repository with mocked API
  - `CreateYnabTransactionUseCaseTest.kt` - UseCase with error scenarios
  - `UploadResultViewModelTest.kt` - Result screen state
- **Steps:**
  1. Write DTO tests with sample YNAB JSON (fail)
  2. Create TransactionDto models
  3. Write API service tests with MockWebServer (fail)
  4. Implement YnabApiService with Retrofit
  5. Write repository tests (fail)
  6. Implement YnabTransactionRepositoryImpl with error handling
  7. Write UseCase tests including network errors, auth errors (fail)
  8. Implement CreateYnabTransactionUseCase with retry logic
  9. Write result screen tests (fail)
  10. Create UploadResultFragment with success/error states
  11. Run all tests (should pass)
  12. Test end-to-end flow: scan → review → upload (manual test)

---

### 9. **Phase 9: Offline Storage & Sync**
- **Objective:** Implement local queue for offline receipts, background sync with WorkManager, duplicate detection
- **Files/Functions to Modify/Create:**
  - [data/local/ReceiptDatabase.kt](data/local/ReceiptDatabase.kt) - Room database
  - [data/local/dao/QueuedTransactionDao.kt](data/local/dao/QueuedTransactionDao.kt) - DAO for queued items
  - [data/local/entity/QueuedTransactionEntity.kt](data/local/entity/QueuedTransactionEntity.kt) - Entity model
  - [data/worker/SyncWorker.kt](data/worker/SyncWorker.kt) - WorkManager worker for background sync
  - [data/repository/OfflineQueueRepository.kt](data/repository/OfflineQueueRepository.kt) - Queue management interface
  - [data/repository/OfflineQueueRepositoryImpl.kt](data/repository/OfflineQueueRepositoryImpl.kt) - Implementation
  - [domain/usecase/QueueTransactionUseCase.kt](domain/usecase/QueueTransactionUseCase.kt) - Add to queue
  - [domain/usecase/SyncQueuedTransactionsUseCase.kt](domain/usecase/SyncQueuedTransactionsUseCase.kt) - Sync logic
  - [domain/usecase/DetectDuplicateUseCase.kt](domain/usecase/DetectDuplicateUseCase.kt) - Duplicate detection
- **Tests to Write:**
  - `QueuedTransactionDaoTest.kt` - DAO CRUD operations
  - `OfflineQueueRepositoryTest.kt` - Queue operations with in-memory DB
  - `QueueTransactionUseCaseTest.kt` - Add to queue logic
  - `SyncQueuedTransactionsUseCaseTest.kt` - Sync orchestration with mocked network
  - `DetectDuplicateUseCaseTest.kt` - Duplicate detection algorithm (same amount+date+payee)
  - `SyncWorkerTest.kt` - WorkManager worker execution
- **Steps:**
  1. Write DAO tests with in-memory database (fail)
  2. Create Room database schema with QueuedTransactionEntity
  3. Implement QueuedTransactionDao
  4. Write repository tests (fail)
  5. Implement OfflineQueueRepositoryImpl
  6. Write duplicate detection tests (fail - define duplicate rules: same amount+date+payee)
  7. Implement DetectDuplicateUseCase
  8. Write sync UseCase tests (fail)
  9. Implement SyncQueuedTransactionsUseCase with batch upload
  10. Write SyncWorker tests (fail)
  11. Implement SyncWorker with WorkManager constraints (network required)
  12. Run all tests (should pass)
  13. Test offline → online sync flow manually

---

### 10. **Phase 10: End-to-End Testing & Polish**
- **Objective:** Comprehensive E2E tests, accuracy validation with public dataset, edge case handling, documentation
- **Files/Functions to Modify/Create:**
  - [androidTest/E2EReceiptFlowTest.kt](androidTest/E2EReceiptFlowTest.kt) - Full flow test (camera → upload)
  - [androidTest/AccuracyValidationTest.kt](androidTest/AccuracyValidationTest.kt) - Test with real receipt dataset
  - [androidTest/EdgeCaseTest.kt](androidTest/EdgeCaseTest.kt) - Poor OCR, missing fields, validation errors
  - [README.md](README.md) - Setup, build, run instructions
  - [ARCHITECTURE.md](ARCHITECTURE.md) - Architecture documentation
  - [TEST_REPORT.md](TEST_REPORT.md) - Accuracy metrics, test coverage report
- **Tests to Write:**
  - `E2EReceiptFlowTest.kt` - Scan → parse → review → upload → verify YNAB
  - `AccuracyValidationTest.kt` - Validate accuracy ≥92% payee, ≥99% amount, ≥98% date
  - `EdgeCaseTest.kt` - Tests for missing fields, OCR failures, validation errors
  - `PerformanceTest.kt` - OCR + parse time < 3s
  - `SecurityTest.kt` - Token storage validation, image encryption test
- **Steps:**
  1. Download public labeled receipt dataset (e.g., SROIE, CORD, or similar)
  2. Write E2E test with mocked YNAB API (fail - integration not complete)
  3. Fix any integration issues found
  4. Run E2E test (should pass)
  5. Write accuracy validation tests with dataset (fail if below threshold)
  6. Tune parsers to meet accuracy targets
  7. Run accuracy tests (should meet all thresholds)
  8. Write edge case tests (fail)
  9. Implement edge case handling (validation, error messages)
  10. Run all tests (should pass)
  11. Generate test coverage report (`./gradlew jacocoTestReport`)
  12. Write README with setup instructions
  13. Write ARCHITECTURE.md explaining module boundaries, iOS readiness
  14. Write TEST_REPORT.md with accuracy numbers and coverage
  15. Build release APK (`./gradlew assembleRelease`)

---

## Summary

This plan delivers a production-ready Android receipt scanner app following TDD and Clean Architecture principles. Each phase builds incrementally with tests written first, ensuring code quality and meeting accuracy requirements. The architecture is designed to be iOS-compatible in the future by abstracting platform-specific code (OCR, camera) behind interfaces.
