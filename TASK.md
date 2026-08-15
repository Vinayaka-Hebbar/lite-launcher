# Build a Production-Grade Lightweight Android Launcher

Create a complete, production-quality Android home-screen launcher application using **Kotlin only**.

The application should be a true Android launcher/home-screen replacement comparable in capability to mature launchers such as Nova Launcher, while having its **own original architecture, UI, branding, interactions and implementation**.

Do not copy Nova Launcher source code, assets, icons, layouts, names or proprietary behavior.

The launcher must prioritize:

* extremely small application size
* excellent performance
* very low memory usage
* very low battery consumption
* deep customization
* smooth interactions
* privacy
* zero advertisements
* zero analytics/tracking
* mobile and tablet support
* maintainable production-quality Kotlin code

---

# 1. Hard Requirements

## Technology

Use:

* Kotlin
* Android SDK
* AndroidX only where necessary
* XML-based Android Views
* RecyclerView where appropriate
* ViewModel only where useful
* Kotlin Coroutines only where asynchronous work is necessary
* SharedPreferences or a similarly lightweight native persistence solution
* Android PackageManager / LauncherApps APIs
* Android AppWidget APIs
* Android shortcut APIs
* Android Intent APIs

Do NOT use:

* Flutter
* React Native
* .NET MAUI
* Rust
* Java unless an Android API absolutely requires interoperability
* Jetpack Compose
* WebView
* JavaScript
* Firebase
* Google Analytics
* AdMob
* advertising SDKs
* telemetry SDKs
* large dependency injection frameworks
* large image-loading frameworks
* unnecessary Material libraries
* third-party UI frameworks

Every dependency must have a clear justification.

---

# 2. Application Size

The final release should target:

**≤ 5 MB download size**

Preferably:

**2–4 MB**

Application size is a major architectural requirement.

Enable:

```gradle
isMinifyEnabled = true
isShrinkResources = true
```

Use:

```text
R8
ProGuard optimization
resource shrinking
WebP/vector resources where useful
minimal resources
minimal dependencies
ABI-conscious packaging
```

Do not bundle:

* large fonts
* icon libraries
* large PNG collections
* videos
* unnecessary native libraries
* animation libraries
* duplicate assets

Use Android system resources whenever appropriate.

Run APK/AAB size analysis as part of release validation.

If a feature requires a large dependency, prefer implementing the required behavior directly using Android APIs.

---

# 3. Launcher Role

The application must register itself as a proper Android Home application.

Support:

* HOME intent
* DEFAULT category
* launcher selection
* becoming the user's default launcher
* opening Android's Home app selection settings
* correct lifecycle behavior when HOME is pressed
* configuration changes
* process recreation
* app installation/removal/change events

The launcher must safely recover after Android kills its process.

---

# 4. Home Screen

Implement a highly customizable multi-page workspace.

Support:

* multiple home-screen pages
* horizontal page navigation
* configurable number of pages
* configurable default page
* page reordering
* add page
* remove page
* configurable desktop grid
* icon placement
* drag-and-drop icons
* folders
* widgets
* shortcuts
* empty spaces
* wallpaper visibility
* configurable margins
* configurable horizontal padding
* configurable vertical padding

Example grid choices:

```text
4 × 5
4 × 6
5 × 5
5 × 6
5 × 7
6 × 6
6 × 7
custom
```

Allow advanced users to configure grid dimensions manually within sensible limits.

---

# 5. Responsive Phone + Tablet Design

The launcher must support:

* small phones
* standard phones
* large phones
* tablets
* landscape tablets
* foldables
* portrait mode
* landscape mode
* split-screen where Android permits it
* different display densities

Do NOT simply stretch the phone layout on tablets.

Calculate layout based on available window dimensions.

Use adaptive breakpoints/window size information.

Tablet layouts should support:

* larger configurable grids
* wider dock layouts
* additional icon columns
* larger widget placement areas
* intelligent spacing
* landscape-friendly layouts

Home-screen layouts may optionally be maintained separately for portrait and landscape.

---

# 6. App Drawer

Implement a full application drawer.

Support:

* alphabetical app list
* grid mode
* list mode
* fast scrolling
* search
* automatic updates when apps are installed/uninstalled
* hidden apps
* app categories
* custom drawer groups
* folders
* recent apps section where technically permitted
* frequently launched apps based only on local launcher usage
* alphabetical section headers

