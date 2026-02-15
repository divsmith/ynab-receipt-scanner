# Developer Guide

## Table of Contents
- [Prerequisites](#prerequisites)
- [Initial Setup](#initial-setup)
- [Building the Project](#building-the-project)
- [Running the App](#running-the-app)
- [YNAB Credentials Setup](#ynab-credentials-setup)
- [Running Tests](#running-tests)
- [Code Style Guidelines](#code-style-guidelines)
- [Git Workflow](#git-workflow)
- [Debugging](#debugging)
- [Common Tasks](#common-tasks)

## Prerequisites

### Required Software

| Tool | Minimum Version | Recommended |
|------|----------------|-------------|
| Android Studio | Hedgehog (2023.1.1) | Latest Stable |
| JDK | 17 | 17 |
| Gradle | 8.2 | 8.4+ |
| Android SDK | API 34 | Latest |
| Git | 2.x | Latest |

### Hardware Requirements

**Minimum**:
- 8 GB RAM
- 4 GB free disk space
- Intel/AMD processor with virtualization support

**Recommended**:
- 16 GB RAM
- 10 GB free disk space
- SSD storage
- Multi-core processor

### Optional Tools

- **Android Emulator**: For testing without a physical device
- **ADB (Android Debug Bridge)**: Comes with Android SDK
- **Scrcpy**: Screen mirroring tool for testing
- **Charles Proxy**: Network debugging

## Initial Setup

### Step 1: Clone the Repository

```bash
git clone <repository-url>
cd ynab-receipt-scanner
```

### Step 2: Install Android Studio

1. Download from: https://developer.android.com/studio
2. Install with default options
3. Open Android Studio
4. Go to **Settings > Appearance & Behavior > System Settings > Android SDK**
5. Install:
   - Android SDK Platform 34
   - Android SDK Build-Tools 34.0.0
   - Android Emulator (if needed)

### Step 3: Install JDK 17

#### macOS (Homebrew)
```bash
brew install openjdk@17

# Add to PATH
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

# Verify
java -version
```

#### Windows
1. Download from: https://adoptium.net/
2. Install and set JAVA_HOME environment variable
3. Verify: `java -version`

#### Linux
```bash
sudo apt update
sudo apt install openjdk-17-jdk

# Verify
java -version
```

### Step 4: Open Project in Android Studio

1. Open Android Studio
2. Click **File > Open**
3. Navigate to `ynab-receipt-scanner` directory
4. Click **OK**
5. Wait for Gradle sync to complete

### Step 5: Configure Local Properties

Create `local.properties` in project root (if not exists):
```properties
sdk.dir=/path/to/Android/sdk

# YNAB OAuth credentials (optional for development)
ynab.client.id=your_client_id_here
ynab.client.secret=your_client_secret_here
```

**Note**: `local.properties` is gitignored and should never be committed.

### Step 6: Sync Gradle

Click **File > Sync Project with Gradle Files** or the elephant icon in toolbar.

## Building the Project

### Command Line Build

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing config)
./gradlew assembleRelease

# Build all variants
./gradlew build
```

### Android Studio Build

1. Click **Build > Make Project** (⌘F9 / Ctrl+F9)
2. Or click **Build > Rebuild Project**

### Build Outputs

```
app/build/outputs/apk/
├── debug/
│   └── app-debug.apk
└── release/
    └── app-release.apk
```

### Build Variants

The project has two build variants:

| Variant | Purpose | ProGuard | Debuggable |
|---------|---------|----------|------------|
| **debug** | Development | No | Yes |
| **release** | Production | Yes | No |

Switch variants in Android Studio:
1. **View > Tool Windows > Build Variants**
2. Select desired variant

## Running the App

### On Physical Device

1. Enable **Developer Options** on your Android device:
   - Go to **Settings > About Phone**
   - Tap **Build Number** 7 times
   
2. Enable **USB Debugging**:
   - Go to **Settings > Developer Options**
   - Turn on **USB Debugging**

3. Connect device via USB

4. Run from Android Studio:
   - Click **Run** (▶) button
   - Select your device
   - Click **OK**

### On Emulator

1. Create emulator:
   - **Tools > Device Manager**
   - Click **Create Device**
   - Choose device (e.g., Pixel 6)
   - Choose system image (API 34)
   - Click **Finish**

2. Start emulator:
   - Click ▶ next to emulator name
   
3. Run app:
   - Click **Run** (▶) in Android Studio
   - Select emulator

### Command Line Run

```bash
# Install debug APK on connected device
./gradlew installDebug

# Run app
adb shell am start -n com.ynab.receiptscanner/.ui.MainActivity

# Or combined
./gradlew installDebug && adb shell am start -n com.ynab.receiptscanner/.ui.MainActivity
```

## YNAB Credentials Setup

### Option 1: OAuth 2.0 (Recommended)

1. Register app at: https://app.youneedabudget.com/settings/developer
2. Add credentials to `local.properties`:
   ```properties
   ynab.client.id=your_client_id
   ynab.client.secret=your_client_secret
   ```
3. Rebuild project
4. Run app and go through OAuth flow

### Option 2: Personal Access Token (Testing Only)

1. Generate token at: https://app.youneedabudget.com/settings/developer
2. In app, go to **Settings**
3. Tap **Developer Options**
4. Enter Personal Access Token
5. Click **Save**

**⚠️ Warning**: Never commit tokens to version control!

## Running Tests

### Run All Tests

```bash
# Run all unit tests
./gradlew test

# Run all instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Run all tests (unit + instrumented)
./gradlew test connectedAndroidTest
```

### Run Specific Test Classes

```bash
# Unit test
./gradlew test --tests "com.ynab.receiptscanner.domain.usecase.CreateTransactionUseCaseTest"

# Instrumented test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ynab.receiptscanner.ui.CameraFlowTest
```

### Run Tests in Android Studio

**Unit Tests**:
1. Right-click on test class/method
2. Click **Run 'TestName'**

**Instrumented Tests**:
1. Connect device/emulator
2. Right-click on test class
3. Click **Run 'TestName'**

### Code Coverage

```bash
# Generate coverage report
./gradlew testDebugUnitTestCoverage

# View report
open app/build/reports/coverage/test/debug/index.html
```

### Test Reports

After running tests, view reports:
```bash
# Unit test report
open app/build/reports/tests/testDebugUnitTest/index.html

# Instrumented test report
open app/build/reports/androidTests/connected/index.html
```

## Code Style Guidelines

### Kotlin Style

Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html):

```kotlin
// ✅ Good
class MyViewModel @Inject constructor(
    private val repository: MyRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    fun loadData() {
        viewModelScope.launch {
            when (val result = repository.getData()) {
                is Result.Success -> _uiState.value = UiState.Success(result.data)
                is Result.Error -> _uiState.value = UiState.Error(result.exception)
            }
        }
    }
}

// ❌ Bad
class myViewModel(repo: MyRepository): ViewModel(){
    var data:String?=null
    fun load(){
        // Logic here
    }
}
```

### Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| **Classes** | PascalCase | `CameraViewModel` |
| **Functions** | camelCase | `captureImage()` |
| **Variables** | camelCase | `receiptList` |
| **Constants** | UPPER_SNAKE_CASE | `MAX_RETRIES` |
| **Packages** | lowercase | `com.ynab.receiptscanner` |

### File Organization

```kotlin
// 1. Package declaration
package com.ynab.receiptscanner.ui.camera

// 2. Imports (grouped and sorted)
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ynab.receiptscanner.core.Result
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// 3. Class declaration with annotations
@AndroidEntryPoint
class CameraFragment : Fragment() {
    
    // 4. Injected dependencies
    @Inject
    lateinit var imageProcessor: ImageProcessor
    
    // 5. View binding
    private val binding by viewBinding(FragmentCameraBinding::bind)
    
    // 6. ViewModel
    private val viewModel: CameraViewModel by viewModels()
    
    // 7. Lifecycle methods
    override fun onCreateView(...): View {
        // ...
    }
    
    override fun onViewCreated(...) {
        // ...
    }
    
    // 8. Private helper methods
    private fun setupCamera() {
        // ...
    }
    
    // 9. Companion object
    companion object {
        private const val TAG = "CameraFragment"
    }
}
```

### Documentation

Use KDoc for public APIs:
```kotlin
/**
 * Creates a new transaction in YNAB from a scanned receipt.
 *
 * @param receipt The scanned receipt data
 * @param accountId The YNAB account ID to use
 * @param categoryId Optional category ID
 * @return [Result] containing the created [Transaction] or an error
 */
suspend fun createTransaction(
    receipt: Receipt,
    accountId: String,
    categoryId: String?
): Result<Transaction>
```

### Code Formatting

Configure Android Studio:
1. **Settings > Editor > Code Style > Kotlin**
2. Click **Set from... > Kotlin style guide**
3. Enable **Reformat code** on save

Run formatter:
```bash
# Format all code
./gradlew ktlintFormat
```

## Git Workflow

### Branch Naming

| Type | Pattern | Example |
|------|---------|---------|
| **Feature** | `feature/description` | `feature/add-receipt-list` |
| **Bug Fix** | `fix/description` | `fix/camera-crash` |
| **Hotfix** | `hotfix/description` | `hotfix/auth-error` |
| **Refactor** | `refactor/description` | `refactor/repository-impl` |
| **Documentation** | `docs/description` | `docs/update-readme` |

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types**: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

**Examples**:
```bash
feat(camera): add flash toggle button

fix(review): prevent crash when amount is null

docs(readme): update setup instructions

test(repository): add unit tests for YnabRepository
```

### Workflow

```bash
# 1. Create feature branch
git checkout -b feature/my-feature

# 2. Make changes and commit
git add .
git commit -m "feat(scope): description"

# 3. Keep branch updated
git fetch origin
git rebase origin/main

# 4. Push to remote
git push origin feature/my-feature

# 5. Create Pull Request on GitHub/GitLab

# 6. After approval, merge and delete branch
git checkout main
git pull
git branch -d feature/my-feature
```

## Debugging

### Logging

Use `Timber` for logging (or Android Log):
```kotlin
import timber.log.Timber

class MyClass {
    fun doSomething() {
        Timber.d("Starting operation")
        
        try {
            // ...
        } catch (e: Exception) {
            Timber.e(e, "Operation failed")
        }
    }
}
```

### Debug Build

1. Set breakpoint in code (click left gutter)
2. Click **Debug** (🐞) in Android Studio
3. App will pause at breakpoint
4. Use debug controls:
   - **Step Over** (F8)
   - **Step Into** (F7)
   - **Resume** (F9)

### Logcat

View app logs:
1. **View > Tool Windows > Logcat**
2. Filter by app package: `package:com.ynab.receiptscanner`
3. Filter by log level: Debug, Info, Warn, Error

### Network Debugging

Use Android Studio's Network Inspector:
1. **View > Tool Windows > App Inspection**
2. Select **Network Inspector** tab
3. Make API requests
4. View request/response details

Or use Charles Proxy:
1. Install SSL certificate on device
2. Configure proxy settings
3. View all HTTP/HTTPS traffic

### Database Inspection

View Room database:
1. **View > Tool Windows > App Inspection**
2. Select **Database Inspector** tab
3. Browse tables and data

Or use ADB:
```bash
# Pull database from device
adb pull /data/data/com.ynab.receiptscanner/databases/ynab_receipt_scanner.db

# Open with SQLite browser
sqlitebrowser ynab_receipt_scanner.db
```

## Common Tasks

### Add New Dependency

1. Open `gradle/libs.versions.toml`
2. Add version:
   ```toml
   [versions]
   newlib = "1.0.0"
   ```
3. Add library:
   ```toml
   [libraries]
   newlib = { module = "com.example:newlib", version.ref = "newlib" }
   ```
4. Add to module's `build.gradle.kts`:
   ```kotlin
   dependencies {
       implementation(libs.newlib)
   }
   ```
5. Sync Gradle

### Add New Fragment

1. Create layout XML: `res/layout/fragment_my.xml`
2. Create Fragment class:
   ```kotlin
   @AndroidEntryPoint
   class MyFragment : Fragment(R.layout.fragment_my) {
       private val binding by viewBinding(FragmentMyBinding::bind)
       private val viewModel: MyViewModel by viewModels()
       
       override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
           super.onViewCreated(view, savedInstanceState)
           setupViews()
           observeViewModel()
       }
   }
   ```
3. Create ViewModel:
   ```kotlin
   @HiltViewModel
   class MyViewModel @Inject constructor() : ViewModel()
   ```
4. Add to navigation graph: `res/navigation/nav_graph.xml`

### Add New Use Case

1. Create in domain module:
   ```kotlin
   class MyNewUseCase @Inject constructor(
       private val repository: MyRepository
   ) {
       suspend operator fun invoke(param: String): Result<Data> {
           return repository.fetchData(param)
       }
   }
   ```
2. Inject in ViewModel:
   ```kotlin
   @HiltViewModel
   class MyViewModel @Inject constructor(
       private val myNewUseCase: MyNewUseCase
   ) : ViewModel()
   ```

### Generate Signed APK

1. Create keystore (first time only):
   ```bash
   keytool -genkey -v -keystore my-release-key.keystore -alias my-key-alias -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Copy `keystore.properties.template` to `keystore.properties`
3. Fill in keystore details
4. Build release:
   ```bash
   ./gradlew assembleRelease
   ```
5. APK is in: `app/build/outputs/apk/release/`

---

## Next Steps

- Read [Testing Guide](../TESTING_GUIDE.md) for testing strategies
- See [Architecture](ARCHITECTURE.md) for system design
- Check [Contributing Guide](CONTRIBUTING.md) for contribution process
- Review [Code of Conduct](../CODE_OF_CONDUCT.md)

## Getting Help

- **Issues**: Create an issue on GitHub
- **Discussions**: Use GitHub Discussions
- **Documentation**: Check `/docs` folder
- **Code Comments**: Read inline documentation

## Useful Commands

```bash
# Clean build
./gradlew clean build

# Run app
./gradlew installDebug

# Run tests
./gradlew test connectedAndroidTest

# Check code style
./gradlew ktlintCheck

# Format code
./gradlew ktlintFormat

# Generate documentation
./gradlew dokkaHtml

# Check for dependency updates
./gradlew dependencyUpdates
```

---

**Happy coding! 🚀**
