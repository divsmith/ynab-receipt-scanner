# User Guide

## Table of Contents
- [Getting Started](#getting-started)
- [Setting Up Your Account](#setting-up-your-account)
- [Scanning Receipts](#scanning-receipts)
- [Reviewing and Editing](#reviewing-and-editing)
- [Selecting Accounts and Categories](#selecting-accounts-and-categories)
- [Understanding Sync Status](#understanding-sync-status)
- [Settings](#settings)
- [Troubleshooting](#troubleshooting)
- [Tips for Best Results](#tips-for-best-results)
- [FAQ](#faq)

## Getting Started

Welcome to YNAB Receipt Scanner! This guide will help you get started with scanning receipts and creating YNAB transactions automatically.

### What You Need

✅ **YNAB Account**: Active subscription to [YNAB](https://www.youneedabudget.com/)  
✅ **Android Device**: Android 7.0 (Nougat) or higher  
✅ **Camera**: Working camera for scanning receipts  
✅ **Internet**: For initial setup and syncing (offline mode available)

### First Launch

When you open the app for the first time:

1. **Welcome Screen**: Learn about key features
2. **Permissions**: Grant camera and storage permissions
3. **YNAB Login**: Connect your YNAB account
4. **Budget Selection**: Choose your default budget
5. **You're Ready!**: Start scanning receipts

## Setting Up Your Account

### OAuth Authentication (Recommended)

The app uses secure OAuth 2.0 to connect to YNAB - we never see or store your password!

**Steps:**

1. Tap **Connect to YNAB** on the onboarding screen
2. You'll be taken to YNAB's website in your browser
3. Log in to your YNAB account
4. Review requested permissions:
   - Read your budgets, accounts, and categories
   - Create transactions
5. Tap **Approve**
6. You'll be redirected back to the app
7. Done! The app can now sync with YNAB

**Security**: Your YNAB password is never shared with the app. The app receives a secure token that can be revoked anytime from your YNAB account settings.

### Selecting Your Budget

If you have multiple budgets:

1. After logging in, select your primary budget
2. You can change this later in **Settings > Budget**
3. The app will remember your choice

### Permissions Explained

**Camera** (Required):
- Needed to take photos of receipts
- Can be disabled anytime in device settings
- Without this, you can't scan receipts

**Storage** (Optional):
- Saves receipt photos locally
- Allows viewing receipts later
- Can be disabled (photos won't be saved)

**Internet** (Required for sync):
- Syncs transactions to YNAB
- Downloads accounts and categories
- Works offline with automatic sync later

## Scanning Receipts

### Taking a Photo

1. Tap the **Camera** tab or **Scan Receipt** button
2. Position your receipt in the frame:
   - Hold device parallel to receipt
   - Ensure good lighting
   - Fill frame with receipt
   - Avoid shadows and glare
3. Tap the **Capture** button
4. The app will process the image (2-5 seconds)
5. You'll be taken to the review screen

### Camera Tips

📸 **Good Photo = Accurate Results**

✅ **Do:**
- Use good lighting (natural light is best)
- Hold phone steady
- Keep receipt flat
- Fill the frame
- Ensure text is sharp and readable

❌ **Avoid:**
- Wrinkled or crumpled receipts
- Faded or thermal paper (take photo ASAP)
- Shadows covering text
- Blurry photos
- Extreme angles

### Supported Receipt Types

✅ **Works Best With:**
- Retail store receipts
- Restaurant receipts
- Grocery store receipts
- Gas station receipts
- Online order printouts

⚠️ **May Have Issues With:**
- Handwritten receipts
- Very small or large receipts
- Faded thermal paper
- Non-English text (currently)
- Receipts with poor print quality

## Reviewing and Editing

After scanning, you'll see the extracted information:

### Fields

**Merchant Name** 🏪
- The store or business name
- Tap to edit if incorrect
- The app learns from your corrections

**Amount** 💰
- Total amount paid
- Automatically formatted as currency
- Enter as positive number (app handles the sign)

**Date** 📅
- Transaction date
- Usually pulled from receipt
- Tap to change if needed
- Default: Today's date

**Memo** 📝 (Optional)
- Additional notes
- Pre-filled with "Receipt: [Merchant]"
- Add details about the purchase

### Editing Fields

1. Tap any field to edit
2. Make your changes
3. Tap **Done** or **Next**
4. Changes are saved automatically

**Keyboard Shortcuts:**
- **Tab**: Move to next field
- **Done**: Close keyboard
- **Next**: Move to next field

### Field Validation

The app checks your entries:

✅ **Valid**: Green checkmark appears  
⚠️ **Warning**: Yellow indicator with suggestion  
❌ **Error**: Red indicator - must fix before continuing

**Common Validations:**
- Amount must be positive
- Date must be valid format
- Merchant name required

## Selecting Accounts and Categories

### Choosing an Account

1. Tap the **Account** field
2. Browse your YNAB accounts:
   - **On Budget**: Checking, savings, cash
   - **Off Budget**: Tracking accounts
   - **Credit Cards**: Credit accounts
3. Tap to select
4. Most recent selection is remembered

**Account Types:**
- 🏦 **Checking**: Standard checking account
- 💳 **Credit Card**: Credit card account
- 💰 **Savings**: Savings account
- 📊 **Tracking**: Off-budget account

### Choosing a Category

1. Tap the **Category** field
2. Browse category groups:
   - Immediate Obligations
   - True Expenses
   - Quality of Life Goals
   - (Your custom groups)
3. Tap a category to select
4. Or leave blank for "Uncategorized"

**Quick Tips:**
- Most recent categories appear at top
- Search for categories with the search bar
- Categories are grouped as in YNAB

### Create Transaction

Once you've reviewed and selected:

1. Tap **Create Transaction**
2. The app will:
   - Validate all fields
   - Send to YNAB (if online)
   - Or queue for later (if offline)
3. You'll see a success message
4. Receipt is saved to your history

## Understanding Sync Status

Each transaction has a sync status badge:

### Status Indicators

✅ **Synced** (Green)
- Successfully sent to YNAB
- Visible in your YNAB budget
- No action needed

⏳ **Pending** (Yellow)
- Waiting to sync
- Saved locally
- Will sync when online

❌ **Failed** (Red)
- Sync error occurred
- Tap to retry
- Check error details

🔄 **Syncing** (Blue, animated)
- Currently uploading
- Please wait
- Usually takes 1-2 seconds

### Manual Retry

If a transaction fails to sync:

1. Tap the failed transaction
2. Tap **Retry Sync**
3. Or wait - the app auto-retries every 15 minutes
4. Check your internet connection

### Sync Settings

Configure automatic sync:

1. Go to **Settings > Sync**
2. Options:
   - **Auto-sync**: On/Off
   - **Sync frequency**: 15/30/60 minutes
   - **WiFi only**: Sync only on WiFi
   - **Retry failed**: Automatically retry failed syncs

## Settings

Access settings via the **Settings** tab.

### Account Settings

**Budget**
- Change active budget
- Refresh budgets from YNAB

**Sign Out**
- Disconnect from YNAB
- Clears synced data
- Doesn't delete receipts

### App Settings

**Default Account**
- Set default account for new transactions
- Saves time when scanning

**Default Category**
- Optional default category
- Can be overridden per transaction

**Camera**
- Flash mode: Auto/On/Off
- Resolution: Standard/High
- Grid overlay: On/Off

**Receipts**
- Save photos: On/Off
- Photo quality: Low/Medium/High
- Auto-delete after sync: On/Off

### Sync Settings

**Sync Options**
- Auto-sync: Enable/Disable
- Sync interval: 15/30/60 minutes
- WiFi only: On/Off
- Background sync: On/Off

**Data Usage**
- Image sync: WiFi only/Always
- Compress images: On/Off
- Download categories: On demand/Automatically

### Privacy & Security

**Data**
- View stored data
- Clear cache
- Export receipts (coming soon)
- Delete all data

**Security**
- Require authentication on app open (coming soon)
- Biometric unlock (coming soon)

### About

**App Information**
- Version number
- Build information
- Open source licenses

**Support**
- Report a bug
- Request feature
- Contact support
- Privacy policy
- Terms of service

## Troubleshooting

### OCR Not Working

**Problem**: Receipt text not extracted correctly

**Solutions**:
1. Retake photo with better lighting
2. Ensure receipt is flat and in focus
3. Try cleaning camera lens
4. Check if receipt text is readable
5. Manually enter fields if needed

### Can't Connect to YNAB

**Problem**: OAuth login fails or times out

**Solutions**:
1. Check internet connection
2. Ensure YNAB is accessible (try browser)
3. Try again in a few minutes
4. Sign out and sign in again
5. Check if YNAB API is operational

### Sync Failures

**Problem**: Transactions stuck in "Failed" status

**Solutions**:
1. Check internet connection
2. Verify YNAB account is active
3. Check if account/category still exists
4. Tap **Retry** on failed transaction
5. Sign out and sign in to refresh token

### App Crashes

**Problem**: App closes unexpectedly

**Solutions**:
1. Update to latest version
2. Clear app cache (Settings > Data > Clear cache)
3. Restart device
4. Reinstall app (data is preserved)
5. Report crash with details

### Poor OCR Accuracy

**Problem**: Extracted fields are often wrong

**Solutions**:
1. Improve photo quality (see Camera Tips)
2. Manually correct and app learns
3. Update app (improvements constantly added)
4. Report receipts with issues (helps us improve)

## Tips for Best Results

### Photography Tips

1. **Lighting**: Natural daylight is best
2. **Angle**: Hold phone parallel to receipt
3. **Distance**: Fill frame but don't crop edges
4. **Steadiness**: Hold phone still or use surface
5. **Timing**: Scan receipts fresh (thermal fades!)

### Efficiency Tips

1. **Batches**: Scan multiple receipts in one session
2. **Defaults**: Set default account/category
3. **Categories**: Use recently-used list
4. **Review Later**: Quick scan now, review later if unsure

### Accuracy Tips

1. **Double Check**: Always review extracted data
2. **Corrections**: The app learns from your edits
3. **Memo**: Add notes for future reference
4. **Consistent**: Use same categories as in YNAB

### Organization Tips

1. **Regular Scanning**: Scan receipts same day
2. **Digital Backup**: Enable "Save photos"
3. **Periodic Cleanup**: Delete old synced receipts
4. **Categories**: Keep YNAB categories organized

## FAQ

### Is my data secure?

Yes! The app uses:
- OAuth 2.0 (never stores your password)
- Encrypted storage for tokens
- HTTPS for all communication
- No tracking or analytics
- Open source code (auditable)

### Does it work offline?

Yes! You can:
- Scan receipts offline
- Review and edit fields
- Create transactions (queued)
- Auto-sync when back online

### How accurate is the OCR?

Typical accuracy:
- Merchant name: 85-95%
- Amount: 90-95%
- Date: 80-90%

Varies based on receipt quality and lighting.

### Can I edit transactions after creating?

Not in the app currently. Edit in YNAB:
1. Open YNAB app/website
2. Find the transaction
3. Edit as needed
4. Changes sync back

### What happens to failed syncs?

Failed transactions:
- Stay in local database
- Auto-retry every 15 minutes
- Manual retry available
- Not lost if you close app

### Can I use multiple YNAB budgets?

Yes! Switch budgets in Settings.
Each budget can have different:
- Accounts
- Categories
- Default settings

### Does it work with shared budgets?

Yes! As long as you have:
- Access to the budget
- Permission to create transactions

### How much storage does it use?

Typical usage:
- App: ~50 MB
- Each receipt photo: 100-500 KB
- 100 receipts = ~30 MB

Configure to delete photos after sync to save space.

---

## Need More Help?

- **Email Support**: support@example.com
- **GitHub Issues**: Report bugs or request features
- **Documentation**: Check `/docs` folder
- **YNAB Help**: https://support.youneedabudget.com/

**Enjoying the app? Leave a rating on the Play Store! ⭐**
