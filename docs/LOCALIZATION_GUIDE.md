# Localization Guide

This guide explains how to add new languages and maintain translations for the YNAB Receipt Scanner app.

## Adding a New Language

### 1. Create Language Directory

Create a new values directory with the language code:

```bash
mkdir app/src/main/res/values-[lang]
```

Common language codes:
- `values-es` - Spanish
- `values-fr` - French
- `values-de` - German
- `values-pt` - Portuguese (Brazil: `values-pt-rBR`)
- `values-ja` - Japanese
- `values-zh` - Chinese (Simplified: `values-zh-rCN`, Traditional: `values-zh-rTW`)

### 2. Copy and Translate String Files

Copy these files from `values/` to your new `values-[lang]/` directory:

1. **strings.xml** - All user-facing text
2. **content_descriptions.xml** - Accessibility descriptions

Translate all string values while keeping the string names unchanged:

```xml
<!-- English (values/strings.xml) -->
<string name="scan_receipt">Scan Receipt</string>

<!-- Spanish (values-es/strings.xml) -->
<string name="scan_receipt">Escanear Recibo</string>
```

### 3. Do NOT Translate

- String resource names (the `name` attribute)
- Format specifiers like `%s`, `%d`, `%1$s`
- HTML tags like `<b>`, `<i>`
- YNAB brand name

### 4. String Naming Conventions

Follow these conventions when adding new strings:

- **Actions**: `action_[name]` - e.g., `action_submit`
- **Titles**: `title_[screen]` - e.g., `title_settings`
- **Messages**: `message_[context]` - e.g., `message_network_error`
- **Labels**: `label_[field]` - e.g., `label_payee`
- **Content Descriptions**: `cd_[element]` - e.g., `cd_scan_button`
- **Preferences**: `pref_[name]` - e.g., `pref_auto_sync`

### 5. Handle Plurals

For languages with different plural rules, use plural resources:

```xml
<plurals name="receipts_count">
    <item quantity="one">%d receipt</item>
    <item quantity="other">%d receipts</item>
</plurals>
```

Spanish example:
```xml
<plurals name="receipts_count">
    <item quantity="one">%d recibo</item>
    <item quantity="other">%d recibos</item>
</plurals>
```

### 6. Testing Translations

#### On Device
1. Open device **Settings** → **System** → **Languages**
2. Add your target language
3. Move it to the top of the list
4. Open the app

#### In Android Studio
1. Run the app
2. In the device toolbar, click the language dropdown
3. Select your language

#### Automated Testing
Run string validation:
```bash
./gradlew validateStrings
```

## Financial Terminology Guidelines

### Use Formal Language
- Use formal "usted" form in Spanish, not informal "tú"
- Professional tone for financial context

### Key Financial Terms

| English | Spanish | Notes |
|---------|---------|-------|
| Payee | Beneficiario | Not "pagador" |
| Amount | Importe | Can also use "cantidad" or "monto" |
| Transaction | Transacción | Standard banking term |
| Account | Cuenta | Banking context |
| Category | Categoría | Budget category |
| Receipt | Recibo | Proof of purchase |
| Sync | Sincronización | Technical term, widely understood |
| Budget | Presupuesto | Financial planning |

## Right-to-Left (RTL) Support

For RTL languages (Arabic, Hebrew), the app already has RTL support enabled:

```xml
<!-- In AndroidManifest.xml -->
android:supportsRtl="true"
```

### RTL Layout Testing
1. Enable RTL in Developer Options:
   - Settings → Developer Options → Force RTL layout direction
2. Test all screens for proper mirroring
3. Icons should mirror except for:
   - Brand logos
   - Media controls (play/pause)
   - Checkmarks

### RTL-Specific Resources

For layouts that need RTL variants:
```bash
# Create layout-ldrtl directory
mkdir app/src/main/res/layout-ldrtl/

# Copy and mirror layouts
cp app/src/main/res/layout/fragment_home.xml \
   app/src/main/res/layout-ldrtl/fragment_home.xml
```

Use start/end instead of left/right:
```xml
<!-- Good -->
android:paddingStart="16dp"
android:paddingEnd="16dp"

<!-- Avoid -->
android:paddingLeft="16dp"
android:paddingRight="16dp"
```

## String Quality Checklist

Before submitting translations:

- [ ] All strings translated (no English fallbacks)
- [ ] Format specifiers preserved (`%s`, `%d`)
- [ ] String length appropriate (not too long for UI)
- [ ] Formal tone for financial context
- [ ] No hardcoded strings in layouts or code
- [ ] Tested on actual device
- [ ] Accessibility descriptions translated
- [ ] Proper capitalization for language
- [ ] Special characters escaped (`'` → `\'`)

## Contributing Translations

1. Fork the repository
2. Create language directory and translate strings
3. Test thoroughly on device
4. Submit pull request with:
   - Language code and name
   - Native speaker verification
   - Screenshots of translated UI

## Translation Management (Future)

For managing translations at scale, consider:

- **Lokalise** - Translation management platform
- **Crowdin** - Community translation tool
- **Google Play Console** - Automatic translation (rough)

## Resources

- [Android Localization Guide](https://developer.android.com/guide/topics/resources/localization)
- [Language Codes (ISO 639-1)](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes)
- [Material Design Localization](https://m3.material.io/foundations/content-design/localization)

## Support

For translation questions or issues:
- Open an issue on GitHub
- Email: support@ynab-receipt-scanner.example.com
- Include language code and specific strings
