# YNAB Receipt Scanner - Branding & Design Guide# YNAB Receipt Scanner - Branding & Design Guide







































































































































































































































































































































































































































































































































**Last Updated**: February 14, 2026**Maintained by**: YNAB Receipt Scanner Design Team  ---- **v1.0.0** (Feb 2026) - Initial design system established## Version History---- [WCAG Accessibility](https://www.w3.org/WAI/WCAG21/quickref/)- [YNAB Brand Guidelines](https://www.youneedabudget.com/press/)- [Android Design Guidelines](https://developer.android.com/design)- [Material Design 3](https://m3.material.io/)### Reference Links- **Vector Asset Studio**: Icon creation (Android Studio)- **Material Theme Builder**: Color scheme generation- **Figma**: UI design and prototyping### Design Tools## Resources---- **Efficient**: Battery-conscious background sync- **Responsive**: No janky scrolling- **Smooth animations**: 60 FPS target- **Fast load**: Optimize images, lazy load### Performance- **Errors**: Specific messages with recovery actions- **Success**: Clear confirmation (visual + text)- **Progress**: Loading indicators for async actions- **Immediate**: Button press states### Feedback- **Whitespace**: Use generously for breathing room- **Progressive disclosure**: Show details on demand- **Chunking**: Group related information- **Comfortable spacing**: Avoid cluttered screens### Information Density4. **Actions**: Prominent buttons, clear labels3. **Tertiary details**: Small, muted color2. **Secondary info**: Medium size, regular weight1. **Primary focus**: Large, bold, high contrast### Visual Hierarchy## Best Practices---```└─────────────────────────┘│                         ││  [Scan Receipt]         │  CTA (optional)│                         ││  any receipts yet.      ││  You haven't scanned    │  Message│                         ││    No Receipts          │  Title│                         ││        (dashed)         ││         📄              │  Illustration│                         │┌─────────────────────────┐```### Empty State```└─────────────────────────┘│  [Submit to YNAB]       │  Primary Action├─────────────────────────┤│  Category: [Groceries▼]││  Account: [Checking ▼ ] ││  Date: [Feb 14, 2026  ] ││  Amount: [$42.99       ]│  Edit Fields│  Payee: [Whole Foods  ] │├─────────────────────────┤│  └───────────────────┘  ││  │                   │  ││  │   Receipt Photo   │  ││  │                   │  │  Receipt Image│  ┌───────────────────┐  │├─────────────────────────┤│  [←] Review Receipt  [✓]│  Toolbar┌─────────────────────────┐```### Review Screen```              [📷] FAB└─────────────────────────┘│  ╰─────╯                ││  │    │ Feb 13 • [⏳]   ││  │    │ $8.25            ││  │ 📄 │ Starbucks        ││  ╭─────╮                │├─────────────────────────┤│  ╰─────╯                ││  │    │ Feb 14 • [✓]    ││  │    │ $42.99           ││  │ 📄 │ Whole Foods      │  Receipt Card│  ╭─────╮                │├─────────────────────────┤│  [Search]  [Filter] 🔔  │  Toolbar┌─────────────────────────┐```### Home Screen (Receipt List)## Screen Design Patterns---- See: `docs/LOCALIZATION_GUIDE.md`- RTL support: Ready for Arabic, Hebrew- Cultural sensitivity: Date/number formats- Financial terminology: Formal, accurate- All strings in `strings.xml`- No hardcoded strings in code/layouts### Guidelines2. **Spanish** (es) - Complete translation1. **English** (en) - Default### Supported Languages## Localization---- Consistent elevation overlay system- Automatic switching via Material3 theme- Defined in `values-night/colors.xml`### Implementation```Primary Dark: #005A8CPrimary: #4A8BB5// Primary (slightly lightened)Text Hint: #666666Text Secondary: #B3B3B3Text Primary: #FFFFFF// TextSurface Variant: #1E1E1ESurface: #121212// Surface colors```kotlin### Dark Mode Colors**System-based**: Follows device theme (Android 10+)### Approach## Dark Mode---- **Navigation**: Semantic structure- **Announcements**: Status changes announced- **Labeled fields**: All inputs have labels- **TalkBack optimized**: Logical reading order### Screen Reader Support- Stored in `content_descriptions.xml`- Context about action/purpose- Descriptive labels for screen readersAll images, icons, and interactive elements include:### Content Descriptions- **Application**: All buttons, icons, list items- **Spacing**: 8dp between targets- **Minimum size**: 48dp x 48dp### Touch Targets- Interactive elements: Clear focus indicators- Large text (18sp+): 3:1 contrast minimum- Normal text: 4.5:1 contrast minimumAll text meets **WCAG AA** standards:### Color Contrast## Accessibility---- **Overshoot**: Success/completion- **Accelerate**: Fade out, disappear- **Decelerate**: Fade in, appear- **Standard**: Accelerate-Decelerate (default)### Easing Curves5. **Shimmer**: Loading states (1500ms loop)4. **Pulse**: Syncing indicator (1000ms loop)3. **Shake**: Error feedback (500ms)2. **Scale In**: Success feedback (200ms)1. **Fade In/Out**: Content appearance/disappearance (300ms)### Animation Types- **Slow**: 500ms (complex animations)- **Medium**: 300ms (standard transitions)- **Fast**: 150-200ms (micro-interactions)### Timing## Animations---```  - ic_launcher_background.xml (background layer)  - ic_launcher_foreground.xml (foreground layer)drawable/  - ic_launcher_round.xml (round variant)  - ic_launcher.xml (adaptive icon)mipmap-anydpi-v26/```### Files- **Export sizes**: All Android densities (mdpi to xxxhdpi)- **Format**: Vector XML- **Safe zone**: 66dp x 66dp (centered)- **Adaptive icon**: 108dp x 108dp canvas### Specifications- **Concept**: Receipt + scanning technology- **Style**: Recognizable at all sizes- **Background**: YNAB blue gradient- **Foreground**: White receipt with scanner line### Design## App Icon---   - Purpose: Success confirmation   - Confetti accents   - Circle with checkmark4. **Success** (`illustration_success.xml`)   - Purpose: Generic error states   - Exclamation mark   - Warning triangle3. **Error** (`illustration_error.xml`)   - Purpose: Offline/network error   - Disconnected WiFi symbol   - Cloud with X2. **No Connection** (`illustration_no_connection.xml`)   - Purpose: No receipts state   - Camera icon in center   - Dashed receipt outline1. **Empty Receipts** (`illustration_empty_receipts.xml`)### Illustration Set- **Corner style**: Rounded- **Stroke width**: 2-3dp- **Colors**: Primary palette with hints of secondary- **Design language**: Minimalist, line-based### Style Guide## Illustrations---- Font: labelSmall (11sp, Medium)- Padding: 8dp horizontal, 4dp vertical- Corner radius: 12dp (pill shape)**Style**:```Text: WhiteBackground: #E74C3C (Error)// ErrorText: WhiteBackground: #27AE60 (Success)// SyncedText: WhiteBackground: #3498DB (Info)// SyncingText: WhiteBackground: #F39C12 (Secondary)// Pending```kotlin### Status Badges- **Label**: Floating label on focus- **Error color**: Error red- **Hint color**: Text secondary- **Corner radius**: 8dp- **Outlined style** (Material 3)### Input Fields- **Margin**: 16dp horizontal, 8dp vertical- **Padding**: 16dp- **Elevation**: 2dp (light), 4dp (hover)- **Corner radius**: 12dp### Cards- **Minimum touch target**: 48dp- **Text**: Primary color- **No background**#### Text Button- **Padding**: 24dp horizontal- **Height**: 48dp (minimum touch target)- **Corner radius**: 8dp- **Text**: White- **Background**: Primary (#005A8C)#### Primary Button### Buttons## Components---- **Warning**: Orange (#F39C12)- **Error**: Red (#E74C3C)- **Success**: Green (#27AE60)- **Inactive**: Text Secondary (#757575)- **Active**: Primary (#005A8C)### Icon Colors- **Category**: `ic_category.xml` - Category selection- **Account**: `ic_account.xml` - Account selection- **Calendar**: `ic_calendar.xml` - Date selection- **Error**: `ic_sync_error.xml` - Error states- **Check**: `ic_check.xml` - Success/completion- **Sync**: `ic_sync.xml` - Synchronization- **Camera**: `ic_camera.xml` - Scanning action- **Receipt**: `ic_receipt.xml` - Main app icon theme### Key App Icons```120dp - Illustration size80dp - Extra large (empty states)40dp - Large (feature icons)24dp - Medium (toolbar, buttons)16dp - Small (list icons)12dp - Extra small (inline icons)```kotlin### Icon Sizes**Material Design Icons** (MDI)### Icon Set## Icons---- **Icon padding**: 8dp- **Button padding**: 24dp horizontal, 12dp vertical- **Screen padding**: 16dp- **List item padding**: 12dp horizontal, 16dp vertical- **Card padding**: 16dp### Component Spacing```48dp - xxl (screen edges, major sections)32dp - xl  (major spacing)24dp - lg  (section separation)16dp - md  (standard spacing, padding)8dp  - sm  (compact elements)4dp  - xs  (tight spacing, small gaps)```kotlin### Standard Spacing Scale## Spacing System---- **On primary**: #FFFFFF (text on colored buttons)- **Disabled text**: #BDBDBD (low emphasis)- **Secondary text**: #757575 (medium emphasis)- **Primary text**: #212121 (high emphasis)### Text Colors by Context- Bold (700)- Medium (500)- Regular (400)**Roboto** (Android system default)### Font Family```labelSmall: 11sp / Medium (Small labels)labelMedium: 12sp / Medium (Chips, badges)labelLarge: 14sp / Medium (Buttons)<!-- Labels -->bodySmall: 12sp / Regular (Captions)bodyMedium: 14sp / Regular (Secondary content)bodyLarge: 16sp / Regular (Primary content)<!-- Body Text -->headlineSmall: 24sp / RegularheadlineMedium: 28sp / RegularheadlineLarge: 32sp / RegulardisplaySmall: 36sp / RegulardisplayMedium: 45sp / RegulardisplayLarge: 57sp / Regular<!-- Headlines -->```xml### Type Scale (Material Design 3)## Typography---```#000000 - Black#FFFFFF - White// Standard#BDBDBD - Text Hint#757575 - Text Secondary#212121 - Text Primary// Text#FFFFFF - Surface (cards, dialogs)#F5F5F5 - Background (light mode)// Background```kotlin### Neutral Colors```#3498DB - Informational messages// Info Blue#E74C3C - Error states, delete actions// Error Red#27AE60 - Success/Synced status// Success Green```kotlin### Accent Colors**Usage**: Pending status, warnings, highlights```#F5B041 - Secondary Light#D68910 - Secondary Dark#F39C12 - Secondary// Secondary - Warning/Attention```kotlin### Secondary Colors**Usage**: Navigation bars, primary buttons, active states, branding```#4A8BB5 - Primary Light#003D5C - Primary Dark#005A8C - Primary// Primary - YNAB Blue```kotlin### Primary Colors (YNAB Inspired)## Color Palette---Transform receipt management with instant scanning, accurate OCR, and seamless YNAB integration.### Value Proposition"Scan receipts and automatically sync them to your YNAB budget with smart OCR."### Tagline**YNAB Receipt Scanner**### App Name## Brand Identity
## Brand Identity