Configuration:

* number of columns
* icon size
* icon label size
* label visibility
* background
* transparency
* spacing
* drawer orientation
* scrolling direction
* search bar visibility
* sorting

Sorting:

```text
Alphabetical
Installation date
Custom
Most launched
Recently launched
```

Never upload usage information anywhere.

---

# 7. Fast App Search

Search must feel instantaneous.

Search by:

* application name
* partial name
* package label
* optional aliases

Support fuzzy/prefix matching where practical.

Examples:

```text
"yt" → YouTube
"wh" → WhatsApp
"calc" → Calculator
```

Keep the search implementation lightweight.

Do not add a search library just for this feature.

---

# 8. Dock

Implement an optional bottom dock.

Support:

* enable/disable dock
* configurable icon count
* configurable rows
* dock pages
* swipe between dock pages
* folders in dock
* configurable dock background
* transparent dock
* rounded dock
* icon size
* spacing
* dock padding
* optional labels

Tablet dock should scale intelligently without oversized controls.

---

# 9. Folder System

Support home-screen and app-drawer folders.

Features:

* drag apps onto another app to create folder
* drag apps into existing folder
* rename folder
* reorder applications
* custom folder color
* folder icon preview
* configurable preview layouts
* configurable opening animation
* full-screen or popup folder style
* configurable columns
* scrollable large folders

Opening a folder should be extremely fast.

---

# 10. Widgets

Implement proper Android widget hosting.

Use native:

```text
AppWidgetHost
AppWidgetHostView
AppWidgetManager
```

Support:

* add widgets
* widget picker
* resize widget
* move widget
* delete widget
* restore widgets when possible
* correct persistence
* widget configuration activities
* responsive widget resizing

Handle missing widget providers safely.

---

# 11. Icon Packs

Support standard Android icon packs.

Features:

* detect installed icon packs
* select icon pack
* apply globally
* select custom icon per app
* reset individual icon
* fallback to application's original adaptive icon
* support adaptive icons
* support legacy icons

Do not bundle icon packs.

---

# 12. Icon Customization

Allow users to configure:

* icon size
* app label
* label visibility
* label text size
* label color
* icon shape where technically appropriate
* per-app custom icon
* per-app custom label

Support:

```text
System icon
Icon-pack icon
Custom selected icon
```

Avoid storing duplicate large bitmaps.

---

# 13. Gestures

Create a configurable gesture system.

Support:

```text
Swipe up
Swipe down
Swipe left
Swipe right
Double tap
Double tap + swipe
Long press
Two-finger swipe up
Two-finger swipe down
Pinch in
Pinch out
```

Possible actions:

```text
Open app drawer
Open notification panel
Open quick settings
Open launcher search
Open specific app
Open launcher settings
Go to default home page
Show recent apps where Android permits
Lock screen where permitted
Do nothing
```

Only request special permissions when absolutely necessary.

Explain to users why a permission is required before requesting it.

---

# 14. App Icon Actions

Long-pressing an application should display contextual actions.

Support where available:

* App info
* Uninstall
* Remove from home
* Edit
* Widgets
* Android app shortcuts
* Pin shortcut
* Hide app

Use Android's native shortcut APIs.

---

# 15. Notification Badges

Support notification dots/badges where Android APIs and permissions allow.

Possible styles:

```text
Dot
Count where available
Disabled
```

Do not aggressively request notification access.

Launcher must remain fully usable without it.

---

# 16. Wallpaper

Support:

* system wallpaper
* wallpaper picker
* home-screen wallpaper
* static wallpaper interaction
* optional wallpaper scrolling
* wallpaper offset based on workspace page

Do not bundle wallpapers by default because application size is important.

---

# 17. Themes

Support:

```text
System
Light
Dark
AMOLED black
Custom
```

Allow customization of:

* accent color
* desktop text
* folder background
* drawer background
* dock background
* search field appearance
* popup appearance

Avoid a giant theme framework.

Implement themes using lightweight Android resources and configuration.

---

# 18. Appearance Customization

Provide settings for:

### Desktop

* grid
* icon size
* labels
* padding
* page indicator
* wallpaper scrolling
* transition effects

### Drawer

* grid
* icon size
* labels
* background
* transparency
* scrolling style
* search
* tabs/groups

### Dock

* icon count
* pages
* rows
* background
* transparency
* padding

### Folders

* style
* shape
* grid
* animation
* colors

