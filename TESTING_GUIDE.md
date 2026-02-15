# Testing Infrastructure Guide

## Quick Start

### Run All Tests
```bash
# Run all unit tests
./gradlew test

# Run all instrumentation tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Generate code coverage report
./gradlew jacocoTestReport
```

### View Results
```bash
# View coverage report (after running jacocoTestReport)
open app/build/reports/jacoco/jacocoTestReport/html/index.html

# View unit test reports
open app/build/reports/tests/testDebugUnitTest/index.html

# View instrumentation test reports
open app/build/reports/androidTests/connected/debug/index.html
```

## Test Categories

### 1. Unit Tests (Domain Layer)
**Location**: `domain/src/test/java/.../domain/usecase/`

Tests business logic in isolation:
- `SaveReceiptUseCaseTest.kt` - Receipt validation and saving
- `CreateTransactionUseCaseTest.kt` - Transaction creation with import IDs
- `GetAccountsUseCaseTest.kt` - Account fetching with caching
- `AuthenticateWithYnabUseCaseTest.kt` - Authentication flow

**Run**: `./gradlew :domain:test`

### 2. Integration Tests (Database)
**Location**: `data/src/androidTest/java/.../data/local/`

Tests database operations with Room:
- `ReceiptDaoTest.kt` - Receipt CRUD and queries
- `PendingTransactionDaoTest.kt` - Transaction queue operations
- `YnabDatabaseTest.kt` - Database integrity and type converters

**Run**: `./gradlew :data:connectedDebugAndroidTest`

### 3. Integration Tests (Repositories)
**Location**: `data/src/androidTest/java/.../data/repository/`

Tests repository layer with real dependencies:
- `ReceiptRepositoryImplTest.kt` - Receipt operations with storage
- `YnabRepositoryImplTest.kt` - API operations with MockWebServer

**Run**: `./gradlew :data:connectedDebugAndroidTest`

### 4. Unit Tests (Security & Storage)
**Location**: `data/src/test/java/.../data/`

Tests security and storage utilities:
- `EncryptionHelperTest.kt` - Encryption/decryption
- `ImageStorageManagerTest.kt` - Image file operations

**Run**: `./gradlew :data:test`

### 5. UI Tests (End-to-End)
**Location**: `app/src/androidTest/java/.../ui/`

Tests complete user flows:
- `MainActivityTest.kt` - Navigation and lifecycle
- `CameraFlowTest.kt` - Camera capture flow
- `ReviewFlowTest.kt` - Receipt review and editing
- `HomeFlowTest.kt` - Receipt list and filtering
- `SettingsFlowTest.kt` - Settings and preferences

**Run**: `./gradlew :app:connectedDebugAndroidTest`

### 6. OCR Accuracy Evaluation
**Location**: `app/src/androidTest/java/.../evaluation/`

Evaluates OCR parsing accuracy against ground truth:
- `AccuracyEvaluationTest.kt` - Automated accuracy testing
- `AccuracyReportGenerator.kt` - Report generation

**Ground Truth**: `app/src/androidTest/assets/test_receipts/ground_truth.json`

**Run**:
```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=\
com.ynab.receiptscanner.evaluation.AccuracyEvaluationTest
```

## Test Utilities

### Mock Factories
**Location**: `app/src/test/java/.../mock/`

Reusable test data generators:
```kotlin
// Create test receipts
val receipt = MockReceiptFactory.createPendingReceipt()
val receipts = MockReceiptFactory.createMixedReceipts(count = 10)

// Create OCR results
val ocrText = MockOcrResultFactory.createStandardReceipt()
val poorQuality = MockOcrResultFactory.createPoorQualityReceipt()

// Create YNAB data
val budget = MockYnabDataFactory.createBudget()
val accounts = MockYnabDataFactory.createTypicalAccounts()
val dataSet = MockYnabDataFactory.createCompleteDataSet()
```

### Test Data (Data Layer)
**Location**: `data/src/test/java/.../data/TestData.kt`

Provides entities, DTOs, and API responses:
```kotlin
val receiptEntity = TestData.createReceiptEntity()
val accountDto = TestData.createAccountDto()
val apiResponse = TestData.createBudgetsResponse()
```

### Fakes
**Location**: `app/src/androidTest/java/com/ynab/receiptscanner/`

- `FakeOcrEngine.kt` - Predictable OCR results for UI tests
- `FakeCameraProvider.kt` - Mock camera without hardware
- `TestUtils.kt` - Helper functions (wait for view, create bitmaps, etc.)

### Hilt Testing
**Location**: `app/src/androidTest/java/com/ynab/receiptscanner/HiltTestRunner.kt`

Custom test runner for dependency injection:
```kotlin
@HiltAndroidTest
class MyTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var repository: ReceiptRepository
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
}
```

## Code Coverage

### Generate Report
```bash
./gradlew jacocoTestReport
open app/build/reports/jacoco/jacocoTestReport/html/index.html
```

### Verify Coverage
```bash
# Checks that coverage meets minimum thresholds
./gradlew jacocoCoverageVerification
```

### Targets
- **Overall**: 60%
- **Per Class**: 50%
- **Branch**: 40%
- **Business Logic**: 80%+ (domain & data layers)

### Exclusions
The following are excluded from coverage:
- Generated code (Dagger, Hilt, Room)
- Android framework classes
- Data classes and DTOs
- UI binding classes

## CI/CD Pipeline

### GitHub Actions
**Location**: `.github/workflows/android-tests.yml`

Runs on every push and pull request:
- ✅ Unit tests on Ubuntu
- ✅ Instrumentation tests on macOS (API 29 & 33)
- ✅ Code coverage with Codecov
- ✅ Lint checks

