# flutter\_device\_apps\_android

<p align="center">
<a href="https://pub.dev/packages/flutter_device_apps_android"><img src="https://img.shields.io/pub/v/flutter_device_apps_android.svg?color=0175C2" alt="Pub"></a>
<a href="https://github.com/okmsbun/flutter_device_apps_android"><img src="https://img.shields.io/github/stars/okmsbun/flutter_device_apps_android.svg?style=flat&logo=github&colorB=deeppink&label=stars" alt="Star on Github"></a>
<a href="https://flutter.dev"><img src="https://img.shields.io/badge/flutter-website-deepskyblue.svg" alt="Flutter Website"></a>
<a href="https://opensource.org/licenses/MIT"><img src="https://img.shields.io/badge/license-MIT-purple.svg" alt="License: MIT"></a>
<a href="https://pub.dev/packages/flutter_device_apps"><img src="https://img.shields.io/badge/umbrella-package-orange.svg" alt="Umbrella Package"></a>
</p>

---

Android implementation of the [flutter\_device\_apps](https://pub.dev/packages/flutter_device_apps) federated plugin.

This package provides the Android-specific code for `flutter_device_apps`. It should **not** be used directly in apps. Instead, depend on the umbrella package:

```yaml
dependencies:
  flutter_device_apps: latest_version
```

The umbrella package will include this implementation transitively.

---

## ✨ Features (on Android)

* **List installed apps** (`listApps`) - Get all installed applications with metadata
* **Get app details** (`getApp`) - Retrieve information for a specific app by package name
* **Open apps** (`openApp`) - Launch applications by package name
* **Open app settings** (`openAppSettings`) - Open system app settings screen
* **Uninstall apps** (`uninstallApp`) - Opens system uninstall screen
* **Get installer store** (`getInstallerStore`) - Get information about which store installed the app
* **Stream app changes** (`appChanges`) - Monitor install, uninstall, update events
* **Control monitoring** (`startAppChangeStream`, `stopAppChangeStream`) - Start/stop app change monitoring

---

## 🛠 For developers

This package registers itself as the Android implementation of `flutter_device_apps` using the federated plugin system.

If you are writing a Flutter app, use [`flutter_device_apps`](https://pub.dev/packages/flutter_device_apps) instead.

## 📋 Required Android Permissions

For uninstall functionality, add this to your app's `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.REQUEST_DELETE_PACKAGES" />
```

---

## 📄 License

MIT © 2025 okmsbun
