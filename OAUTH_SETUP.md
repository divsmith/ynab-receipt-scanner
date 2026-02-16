# YNAB OAuth Configuration Guide

## Overview
This app uses YNAB's OAuth 2.0 API for secure authentication. You must configure OAuth credentials before the app will function.

## Setup Instructions

### 1. Register Your Application with YNAB

1. Go to [YNAB Developer Settings](https://app.ynab.com/settings/developer)
2. Click **New Application**
3. Fill in the application details:
   - **Name**: `Receipt Scanner` (or your preferred name)
   - **Redirect URI(s)**: `receiptscanner://oauth/callback`
   - **Description**: (optional) "Mobile app for scanning receipts and creating YNAB transactions"
4. Click **Save**
5. Copy your **Client ID** and **Client Secret**

### 2. Configure Local Credentials

**Option A: Using gradle.properties (Recommended)**

1. Copy the example properties file:
   ```bash
   cp gradle.properties.example gradle.properties
   ```

2. Edit `gradle.properties` and add your credentials:
   ```properties
   ynab.client.id=your_actual_client_id_from_ynab
   ynab.client.secret=your_actual_client_secret_from_ynab
   ```

3. Verify `gradle.properties` is in `.gitignore` (it should be by default)

**Option B: Using Environment Variables**

Set environment variables before building:
```bash
export ynab.client.id="your_actual_client_id"
export ynab.client.secret="your_actual_client_secret"
./gradlew assembleDebug
```

### 3. Build the Application

```bash
# Clean previous builds
./gradlew clean

# Build debug version
./gradlew assembleDebug

# Or build and install to connected device
./gradlew installDebug
```

## Security Notes

⚠️ **CRITICAL SECURITY REQUIREMENTS:**

1. **Never commit credentials to version control**
   - `gradle.properties` should be in `.gitignore`
   - Check before committing: `git status` should not show gradle.properties

2. **Rotate secrets if exposed**
   - If credentials are accidentally committed, immediately:
     - Delete the application from YNAB Developer Settings
     - Create a new application with new credentials
     - Update your local gradle.properties

3. **Protect your local development environment**
   - Keep gradle.properties permissions restricted
   - Don't share screenshots that include credentials

## OAuth Flow Details

The app implements the OAuth 2.0 Authorization Code flow:

1. **Authorization Request**: User clicks "Connect YNAB" → Opens YNAB in Chrome Custom Tab
2. **User Authorization**: User grants permission to the app
3. **Callback**: YNAB redirects to `receiptscanner://oauth/callback?code=...`
4. **Token Exchange**: App exchanges authorization code for access token
5. **Secure Storage**: Tokens stored in Android Keystore (hardware-backed encryption)

## Redirect URI Configuration

The redirect URI **must** match exactly what's configured in YNAB:
- **YNAB Developer Settings**: `receiptscanner://oauth/callback`
- **AndroidManifest.xml**: Automatically configured with intent-filter
- **BuildConfig**: `YNAB_REDIRECT_URI = "receiptscanner://oauth/callback"`

## Troubleshooting

### Build fails with "YNAB_CLIENT_ID is empty"
- Ensure gradle.properties exists and has valid credentials
- Check for typos in property names (must be `ynab.client.id` and `ynab.client.secret`)
- Try: `./gradlew clean && ./gradlew assembleDebug`

### OAuth callback not working
- Verify redirect URI in YNAB Developer Settings matches: `receiptscanner://oauth/callback`
- Check AndroidManifest.xml has the correct intent-filter
- Enable debug logging and check Logcat for errors

### "Invalid client" error
- Client ID or Client Secret is incorrect
- Double-check credentials in YNAB Developer Settings
- Ensure no extra spaces or quotes in gradle.properties

### Token expires or gets revoked
- User can revoke access from YNAB → Settings → Authorized Applications
- App will automatically prompt for re-authentication
- Access tokens expire after specified duration (check YNAB docs)

## Testing OAuth Without Real Credentials

For unit testing, the app uses mock repositories that don't require real credentials. See:
- `YnabAuthRepositoryImplTest` - Tests with mock API responses
- `YnabAuthViewModelTest` - Tests with fake repository

## Additional Resources

- [YNAB API Documentation](https://api.ynab.com/)
- [YNAB Developer Forum](https://support.ynab.com/t/api-documentation)
- [OAuth 2.0 Specification](https://oauth.net/2/)

---

**Questions?** Check the [README.md](README.md) or review the code in:
- `YnabAuthActivity.kt` - OAuth flow implementation
- `YnabAuthService.kt` - API endpoints
- `KeystoreManager.kt` - Token storage