### Search

* position
* appearance
* provider behavior
* app-only search

---

# 19. Animations

Animations must be subtle, fast and inexpensive.

Support lightweight transitions such as:

```text
None
Fade
Slide
Scale
Zoom
```

Prioritize:

```text
60 FPS
90 FPS
120 FPS
```

depending on the device refresh rate.

Never sacrifice responsiveness for decorative animations.

Avoid large animation libraries.

Use native View/property animations.

---

# 20. Launcher Search Widget

Provide an optional launcher-owned search bar.

It may search:

* installed applications
* launcher shortcuts
* local folders

It should NOT send search queries to a server unless the user explicitly chooses an external search action.

Privacy should be the default.

---

# 21. Hidden Apps

Allow users to hide applications from the app drawer.

Hidden apps:

* remain installed
* remain searchable only if the user enables that behavior
* can be restored from launcher settings

Do not pretend this is a security mechanism.

Clearly describe it as organizational/privacy convenience.

---

# 22. Backup and Restore

Provide local launcher backup.

Backup:

* home-screen layout
* folders
* launcher preferences
* gestures
* icon configuration
* drawer configuration
* hidden app configuration

Use a compact versioned JSON format.

Example:

```json
{
  "version": 1,
  "workspace": {},
  "settings": {},
  "folders": [],
  "gestures": {}
}
```

Support:

* export backup
* import backup
* validation
* version migration
* corrupted-file handling

Do not require cloud storage.

---

# 23. Settings Architecture

Create a clean settings hierarchy:

```text
Settings
├── Home Screen
│   ├── Grid
│   ├── Icons
│   ├── Labels
│   ├── Pages
│   └── Scrolling
│
├── App Drawer
│   ├── Layout
│   ├── Search
│   ├── Groups
│   └── Hidden Apps
│
├── Dock
│   ├── Layout
│   ├── Appearance
│   └── Pages
│
├── Folders
│   ├── Layout
│   └── Appearance
│
├── Gestures
│
├── Appearance
│   ├── Theme
│   ├── Colors
│   └── Animations
│
├── Icon Pack
│
├── Backup & Restore
│
├── Default Launcher
│
└── About
```

Settings should themselves adapt well to tablets.

On a tablet, preferably show:

```text
Settings categories | Selected settings
```

as a two-pane layout when there is sufficient horizontal space.

---

# 24. Architecture

Keep architecture understandable and lightweight.

Suggested package structure:

```text
com.example.launcher

├── LauncherActivity.kt
├── LauncherApplication.kt
│
├── workspace/
│   ├── WorkspaceView.kt
│   ├── WorkspacePage.kt
│   ├── WorkspaceController.kt
│   ├── WorkspaceLayout.kt
│   └── WorkspaceRepository.kt
│
├── drawer/
│   ├── AppDrawerActivity.kt
│   ├── AppDrawerView.kt
│   ├── AppAdapter.kt
│   ├── AppSearch.kt
│   └── AppRepository.kt
│
├── dock/
├── folders/
├── widgets/
├── gestures/
├── icons/
├── shortcuts/
├── search/
├── wallpaper/
├── settings/
├── backup/
├── model/
├── persistence/
└── util/
```

Do NOT create layers merely to follow an architecture trend.

Avoid:

```text
Repository → UseCase → Interactor → Manager → Provider
```

for trivial operations.

Use abstraction only where it genuinely improves maintainability.

---

# 25. Data Model

Design efficient lightweight models.

Example:

```kotlin
sealed interface WorkspaceItem {
    val id: Long
    val cellX: Int
    val cellY: Int
    val spanX: Int
    val spanY: Int
}
```

Possible item types:

```text
Application
Shortcut
Folder
Widget
```

Keep models immutable where practical.

Avoid unnecessary allocation in hot rendering/scrolling paths.

---

# 26. Installed Application Repository

Maintain an efficient cached representation of installed launchable applications.

Store only required information.

Example:

```kotlin
data class LaunchableApp(
    val packageName: String,
    val activityName: String,
    val label: String
)
```

Do not permanently retain unnecessary large Drawable/Bitmap objects.

Load icons lazily.

Cache only what provides measurable benefit.

Respond to package changes incrementally rather than rescanning everything unnecessarily.

---

# 27. Performance Requirements

Launcher startup must be exceptionally fast.

Target:

