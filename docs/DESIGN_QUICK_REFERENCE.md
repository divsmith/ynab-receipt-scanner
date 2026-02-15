# YNAB Receipt Scanner - Design Quick Reference

## 🎨 Color Palette

### Primary Colors
```xml
<color name="primary">#005A8C</color>          <!-- YNAB Blue -->
<color name="primary_dark">#003D5C</color>     <!-- Deep Ocean -->
<color name="primary_light">#4A8BB5</color>    <!-- Sky Blue -->
```

### Secondary Colors
```xml
<color name="secondary">#F39C12</color>        <!-- Sunset Orange -->
<color name="secondary_dark">#D68910</color>
<color name="secondary_light">#F5B041</color>
```

### Status Colors
```xml
<color name="success">#27AE60</color>          <!-- Money Green -->
<color name="error">#E74C3C</color>            <!-- Alert Red -->
<color name="warning">#F39C12</color>          <!-- Caution Amber -->
<color name="info">#3498DB</color>             <!-- Info Blue -->
```

### Neutral Colors
```xml
<color name="white">#FFFFFF</color>
<color name="black">#000000</color>
<color name="background">#F5F5F5</color>
<color name="surface">#FFFFFF</color>
<color name="text_primary">#212121</color>
<color name="text_secondary">#757575</color>
<color name="text_hint">#BDBDBD</color>
```

---

## 📏 Spacing Scale

```kotlin
4dp   (xs)  - Tight spacing, icon padding
8dp   (sm)  - Component internal spacing
16dp  (md)  - Standard margin/padding (most common)
24dp  (lg)  - Section spacing
32dp  (xl)  - Screen margins (top/bottom)
48dp  (xxl) - Large sections, hero spacing
```

**Touch targets**: Always 48dp minimum (accessibility)

---

## 🔤 Typography

### Material Design 3 Type Scale

```kotlin
Display Large:   57sp / 64sp line height
Display Medium:  45sp / 52sp
Display Small:   36sp / 44sp

Headline Large:  32sp / 40sp
Headline Medium: 28sp / 36sp
Headline Small:  24sp / 32sp  ← Screen titles

Title Large:     22sp / 28sp
Title Medium:    16sp / 24sp  ← Card headers
Title Small:     14sp / 20sp

Body Large:      16sp / 24sp  ← Main content
Body Medium:     14sp / 20sp  ← Secondary content
Body Small:      12sp / 16sp  ← Captions

Label Large:     14sp / 20sp  ← Buttons
Label Medium:    12sp / 16sp  ← Chips, badges
Label Small:     11sp / 16sp
```

**Font**: Roboto (system default)  
**Weights**: Regular (400), Medium (500), Bold (700)

---

## 🎭 Icons

### Sizes
```kotlin
12dp - Extra small (inline)
16dp - Small (list icons)
24dp - Medium (toolbar, buttons) ← Standard
40dp - Large (feature icons)
80dp - Extra large (empty states)
120dp - Illustrations
```

### Key Icons
- `ic_home` - Home navigation
- `ic_camera` - Scan action
- `ic_receipt` - Receipt representation
- `ic_sync` - Synchronization
- `ic_check` - Success/complete
- `ic_delete` - Destructive action
- `ic_edit` - Modification
- `ic_calendar` - Date selection
- `ic_account` - Account type
- `ic_category` - Category selection

---

## 🧩 Components

### Cards
```kotlin
Corner radius: 12dp
Elevation: 2dp
Padding: 16dp
Margin: 16dp horizontal, 8dp vertical
```

### Buttons
```kotlin
Height: 48dp
Corner radius: 8dp
Padding: 24dp horizontal

Primary: Background = primary, Text = on primary
Text: No background, Text = primary
Icon: 48×48dp container, 24dp icon
```

### Input Fields
```kotlin
Height: 56dp
Corner radius: 8dp
Stroke: 1dp (2dp focused)
Style: Outlined with floating label
```

### Status Badges
```kotlin
Corner radius: 12dp (pill shape)
Padding: 8dp horizontal, 4dp vertical
Font: labelSmall (11sp, Medium)

Pending: #F39C12
Syncing: #3498DB
Synced: #27AE60
Error: #E74C3C
```

---

## 🎬 Animations

### Durations
```kotlin
Fast:   150-200ms  - Micro-interactions
Medium: 300ms      - Standard transitions (most common)
Slow:   500ms      - Complex animations
Loop:   1000ms+    - Pulse, shimmer effects
```

### Easing
```kotlin
Decelerate - Enter transitions
Accelerate - Exit transitions
Standard - Most animations (AccelerateDecelerate)
Overshoot - Success feedback
```

### Common Animations
```kotlin
Fade: 300ms decelerate
Scale: 200ms overshoot
Shake: 500ms (error feedback)
Pulse: 1000ms loop (sync indicators)
Shimmer: 1500ms loop (loading)
```

---

## ♿ Accessibility

### Touch Targets
**Minimum**: 48dp × 48dp (always!)

### Color Contrast (WCAG AA)
```kotlin
Normal text (14-18sp): 4.5:1 minimum
Large text (18sp+):    3.0:1 minimum
Icons, graphics:       3.0:1 minimum

✅ Primary on white:   7.3:1 (AAA)
✅ Text on white:      16.1:1 (AAA)
✅ Secondary on white: 4.6:1 (AA)
```

### Content Descriptions
All images, icons, buttons need `android:contentDescription` referencing:
- `@string/cd_[element_name]` (English)
- Translated in `values-es/content_descriptions.xml` (Spanish)

