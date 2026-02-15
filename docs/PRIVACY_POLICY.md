# Privacy Policy

**Last Updated: February 14, 2026**

## Introduction

YNAB Receipt Scanner ("we", "our", or "the app") is committed to protecting your privacy. This Privacy Policy explains how we collect, use, and safeguard your information when you use our mobile application.

By using YNAB Receipt Scanner, you agree to the collection and use of information in accordance with this policy.

## Information We Collect

### 1. Receipt Images

- **What we collect**: Photos of receipts you capture or select
- **How we use it**: To extract transaction information using Optical Character Recognition (OCR)
- **Storage**: Images are stored locally on your device and optionally synced to YNAB
- **Retention**: Controlled by your app settings (can delete after sync or keep indefinitely)

### 2. Transaction Data

Information extracted from receipts:
- Payee name
- Transaction amount
- Date
- Account information
- Category (if specified)

**Usage**: This data is sent to your YNAB account via the YNAB API to create transactions.

### 3. YNAB Account Access

- **Authentication**: We use OAuth 2.0 to securely access your YNAB account
- **Permissions**: Read budget data, create transactions
- **Token storage**: Access tokens stored securely on device using Android Keystore
- **No password storage**: We never store your YNAB password

### 4. Device Information

- **Technical data**: Android version, device model, app version
- **Usage**: For debugging and improving app performance
- **Not personally identifiable**: No device IDs or advertising IDs collected

### 5. Usage Analytics (Optional)

If you enable analytics:
- App feature usage
- Performance metrics
- Crash reports

**Opt-out**: You can disable analytics in app settings.

## How We Use Your Information

### Primary Purposes

1. **Receipt Processing**: Extract transaction details from receipt images
2. **YNAB Integration**: Create transactions in your YNAB budget
3. **Local Storage**: Save receipts for your records
4. **Sync Management**: Track synchronization status

### We DO NOT

- ❌ Sell your data to third parties
- ❌ Use your data for advertising
- ❌ Share your receipts with anyone except YNAB (at your direction)
- ❌ Store your data on our servers (all data is local or in your YNAB account)

## Data Storage and Security

### Local Storage

- **Location**: Private app directory on your Android device
- **Encryption**: Android's file-based encryption (Android 7.0+)
- **Access**: Only accessible by the app and device owner

### YNAB API

- **Connection**: Encrypted HTTPS (TLS 1.2+)
- **Authentication**: OAuth 2.0 with refresh tokens
- **Data sent**: Only transaction data you approve
- **YNAB's Privacy Policy**: [https://www.youneedabudget.com/privacy-policy/](https://www.youneedabudget.com/privacy-policy/)

### Security Measures

1. No server-side storage of receipt images
2. Secure token storage using Android Keystore
3. HTTPS for all network communication
4. Local data protected by device encryption
5. No cloud backup of sensitive data (unless explicitly enabled by user)

## Data Retention

### Receipt Images

- **Default**: Kept locally until you delete them
- **Optional**: Auto-delete after successful sync (configurable in settings)
- **Manual deletion**: You can delete receipts at any time

### Transaction Data

- **YNAB Account**: Persists according to YNAB's retention policy
- **Local Cache**: Cleared when you sign out or clear app cache

### Authentication Tokens

- Removed when you sign out
- Automatically refreshed as needed
- Cleared if app is uninstalled

## Third-Party Services

### YNAB (You Need A Budget)

- **Purpose**: Budget management and transaction tracking
- **Data shared**: Transaction details from receipts
- **Privacy policy**: [https://www.youneedabudget.com/privacy-policy/](https://www.youneedabudget.com/privacy-policy/)
- **Control**: You explicitly authorize each transaction

### No Other Third Parties

We do not integrate with any other third-party services, analytics platforms, or advertising networks.

## Your Rights

### Access and Control

You have the right to:

1. **Access**: View all your stored receipts and data
2. **Delete**: Remove receipts and clear cached data
3. **Export**: Receipts are accessible via Android file system
4. **Revoke**: Disconnect YNAB access at any time
5. **Opt-out**: Disable optional features like analytics

### How to Exercise Your Rights

- **Delete receipts**: Swipe to delete individual receipts
- **Clear cache**: Settings → Storage → Clear Cache
- **Sign out**: Settings → Account → Sign Out (removes tokens)
- **Uninstall**: Removes all local data

## Children's Privacy

YNAB Receipt Scanner is not intended for use by children under 13. We do not knowingly collect personal information from children under 13. If we discover we have collected information from a child under 13, we will delete it immediately.

## International Users

### GDPR (European Users)

If you are in the European Economic Area (EEA), you have additional rights under GDPR:

- **Right to access**: Request a copy of your data
- **Right to rectification**: Correct inaccurate data
- **Right to erasure**: Request deletion of your data
- **Right to data portability**: Receive your data in a structured format
- **Right to object**: Object to data processing

To exercise these rights, contact us at privacy@example.com.

### CCPA (California Users)

California residents have the right to:

- Know what personal information is collected
- Know if personal information is sold (we do not sell data)
- Opt-out of data sales (not applicable)
- Request deletion of personal information

## Changes to This Privacy Policy

We may update this Privacy Policy from time to time. Changes will be noted by:

1. Updating the "Last Updated" date at the top
2. Posting the new policy in the app
3. Notifying you via in-app message (for material changes)

Continued use of the app after changes constitutes acceptance of the updated policy.

## Open Source

YNAB Receipt Scanner is open source. You can review our code and data handling practices at:

**GitHub Repository**: [https://github.com/example/ynab-receipt-scanner](https://github.com/example/ynab-receipt-scanner)

## Contact Us

If you have questions or concerns about this Privacy Policy or our data practices:

**Email**: privacy@ynab-receipt-scanner.example.com  
**GitHub Issues**: [https://github.com/example/ynab-receipt-scanner/issues](https://github.com/example/ynab-receipt-scanner/issues)

## Consent

By using YNAB Receipt Scanner, you consent to:

1. Processing of receipt images for OCR
2. Transmission of transaction data to YNAB (when you submit)
3. Local storage of receipts and transaction data
4. Use of YNAB API according to their terms

You can withdraw consent at any time by signing out and uninstalling the app.

---

**Summary**: We take your privacy seriously. Your receipt data stays on your device or in your YNAB account. We don't collect, store, or share your information with anyone except YNAB (and only when you explicitly submit a transaction).