### Mission Statement
To make expense tracking effortless for YNAB users by combining powerful OCR technology with seamless budget integration.

### Brand Values
- **Trustworthy**: Financial data security is paramount
- **Efficient**: Save time with automated receipt processing
- **Clean**: Uncluttered interface that focuses on the task
- **Accessible**: Works for everyone, regardless of ability

---

## Color Palette

### Primary Colors (YNAB-inspired)

#### Primary Blue
- **HEX**: `#005A8C`
- **Usage**: Primary actions, buttons, headers
- **Name**: YNAB Blue
- **Accessibility**: WCAG AA compliant on white backgrounds

#### Primary Dark
- **HEX**: `#003D5C`
- **Usage**: Status bars, dark accents
- **Name**: Deep Ocean

#### Primary Light
- **HEX**: `#4A8BB5`
- **Usage**: Disabled states, secondary elements
- **Name**: Sky Blue

### Secondary Colors

#### Accent Orange
- **HEX**: `#F39C12`
- **Usage**: Highlights, calls-to-action
- **Name**: Sunset Orange

#### Success Green
- **HEX**: `#27AE60`
- **Usage**: Success states, "synced" indicators
- **Name**: Money Green

#### Error Red
- **HEX**: `#E74C3C`
- **Usage**: Error states, destructive actions
- **Name**: Alert Red

