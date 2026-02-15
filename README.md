# Receipt to YNAB Scanner - Android App

A native Android application that scans receipts using ML Kit OCR and automatically creates YNAB transactions.

## Build Requirements

- JDK 17+
- Android SDK API 26+ (Android 8.0+)
- Gradle 8.6+

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run lint checks  
./gradlew lintDebug

# Clean build
./gradle clean
```

## Project Structure

```
app/src/main/kotlin/com/receiptscanner/
├── ReceiptScannerApp.kt       # Application entry point with @HiltAndroidApp
├── data/                       # Data layer (repositories, data sources)
│   ├── image/
│   ├── local/
│   ├── ocr/
│   ├── parser/
│   ├── remote/
│   ├── repository/
│   └── worker/
├── domain/                     # Domain layer (business logic)
│   ├── model/
│   ├── repository/
│   └── usecase/
├── presentation/               # Presentation layer (UI, ViewModels)
│   ├── auth/
│   ├── camera/
│   ├── review/
│   └── result/
└── di/                         # Dependency injection modules
    └── AppModule.kt
```

## Architecture

This project follows Clean Architecture principles:
- **Domain Layer**: Pure Kotlin, no Android dependencies
- **Data Layer**: Repository implementations, data sources  
- **Presentation Layer**: UI components, ViewModels

## Dependencies

- Hilt 2.51 - Dependency Injection
- Kotlin 1.9.23 
- CameraX 1.3.1 - Camera APIs
- ML Kit Text Recognition - OCR
- Retrofit 2.9.0 + OkHttp 4.12 - Networking
- Room 2.6.1 - Local database
- WorkManager 2.9.0 - Background sync
- JUnit 4 + Mockk - Testing

## Phase 1 Status

✅ Project initialization complete
✅ Clean Architecture package structure
✅ Hilt dependency injection configured
✅ Test infrastructure in place
✅ All Phase 1 files created
