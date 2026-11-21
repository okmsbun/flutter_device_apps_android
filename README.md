# flutter_device_apps_android

<p align="center">
<a href="https://pub.dev/packages/flutter_device_apps_android"><img src="https://img.shields.io/pub/v/flutter_device_apps_android.svg?color=0175C2" alt="Pub"></a>
<a href="https://pub.dev/packages/flutter_device_apps_android/score"><img src="https://img.shields.io/pub/points/flutter_device_apps_android" alt="flutter_device_apps_android Pub points"></a>
<a href="https://github.com/okmsbun/flutter_device_apps"><img src="https://img.shields.io/github/stars/okmsbun/flutter_device_apps.svg?style=flat&logo=github&colorB=deeppink&label=stars" alt="GitHub stars for flutter_device_apps"></a>
<a href="https://opensource.org/licenses/MIT"><img src="https://img.shields.io/badge/license-MIT-purple.svg" alt="License: MIT"></a>
<a href="https://pub.dev/packages/flutter_device_apps"><img src="https://img.shields.io/badge/Main%20Package-orange.svg" alt="Main Package"></a>
</p>

A Flutter plugin to list installed Android apps, get app icons, check installation
status, launch apps, and monitor app changes. Also detects packaged frameworks.

## Requirements

Requires Android API 24+, Flutter 3.44+, and Dart 3.12+.
[API reference](https://pub.dev/documentation/flutter_device_apps_android/latest/flutter_device_apps_android/)
· [Guide: List installed Android apps in Flutter](doc/list-installed-android-apps.md)

## Main package

`flutter_device_apps` provides a single entry point for Android and macOS APIs
and uses this plugin on Android. [Main package](https://pub.dev/packages/flutter_device_apps) · [Main package README](https://github.com/okmsbun/flutter_device_apps#readme)

## Usage

Add the Android package to your Flutter app.

```sh
flutter pub add flutter_device_apps_android
```

```dart
import 'package:flutter_device_apps_android/flutter_device_apps_android.dart';
```

Call `WidgetsFlutterBinding.ensureInitialized()` if you use the API before
`runApp()`. The binding is already available in `initState()` and UI callbacks.

### List installed Android apps

```dart
final apps = await FlutterDeviceAppsAndroid.listApps(
  includeSystem: false,
  onlyLaunchable: true,
  includeIcons: false,
  packageNamePrefix: null, // e.g. 'com.google.'
  packageNameContains: null, // e.g. 'photos'
);
```

Both filters are case-sensitive and must match when provided. Use `null` or `''` to disable a filter.

### Get apps by package name

```dart
final app = await FlutterDeviceAppsAndroid.getApp('com.whatsapp', includeIcon: true);

final apps = await FlutterDeviceAppsAndroid.getApps(
  ['com.whatsapp', 'org.telegram.messenger'],
  includeIcons: true,
);
```

`getApp()` returns `null` if the app is unavailable. `getApps()` returns `[]` when no apps are found.

### Detect packaged app frameworks

```dart
final frameworks = await FlutterDeviceAppsAndroid.detectAppFrameworks('com.example.app');
```

Detects packaged indicators for **Flutter, React Native, Unity, Unreal Engine,
Godot, .NET Android, Jetpack Compose, Kotlin standard library, AndroidX AppCompat,
Capacitor, Cordova, and Qt**. Matches describe packaged components, including
transitive dependencies. An empty list means the scan found no supported
indicator. A failed scan throws.

### Listen to app install, update, and uninstall events

```dart
final subscription = FlutterDeviceAppsAndroid.appChanges.listen(
  (event) => print('${event.type}: ${event.packageName}'),
  onError: (error) => print(error),
);
```

Stop listening with `await subscription.cancel()`.

## More APIs

All methods below belong to `FlutterDeviceAppsAndroid`.

| Method                                 | Purpose                                                 |
| -------------------------------------- | ------------------------------------------------------- |
| `getAppIcon(packageName)`              | Get PNG icon bytes.                                     |
| `isAppInstalled(packageName)`          | Check whether the app is installed and visible.         |
| `isSystemApp(packageName)`             | Check system app status. Returns `null` if unavailable. |
| `isAppEnabled(packageName)`            | Check enabled status. Returns `null` if unavailable.    |
| `isAppLaunchable(packageName)`         | Check whether a launch intent exists.                   |
| `getRequestedPermissions(packageName)` | Get requested permission names.                         |
| `getInstallSourceInfo(packageName)`    | Get installer and install source metadata.              |
| `openApp(packageName)`                 | Launch the app.                                         |
| `openAppSettings(packageName)`         | Open Android app settings.                              |
| `uninstallApp(packageName)`            | Open the uninstall confirmation screen.                 |

## Android setup

Android limits app queries to visible packages. The plugin declares visibility for
launcher apps. Add any of the declarations below inside `<manifest>` and outside
`<application>` in `android/app/src/main/AndroidManifest.xml`.

Declare each additional package you need to query.

```xml
<queries>
    <package android:name="com.example.service" />
</queries>
```

Add `QUERY_ALL_PACKAGES` when your app needs broader visibility and meets Google
Play’s eligibility requirements. [Android package visibility](https://developer.android.com/training/package-visibility/declaring) · [Google Play policy](https://support.google.com/googleplay/android-developer/answer/10158779?hl=en)

```xml
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
```

Apps targeting Android 9 (API 28) or later require `REQUEST_DELETE_PACKAGES` to
open the uninstall confirmation screen with `uninstallApp()`. [Android permission reference](https://developer.android.com/reference/android/Manifest.permission#REQUEST_DELETE_PACKAGES)

```xml
<uses-permission android:name="android.permission.REQUEST_DELETE_PACKAGES" />
```

## Community Standards

[Issues](https://github.com/okmsbun/flutter_device_apps_android/issues) · [Contributing](CONTRIBUTING.md) · [Code of Conduct](CODE_OF_CONDUCT.md) · [Security Policy](SECURITY.md)

## License

[MIT](LICENSE) © 2026 okmsbun
