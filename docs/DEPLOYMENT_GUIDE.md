# Deployment Guide

## Table of Contents
- [Overview](#overview)
- [Pre-Release Checklist](#pre-release-checklist)
- [Creating Release Builds](#creating-release-builds)
- [Signing Configuration](#signing-configuration)
- [Version Bumping Strategy](#version-bumping-strategy)
- [Play Store Submission](#play-store-submission)
- [Internal Testing Track](#internal-testing-track)
- [Beta Testing](#beta-testing)
- [Production Release](#production-release)
- [Post-Release Monitoring](#post-release-monitoring)
- [Rollback Procedure](#rollback-procedure)

## Overview

This guide covers the complete process for releasing the YNAB Receipt Scanner app to the Google Play Store, from creating signed builds to production deployment.

### Release Types

| Type | Purpose | Audience | Rollout |
|------|---------|----------|---------|
| **Internal** | Quick testing | Development team | Instant |
| **Alpha** | Initial testing | Internal testers | Instant |
| **Beta** | Public testing | Opt-in users | Staged (20%, 50%, 100%) |
| **Production** | Public release | All users | Staged (1%, 5%, 10%, 25%, 50%, 100%) |

## Pre-Release Checklist

### Code Quality

- [ ] All tests passing (`./gradlew test connectedAndroidTest`)
- [ ] No compiler warnings
- [ ] Code coverage meets thresholds (>80%)
- [ ] Lint checks pass (`./gradlew lint`)
- [ ] ProGuard rules verified

### Documentation

- [ ] CHANGELOG.md updated with new features/fixes
- [ ] README.md updated if needed
- [ ] API documentation current
- [ ] User-facing documentation updated

### Features & Bugs

- [ ] All planned features complete
- [ ] No critical bugs
- [ ] High-priority bugs fixed
- [ ] Known issues documented

### Testing

- [ ] Manual testing on multiple devices
- [ ] Regression testing complete
- [ ] OCR accuracy verified
- [ ] YNAB integration tested
- [ ] Offline mode tested
- [ ] Background sync verified

### Assets & Resources

- [ ] App icon finalized
- [ ] Screenshots updated
- [ ] Store listing copy reviewed
- [ ] Privacy policy updated
- [ ] Terms of service current

### Compliance

- [ ] GDPR compliance verified
- [ ] Data encryption enabled
- [ ] Third-party licenses acknowledged
- [ ] Security audit complete (if applicable)

## Creating Release Builds

### Step 1: Set Up Signing

Create signing keystore (first time only):

```bash
keytool -genkey -v \
  -keystore ynab-receipt-scanner-release.keystore \
  -alias ynab-scanner \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# Follow prompts:
# - Enter keystore password
# - Re-enter password
# - Enter your name/organization details
# - Confirm information
# - Enter key password (can be same as keystore password)
```

**⚠️ IMPORTANT**: 
- Store keystore file securely (never commit to git!)
- Backup keystore and passwords (losing them means you can't update the app!)
- Consider using Google Play App Signing (recommended)

### Step 2: Configure Signing

Copy template:
```bash
cp keystore.properties.template keystore.properties
```

Edit `keystore.properties`:
```properties
storeFile=/path/to/ynab-receipt-scanner-release.keystore
storePassword=your_keystore_password
keyAlias=ynab-scanner
keyPassword=your_key_password
```

**⚠️ NOTE**: `keystore.properties` is gitignored for security.

### Step 3: Update Version

Edit `app/build.gradle.kts`:
```kotlin
android {
    defaultConfig {
        versionCode = 2  // Increment by 1
        versionName = "1.1.0"  // See versioning strategy
    }
}
```

### Step 4: Build Release APK

```bash
# Clean and build
./gradlew clean

# Build release APK
./gradlew assembleRelease

# Output location
ls app/build/outputs/apk/release/app-release.apk
```

### Step 5: Build Android App Bundle (AAB)

**Recommended** for Play Store (smaller downloads, dynamic delivery):

```bash
# Build AAB
./gradlew bundleRelease

# Output location
ls app/build/outputs/bundle/release/app-release.aab
```

### Step 6: Verify Build

```bash
# Install release build on device
adb install app/build/outputs/apk/release/app-release.apk

# Or test AAB with bundletool
bundletool build-apks \
  --bundle=app/build/outputs/bundle/release/app-release.aab \
  --output=app.apks \
  --mode=universal

bundletool install-apks --apks=app.apks
```

## Signing Configuration

### Option 1: Local Keystore (Manual)

In `app/build.gradle.kts`:
```kotlin
android {
    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
                
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

### Option 2: Google Play App Signing (Recommended)

1. Generate upload key (one-time):
   ```bash
   keytool -genkey -v \
     -keystore upload-key.keystore \
     -alias upload \
     -keyalg RSA \
     -keysize 2048 \
     -validity 10000
   ```

2. Enroll in Play App Signing:
   - Go to Play Console
   - Select your app
   - **Release > Setup > App Signing**
   - Follow instructions to enroll

3. Upload signed bundle with upload key
4. Google manages app signing key securely

**Benefits**:
- Google securely stores app signing key
- Can reset lost upload key
- Optimized APK delivery
- Automatic APK optimization

## Version Bumping Strategy

Follow [Semantic Versioning](https://semver.org/): `MAJOR.MINOR.PATCH`

### Version Code

Sequential integer, increment by 1 for each release:
```kotlin
versionCode = 1  // First release
versionCode = 2  // Second release
versionCode = 3  // Third release
```

### Version Name

Semantic version string:
```kotlin
versionName = "1.0.0"  // Initial release
versionName = "1.0.1"  // Patch (bug fixes)
versionName = "1.1.0"  // Minor (new features, backward compatible)
versionName = "2.0.0"  // Major (breaking changes)
```

### Versioning Rules

**MAJOR (X.0.0)**: Breaking changes
- API changes requiring user action
- Major architectural changes
- Removed features

**MINOR (1.X.0)**: New features
- New functionality (backward compatible)
- New screens/capabilities
- Performance improvements

**PATCH (1.0.X)**: Bug fixes
- Bug fixes
- Security patches
- Minor improvements

### Pre-Release Versions

For testing:
```kotlin
versionName = "1.1.0-alpha01"  // Alpha testing
versionName = "1.1.0-beta02"   // Beta testing
versionName = "1.1.0-rc01"     // Release candidate
```

### Update CHANGELOG

```markdown
## [1.1.0] - 2026-02-14

### Added
- Receipt list sorting options
- Dark mode support
- Spanish localization

### Changed
- Improved OCR accuracy
- Updated Material Design 3

### Fixed
- Camera crash on Pixel devices
- Sync failure when offline

### Security
- Updated encryption library
```

## Play Store Submission

### Step 1: Create App in Play Console

1. Go to https://play.google.com/console
2. Click **Create app**
3. Fill in details:
   - App name: **YNAB Receipt Scanner**
   - Default language: **English (US)**
   - App or game: **App**
   - Free or paid: **Free**
4. Accept declarations and click **Create app**

### Step 2: Complete Store Listing

Navigate to **Store presence > Main store listing**:

**App details**:
- **App name**: YNAB Receipt Scanner
- **Short description** (80 chars):
  ```
  Scan receipts and automatically create YNAB transactions with OCR technology
  ```
  
- **Full description** (4000 chars):
  ```
  Turn your receipts into YNAB transactions instantly! 
  
  YNAB Receipt Scanner uses advanced OCR (Optical Character Recognition) 
  to extract merchant name, date, and amount from your receipts, then 
  creates transactions in your YNAB budget with just a few taps.
  
  FEATURES:
  • 📸 Quick receipt capture with camera
  • 🔍 Accurate OCR text recognition
  • 💰 Automatic amount, date, and merchant extraction
  • 📱 Clean, intuitive interface
  • 🔄 Offline support with automatic sync
  • 🔒 Secure OAuth 2.0 authentication
  • 📊 Receipt history with sync status
  • 🌙 Dark mode support
  
  HOW IT WORKS:
  1. Scan your receipt with your phone camera
  2. Review and edit extracted fields
  3. Select account and category
  4. Create transaction in YNAB
  5. Done! Your budget is updated
  
  OFFLINE SUPPORT:
  No internet? No problem! Transactions are saved locally and 
  automatically synced when you're back online.
  
  SECURITY & PRIVACY:
  • OAuth 2.0 authentication (never stores your password)
  • Encrypted data storage
  • No data collection or analytics
  • Open source code
  
  REQUIREMENTS:
  • YNAB account (www.youneedabudget.com)
  • Android 7.0 or higher
  • Camera permission for receipt scanning
  
  Get started today and stop manually entering receipts!
  ```

**Graphics**:
- **App icon**: 512x512 PNG
- **Feature graphic**: 1024x500 JPG/PNG
- **Phone screenshots**: At least 2, up to 8 (16:9 ratio)
  - Camera screen
  - Review screen
  - Home screen with receipts
  - Settings screen
- **Tablet screenshots** (optional): 7" and 10"

**Categorization**:
- **App category**: Finance
- **Tags**: budgeting, receipts, expenses

**Contact details**:
- **Email**: your-email@example.com
- **Website**: https://github.com/your-username/ynab-receipt-scanner
- **Privacy policy**: https://your-domain.com/privacy (required)

### Step 3: Complete App Content

**Privacy Policy** (required):
- Host privacy policy on your website
- Provide URL in Play Console

**Data Safety**:
- Declare what data you collect
- Explain security practices
- **Data collected**:
  - Financial info (YNAB transactions)
  - Photos (receipts)
- **Data sharing**: No data shared with third parties
- **Security**: Data encrypted in transit and at rest

**Content Rating**:
- Complete questionnaire
- Likely rating: **Everyone**

**Target audience**:
- Age groups: 18+

### Step 4: Set Up Testing Track

Before production release, use testing tracks:

**Internal Testing**:
- Up to 100 testers
- Instant publishing
- For development team

**Closed Testing (Beta)**:
- Up to 100,000 testers
- Requires email list or Google Group
- Staged rollout available

**Open Testing**:
- Anyone can join
- Public on Play Store with "Join beta" button
- Good for public beta

### Step 5: Upload Build

1. Go to **Release > Production**
2. Click **Create new release**
3. Upload `app-release.aab`
4. Fill in **Release details**:
   - Release name: `1.0.0`
   - Release notes:
     ```
     🎉 Initial release
     
     Features:
     • Receipt scanning with OCR
     • YNAB integration
     • Offline support
     • Dark mode
     ```

5. Review and **Save**

## Internal Testing Track

### Setup

1. Go to **Release > Testing > Internal testing**
2. Click **Create new release**
3. Upload AAB
4. Add testers:
   - **Email list**: Add individual emails
   - **Google Group**: Create group and add
5. Click **Save**

### Distribution

1. Click **Review release**
2. Click **Start rollout to Internal testing**
3. Share opt-in link with testers

### Testing Period

- Minimum: 1 week
- Test all features
- Fix critical bugs
- Gather feedback

## Beta Testing

### Closed Beta

**Setup**:
1. Go to **Release > Testing > Closed testing**
2. Create track (e.g., "beta")
3. Upload AAB
4. Create testers list or Google Group
5. Start rollout

**Staged Rollout**:
```
Day 1:  20% of testers
Day 3:  50% of testers
Day 7:  100% of testers
```

**Duration**: 2-4 weeks

### Open Beta

**Setup**:
1. Go to **Release > Testing > Open testing**
2. Upload AAB
3. Start rollout
4. Available on Play Store with "Join beta" option

**Purpose**:
- Public testing before production
- Gather feedback from real users
- Identify edge cases

## Production Release

### Pre-Production Checklist

- [ ] All testing tracks successful
- [ ] No critical bugs reported
- [ ] Performance metrics acceptable
- [ ] Store listing finalized
- [ ] Release notes prepared
- [ ] Team notified
- [ ] Rollback plan ready

### Staged Rollout (Recommended)

Gradual release to minimize impact of issues:

**Day 1**: 1% of users
- Monitor crash rate
- Check performance metrics
- Watch for critical issues

**Day 2**: 5% of users (if stable)
- Continue monitoring
- Address feedback

**Day 4**: 20% of users
- Confirm stability
- Check reviews

**Day 7**: 50% of users
- Broader feedback
- Verify at scale

**Day 10**: 100% of users
- Full rollout
- Celebrate! 🎉

### Submit for Production

1. Go to **Release > Production**
2. Click **Create new release**
3. Upload AAB
4. Add release notes (user-facing)
5. **Staged rollout** percentage: Start at 1%
6. Click **Review release**
7. Click **Start rollout to Production**

### Rollout Controls

From Play Console:
- **Pause rollout**: Stop distribution temporarily
- **Increase percentage**: Roll out to more users
- **Halt release**: Stop and revert
- **Update release**: Push new version

## Post-Release Monitoring

### Metrics to Watch

**Crashes & ANRs**:
- Target: <1% crash rate
- Monitor via Play Console > Quality > Android vitals

**Performance**:
- App startup time
- Low memory issues
- Slow rendering

**User Ratings**:
- Target: 4.0+ stars
- Respond to negative reviews

**Installs & Uninstalls**:
- Track install/uninstall ratio
- Monitor retention rates

### Monitoring Tools

**Google Play Console**:
- Android Vitals
- Crash reports
- ANR reports
- User feedback

**Firebase (if integrated)**:
- Crashlytics
- Performance Monitoring
- Analytics

**Third-party**:
- Sentry
- Bugsnag  
- AppDynamics

### Response Plan

**Critical Bug (Crash >2%)**:
1. Halt rollout immediately
2. Analyze crash reports
3. Fix bug
4. Test thoroughly
5. Release hotfix

**Minor Bug**:
1. Log issue
2. Prioritize for next release
3. Continue rollout

**Negative Reviews**:
1. Respond professionally
2. Ask for details
3. Fix issues
4. Follow up with users

## Rollback Procedure

### When to Rollback

- Critical crash affecting >2% of users
- Data loss bug
- Security vulnerability
- API breaking change
- Widespread negative feedback

### How to Rollback

**Option 1: Halt Staged Rollout**
1. Go to **Release > Production**
2. Click **Halt rollout**
3. Most users stay on previous version
4. Fix issues and re-release

**Option 2: Release Previous Version**
(If fully rolled out)
1. Bump version code (must be higher)
2. Use previous version's code
3. Upload as new release
4. Roll out immediately to 100%

**Option 3: Hotfix Release**
1. Create branch from last stable tag
2. Apply critical fix
3. Bump version (patch)
4. Fast-track testing
5. Release immediately

Example:
```bash
# Create hotfix branch
git checkout v1.0.0
git checkout -b hotfix/critical-crash

# Fix the issue
# ... make changes ...

# Commit and tag
git commit -m "fix: critical crash on startup"
git tag v1.0.1

# Build and deploy
./gradlew bundleRelease
# Upload to Play Console
```

### Communication

1. Notify users via in-app message
2. Update store listing
3. Post on social media/website
4. Email beta testers
5. Document in CHANGELOG

---

## Release Checklist Summary

### Pre-Release
- [ ] Code quality checks pass
- [ ] All tests pass
- [ ] Version bumped
- [ ] CHANGELOG updated
- [ ] Store listing ready
- [ ] Screenshots updated

### Build
- [ ] Keystore configured
- [ ] Release build signed
- [ ] AAB generated
- [ ] Build tested manually

### Submission
- [ ] Store listing complete
- [ ] Privacy policy published
- [ ] Data safety completed
- [ ] Content rating obtained
- [ ] AAB uploaded

### Testing Tracks
- [ ] Internal testing (1 week)
- [ ] Closed beta (2-4 weeks)
- [ ] Open beta (optional)

### Production
- [ ] Staged rollout (1% → 100%)
- [ ] Monitoring enabled
- [ ] Team notified
- [ ] Rollback plan ready

### Post-Release
- [ ] Monitor crash rates
- [ ] Track performance
- [ ] Respond to reviews
- [ ] Document lessons learned

---

## Related Documentation

- [Developer Guide](DEVELOPER_GUIDE.md) - Build instructions
- [Testing Guide](../TESTING_GUIDE.md) - Testing strategies
- [Release Checklist](../release-checklist.md) - Quick checklist
