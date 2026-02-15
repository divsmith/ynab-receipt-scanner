# Play Store Screenshots Guide

## Requirements

### Phone Screenshots (Required)
- **Dimensions**: 1080 x 1920 pixels (9:16 aspect ratio)
- **Format**: PNG or JPEG (PNG recommended for quality)
- **Minimum**: 2 screenshots
- **Maximum**: 8 screenshots
- **Recommended**: 4-6 screenshots
- **File size**: Max 8 MB each

### 7-inch Tablet Screenshots (Optional but Recommended)
- **Dimensions**: 1200 x 1920 pixels
- **Format**: PNG or JPEG
- **Note**: Shows app optimized for tablets

### 10-inch Tablet Screenshots (Optional)
- **Dimensions**: 1600 x 2560 pixels
- **Format**: PNG or JPEG

## Screenshot Strategy

### 1. Hero Shot (First Screenshot)
**Purpose**: Showcase the main feature—scanning a receipt  
**Screen**: Camera view with receipt in frame  
**Caption**: "Scan receipts instantly"  
**Elements**:
- Receipt clearly visible in camera viewfinder
- Camera UI with capture button
- Clean, professional composition

### 2. OCR Results (Second Screenshot)
**Purpose**: Show accuracy and time-saving benefit  
**Screen**: Review screen with extracted data  
**Caption**: "Smart OCR extracts details automatically"  
**Elements**:
- Filled-in fields (payee, amount, date)
- Receipt image thumbnail
- Account and category selectors
- "Submit to YNAB" button prominent

### 3. Receipt List (Third Screenshot)
**Purpose**: Demonstrate organization and history  
**Screen**: Home screen with multiple receipts  
**Caption**: "Keep all your receipts organized"  
**Elements**:
- List of receipts with different statuses
- Sync status badges visible
- Clean, organized layout
- Search/filter options visible

### 4. YNAB Integration (Fourth Screenshot)
**Purpose**: Highlight seamless YNAB connection  
**Screen**: Settings or success dialog  
**Caption**: "Syncs directly to your YNAB budget"  
**Elements**:
- YNAB branding (if permitted)
- Connected account info
- Success checkmark or sync indicator
- Professional, trustworthy appearance

### 5. Dark Mode (Optional Fifth Screenshot)
**Purpose**: Show modern design and dark mode support  
**Screen**: Any main screen in dark theme  
**Caption**: "Beautiful in light or dark mode"  
**Elements**:
- Attractive dark theme
- Good contrast
- Modern Material Design

### 6. Offline Mode (Optional Sixth Screenshot)
**Purpose**: Highlight offline capability  
**Screen**: Receipt list with "Pending" status  
**Caption**: "Works offline—sync when you're ready"  
**Elements**:
- Multiple receipts with "Pending" status
- Easy-to-understand sync indicators
- Reassuring message about offline functionality

## Creating Screenshots

### Method 1: Android Studio Emulator
1. Run app in Android Studio
2. Use Pixel 5 or similar device (1080x1920)
3. Navigate to key screens
4. Click camera icon in emulator toolbar
5. Screenshots saved to desktop

### Method 2: Real Device via ADB
```bash
# Take screenshot
adb exec-out screencap -p > screenshot.png

# Or use Android Studio Device Manager
# Device File Explorer → Screenshot button
```

### Method 3: Manual Cropping
If screenshots are different dimensions:
```bash
# Resize with ImageMagick
convert input.png -resize 1080x1920 -background white -gravity center -extent 1080x1920 output.png
```

## Screenshot Template

For professional mockups with device frames:

### Tools
- **Figma** + DaVinci Device Mockups plugin
- **Photoshop** + Smart Objects
- **shots.so** - Online screenshot tool
- **Placeit** by Envato

### Template Layout
```
┌─────────────────────────┐
│   Device Frame (optional)│
│  ┌───────────────────┐  │
│  │                   │  │
│  │   App Screenshot  │  │
│  │                   │  │
│  └───────────────────┘  │
│                         │
│ "Caption Text Here"     │
└─────────────────────────┘
```

