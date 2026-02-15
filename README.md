# 📱 YNAB Receipt Scanner

> Snap. Scan. Sync. Transform your receipts into YNAB transactions in seconds.

An Android application that uses on-device OCR to extract transaction details from receipt photos and seamlessly sync them to your YNAB (You Need A Budget) account. Built with Clean Architecture, Material Design 3, and modern Android development best practices.

[![Android](https://img.shields.io/badge/Android-7.0+-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-orange.svg)](LICENSE)

---

## ✨ Features

- 📸 **Camera Integration** - Capture receipts directly with optimized camera controls
- 🔍 **OCR Recognition** - On-device text extraction using Google ML Kit (85-95% accuracy)
- 🎯 **Smart Parsing** - Automatically detect merchant name, amount, and date
- ✏️ **Manual Editing** - Review and edit extracted fields before syncing
- 💰 **YNAB Integration** - Seamless OAuth 2.0 authentication and transaction creation
- 🔄 **Offline Sync** - Queue transactions offline, sync when reconnected
- 📊 **Sync Status** - Real-time sync indicators and pending transaction counts
- 🎨 **Material Design 3** - Modern UI with dynamic color theming
- 🌙 **Dark Mode** - Full dark theme support
- 🔒 **Secure Storage** - Encrypted tokens and SQLCipher database encryption
- 🌐 **Multi-language** - English and Spanish localization

---

## 📸 Screenshots

<table>
  <tr>
    <td><img src="docs/screenshots/home.png" alt="Home Screen" width="200"/><br/><sub>Home Screen</sub></td>
    <td><img src="docs/screenshots/camera.png" alt="Camera" width="200"/><br/><sub>Camera Capture</sub></td>
    <td><img src="docs/screenshots/review.png" alt="Review" width="200"/><br/><sub>Review & Edit</sub></td>
    <td><img src="docs/screenshots/sync.png" alt="Sync Status" width="200"/><br/><sub>Sync Status</sub></td>
  </tr>
</table>

> 📝 *Screenshots coming soon. Placeholders show typical app workflow.*

---

## 🏗️ Architecture

This project follows **Clean Architecture** principles with a **multi-module structure**:

```
ynab-receipt-scanner/
├── app/         # UI Layer (Activities, Fragments, ViewModels)
├── domain/      # Business Logic (Use Cases, Domain Models)
├── data/        # Data Layer (Repositories, API, Database)
└── core/        # Shared Utilities (DI, Extensions, Constants)
```

**Key Design Patterns:**
- MVVM (Model-View-ViewModel) for presentation
- Repository pattern for data access
- Use cases for business logic
- Dependency injection with Hilt

📚 [**Read more about the architecture →**](docs/ARCHITECTURE.md)

---

## 🛠️ Tech Stack

| Category | Technologies |
|----------|-------------|
| **Language** | Kotlin 1.9.22 |
| **UI** | Material Design 3, Jetpack Compose (future), Navigation Component |
| **DI** | Hilt 2.48 |
| **Database** | Room 2.6.1, SQLCipher 4.5.4 |
| **Network** | Retrofit 2.9.0, OkHttp 4.12.0, Moshi |
| **Camera** | CameraX 1.3.1 |
| **OCR** | ML Kit Text Recognition 16.0.0 |
| **Async** | Kotlin Coroutines 1.7.3, Flow |
| **Security** | EncryptedSharedPreferences, HTTPS, OAuth 2.0 |
| **Testing** | JUnit 4, MockK, Espresso, Turbine, Robolectric |
| **Build** | Gradle 8.2+ with Kotlin DSL, Version Catalog |

---

## 🚀 Quick Start

### Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or later
- **JDK 17** or higher
- **Android SDK 34** (API level 34)
- **Gradle 8.2+**
- **YNAB Account** with Personal Access Token

### Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/yourusername/ynab-receipt-scanner.git
   cd ynab-receipt-scanner
   ```

2. **Open in Android Studio:**
   ```bash
   # Or open the project directory in Android Studio
   studio .
   ```

3. **Configure YNAB API:**
   - Get your Personal Access Token from [YNAB Developer Settings](https://app.youneedabudget.com/settings/developer)
   - The app will prompt for authentication on first launch

4. **Build and run:**
   ```bash
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```

   Or click ▶️ **Run** in Android Studio

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test suite
./gradlew :domain:test
./gradlew :data:testDebugUnitTest

# Run instrumentation tests
./gradlew connectedAndroidTest

# Generate coverage report
./gradlew testDebugUnitTestCoverage
```

📚 [**Detailed setup instructions →**](docs/DEVELOPER_GUIDE.md)

---

## 📚 Documentation

Comprehensive documentation is available in the `/docs` directory:

### 📖 For Users
- [**User Guide**](docs/USER_GUIDE.md) - How to use the app
- [**FAQ**](docs/FAQ.md) - Frequently asked questions
- [**Troubleshooting**](docs/TROUBLESHOOTING.md) - Common issues and solutions

### 🛠️ For Developers
- [**Developer Guide**](docs/DEVELOPER_GUIDE.md) - Getting started with development
- [**Architecture**](docs/ARCHITECTURE.md) - System design and patterns
- [**Module Structure**](docs/MODULE_STRUCTURE.md) - Module breakdown and dependencies
- [**API Reference**](docs/API.md) - Public API and usage examples
- [**OCR Parser Guide**](docs/OCR_PARSER_GUIDE.md) - Customize receipt parsing
- [**Testing Guide**](TESTING_GUIDE.md) - Testing strategies and tools

### 🚀 For Contributors
- [**Contributing**](docs/CONTRIBUTING.md) - Contribution guidelines
- [**Deployment Guide**](docs/DEPLOYMENT_GUIDE.md) - Release and deployment process
- [**API Integration**](docs/API_INTEGRATION.md) - YNAB API usage
- [**Cross-Platform Readiness**](docs/CROSS_PLATFORM_READINESS.md) - iOS migration guide

### 🔒 Security
- [**Security**](docs/SECURITY.md) - Security architecture and best practices
- [**Privacy Policy**](docs/PRIVACY_POLICY.md) - Privacy practices

---

## 🤝 Contributing

Contributions are welcome! Whether you're fixing bugs, adding features, or improving documentation, your help is appreciated.

### How to Contribute

1. **Fork the repository**
2. **Create a feature branch** (`git checkout -b feature/amazing-feature`)
3. **Make your changes** following our [coding standards](docs/CONTRIBUTING.md)
4. **Write tests** for your changes
5. **Commit your changes** (`git commit -m 'Add amazing feature'`)
6. **Push to your branch** (`git push origin feature/amazing-feature`)
7. **Open a Pull Request**

### Development Workflow

```bash
# Run pre-commit checks
./gradlew check

# Format code
./gradlew spotlessApply

# Run tests
./gradlew test

# Generate coverage
./gradlew testDebugUnitTestCoverage
```

📚 [**Read the full contributing guide →**](docs/CONTRIBUTING.md)

---

## 📋 Project Status

**Current Version:** 1.0.0 (Production Ready) ✅

### ✅ Completed Phases

- [x] Phase 1: Project Foundation
- [x] Phase 2: Domain Layer Implementation
- [x] Phase 3: Data Layer & API Integration
- [x] Phase 4: UI Implementation
- [x] Phase 5: OCR Integration
- [x] Phase 6: Testing Infrastructure
- [x] Phase 7: Offline Sync & WorkManager
- [x] Phase 8: Production Polish

**All 8 implementation phases complete!** 🎉

📚 [**View detailed changelog →**](CHANGELOG.md)

---

## 📄 License

This project is licensed under the **Apache License 2.0** - see the [LICENSE](LICENSE) file for details.

```
Copyright 2024 YNAB Receipt Scanner Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## 🙏 Acknowledgments

- **YNAB** - For the excellent budgeting platform and API
- **Google ML Kit** - For on-device text recognition
- **Material Design** - For design guidelines and components
- **Android Community** - For libraries and best practices

---

## 📧 Contact

- **Issues**: [GitHub Issues](https://github.com/yourusername/ynab-receipt-scanner/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/ynab-receipt-scanner/discussions)
- **Security**: [security@example.com](mailto:security@example.com)

---

## ⭐ Support

If you find this project useful, please consider:
- Giving it a ⭐ on GitHub
- Sharing it with others
- Contributing improvements
- Reporting bugs

---

<div align="center">

**Made with ❤️ using Kotlin and Modern Android Development**

[Report Bug](https://github.com/yourusername/ynab-receipt-scanner/issues) · [Request Feature](https://github.com/yourusername/ynab-receipt-scanner/issues) · [View Documentation](docs/)

</div>
