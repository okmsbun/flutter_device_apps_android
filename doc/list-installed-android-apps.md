# List installed Android apps in Flutter

Use `FlutterDeviceAppsAndroid.listApps()` to list visible installed apps and
display their names, package names, and icons.
Requires Android API 24+, Flutter 3.44+, and Dart 3.12+.

## Install the plugin

```bash
flutter pub add flutter_device_apps_android
```

## Display installed apps with icons

Add `material_ui` for the example UI, then put the code in `lib/main.dart` to
show visible user apps that have a launcher entry.

```bash
flutter pub add material_ui
```

```dart
import 'package:flutter_device_apps_android/flutter_device_apps_android.dart';
import 'package:material_ui/material_ui.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final apps = await FlutterDeviceAppsAndroid.listApps(includeIcons: true);

  runApp(
    MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('Installed Android apps')),
        body: ListView.builder(
          itemCount: apps.length,
          itemBuilder: (context, index) {
            final app = apps[index];
            final icon = app.iconBytes;
            return ListTile(
              leading: icon == null
                  ? const Icon(Icons.android)
                  : Image.memory(icon, width: 40, height: 40),
              title: Text(app.appName ?? app.packageName ?? 'Unknown app'),
              subtitle: Text(app.packageName ?? ''),
            );
          },
        ),
      ),
    ),
  );
}
```

`ensureInitialized()` is needed here because the plugin is called before
`runApp()`. The binding is already available in `initState()` and UI callbacks.
Leave `includeIcons` off when you only need metadata.

## Include system apps or filter by package name

Set `includeSystem: true` to include visible system apps. Set
`onlyLaunchable: false` to include visible packages without a launcher entry.
To narrow the list, pass `packageNamePrefix` or `packageNameContains`. Both
filters are case-sensitive and must match when supplied together.

## Check or launch an app by package name

```dart
Future<bool> openWhatsApp() async {
  const packageName = 'com.whatsapp';
  if (!await FlutterDeviceAppsAndroid.isAppLaunchable(packageName)) {
    return false;
  }
  return FlutterDeviceAppsAndroid.openApp(packageName);
}
```

Use `isAppInstalled` to check for a visible package, including disabled apps.
Use `getApp` for metadata about one exact package name or `getApps` for several.

## Android package visibility

Android 11+ filters package queries from apps that target API 30 or later. The
plugin declares visibility for launcher apps. To query an additional package,
add a declaration inside `<manifest>` and outside `<application>` in
`android/app/src/main/AndroidManifest.xml`.

```xml
<queries>
    <package android:name="com.whatsapp" />
</queries>
```

Hidden packages can be omitted from lists, and `isAppInstalled()` can return
`false` even when the app is installed. Broader visibility through
`QUERY_ALL_PACKAGES` is subject to Google Play policy. See the
[Android package visibility guide](https://developer.android.com/training/package-visibility/declaring)
and [Android setup notes](../README.md#android-setup).

## Detect technologies in an installed APK

Pass a visible package name to `detectAppTechnologies` to scan the base and
installed split APKs. The result groups matches into `frameworks`,
`programmingLanguages`, `uiToolkits`, `gameEngines`, `runtimes`, and `libraries`.
A framework such as Flutter can also appear under UI toolkits.

Languages require a direct packaged code marker. For example, Dart is reported
when the Flutter assets contain a Dart Kernel header. A Kotlin standard library
match appears under libraries. A category can be empty, and an invisible
package or an unreadable APK throws.

See the [supported technologies](../README.md#detect-packaged-app-technologies).
Existing `detectAppFrameworks` calls retain their flat results and emit a
deprecation warning on analysis.

See the [Android API reference](https://pub.dev/documentation/flutter_device_apps_android/latest/flutter_device_apps_android/)
or the [Flutter guide for Android and macOS](https://github.com/okmsbun/flutter_device_apps/blob/HEAD/doc/list-installed-android-apps.md).
