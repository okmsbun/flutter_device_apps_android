## 1.0.0

- **BREAKING**: Raised the minimum requirements to Dart 3.12.0 and Flutter 3.44.0.
- Updated `flutter_device_apps_platform_interface` to `^1.0.0`.
- Added native `isAppInstalled`, `isSystemApp`, and `isAppEnabled` queries using `PackageManager.getApplicationInfo`, without building full app metadata.
- Added `isAppLaunchable` to check for a launch intent without opening the app.
- Added `getAppIcon` to load an app icon separately as PNG bytes.
- Added `getInstallSourceInfo` with installer, initiating package, originating package, package source, and update owner information where supported. Android versions before API 30 return only the installer package name.
- Deprecated `getInstallerStore`. Use `getInstallSourceInfo` and its `installingPackageName` field instead. The existing method now uses the modern install source API on Android API 30 and later.
- Added optional, case-sensitive `packageNamePrefix` filtering to `listApps`, applied before loading app metadata and icons. Null or empty disables the filter.
- Fixed `appChanges` subscription cleanup and restart behavior: cancel the internal EventChannel subscription when the last listener leaves and serialize stream startup and shutdown.
- Updated Kotlin configuration to use `compilerOptions` and rely on Flutter's Kotlin plugin handling.
- Expanded Dart and Kotlin tests for the new queries, filtering, and stream subscription lifecycle.
- Updated package topics and directed issue reports to the main `flutter_device_apps` repository.

## 0.8.0

- Adds Android Gradle Plugin 9 / built-in Kotlin compatibility.
- Keeps Kotlin Gradle Plugin compatibility for Android Gradle Plugin versions earlier than 9.

## 0.7.0

- Expanded Android app details payload with additional raw fields: `uid`, `apkPath`, `apkSizeBytes`, `dataPath`, and `isOnExternalStorage`.
- Added APK size calculation in bytes using `ApplicationInfo.sourceDir` and `splitSourceDirs`.
- Updated Dart unit tests for method channel mapping of the new fields.

## 0.6.0

- **BREAKING**: Updated to match platform interface 0.6.0 - `requestedPermissions` removed from `AppInfo`
- Added `getRequestedPermissions(String packageName)` method implementation for on-demand permission retrieval
- Added GitHub Actions workflows for Android unit tests, quality checks, and PR enforcement
- Added comprehensive Dart unit tests (27 tests) for method channel mocking
- Added Kotlin unit tests (11 tests) with Robolectric for Android plugin
- Made `FlutterDeviceAppsAndroidPlugin` class `open` with `protected` fields for testability

## 0.5.1

- Added support for additional `AppInfo` fields from the Android package manager: `category`, `targetSdkVersion`, `minSdkVersion`, `enabled`, `processName`, `installLocation`, `requestedPermissions`.
- Populates `requestedPermissions` via `PackageManager.GET_PERMISSIONS`.

## 0.4.0

App change events now forward the raw Android action string to Dart, letting the Dart side handle event type mapping; no breaking changes.

## 0.2.0

- Enhanced README.md with professional badge layout for better package visibility
- Added centered HTML badges for pub.dev, GitHub stars, Flutter documentation, and MIT license
- Added umbrella package badge linking to main flutter_device_apps package
- Improved documentation presentation following modern Flutter package standards
- Updated flutter_device_apps_platform_interface dependency to ^0.2.0
- Enhanced package branding and visual consistency across federated plugin family

## 0.1.2

- Added `openAppSettings` implementation using `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`
- Added `uninstallApp` implementation using `Intent.ACTION_UNINSTALL_PACKAGE` with fallback to `ACTION_DELETE`
- Added `getInstallerStore` implementation using `PackageManager.getInstallerPackageName()`
- Improved error handling with specific error codes (ERR_OPEN_SETTINGS, ERR_UNINSTALL, ERR_INSTALLER)
- Added proper Android Intent.ACTION mapping for package events:
  - `ACTION_PACKAGE_ADDED` → installed
  - `ACTION_PACKAGE_REMOVED` → removed
  - `ACTION_PACKAGE_CHANGED`/`ACTION_PACKAGE_REPLACED` → updated
  - `ACTION_PACKAGE_FULLY_REMOVED` → removed
- Added support for `Intent.EXTRA_REPLACING` to distinguish updates from uninstalls
- Improved broadcast receiver with proper IntentFilter setup
- Added coroutine-based async operations for better performance
- Removed unused `enabled`/`disabled` event types that were never implemented

## 0.1.0

- First public Android implementation for `flutter_device_apps`
- Adds listApps, getApp, openApp, and appChanges (EventChannel)