### View Results
Check the **Actions** tab in GitHub for test results and coverage reports.

## Adding New Tests

### 1. Domain Use Case Test
```kotlin
@Test
fun `methodName_condition_expectedResult`() = runTest {
    // Given
    val useCase = SaveReceiptUseCase(mockRepository)
    val receipt = MockReceiptFactory.createReceipt()
    
    // When
    val result = useCase(receipt)
    
    // Then
    assertThat(result.isSuccess).isTrue()
    verify { mockRepository.save(receipt) }
}
```

### 2. Integration Test (Database)
```kotlin
@Test
fun testName() = runTest {
    // Given
    val entity = TestData.createReceiptEntity()
    
    // When
    dao.insert(entity)
    val result = dao.getById(entity.id)
    
    // Then
    assertThat(result).isEqualTo(entity)
}
```

### 3. UI Test
```kotlin
@Test
fun testName() {
    // Given
    activityScenario.launch(MainActivity::class.java)
    
    // When
    onView(withId(R.id.button)).perform(click())
    
    // Then
    onView(withId(R.id.text))
        .check(matches(isDisplayed()))
}
```

### 4. Repository Test with MockWebServer
```kotlin
@Test
fun testName() = runTest {
    // Given
    mockWebServer.enqueue(
        MockResponse()
            .setResponseCode(200)
            .setBody(TestData.createBudgetsResponse())
    )
    
    // When
    val result = repository.getBudgets()
    
    // Then
    assertThat(result.isSuccess).isTrue()
}
```

## Adding Test Receipts

### 1. Add Images
Place receipt images in: `app/src/androidTest/assets/test_receipts/`

Naming: `receipt_001.jpg`, `receipt_002.jpg`, etc.

### 2. Add Ground Truth
Update `ground_truth.json`:
```json
{
  "receipts":[
    {
      "filename": "receipt_006.jpg",
      "expected_payee": "Store Name",
      "expected_amount": 45.99,
      "expected_date": "2024-01-15",
      "notes": "Clear receipt, good lighting"
    }
  ]
}
```

### 3. Run Evaluation
```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=\
com.ynab.receiptscanner.evaluation.AccuracyEvaluationTest
```

### 4. View Report
Check: `app/build/reports/evaluation/accuracy_report_[timestamp].md`

## Tips & Best Practices

### Use Mock Factories
```kotlin
// ❌ Avoid creating test data manually
val receipt = Receipt(
    id = "test-id",
    payee = "Store",
    amount = 25.50,
    // ... many more fields
)

// ✅ Use factories
val receipt = MockReceiptFactory.createReceipt(
    payee = "Store",
    amount = 25.50
)
```

### Verify Behavior
```kotlin
// ❌ Don't just check results
assertThat(result.isSuccess).isTrue()

// ✅ Verify interactions
verify { repository.save(any()) }
verify(exactly = 1) { database.insert(entity) }
```

### Use Truth Assertions
```kotlin
// ❌ JUnit assertions
assertEquals(expected, actual)
assertTrue(condition)

// ✅ Truth assertions (more readable)
assertThat(actual).isEqualTo(expected)
assertThat(condition).isTrue()
assertThat(list).containsExactly(item1, item2)
```

### Test Edge Cases
```kotlin
@Test
fun `save_nullPayee_throwsValidationError`()

@Test
fun `fetch_networkError_returnsFailure`()

@Test
fun `parse_invalidDate_usesToday`()
```

### Use Coroutine Test Scope
```kotlin
@Test
fun testAsync() = runTest {  // Use runTest for suspending functions
    val result = repository.fetchData()
    assertThat(result).isNotNull()
}
```

## Troubleshooting

### Tests Don't Compile
```bash
# Sync Gradle files
./gradlew clean build
```

### Hilt Injection Fails
1. Check `@HiltAndroidTest` annotation
2. Verify `HiltAndroidRule` is set up
3. Call `hiltRule.inject()` in `@Before`
4. Ensure `testInstrumentationRunner` uses `HiltTestRunner`

### Database Tests Fail
1. Use `@get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()`
2. Close database in `@After` method
3. Use `runTest` for coroutines

### UI Tests Flaky
1. Use `IdlingResources` for async operations
2. Add explicit waits: `Thread.sleep(100)` or TestUtils.waitForView()
3. Disable animations on test device
4. Use `closeSoftKeyboard()` before assertions

### Coverage Not Generated
1. Ensure tests ran first: `./gradlew test connectedAndroidTest`
2. Check `jacoco.gradle` is applied: `apply(from = "jacoco.gradle")`
3. Look for `.exec` files in `build/jacoco/`

## Resources

### Documentation
- [TESTING_SUMMARY.md](TESTING_SUMMARY.md) - Complete implementation summary
- [test_receipts/README.md](app/src/androidTest/assets/test_receipts/README.md) - OCR evaluation guide

### External Resources
- [Android Testing Guide](https://developer.android.com/training/testing)
- [MockK Documentation](https://mockk.io/)
- [Truth Assertions](https://truth.dev/)
- [Espresso Cheat Sheet](https://developer.android.com/training/testing/espresso/cheat-sheet)
- [Hilt Testing](https://developer.android.com/training/dependency-injection/hilt-testing)

## Questions?

For issues or questions about the testing infrastructure:
1. Check this guide
2. Review existing tests for patterns
3. Check [TESTING_SUMMARY.md](TESTING_SUMMARY.md) for implementation details
4. Review CI/CD pipeline logs in GitHub Actions