## Screenshot Checklist

- [ ] All text is legible at full size
- [ ] No personal or sensitive information visible
- [ ] Sample data looks realistic but fake
- [ ] Branding colors match app theme (YNAB blue)
- [ ] Status bar is clean (full battery, good signal)
- [ ] Time shown is 10:00 or 2:00 (common convention)
- [ ] No visible errors or placeholder text
- [ ] Dark mode screenshots have good contrast
- [ ] Demonstrates clear user benefit
- [ ] Highlights unique features

## Sample Data Guidelines

Use realistic but fake data:

### Receipt Examples
- ✅ "Whole Foods Market" - $42.67
- ✅ "Shell Gas Station" - $55.00
- ✅ "Starbucks Coffee" - $8.25
- ❌ "ACME Corp" - $999,999.99 (unrealistic)
- ❌ "Test Store" - $0.00 (obvious placeholder)

### Dates
- Use recent but not today's date
- Vary dates to show history
- ✅ "2 days ago", "Mar 14", "Feb 28"

### Accounts
- ✅ "Checking", "Credit Card", "Savings"
- ❌ "John's Account 1234" (too specific/personal)

## Feature Graphic (Optional)

**Dimensions**: 1024 x 500 pixels  
**Purpose**: Displayed at top of Play Store listing  
**Design**:
```
┌─────────────────────────────────┐
│  📸  YNAB Receipt Scanner       │
│                                 │
│  Scan → Review → Budget         │
│                                 │
│  [App Icon]  [Receipt Mockup]   │
└─────────────────────────────────┘
```

**Elements**:
- App name and icon
- Key visual (receipt + phone mockup)
- Brief tagline
- YNAB brand colors
- High contrast for visibility

## Promo Video (Optional)

**Length**: 30 seconds - 2 minutes (30-60 seconds ideal)  
**Aspect Ratio**: 16:9 landscape or 9:16 portrait  
**Maximum Size**: 100 MB

### Video Structure
1. **Hook (5s)**: Problem statement  
   "Tired of typing receipts into your budget?"

2. **Solution (10s)**: Show scanning a receipt  
   "YNAB Receipt Scanner uses AI to read receipts instantly."

3. **Features (15s)**: Quick demo  
   - Scan receipt
   - Review extracted data
   - Submit to YNAB
   
4. **Call to Action (5s)**: Download prompt  
   "Download now and start scanning!"

### Video Tools
- **ScreenFlow** (Mac) - Screen recording
- **OBS Studio** (Free) - Screen capture
- **Camtasia** - Video editing
- **After Effects** - Motion graphics

## Screenshot Order

Recommended sequence:
1. 📸 **Camera/Scan** - First impression
2. ✏️ **Review/Edit** - Show intelligence
3. 📱 **Receipt List** - Organization
4. ✅ **YNAB Sync** - Integration benefit
5. 🌙 **Dark Mode** - Design quality
6. ⚙️ **Settings** - Customization

## Localization

Create separate screenshots for each language:
- **English**: Required
- **Spanish**: Recommended
- **Other languages**: As needed

Store in:
```
docs/play_store/screenshots/
├── en-US/
│   ├── 01-scan.png
│   ├── 02-review.png
│   └── ...
├── es-ES/
│   ├── 01-escanear.png
│   ├── 02-revisar.png
│   └── ...
```

## Resources

- [Google Play Screenshot Guidelines](https://support.google.com/googleplay/android-developer/answer/9866151)
- [Material Design Screenshot Best Practices](https://m3.material.io/foundations/content-design/app-store-listing)
- [ASO Screenshot Optimization Guide](https://splitmetrics.com/blog/app-store-screenshot-guidelines/)

## Notes

- Update screenshots with each major version
- A/B test different orderings to maximize conversions
- Monitor Play Store analytics to see which screenshots drive installs
- Keep source files (PSD, Figma) for easy updates
