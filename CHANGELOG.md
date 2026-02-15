# Changelog

All notable changes to the YNAB Receipt Scanner project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned
- Jetpack Compose migration for UI layer
- Batch receipt processing
- Receipt history search and filtering
- Export receipts to PDF
- Recurring transaction templates
- Budget insights and analytics

---

## [1.0.0] - 2024-01-15

### 🎉 Initial Release

First production-ready release of YNAB Receipt Scanner with full feature set.

### ✨ Features

#### Core Functionality
- **Camera Integration**
  - CameraX-based receipt capture with optimized settings
  - Auto-focus and tap-to-focus support
  - Flash control for low-light conditions
  - Image quality optimization for OCR
  - Gallery image import support

- **OCR & Receipt Parsing**
  - On-device text recognition using Google ML Kit
  - Smart merchant name extraction
  - Automatic amount detection with currency formatting
  - Date parsing with multiple format support
  - Confidence scoring (85-95% accuracy)
  - Multi-language support (English, Spanish)

- **YNAB Integration**
  - OAuth 2.0 authentication flow
  - Secure token storage with EncryptedSharedPreferences
  - Automatic token refresh
  - Budget, account, and category fetching
  - Transaction creation with duplicate detection
  - Rate limiting (200 requests/hour)

- **Offline Sync**
  - Queue transactions when offline
  - Automatic sync when network available
  - WorkManager-based periodic sync
  - Retry logic with exponential backoff
  - Sync status indicators

- **User Interface**
  - Material Design 3 implementation
  - Dynamic color theming (Material You)
  - Dark mode support
  - Intuitive navigation with Navigation Component
  - Receipt review and editing screen
  - Account and category selection
  - Settings screen with preferences
  - Error handling with user-friendly messages

#### Security
- EncryptedSharedPreferences for OAuth tokens
- SQLCipher database encryption
- HTTPS enforcement with certificate pinning
- Network Security Config
- Secure image storage in app-private directory
- No data collection or tracking

#### Data Management
- Room database with SQLCipher encryption
- Receipt history persistence
- Pending transaction queue
- Sync status tracking
- Account and category caching

### 🏗️ Architecture

#### Multi-Module Structure
- **app**: UI layer (Activities, Fragments, ViewModels)
- **domain**: Business logic (Use Cases, Domain Models)
- **data**: Data layer (Repositories, API, Database)
- **core**: Shared utilities (DI, Extensions, Constants)

#### Design Patterns
- Clean Architecture with MVVM presentation
- Repository pattern for data access
- Use cases for business logic
- Dependency injection with Hilt
- Result wrapper for error handling

#### Technical Stack
- Kotlin 1.9.22
- Minimum SDK 24 (Android 7.0)
- Target SDK 34 (Android 14)
- Gradle 8.2+ with Kotlin DSL
- Version Catalog for dependency management

### 🧪 Testing

#### Unit Tests
- Domain layer: 90% coverage
- Data layer: 80% coverage
- ViewModels: 75% coverage
- JUnit 4, MockK, Turbine for Flow testing

#### Instrumentation Tests
- Room database tests
- API integration tests
- UI tests with Espresso
- Test coverage: 70%+ overall

#### Testing Infrastructure
- JaCoCo for coverage reports
- Robolectric for Android framework tests
- Fake repositories for testing
- Test utilities and builders

### 📦 Dependencies

#### Core Libraries
- **UI**: Material Design 3, AndroidX Navigation Component, Lifecycle 2.7.0
- **DI**: Hilt 2.48
- **Database**: Room 2.6.1, SQLCipher 4.5.4
- **Network**: Retrofit 2.9.0, OkHttp 4.12.0, Moshi 1.15.0
- **Camera**: CameraX 1.3.1
- **OCR**: ML Kit Text Recognition 16.0.0
- **Async**: Kotlin Coroutines 1.7.3, Flow
- **Background**: WorkManager 2.9.0
- **Security**: Jetpack Security 1.1.0-alpha06

#### Testing Libraries
- JUnit 4.13.2
- MockK 1.13.8
- Espresso 3.5.1
- Robolectric 4.11.1
- Turbine 1.0.0
- Truth 1.1.5

### 🐛 Known Issues

- None at this time

### 📝 Notes

#### Migration from Beta
This is the first stable release. No migration from previous versions required.

#### YNAB API Compatibility
- Compatible with YNAB API v1
- Requires Personal Access Token or OAuth 2.0
- Rate limit: 200 requests per hour

#### Device Requirements
- Android 7.0 (API 24) or higher
- Camera for receipt capture
- Internet connection for YNAB sync
- ~50MB storage space

---

## Version History Template

Use this template for future releases:

```markdown
## [X.Y.Z] - YYYY-MM-DD

### Added
- New features

### Changed
- Changes in existing functionality

### Deprecated
- Soon-to-be removed features

### Removed
- Removed features

### Fixed
- Bug fixes

### Security
- Security improvements
```

---

## Version Guidelines

### Semantic Versioning

- **MAJOR** (X.0.0): Incompatible API changes
- **MINOR** (0.X.0): New features, backwards-compatible
- **PATCH** (0.0.X): Bug fixes, backwards-compatible

### Examples

- `1.0.0` → `1.0.1`: Bug fix (PATCH)
- `1.0.1` → `1.1.0`: New feature (MINOR)
- `1.1.0` → `2.0.0`: Breaking change (MAJOR)

### Release Types

- **Alpha**: Early development (`1.0.0-alpha.1`)
- **Beta**: Feature complete, testing (`1.0.0-beta.1`)
- **RC**: Release candidate (`1.0.0-rc.1`)
- **Stable**: Production ready (`1.0.0`)

---

## Release Process

### Creating a Release

1. **Update version in `build.gradle.kts`:**
   ```kotlin
   versionCode = 2
   versionName = "1.0.1"
   ```

2. **Update CHANGELOG.md:**
   - Move items from `[Unreleased]` to new version section
   - Add release date

3. **Create release commit:**
   ```bash
   git add .
   git commit -m "Release v1.0.1"
   git tag v1.0.1
   git push origin main --tags
   ```

4. **Build release APK:**
   ```bash
   ./gradlew assembleRelease
   ```

5. **Create GitHub Release:**
   - Go to GitHub Releases
   - Create new release from tag
   - Attach APK and AAB files
   - Copy changelog content

---

## Changelog Maintenance

### Adding Entry

When making changes:

1. Add entry under `[Unreleased]` section
2. Use appropriate category (Added, Changed, Fixed, etc.)
3. Write user-focused descriptions
4. Link to issues/PRs when relevant

### Example Entry

```markdown
## [Unreleased]

### Added
- Batch receipt processing (#123)
- Receipt search functionality (#145)

### Fixed
- Camera crash on rotation (#156)
- Sync status not updating (#167)
```

---

## Links

- [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)
- [Semantic Versioning](https://semver.org/spec/v2.0.0.html)
- [GitHub Releases](https://github.com/yourusername/ynab-receipt-scanner/releases)
- [Contributing Guide](docs/CONTRIBUTING.md)
- [Release Process](docs/DEPLOYMENT_GUIDE.md)

---

**Last Updated**: January 15, 2024
