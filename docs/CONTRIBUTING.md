# Contributing to YNAB Receipt Scanner

First off, thank you for considering contributing to YNAB Receipt Scanner! It's people like you that make this app better for everyone.

## Table of Contents
- [Code of Conduct](#code-of-conduct)
- [How Can I Contribute?](#how-can-i-contribute)
- [Development Setup](#development-setup)
- [Pull Request Process](#pull-request-process)
- [Coding Standards](#coding-standards)
- [Commit Message Guidelines](#commit-message-guidelines)
- [Branching Strategy](#branching-strategy)
- [Issue Reporting](#issue-reporting)
- [Testing Requirements](#testing-requirements)

## Code of Conduct

This project and everyone participating in it is governed by our Code of Conduct. By participating, you are expected to uphold this code. Please report unacceptable behavior to [project-email@example.com].

### Our Pledge

We pledge to make participation in our project a harassment-free experience for everyone, regardless of age, body size, disability, ethnicity, gender identity and expression, level of experience, nationality, personal appearance, race, religion, or sexual identity and orientation.

### Our Standards

**Positive behavior includes:**
- Using welcoming and inclusive language
- Being respectful of differing viewpoints
- Gracefully accepting constructive criticism
- Focusing on what is best for the community
- Showing empathy towards other community members

**Unacceptable behavior includes:**
- Trolling, insulting/derogatory comments, and personal attacks
- Public or private harassment
- Publishing others' private information without permission
- Other conduct which could reasonably be considered inappropriate

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check existing issues to avoid duplicates.

**When reporting a bug, include:**
- **Clear title and description**
- **Steps to reproduce** the problem
- **Expected behavior** and **actual behavior**
- **Screenshots** (if applicable)
- **Device/Android version** information
- **App version**
- **Relevant logs** (if available)

**Bug Report Template:**
```markdown
## Description
Brief description of the bug

## Steps to Reproduce
1. Go to '...'
2. Click on '...'
3. Scroll down to '...'
4. See error

## Expected Behavior
What should happen

## Actual Behavior
What actually happens

## Environment
- Device: Pixel 6
- Android Version: 14
- App Version: 1.0.0

## Logs
```
Paste relevant logs here
```

## Screenshots
[If applicable]
```

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues.

**When suggesting an enhancement:**
- **Use a clear and descriptive title**
- **Provide detailed description** of the suggested enhancement
- **Explain why this enhancement would be useful**
- **Provide examples** of how it would work
- **Include mockups/wireframes** (if applicable)

### Your First Code Contribution

Unsure where to begin? Look for issues labeled:
- `good first issue` - Simple issues for beginners
- `help wanted` - Issues where we need help
- `documentation` - Documentation improvements

**Steps:**
1. Fork the repository
2. Create your feature branch
3. Make your changes
4. Write/update tests
5. Update documentation
6. Submit a pull request

### Pull Requests

1. **Fork the repo** and create your branch from `main`
2. **Make your changes** following our coding standards
3. **Add tests** if you've added code functionality
4. **Update documentation** if necessary
5. **Ensure tests pass**: `./gradlew test connectedAndroidTest`
6. **Follow the PR template**
7. **Link related issues**

## Development Setup

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34
- Git

### Setup Steps

1. **Fork and clone**:
   ```bash
   git clone https://github.com/YOUR-USERNAME/ynab-receipt-scanner.git
   cd ynab-receipt-scanner
   ```

2. **Open in Android Studio**:
   - File > Open
   - Select project directory

3. **Create `local.properties`**:
   ```properties
   sdk.dir=/path/to/Android/sdk
   ynab.client.id=your_test_client_id
   ynab.client.secret=your_test_client_secret
   ```

4. **Sync Gradle**:
   ```bash
   ./gradlew build
   ```

5. **Run tests**:
   ```bash
   ./gradlew test
   ```

See [Developer Guide](DEVELOPER_GUIDE.md) for detailed setup instructions.

## Pull Request Process

### Before Submitting

✅ **Code Quality**
- [ ] Code follows project style guidelines
- [ ] No compiler warnings
- [ ] All tests pass locally
- [ ] Code has been self-reviewed

✅ **Testing**
- [ ] New tests added for new features
- [ ] Existing tests updated if needed
- [ ] Edge cases covered
- [ ] UI tests added for UI changes

✅ **Documentation**
- [ ] Code comments added where needed
- [ ] README updated (if applicable)
- [ ] API documentation updated (if applicable)
- [ ] CHANGELOG updated

✅ **Commits**
- [ ] Commits follow conventional commit format
- [ ] Commits are logical and atomic
- [ ] No merge commits (use rebase)

### PR Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix (non-breaking change fixing an issue)
- [ ] New feature (non-breaking change adding functionality)
- [ ] Breaking change (fix or feature causing existing functionality to change)
- [ ] Documentation update

## Testing
Describe the tests you ran and how to reproduce them:
- [ ] Unit tests
- [ ] Instrumented tests
- [ ] Manual testing

## Screenshots (if applicable)
Before | After
-------|-------
[screenshot] | [screenshot]

## Checklist
- [ ] My code follows the style guidelines
- [ ] I have performed a self-review
- [ ] I have commented my code where needed
- [ ] I have updated documentation
- [ ] My changes generate no new warnings
- [ ] I have added tests
- [ ] All tests pass locally
- [ ] I have updated the CHANGELOG

## Related Issues
Closes #(issue number)
```

### Review Process

1. **Automated Checks**: CI/CD runs tests and linters
2. **Code Review**: At least one maintainer must approve
3. **Address Feedback**: Make requested changes
4. **Final Approval**: Maintainer merges PR

### After Merge

- Delete your feature branch
- Pull latest main: `git pull origin main`
- Start new feature from updated main

## Coding Standards

### Kotlin Style

Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).

**Key Points:**
```kotlin
// ✅ Good: Proper naming, spacing, and structure
class ReceiptViewModel @Inject constructor(
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val validateReceiptUseCase: ValidateReceiptUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    fun createTransaction(receipt: Receipt) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            when (val result = createTransactionUseCase(receipt)) {
                is Result.Success -> {
                    _uiState.value = UiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = UiState.Error(result.exception.message)
                }
            }
        }
    }
}

// ❌ Bad: Poor naming, no spacing, unclear structure
class vm(val repo:Repo):ViewModel(){
    var state:String?=null
    fun create(r:Receipt){
        // Logic
    }
}
```

### Architecture Guidelines

Follow Clean Architecture:
- **domain**: Pure Kotlin, no Android dependencies
- **data**: Implementations of repositories
- **app**: UI code, ViewModels, Fragments

**Dependency Rule**: Higher layers can depend on lower layers, never the reverse.

### Testing Guidelines

**Unit Tests (domain, data modules)**:
```kotlin
class CreateTransactionUseCaseTest {
    private lateinit var useCase: CreateTransactionUseCase
    private lateinit var mockRepository: YnabRepository
    
    @Before
    fun setup() {
        mockRepository = mockk()
        useCase = CreateTransactionUseCase(mockRepository)
    }
    
    @Test
    fun `creates transaction successfully`() = runTest {
        // Given
        val receipt = createTestReceipt()
        coEvery { mockRepository.createTransaction(any()) } returns Result.Success(mockTransaction)
        
        // When
        val result = useCase(receipt, "account-123", null)
        
        // Then
        assertTrue(result is Result.Success)
        coVerify { mockRepository.createTransaction(any()) }
    }
}
```

**UI Tests (app module)**:
```kotlin
@HiltAndroidTest
class CameraFlowTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @Test
    fun captureReceiptAndReview() {
        // Launch camera screen
        launchFragmentInHiltContainer<CameraFragment>()
        
        // Capture photo
        onView(withId(R.id.btn_capture)).perform(click())
        
        // Verify navigation to review
        onView(withId(R.id.fragment_review)).check(matches(isDisplayed()))
    }
}
```

### Documentation Guidelines

**Public APIs need KDoc**:
```kotlin
/**
 * Creates a transaction in YNAB from a scanned receipt.
 *
 * This function validates the receipt data, converts it to a YNAB transaction,
 * and submits it to the API. If the device is offline, the transaction is queued
 * for later sync.
 *
 * @param receipt The scanned receipt with extracted fields
 * @param accountId The YNAB account ID to charge
 * @param categoryId Optional category ID for the transaction
 * @return [Result.Success] with the created transaction, or [Result.Error]
 * @throws ValidationException if receipt data is invalid
 */
suspend fun createTransaction(
    receipt: Receipt,
    accountId: String,
    categoryId: String?
): Result<Transaction>
```

**Complex logic needs comments**:
```kotlin
// Parse amount with various currency formats
// Handles: $XX.XX, XX.XX USD, XX,XX EUR, etc.
fun parseAmount(text: String): Double? {
    val patterns = listOf(
        Regex("""\$\s*(\d{1,3}(?:,\d{3})*\.\d{2})"""),  // $1,234.56
        Regex("""(\d{1,3}(?:,\d{3})*\.\d{2})\s*USD"""),  // 1,234.56 USD
        Regex("""(\d{1,3}(?:\.\d{3})*,\d{2})\s*EUR""")   // 1.234,56 EUR
    )
    // ... implementation
}
```

## Commit Message Guidelines

Follow [Conventional Commits](https://www.conventionalcommits.org/).

### Format

```
<type>(<scope>): <subject>

[optional body]

[optional footer]
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, no logic change)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Build process, dependencies, etc.
- `perf`: Performance improvements

### Scopes

- `camera`: Camera functionality
- `review`: Review screen
- `home`: Home/receipt list
- `settings`: Settings
- `auth`: Authentication
- `sync`: Sync functionality
- `ocr`: OCR/parsing
- `api`: YNAB API integration
- `db`: Database
- `ui`: UI components

### Examples

```bash
feat(camera): add flash toggle button

fix(review): prevent NullPointerException when amount is null

docs(readme): update installation instructions

refactor(repository): simplify error handling

test(ocr): add tests for amount extraction

chore(deps): update Retrofit to 2.9.0
```

### Breaking Changes

```bash
feat(api)!: change createTransaction return type

BREAKING CHANGE: createTransaction now returns Result<Transaction> instead of Transaction?
```

## Branching Strategy

### Main Branches

- **`main`**: Production-ready code
- **`develop`**: Integration branch for features (if needed)

### Feature Branches

Pattern: `<type>/<description>`

**Examples:**
```bash
feature/receipt-list-sorting
fix/camera-permission-crash
docs/api-integration-guide
refactor/repository-pattern
```

### Workflow

```bash
# 1. Update main
git checkout main
git pull origin main

# 2. Create feature branch
git checkout -b feature/my-feature

# 3. Make changes and commit
git add .
git commit -m "feat(scope): description"

# 4. Keep branch updated (rebase, don't merge)
git fetch origin
git rebase origin/main

# 5. Push to your fork
git push origin feature/my-feature

# 6. Create Pull Request

# 7. After merge, delete branch
git branch -d feature/my-feature
```

## Issue Reporting

### Bug Reports

Use the bug report template to include:
- Clear, descriptive title
- Steps to reproduce
- Expected vs actual behavior
- Environment details
- Screenshots/logs

### Feature Requests

Use the feature request template to include:
- Problem description
- Proposed solution
- Alternative solutions considered
- Additional context

### Labels

Issues are tagged with labels:
- **Type**: `bug`, `feature`, `enhancement`, `documentation`
- **Priority**: `critical`, `high`, `medium`, `low`
- **Status**: `needs-triage`, `in-progress`, `blocked`
- **Good First Issue**: `good first issue`, `help wanted`

## Testing Requirements

### Required Tests

For **all new features**:
- ✅ Unit tests in domain/data modules
- ✅ ViewModel tests
- ✅ UI tests for critical flows

For **bug fixes**:
- ✅ Regression test to prevent recurrence

### Coverage Goals

- **Domain module**: 90%+ coverage
- **Data module**: 80%+ coverage
- **App module**: 60%+ coverage

### Running Tests Locally

```bash
# All unit tests
./gradlew test

# All instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Code coverage
./gradlew testDebugUnitTestCoverage
open app/build/reports/coverage/test/debug/index.html
```

### CI/CD

All PRs must pass:
- ✅ All unit tests
- ✅ All instrumented tests
- ✅ Lint checks
- ✅ Code style checks
- ✅ Build successful

## Review Process

### Timeline

- **Initial Review**: Within 2-3 business days
- **Follow-up Reviews**: Within 1-2 business days
- **Merge**: After approval and passing CI

### Approval Criteria

Reviewers check:
- ✅ Code quality and style
- ✅ Tests pass and provide good coverage
- ✅ Documentation is updated
- ✅ No breaking changes (unless intentional and documented)
- ✅ Follows architectural patterns

### Addressing Feedback

- Be responsive to reviewer comments
- Ask questions if feedback is unclear
- Make requested changes promptly
- Mark conversations as resolved after addressing

## Recognition

Contributors are recognized in:
- **README.md**: Contributors section
- **CHANGELOG.md**: Release notes
- **GitHub**: Contributors graph

## Questions?

- **Documentation**: Check `/docs` folder
- **Issues**: Search existing issues
- **Discussions**: Use GitHub Discussions
- **Email**: [project-email@example.com]

---

**Thank you for contributing! 🎉**

## Related Documentation

- [Developer Guide](DEVELOPER_GUIDE.md) - Development setup
- [Architecture](ARCHITECTURE.md) - System architecture
- [Testing Guide](../TESTING_GUIDE.md) - Testing strategies
- [Code of Conduct](../CODE_OF_CONDUCT.md) - Community guidelines