```text
warm launch: effectively immediate
home interaction: no visible delay
drawer opening: immediate
search: results while typing
scrolling: frame-rate smooth
```

Avoid disk access on the main thread.

Avoid parsing large configuration files at startup.

Avoid recreating app lists repeatedly.

Avoid unnecessary allocations during:

* drawing
* scrolling
* icon layout
* gesture handling

Profile using:

* Android Studio profiler
* CPU profiler
* memory profiler
* Layout Inspector
* APK Analyzer

---

# 28. Memory Management

Treat memory efficiency as a core feature.

Be careful with:

* Drawable references
* icon bitmap caches
* widgets
* Context references
* listeners
* broadcast receivers

Avoid memory leaks.

Use lifecycle-aware registration/unregistration.

Implement a bounded icon cache if caching is necessary.

React appropriately to Android memory-pressure callbacks.

---

# 29. Battery Usage

The launcher should perform almost no background work when idle.

Avoid:

* polling
* repeating background jobs
* unnecessary WorkManager jobs
* permanent background services
* high-frequency timers
* unnecessary wake locks

Prefer event-driven Android APIs.

---

# 30. Privacy

The launcher must contain:

```text
NO ADS
NO ANALYTICS
NO TRACKERS
NO TELEMETRY
NO USER PROFILING
NO REMOTE CONFIGURATION
```

Do not collect:

* app usage
* installed app list
* search history
* launcher configuration
* device identifiers

for transmission to external servers.

Usage statistics required internally for features such as frequently used apps must remain on-device.

Provide a clear privacy statement:

> This launcher does not collect or transmit personal data.

---

# 31. Internet Permission

The core launcher should preferably NOT require:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

If no feature actually requires network access, omit the permission entirely.

The base launcher should work completely offline.

---

# 32. Permissions

Request the absolute minimum permissions.

Each optional permission-dependent feature should gracefully degrade if permission is denied.

Never request unrelated permissions during initial startup.

---

# 33. Accessibility

Support:

* TalkBack
* sensible content descriptions
* keyboard navigation
* external keyboard
* D-pad where reasonable
* large font scales
* large touch targets
* proper focus order
* sufficient contrast

Tablet users may use keyboard and mouse.

Support:

* hover where useful
* right-click/context-menu behavior where appropriate
* keyboard shortcuts where valuable

---

# 34. Tablet Experience

Tablet support is a first-class feature.

Examples:

### Home

```text
Phone:
5 × 6

Tablet portrait:
7 × 7

Tablet landscape:
10 × 6
```

These are defaults only and remain customizable.

### App drawer

Phone:

```text
4–6 columns
```

Tablet:

```text
7–10+ columns depending on available width
```

Determine actual dimensions dynamically.

Do not hard-code assumptions based solely on device model.

---

# 35. Orientation Changes

Changing orientation must:

* preserve home-screen state
* preserve current page
* preserve open folder where practical
* avoid crashes
* avoid losing widget state
* intelligently recalculate grid placement

Support either:

```text
shared portrait/landscape layout
```

or an optional advanced setting:

```text
separate portrait and landscape layouts
```

---

# 36. Import / Migration

Design the internal architecture so migration/import features can be added later.

Do not directly copy proprietary launcher backup formats without proper specification and permission.

---

# 37. Default Experience

The default setup should be beautiful without requiring configuration.

Initial configuration:

```text
Home
├── Search
├── Main workspace
└── Dock
```

Choose sensible grid dimensions based on screen width.

The launcher should feel:

* clean
* modern
* calm
* fast
* native
* lightweight

Avoid excessive visual effects.

---

# 38. First Launch

First launch should be minimal.

Show:

```text
Welcome
↓
Choose basic layout
↓
Set as default launcher
↓
Home
```

Do not show a long tutorial.

Provide contextual help later.

---

# 39. Undo

For destructive layout operations such as:

```text
Remove shortcut
Delete folder
Remove widget
```

provide a lightweight temporary Undo action where appropriate.

---

# 40. Crash Safety

A launcher is more critical than a normal app because it is the user's HOME application.

Therefore:

* never leave the user with a blank screen after recoverable errors
* handle corrupted preferences
* handle missing apps
* handle removed widgets
* handle invalid shortcuts
* handle failed icon-pack resources
* handle Android process death

Create sensible fallback behavior.

---

# 41. Testing

Add unit tests for important non-UI logic.

Test:

* app sorting
* search
* grid positioning
* folder operations
* configuration serialization
* backup parsing
* migration
* gesture mapping

Add instrumentation tests for critical launcher flows where practical.

Test manually on:

```text
small phone
standard phone
large phone
7–8" tablet
10–11" tablet
landscape tablet
foldable/emulator
```

Also test:

```text
different densities
font scaling
dark mode
rotation
process death
app installation
app removal
widget provider removal
```

---

# 42. Release Build Requirements

Release build must:

* enable R8
* enable resource shrinking
* contain no debug logging
* contain no debug assets
* contain no analytics
* contain no test libraries
* contain no unnecessary permissions
* be inspected using APK Analyzer

Report:

```text
APK/AAB download size
DEX size
resource size
native library size
largest bundled assets
```

If download size exceeds **5 MB**, investigate and reduce it before considering the build production-ready.

---

# 43. Quality Priorities

When requirements conflict, use this priority:

```text
1. Reliability
2. Performance
3. Privacy
4. Core launcher usability
5. Application size ≤ 5 MB
6. Battery efficiency
7. Customization
8. Visual effects
```

Do not sacrifice reliability merely to save a few kilobytes.

However, continuously protect the 5 MB budget during development.

---

# 44. Development Approach

Do NOT attempt to implement the entire launcher as one gigantic code generation step.

Build incrementally.

## Phase 1 — Launcher Foundation

Implement:

* Android project
* HOME registration
* LauncherActivity
* installed app discovery
* app launch
* basic workspace
* app drawer
* persistence
* phone/tablet adaptive layout

Deliver a working launcher.

## Phase 2 — Workspace

Implement:

* multiple pages
* grid system
* drag-and-drop
* dock
* folders

## Phase 3 — Customization

Implement:

* grid configuration
* icon size
* labels
* drawer customization
* dock customization
* themes

## Phase 4 — Android Integration

Implement:

* shortcuts
* widgets
* adaptive icons
* icon packs
* package-change handling

## Phase 5 — Productivity

Implement:

* gestures
* fast search
* hidden apps
* groups
* app actions

## Phase 6 — Advanced Features

Implement:

* backup/restore
* notification dots where possible
* orientation-specific layouts
* advanced folder customization

## Phase 7 — Optimization

Perform:

* startup profiling
* memory profiling
* battery profiling
* APK size analysis
* R8 optimization
* resource cleanup

---

# 45. Coding Standards

Use idiomatic modern Kotlin.

Prefer:

```kotlin
val
```

over mutable state whenever practical.

Avoid:

```kotlin
!!
```

unless correctness guarantees are explicit.

Use meaningful names.

Keep methods small but do not fragment trivial logic into unnecessary abstractions.

Avoid verbose comments.

Comments should explain **why**, not repeat what the code already expresses.

Use KDoc only for important public/internal contracts where it adds real value.

---

# 46. No Placeholder Implementation

Do not produce fake features such as:

```kotlin
fun loadApps() {
    // TODO
}
```

Core features must actually work.

If Android restrictions prevent a feature from being implemented exactly as requested, explain:

1. the Android restriction
2. the supported alternative
3. the implementation chosen

Do not silently fake functionality.

---

# 47. Feature Benchmark

The final launcher should aim for the same overall category of capability expected from mature customizable Android launchers:

* customizable home grid
* multiple pages
* app drawer
* folders
* dock
* icon packs
* custom icons
* widgets
* gestures
* shortcuts
* search
* hidden apps
* themes
* backup/restore
* configurable animations
* custom labels
* tablet layouts
* landscape support
* notification indicators where Android permits
* extensive visual customization

But make the product distinct.

The objective is:

> **Nova-level depth, but smaller, cleaner, private, completely ad-free and built specifically around native Kotlin performance.**

---

# 48. Final Product Goal

Create an Android launcher that can credibly be described as:

> A fast, private, deeply customizable Android launcher for phones and tablets with zero ads, zero tracking and a download size below 5 MB.

The experience should feel premium despite being completely free.

The user should never feel that the small binary size resulted in a compromised launcher.

Favor excellent engineering and native Android behavior over flashy dependencies.

Start by generating the **complete Phase 1 project structure, Gradle configuration, AndroidManifest.xml, data models, LauncherActivity, responsive workspace, installed-app repository and functional app drawer**.

The Phase 1 output must compile and run before proceeding to later phases.