#### Warning Yellow
- **HEX**: `#F39C12`
- **Usage**: Warning messages
- **Name**: Caution Amber

### Neutral Colors

- **White**: `#FFFFFF` - Backgrounds, text on dark
- **Black**: `#000000` - Reserved for high contrast
- **Surface**: `#F5F5F5` - Background surfaces
- **Text Primary**: `#212121` - Main body text
- **Text Secondary**: `#757575` - Supporting text
- **Text Hint**: `#BDBDBD` - Placeholders, hints

### Color Contrast

All color combinations meet WCAG AA standards:
- Primary on white: 7.3:1 (AAA)
- Text primary on white: 16.1:1 (AAA)
- Text secondary on white: 4.6:1 (AA)

---

## Typography

### Type Scale (Material Design 3)

#### Display
- **Large**: 57sp / 64sp line height
- **Usage**: Hero text, marketing (not used in app UI)

#### Headline
- **Large**: 32sp / 40sp
- **Medium**: 28sp / 36sp
- **Small**: 24sp / 32sp
- **Usage**: Screen titles, section headers

#### Title
- **Large**: 22sp / 28sp
- **Medium**: 16sp / 24sp
- **Small**: 14sp / 20sp
- **Usage**: Card titles, list item headers

#### Body
- **Large**: 16sp / 24sp
- **Medium**: 14sp / 20sp
- **Small**: 12sp / 16sp
- **Usage**: Main content, descriptions

