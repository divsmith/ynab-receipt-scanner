# Frequently Asked Questions (FAQ)

## Table of Contents
- [General Questions](#general-questions)
- [Account & Security](#account--security)
- [OCR & Receipt Scanning](#ocr--receipt-scanning)
- [Sync & Offline Mode](#sync--offline-mode)
- [YNAB Integration](#ynab-integration)
- [Privacy & Data](#privacy--data)
- [Technical Questions](#technical-questions)
- [Troubleshooting](#troubleshooting)

## General Questions

### What is YNAB Receipt Scanner?

YNAB Receipt Scanner is an Android app that uses OCR (Optical Character Recognition) to extract information from receipt photos and automatically create transactions in your YNAB budget. It saves you time by eliminating manual data entry.

### Is the app free?

Yes, the app is free and open source. However, you need an active YNAB subscription to use the app, as it integrates with YNAB's API.

### What is YNAB?

YNAB (You Need A Budget) is a budgeting software that helps you manage your money. Learn more at [www.youneedabudget.com](https://www.youneedabudget.com/).

### Do I need a YNAB subscription?

Yes, you need an active YNAB subscription to use this app. The app connects to your YNAB account to create transactions.

Subscribe at: https://www.youneedabudget.com/pricing/

### Is this an official YNAB app?

No, this is an **unofficial, community-created app**. It uses YNAB's public API. YNAB itself offers an official mobile app for managing your budget.

### What platforms are supported?

Currently:
- ✅ **Android**: Android 7.0 (API 24) and higher

Future plans:
- 🎯 **iOS**: Planned for future development
- 🎯 **Web**: Under consideration

### Is the app open source?

Yes! The app is open source and available on GitHub. You can review the code, report issues, and contribute.

Repository: [GitHub link]

### How do I get support?

- **User Guide**: [USER_GUIDE.md](USER_GUIDE.md)
- **Troubleshooting**: [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
- **GitHub Issues**: Report bugs or request features
- **Email**: support@example.com

## Account & Security

### How do I connect my YNAB account?

The app uses OAuth 2.0 for secure authentication:

1. Open the app for the first time
2. Tap "Connect to YNAB"
3. Log in to YNAB in your browser
4. Review and approve permissions
5. Redirected back to app - connected!

### Is my YNAB password stored in the app?

**No!** The app never sees or stores your YNAB password. It uses OAuth 2.0, which means:
- You log in directly on YNAB's website
- The app receives a secure token
- Token can be revoked anytime
- Your password stays with YNAB

### What permissions does the app request from YNAB?

The app requests:
- ✅ Read your budgets
- ✅ Read your accounts
- ✅ Read your categories  
- ✅ Create transactions

The app **cannot**:
- ❌ See your password
- ❌ Delete transactions
- ❌ Modify budget amounts
- ❌ Access other YNAB users

### How do I revoke app access?

To disconnect the app from YNAB:

**In App:**
1. Go to Settings
2. Tap "Sign Out"
3. Confirm

**In YNAB:**
1. Go to [YNAB Account Settings](https://app.youneedabudget.com/settings)
2. Navigate to "Authorized Applications"
3. Find "YNAB Receipt Scanner"
4. Click "Revoke Access"

### Is my data encrypted?

Yes! The app uses encryption:
- **In Transit**: HTTPS for all network communication
- **At Rest**: Encrypted storage for OAuth tokens
- **Database**: Optional SQLCipher encryption
- **No Analytics**: No tracking or data collection

### What happens if I lose my phone?

Your data is safe:
- **YNAB Data**: Stored in YNAB's cloud (not on device)
- **Receipt Photos**: Only stored locally (if enabled)
- **Tokens**: Encrypted storage
- **Action**: Revoke app access from YNAB settings

## OCR & Receipt Scanning

### What is OCR?

OCR (Optical Character Recognition) is technology that extracts text from images. The app uses Google ML Kit to read text from receipt photos.

### How accurate is the OCR?

Typical accuracy rates:
- **Merchant Name**: 85-95%
- **Amount**: 90-95%
- **Date**: 80-90%
- **Overall**: Varies by receipt quality

Factors affecting accuracy:
- ✅ Good lighting → better accuracy
- ✅ Sharp focus → better accuracy
- ✅ Clear print → better accuracy
- ❌ Faded receipts → lower accuracy
- ❌ Wrinkled paper → lower accuracy

### What types of receipts work best?

**Best Results:**
- Printed receipts (retail, restaurant, grocery)
- Fresh thermal paper
- Clear, high-contrast text
- English language
- Standard receipt layouts

**May Have Issues:**
- Handwritten receipts
- Faded thermal paper (scan immediately!)
- Non-English text (currently)
- Unusual layouts
- Low-quality printing

### Can I scan receipts in other languages?

Currently, the app is optimized for **English** receipts only. Support for additional languages is planned for future releases.

### What if OCR makes a mistake?

No problem! You can:
1. Review extracted data before creating transaction
2. Edit any field manually
3. The app learns from your corrections
4. Next time, accuracy improves

### Can I manually enter transaction details?

Yes! You can:
- Edit all fields after scanning
- Or skip camera and enter manually (tap "Manual Entry" in settings)

### Why is thermal paper a problem?

Thermal receipts fade over time:
- Heat-sensitive coating fades
- Text becomes unreadable in weeks/months
- **Solution**: Scan receipts immediately after purchase!

### Does the app keep the receipt photo?

Optional! In Settings:
- **Save Photos**: On → Stores receipt images
- **Save Photos**: Off → Deletes after processing

Storage usage:
- Each receipt: ~100-500 KB
- 100 receipts = ~30 MB

## Sync & Offline Mode

### Does the app work offline?

Yes! Offline capabilities:
- ✅ Scan receipts
- ✅ Review and edit
- ✅ Create transactions (queued locally)
- ❌ Can't sync to YNAB (until online)

When back online:
- Auto-syncs pending transactions
- Refreshes accounts/categories
- Updates sync status

### How does automatic sync work?

The app syncs in the background:
- **Interval**: Every 15 minutes (configurable)
- **Trigger**: When online
- **Retry**: Auto-retries failed transactions
- **Manual**: Tap "Sync Now" in Settings

Configure sync: Settings > Sync

### What happens to transactions created offline?

Offline transactions:
1. Saved locally with "Pending" status
2. Queued for sync
3. Auto-sync when internet available
4. Status changes to "Synced" when complete
5. Visible in YNAB

### How do I know if a transaction synced?

Check the sync status badge:
- ✅ **Green "Synced"**: Successfully in YNAB
- ⏳ **Yellow "Pending"**: Waiting to sync
- ❌ **Red "Failed"**: Sync error, tap to retry
- 🔄 **Blue "Syncing"**: Currently uploading

### Can I sync only on WiFi?

Yes! To save mobile data:
1. Go to Settings > Sync
2. Enable "WiFi only"
3. App will sync only when on WiFi

### What if sync fails?

Failed transactions:
- Stay in local database
- Auto-retry every 15 minutes
- Manual retry: Tap transaction > "Retry"
- Check error message for details

Common causes:
- No internet connection
- YNAB API temporarily down
- Invalid account/category (deleted in YNAB)
- Token expired (sign in again)

## YNAB Integration

### Which YNAB features are supported?

Supported:
- ✅ Create transactions
- ✅ Select accounts (on-budget, credit cards)
- ✅ Select categories
- ✅ Add memos
- ✅ Set cleared status
- ✅ Multiple budgets

Not Supported (yet):
- ❌ Edit existing transactions
- ❌ Split transactions
- ❌ Add flags/colors
- ❌ Attach receipt images to YNAB

### Can I use multiple budgets?

Yes! If you have multiple YNAB budgets:
1. Connect your YNAB account
2. Select primary budget
3. Switch budgets in Settings > Budget
4. Each budget has its own accounts/categories

### Do transactions appear immediately in YNAB?

Almost! After creating:
- **Sync time**: 1-2 seconds (if online)
- **YNAB refresh**: May take a few seconds in YNAB app
- **Pending offline**: Syncs when back online

Tip: Refresh YNAB app to see new transactions.

### Can I edit transactions after creating them?

Not directly in the app. To edit:
1. Open YNAB app or website
2. Find the transaction
3. Make your edits
4. Changes sync across YNAB

Future update may add transaction editing.

### What about split transactions?

Split transactions are not currently supported. Workaround:
1. Create main transaction in app
2. Split it in YNAB app/web
3. Or create multiple transactions

### Can I delete transactions?

Not in the app. To delete:
1. Go to YNAB app/web
2. Find transaction
3. Delete it there

Or don't create it in the scanner app if incorrect.

### What's the memo field for?

The memo field adds notes to your transaction:
- Default: "Receipt: [Merchant Name]"
- Add details: "Team lunch", "Gas - road trip"
- Visible in YNAB for reference

### Do transactions affect my YNAB budget immediately?

Yes! Transactions created are:
- Added to selected account
- Assigned to selected category (if any)
- Affect budget immediately
- Count toward category balance

## Privacy & Data

### What data does the app collect?

**Collected and stored locally:**
- Receipt photos (if enabled)
- Extracted transaction data
- OAuth token (encrypted)
- Sync status

**NOT collected:**
- Personal information
- Location data
- Device information
- Usage analytics
- Crash reports (unless you opt-in)

### Is my data shared with third parties?

**No!** The app only communicates with:
- YNAB API (to create transactions)
- Google ML Kit (on-device OCR, no data sent to Google)

**No data** is shared with:
- Advertisers
- Analytics platforms
- Other third parties

### Can I export my data?

Currently, you can:
- View receipts in app
- Access YNAB transactions in YNAB

Future feature: Export receipts to CSV/PDF.

### How do I delete all my data?

**From app:**
1. Settings > Privacy & Data
2. "Delete All Data"
3. Confirm
4. Clears all local data

**From YNAB:**
1. Sign out in app
2. Revoke access in YNAB settings

**Note**: Transactions already in YNAB must be deleted from YNAB.

### Is the app GDPR compliant?

Yes! The app:
- Collects minimal data
- Stores data locally
- Provides data deletion
- Uses secure authentication
- No unauthorized sharing
- Open source (auditable)

See [Privacy Policy](PRIVACY_POLICY.md) for details.

## Technical Questions

### What Android versions are supported?

**Minimum**: Android 7.0 (Nougat, API 24)  
**Recommended**: Android 10+  
**Target**: Android 14 (API 34)

Works on:
- Phones and tablets
- Various screen sizes
- Most Android devices since 2016

### How much storage does the app use?

Typical usage:
- **App**: ~50-100 MB
- **Receipt photos**: ~100-500 KB each
  - 100 receipts ≈ 30 MB
  - 1000 receipts ≈ 300 MB
- **Database**: ~5-10 MB

Tip: Enable "Delete photos after sync" to save space.

### Does the app use a lot of battery?

No! The app is optimized:
- Background sync uses job scheduling
- OCR is on-device (no cloud processing)
- Minimal background activity
- No constant tracking

Battery usage: <1% per day with normal use

### Does it require constant internet?

No! Internet needed for:
- Initial login
- Syncing transactions
- Refreshing accounts/categories

Works offline for:
- Scanning receipts
- Reviewing and editing
- Creating queued transactions

### What permissions does the app need?

**Required:**
- 📷 **Camera**: Take receipt photos
- 🌐 **Internet**: Sync with YNAB

**Optional:**
- 💾 **Storage**: Save receipt photos locally

**Never requested:**
- ❌ Location
- ❌ Contacts
- ❌ Microphone
- ❌ Phone calls

### Is the app available on iOS?

Not yet! Currently Android only.

iOS development is planned for the future using Kotlin Multiplatform to share business logic.

Follow the project on GitHub for updates!

## Troubleshooting

### App won't open or crashes immediately

**Try:**
1. Restart your device
2. Clear app cache: Settings > Apps > YNAB Scanner > Clear cache
3. Update to latest version from Play Store
4. Reinstall app (data is preserved in YNAB)
5. Check if Android version is supported (7.0+)

### Camera not working

**Try:**
1. Grant camera permission: Settings > Apps > YNAB Scanner > Permissions
2. Close other apps using camera
3. Restart device
4. Check if camera works in other apps
5. Clear app cache

### OCR not extracting anything

**Try:**
1. Retake photo with better lighting
2. Ensure receipt is in focus
3. Clean camera lens
4. Check if text is readable to human eye
5. Try a different receipt

### Can't sign in to YNAB

**Try:**
1. Check internet connection
2. Try opening YNAB website in browser
3. Clear app data (will lose local receipts)
4. Use latest app version
5. Check YNAB status page

### Transactions not syncing

**Try:**
1. Check internet connection
2. Tap "Sync Now" in Settings
3. Verify account/category still exists in YNAB
4. Sign out and sign in again
5. Check sync settings (WiFi-only?)

### For more troubleshooting, see [TROUBLESHOOTING.md](TROUBLESHOOTING.md)

---

## Still Have Questions?

- **Documentation**: Browse the [docs](/) folder
- **User Guide**: [USER_GUIDE.md](USER_GUIDE.md)
- **GitHub Issues**: Search or create issue
- **Email Support**: support@example.com

**Found the answer helpful? Star the project on GitHub! ⭐**
