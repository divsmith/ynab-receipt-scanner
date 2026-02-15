# Release Checklist

Use this checklist before releasing a new version of the YNAB Receipt Scanner app.

## Pre-Release

### Version & Build
- [ ] Version name updated in `app/build.gradle.kts` (e.g., 1.0.0)
- [ ] Version code incremented in `app/build.gradle.kts`
- [ ] Changelog updated in `CHANGELOG.md` or similar
- [ ] Release notes prepared for Google Play

### Code Quality
- [ ] All unit tests passing (`./gradlew test`)
- [ ] All instrumented tests passing (`./gradlew connectedAndroidTest`)
- [ ] No compiler warnings
- [ ] No lint errors (`./gradlew lint`)
- [ ] Code review completed
- [ ] TODOs reviewed and addressed or documented

### ProGuard/R8
- [ ] ProGuard rules tested with release build
- [ ] Release build tested on physical device
- [ ] App functions correctly with minification enabled
- [ ] No crashes in release build
- [ ] All reflection-based code protected in ProGuard rules

### Signing
- [ ] Signing config created (not checked into git)
- [ ] Keystore file secured (backed up safely)
- [ ] Keystore password stored securely
- [ ] Release APK/AAB signed correctly
- [ ] Signature verified: `keytool -printcert -jarfile app-release.aab`

### Security
- [ ] No debug code in release build
- [ ] No hardcoded API keys or secrets
- [ ] HTTPS used for all network calls
- [ ] API keys stored securely (not in git)
- [ ] Security checks passing
- [ ] No root detection bypass issues

### Testing
- [ ] Smoke test on clean install
- [ ] Upgrade test from previous version
- [ ] Test on different screen sizes/densities
- [ ] Test on different Android API levels (min SDK to latest)
- [ ] Test with airplane mode (offline functionality)
- [ ] Test with poor network conditions
- [ ] Test memory usage and battery drain
- [ ] Test with screen rotation
- [ ] Test accessibility features (TalkBack)

### Features
- [ ] All main user flows tested
  - [ ] Login/Logout
  - [ ] Receipt scanning
  - [ ] OCR extraction
  - [ ] Transaction review
  - [ ] Transaction submission
  - [ ] Offline sync
  - [ ] Settings
- [ ] Error handling tested
- [ ] Edge cases tested
- [ ] Performance acceptable (no ANRs)

### Privacy & Compliance
- [ ] Privacy policy updated and accessible
- [ ] Terms of service updated
- [ ] Data collection disclosed clearly
- [ ] GDPR compliance reviewed (if applicable)
- [ ] No PII logged or tracked
- [ ] Analytics opt-out functional

### Google Play Store
- [ ] Store listing updated
- [ ] Screenshots captured (multiple devices/locales)
  - [ ] Phone screenshots (at least 2)
  - [ ] Tablet screenshots (optional)
  - [ ] Feature graphic updated
- [ ] App description updated
- [ ] What's new section filled
- [ ] Content rating questionnaire completed
- [ ] App category correct
- [ ] Age rating appropriate

### Backend/API
- [ ] YNAB API integration tested
- [ ] API rate limiting handled
- [ ] Error responses handled gracefully
- [ ] Offline capabilities tested

### Documentation
- [ ] README.md updated
- [ ] SETUP.md updated with any new requirements
- [ ] API documentation current
- [ ] User guide updated (if exists)
- [ ] Developer documentation updated

### Dependencies
- [ ] All dependencies up-to-date (security patches)
- [ ] No deprecated dependencies
- [ ] License compliance checked
- [ ] ProGuard rules for third-party libraries included

### Performance
- [ ] App size reasonable (< 50 MB ideally)
- [ ] Cold start time < 2 seconds
- [ ] Memory usage < 100 MB (typical usage)
- [ ] No memory leaks detected (LeakCanary)
- [ ] Battery usage minimal

### Firebase/Analytics (if configured)
- [ ] Firebase configured correctly
- [ ] Analytics events tracked properly
- [ ] Crashlytics enabled
- [ ] Test crash reports working
- [ ] Remote config tested (if used)

## Release

### Building
- [ ] Clean build: `./gradlew clean`
- [ ] Release build: `./gradlew assembleRelease` or `./gradlew bundleRelease`
- [ ] Build successful with no errors
- [ ] APK/AAB size acceptable
- [ ] APK analyzer reviewed (Android Studio)

### Upload
- [ ] AAB uploaded to Google Play Console
- [ ] Release track selected (internal/alpha/beta/production)
- [ ] Release notes added
- [ ] Staged rollout percentage set (e.g., 5%, 10%, 25%, 50%, 100%)
- [ ] Compliance questions answered

### Git/Version Control
- [ ] All changes committed
- [ ] Release branch created (if following git-flow)
- [ ] Tagged release: `git tag -a v1.0.0 -m "Version 1.0.0"`
- [ ] Pushed to remote: `git push --tags`

## Post-Release

### Monitoring
- [ ] Monitor crash reports (first 24 hours)
- [ ] Monitor app ratings/reviews
- [ ] Monitor analytics for issues
- [ ] Check for ANRs in Play Console
- [ ] Monitor server logs (if applicable)

### Communication
- [ ] Announce release (social media, blog, etc.)
- [ ] Notify beta testers
- [ ] Update website (if applicable)

### Backup
- [ ] Keystore backed up securely
- [ ] Build artifacts archived
- [ ] Release APK/AAB saved

## Rollback Plan
If critical issues are found:
- [ ] Prepare hotfix branch
- [ ] Roll back to previous version in Play Console
- [ ] Communicate issue to users
- [ ] Fix critical issues
- [ ] Follow release checklist again for hotfix

---

**Release Manager:** _______________  
**Date:** _______________  
**Version:** _______________  
**Approved by:** _______________