#### Label
- **Large**: 14sp / 20sp
- **Medium**: 12sp / 16sp
- **Small**: 11sp / 14sp
- **Usage**: Buttons, badges, input labels

### Font Family
- **Default**: Roboto (Android system)
- **Fallback**: System default sans-serif

### Font Weights
- **Regular** (400): Body text
- **Medium** (500): Emphasized text
- **Bold** (700): Headers, important info

---

## Spacing System

### Standard Scale (Material Design 3)
```
4dp   (xs)  - Tight spacing, icon padding
8dp   (sm)  - Component internal spacing
16dp  (md)  - Standard margin/padding
24dp  (lg)  - Section spacing
32dp  (xl)  - Screen margins (top/bottom)
48dp  (xxl) - Large sections, hero spacing
```

### Touch Targets
- **Minimum**: 48dp × 48dp (WCAG compliance)
- **Recommended**: 48dp × 48dp for all interactive elements
- **Icon buttons**: 48dp × 48dp container, 24dp icon

---

## Iconography

### Style
- **Outline style**: Material Design Icons (outlined variant)
- **Stroke width**: 2dp
- **Size**: 24dp standard (16dp, 40dp, and 80dp for special cases)
- **Color**: Primary or OnSurface colors

### Custom Icons
- Receipt with scanner line (app icon foreground)
- All icons follow Material Design guidelines
- Consistent visual weight across all icons

### Icon Usage
```
ic_home        - Navigation
ic_camera      - Scanning action
ic_receipt     - Receipt representation
ic_sync        - Synchronization
ic_check       - Success/complete
ic_delete      - Destructive action
ic_edit        - Modification
ic_calendar    - Date selection
```

---

## Components

### Cards
- **Corner radius**: 12dp
- **Elevation**: 2dp
- **Padding**: 16dp
- **Margin**: 16dp horizontal, 8dp vertical
- **Background**: Surface color

### Buttons

#### Primary (Filled)
- **Height**: 48dp
- **Corner radius**: 8dp
- **Padding**: 24dp horizontal
- **Text**: Label Large, all caps
- **Background**: Primary color
- **Text color**: On Primary

#### Text Button
- **Height**: 48dp
- **Padding**: 16dp horizontal
- **Text**: Label Large
- **Color**: Primary

#### Icon Button
- **Size**: 48dp × 48dp
- **Icon**: 24dp centered
- **Ripple**: Circular

### Text Fields (Outlined)
- **Height**: 56dp
- **Corner radius**: 4dp
- **Stroke**: 1dp (2dp when focused)
- **Label**: Floating label style
- **Icon**: 24dp leading/trailing icons

### Bottom Navigation
- **Height**: 80dp (with labels)
- **Active**: Primary color + label
- **Inactive**: OnSurfaceVariant color

### FAB (Floating Action Button)
- **Size**: 56dp
- **Icon**: 24dp
- **Elevation**: 6dp
- **Color**: Primary
- **Position**: 16dp from edges

---

## Layout Principles

### Responsive Design
- **Phone portrait**: 360dp - 420dp width (primary)
- **Phone landscape**: Supported, nav bar adapts
- **Tablet**: 600dp+ (use master-detail layouts)
- **Foldable**: Support multi-window

### Grid System
- **Margin**: 16dp on phone, 24dp on tablet
- **Gutter**: 16dp between columns
- **Columns**: 4 (phone), 8 (tablet), 12 (desktop)

### Visual Hierarchy
1. **Primary action**: Most prominent (FAB or filled button)
2. **Secondary action**: Text buttons
3. **Tertiary**: Icon buttons

---

## App Icon

