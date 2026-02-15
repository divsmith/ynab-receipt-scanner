# Testing Quick Reference

## 🚀 Common Commands

```bash
# Run all unit tests
./gradlew test

# Run all instrumentation tests
./gradlew connectedAndroidTest

# Run tests for specific module
./gradlew :domain:test
./gradlew :data:test
./gradlew :app:testDebugUnitTest

# Generate coverage report
./gradlew jacocoTestReport

# Verify coverage meets thresholds
./gradlew jacocoCoverageVerification

# Run specific test class
./gradlew :app:testDebugUnitTest --tests SaveReceiptUseCaseTest

# Run specific test method
./gradlew :app:testDebugUnitTest --tests SaveReceiptUseCaseTest.testSaveValidReceipt

# Run OCR accuracy evaluation
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=\
com.ynab.receiptscanner.evaluation.AccuracyEvaluationTest

# Clean and rebuild
./gradlew clean build
```

## 📊 View Reports

```bash
# Coverage report
open app/build/reports/jacoco/jacocoTestReport/html/index.html

# Unit test report
open app/build/reports/tests/testDebugUnitTest/index.html

# Instrumentation test report
open app/build/reports/androidTests/connected/debug/index.html

# Lint report
open app/build/reports/lint-results-debug.html

# OCR accuracy reports
ls app/build/reports/evaluation/
```

## 📁 Test Structure

```
domain/src/test/              → Unit tests (use cases)
data/src/test/                → Unit tests (security, storage)
data/src/androidTest/         → Integration tests (DB, repos)
app/src/test/                 → Unit tests, mocks
app/src/androidTest/          → UI tests, evaluation
```

## 🧪 Test Patterns

### Domain Use Case Test
```kotlin
@Test
fun `save_validReceipt_succeeds`() = runTest {
    val receipt = MockReceiptFactory.createReceipt()
    val result = useCase(receipt)
    assertThat(result.isSuccess).isTrue()
}
```

### Database Integration Test
```kotlin
@Test
fun insert_validEntity_persists() = runTest {
    val entity = TestData.createReceiptEntity()
    dao.insert(entity)
    val result = dao.getById(entity.id)
    assertThat(result).isEqualTo(entity)
}
```

### UI Flow Test
```kotlin
@Test
fun clickButton_navigatesToScreen() {
    onView(withId(R.id.button)).perform(click())
    onView(withId(R.id.screen)).check(matches(isDisplayed()))
}
```

### Repository with MockWebServer
```kotlin
@Test
fun fetch_success_returnsData() = runTest {
    mockWebServer.enqueue(MockResponse()
        .setResponseCode(200)
        .setBody(TestData.createResponse()))
    val result = repository.fetch()
    assertThat(result.isSuccess).isTrue()
}
```

## 🎯 Coverage Targets

| Layer | Target | Current |
|-------|--------|---------|
| Domain | 80%+ | ✅ |
| Data | 70%+ | ✅ |
| Overall | 60%+ | 🎯 |
| Per Class | 50%+ | 🎯 |
| Branch | 40%+ | 🎯 |

## 🧰 Test Utilities

```kotlin
// Mock factories
MockReceiptFactory.createReceipt()
MockOcrResultFactory.createStandardReceipt()
MockYnabDataFactory.createCompleteDataSet()

// Test data
TestData.createReceiptEntity()
TestData.createAccountDto()
TestData.createBudgetsResponse()

// UI helpers
TestUtils.waitForView(R.id.view)
TestUtils.createTestBitmap()

// Fakes
FakeOcrEngine() // Predictable OCR
FakeCameraProvider() // Mock camera
```

## ✅ Pre-Commit Checklist

- [ ] All tests pass: `./gradlew test`
- [ ] UI tests pass: `./gradlew connectedAndroidTest`
- [ ] No lint errors: `./gradlew lint`
- [ ] Coverage meets targets: `./gradlew jacocoCoverageVerification`
- [ ] New code has tests
- [ ] Tests follow naming convention

## 🐛 Common Issues

### Hilt injection fails
✅ Add `@HiltAndroidTest` and `HiltAndroidRule`

### Database tests fail
✅ Use `InstantTaskExecutorRule` and `runTest`

### UI tests flaky
✅ Use `IdlingResources` or explicit waits

### Coverage not generated
✅ Run tests first, then `jacocoTestReport`

## 📚 Documentation

- [TESTING_GUIDE.md](TESTING_GUIDE.md) - Complete guide
- [TESTING_SUMMARY.md](TESTING_SUMMARY.md) - Implementation details
- [test_receipts/README.md](app/src/androidTest/assets/test_receipts/README.md) - OCR evaluation

## 📈 Current Stats

- **Test Files**: 29
- **Test Cases**: 150+
- **Coverage**: 60%+ target
- **CI/CD**: ✅ Automated

## 🎉 Quick Wins

```bash
# Run fastest tests (unit only)
./gradlew test --parallel

# Run with detailed output
./gradlew test --info

# Run tests and generate report in one command
./gradlew test jacocoTestReport printCoverageSummary

# Watch mode (rerun on file change) - requires gradle-watch plugin
./gradlew test --continuous
```
