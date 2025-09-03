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
