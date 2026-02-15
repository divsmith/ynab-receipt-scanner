# Screenshots

This directory contains screenshots for the README and documentation.

## Required Screenshots

The following screenshots should be placed in this directory:

1. **home.png** - Home screen showing receipt list
2. **camera.png** - Camera capture screen
3. **review.png** - Receipt review and edit screen
4. **sync.png** - Sync status screen
5. **settings.png** - Settings screen
6. **auth.png** - OAuth authentication flow

## Screenshot Guidelines

### Device
- Use Pixel 6 or similar modern Android device
- Portrait orientation
- Clean status bar (or use Android Studio's screenshot feature to remove it)

### Settings
- Light mode for consistency (unless showing dark mode feature)
- Demo mode enabled (clean status bar):
  ```bash
  adb shell settings put global sysui_demo_allowed 1
  adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1200
  adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4
  adb shell am broadcast -a com.android.systemui.demo -e command battery -e plugged false -e level 100
  ```

### Content
- Use realistic but anonymized data
- Example merchant: "Coffee Shop", "Grocery Store", "Gas Station"
- Example amounts: $5.00, $23.45, $67.89
- Example dates: Recent dates (last 7 days)

### Format
- PNG format
- 1080x2400 resolution (or equivalent aspect ratio)
- File size < 500KB (use PNG compression)

### Taking Screenshots

#### Using Android Studio
1. Open Device Manager
2. Launch emulator or connect device
3. Run the app
4. Use screenshot tool (Camera icon) in Device Manager
5. Save to this directory

#### Using ADB
```bash
adb exec-out screencap -p > screenshot.png
```

#### Using Device
- Press Power + Volume Down
- Find in Photos app
- Transfer to computer

## Placeholder Images

Until real screenshots are available, the README shows placeholder text indicating "Screenshots coming soon."

To add screenshots:
1. Take screenshots following guidelines above
2. Name files according to the list above
3. Place in this directory
4. Screenshots will automatically appear in README
