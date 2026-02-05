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