---

## 🌍 Localization

### Supported Languages
- **en** (English) - Default
- **es** (Spanish) - Complete

### String Naming Convention
```kotlin
[context]_[description]

Examples:
action_submit
title_settings
message_network_error
label_payee
cd_scan_button (content descriptions)
pref_auto_sync (settings)
```

### RTL Support
```xml
<!-- Always use start/end -->
android:paddingStart="16dp"  ✅
android:paddingLeft="16dp"   ❌

<!-- RTL enabled -->
android:supportsRtl="true"
```

---

## 📝 Code Examples

### Using Animations
```kotlin
import com.ynab.receiptscanner.ui.common.AnimationUtils

// Fade in a view
view.fadeIn()

// Success animation
successIcon.animateSuccess {
    // Optional callback
}

// Error shake
errorView.shake()

// Pulse sync indicator
syncIcon.startPulseAnimation()
// Later: syncIcon.stopPulseAnimation()
```

### Access String Resources
```kotlin
// In Activity/Fragment
val title = getString(R.string.app_name)
val formatted = getString(R.string.amount_format, 42.99)

// Content description
imageView.contentDescription = getString(R.string.cd_receipt_thumbnail)
```

### Applying Spacing
```xml
<!-- Use dimen resources -->
android:padding="@dimen/spacing_md"
android:layout_margin="@dimen/spacing_lg"
android:layout_marginTop="@dimen/spacing_xl"
```

---

## 🏗️ Build & Test Commands

### Build
```bash
# Clean build
./gradlew clean build

# Release build (signed)
./gradlew assembleRelease

# Generate bundle for Play Store
./gradlew bundleRelease
```

### Test
```bash
# Unit tests
./gradlew test

# Instrumentation tests
./gradlew connectedAndroidTest

# Specific test
./gradlew test --tests=ReceiptViewModelTest
```

### Run
```bash
# Install and run
./gradlew installDebug
adb shell am start -n com.ynab.receiptscanner/.MainActivity

# Uninstall
./gradlew uninstallAll
```

---

## 🔍 Testing Checklist

### Visual Testing
```bash
# Test with different languages
adb shell "setprop persist.sys.locale es-ES; setprop ctl.restart zygote"

# Test with large text
adb shell settings put system font_scale 1.5

# Test dark mode
adb shell "cmd uimode night yes"
```

### Accessibility Testing
```bash
# Enable TalkBack
adb shell settings put secure enabled_accessibility_services \
  com.android.talkback/.TalkBackService

# Disable TalkBack
adb shell settings put secure enabled_accessibility_services ""
```

### Screenshot Capture
```bash
# Via ADB
adb exec-out screencap -p > screenshot.png

# Or use Android Studio Device Manager → Screenshot button
```

---

## 📦 Key Files

### Resources
```
values/
  ├── colors.xml            - Color palette
  ├── dimens.xml            - Spacing scale
  ├── strings.xml           - English strings
  ├── content_descriptions.xml - Accessibility
  └── themes.xml            - App theme + splash

values-es/
  ├── strings.xml           - Spanish strings
  └── content_descriptions.xml - Spanish a11y

drawable/
  ├── ic_launcher_foreground.xml - App icon
  ├── ic_launcher_background.xml
  ├── illustration_*.xml    - Empty states
  └── ic_*.xml              - UI icons

anim/
  ├── fade_in.xml
  ├── fade_out.xml
  ├── scale_in.xml
  └── shake.xml
```

### Code
```
ui/
  ├── common/AnimationUtils.kt     - Animation helpers
  ├── home/HomeFragment.kt         - Receipt list
  ├── camera/CameraFragment.kt     - Scanning
  ├── review/ReviewFragment.kt     - Edit receipt
  ├── settings/SettingsFragment.kt - Settings
  └── about/AboutFragment.kt       - About screen
```

---

## 🎯 Common Tasks

### Add New String
1. Add to `values/strings.xml`
2. Add Spanish translation to `values-es/strings.xml`
3. Reference in code: `getString(R.string.my_string)`

### Add Content Description
1. Add to `values/content_descriptions.xml`
2. Add Spanish to `values-es/content_descriptions.xml`
3. Use in layout: `android:contentDescription="@string/cd_my_element"`

### Add New Color
1. Add to `values/colors.xml`
2. Use in layout: `android:textColor="@color/my_color"`
3. Use in code: `ContextCompat.getColor(context, R.color.my_color)`

### Apply Animation
1. Import: `import com.ynab.receiptscanner.ui.common.AnimationUtils`
2. Use: `view.fadeIn()` or `view.startPulseAnimation()`

---

## 📚 Documentation

Full documentation in `docs/`:

- **[BRANDING_GUIDE.md](../docs/BRANDING_GUIDE.md)** - Complete design system
- **[LOCALIZATION_GUIDE.md](../docs/LOCALIZATION_GUIDE.md)** - Translation guide
- **[PRIVACY_POLICY.md](../docs/PRIVACY_POLICY.md)** - Privacy policy
- **[play_store/](../docs/play_store/)** - Store assets

---

## 🚨 Remember

- ✅ Always use dimen resources for spacing
- ✅ All touch targets must be 48dp minimum
- ✅ Every image needs contentDescription
- ✅ Never hardcode strings in layouts
- ✅ Test with TalkBack enabled
- ✅ Test with Spanish language
- ✅ Test with large font (1.5x-2.0x)
- ✅ Test in dark mode

---

**Quick Reference v1.0** | Updated: Feb 14, 2026