### Design
- Paper receipt with scanner line
- YNAB blue gradient background
- White foreground elements
- Recognizable at all sizes (16dp to 512dp)

### Adaptive Icon Layers
- **Background**: Gradient (#005A8C → #003D5C)
- **Foreground**: Receipt illustration (safe zone: 66dp radius)

### Icon Sizes
```
mdpi:    48×48   px (1x)
hdpi:    72×72   px (1.5x)
xhdpi:   96×96   px (2x)
xxhdpi:  144×144 px (3x)
xxxhdpi: 192×192 px (4x)
```

---

## Illustrations

### Style
- **Flat design**: No 3D effects or shadows
- **Outlined**: Stroke-based illustrations
- **Color**: Primary palette only
- **Simple**: Minimal detail, clear meaning

### Empty States
1. **No receipts**: Dashed receipt outline with camera
2. **No connection**: Cloud with X
3. **Error**: Warning triangle
4. **Success**: Checkmark in circle

### Onboarding
- Simple, friendly illustrations
- Show key features: scan, review, sync
- Consistent with app icon style

---

## Animation

### Duration
- **Fast**: 100ms - Fade in/out, scale
- **Normal**: 200-300ms - Most transitions
- **Slow**: 400-500ms - Complex animations

### Easing
- **Decelerate**: Enter transitions
- **Accelerate**: Exit transitions
- **Standard**: Most general animations

### Types
- **Fade**: Alpha 0 ↔ 1
- **Scale**: Buttons, success indicators
- **Slide**: Screen transitions
- **Pulse**: Sync indicators (subtle)
- **Shake**: Error feedback

### Principles
- **Purposeful**: Every animation has a reason
- **Subtle**: Don't distract from content
- **Consistent**: Same action = same animation
- **Performant**: 60fps minimum

---

## Accessibility

### Color Contrast
- **Normal text**: 4.5:1 minimum (WCAG AA)
- **Large text**: 3:1 minimum
- **All combinations tested**: Meets AA standards

### Touch Targets
- **Minimum**: 48dp × 48dp
- **Spacing**: 8dp between targets

### Screen Reader Support
- All images have `contentDescription`
- Buttons have clear labels
- Status changes announced

### Font Scaling
- Support up to 200% font size
- Layouts adapt to larger text
- No text truncation at large sizes

---

## Voice & Tone

### Voice Characteristics
- **Clear**: No jargon, plain language
- **Concise**: Short sentences, get to the point
- **Helpful**: Guide users, don't lecture
- **Professional**: Financial context requires trust

### Error Messages
- ✅ "Camera permission is required to scan receipts"
- ❌ "ERROR: Permission denied (code: 403)"

### Success Messages
- ✅ "Transaction submitted successfully"
- ❌ "Operation completed"

### Empty States
- ✅ "You haven't scanned any receipts yet. Tap the camera button to get started."
- ❌ "No data available"

---

## Design Resources

### Figma File
- Complete design system
- All screens and components
- Prototypes and flows

### Icon Library
- Material Design Icons
- Custom app icons (vector)
- All sizes and variants

### Style Guide
- Color swatches
- Typography samples
- Component library

---

## Brand Guidelines

### Logo Usage
- Never modify the app icon
- Maintain clear space (16dp minimum)
- Don't add effects or filters

### YNAB Brand Disclaimer
**Important**: This app is not officially affiliated with YNAB.

Always include disclaimer:
> "This app is an independent project and is not officially affiliated with or endorsed by You Need A Budget LLC (YNAB). YNAB is a trademark of You Need A Budget LLC."

### Attribution
- Open source under MIT License
- Credit contributors
- Link to GitHub repository

---

## Implementation Checklist

- [x] Color palette defined in `colors.xml`
- [x] Typography scale in `themes.xml`
- [x] Spacing system in `dimens.xml`
- [x] App icon (adaptive, all densities)
- [x] Illustrations (empty states)
- [x] Animations (fade, scale, shake)
- [x] Accessibility (content descriptions, touch targets)
- [x] Localization (English, Spanish)
- [x] Dark theme support
- [x] Material Design 3 components

---

## Maintenance

### Version Control
- Store design files in repo (`/design` folder)
- Version with app releases
- Document all changes in changelog

### Design Updates
- Test on multiple devices
- Validate accessibility
- Get user feedback
- A/B test major changes

### Consistency Checks
- Regular design audits
- Component inventory
- Accessibility testing
- Localization review

---

**Last Updated**: February 14, 2026  
**Version**: 1.0  
**Maintained by**: YNAB Receipt Scanner Contributors
