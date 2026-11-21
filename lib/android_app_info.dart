import 'dart:typed_data';

/// Frameworks and runtime components identified by known Android APK entries.
///
/// A match indicates a packaged component, not which technology renders every
/// screen or whether that component is currently in use.
enum AndroidAppFramework {
  /// Flutter engine libraries.
  flutter,

  /// React Native libraries, including the legacy JNI library.
  reactNative,

  /// Unity engine libraries.
  unity,

  /// Unreal Engine libraries, including the legacy UE4 library.
  unrealEngine,

  /// Godot engine libraries.
  godot,

  /// .NET Android runtime glue; does not distinguish MAUI from Xamarin.
  dotnetAndroid,

  /// Jetpack Compose UI or runtime module metadata.
  jetpackCompose,

  /// Kotlin standard library metadata, which can come from a dependency.
  kotlin,

  /// AndroidX AppCompat library metadata.
  androidxAppCompat,

  /// Capacitor Android configuration asset.
  capacitor,

  /// Apache Cordova Android JavaScript runtime asset.
  cordova,

  /// Qt 5 or Qt 6 Core native library.
  qt,
}

/// Information about an installed app on the device.
///
/// Contains metadata like package name, version, install times, and optional icon data.
class AndroidAppInfo {
  /// Creates an [AndroidAppInfo] with the specified properties.
  ///
  /// All parameters are optional and represent various pieces of app metadata.
  const AndroidAppInfo({
    this.packageName,
    this.appName,
    this.versionName,
    this.versionCode,
    this.uid,
    this.apkPath,
    this.apkSizeBytes,
    this.dataPath,
    this.isOnExternalStorage,
    this.firstInstallTime,
    this.lastUpdateTime,
    this.isSystem,
    this.iconBytes,
    this.category,
    this.targetSdkVersion,
    this.minSdkVersion,
    this.enabled,
    this.processName,
    this.installLocation,
  });

  /// Creates an [AndroidAppInfo] from a map of key-value pairs.
  ///
  /// The map typically comes from platform-specific implementations.
  /// Handles type conversion and null safety for various data types.
  factory AndroidAppInfo.fromMap(Map<String, Object?> m) {
    final int? firstInstallTime = m['firstInstallTime'] != null
        ? int.tryParse(m['firstInstallTime']!.toString())
        : null;
    final DateTime? firstInstallTimeDate = firstInstallTime != null
        ? DateTime.fromMillisecondsSinceEpoch(firstInstallTime)
        : null;

    final int? lastUpdateTime = m['lastUpdateTime'] != null
        ? int.tryParse(m['lastUpdateTime']!.toString())
        : null;
    final DateTime? lastUpdateTimeDate = lastUpdateTime != null
        ? DateTime.fromMillisecondsSinceEpoch(lastUpdateTime)
        : null;

    final int? category = m['category'] != null ? int.tryParse(m['category']!.toString()) : null;
    final int? targetSdkVersion = m['targetSdkVersion'] != null
        ? int.tryParse(m['targetSdkVersion']!.toString())
        : null;
    final int? minSdkVersion = m['minSdkVersion'] != null
        ? int.tryParse(m['minSdkVersion']!.toString())
        : null;
    final bool? enabled = m['enabled'] != null ? bool.tryParse(m['enabled']!.toString()) : null;
    final int? installLocation = m['installLocation'] != null
        ? int.tryParse(m['installLocation']!.toString())
        : null;

    return AndroidAppInfo(
      packageName: m['packageName']?.toString(),
      appName: m['appName']?.toString(),
      versionName: m['versionName']?.toString(),
      versionCode: m['versionCode'] != null ? int.tryParse(m['versionCode']!.toString()) : null,
      uid: m['uid'] != null ? int.tryParse(m['uid']!.toString()) : null,
      apkPath: m['apkPath']?.toString(),
      apkSizeBytes: m['apkSizeBytes'] != null ? int.tryParse(m['apkSizeBytes']!.toString()) : null,
      dataPath: m['dataPath']?.toString(),
      isOnExternalStorage: m['isOnExternalStorage'] != null
          ? bool.tryParse(m['isOnExternalStorage']!.toString())
          : null,
      firstInstallTime: firstInstallTimeDate,
      lastUpdateTime: lastUpdateTimeDate,
      isSystem: m['isSystem'] != null ? bool.tryParse(m['isSystem']!.toString()) : null,
      iconBytes: m['iconBytes'] is List<int>
          ? Uint8List.fromList(m['iconBytes']! as List<int>)
          : null,
      category: category,
      targetSdkVersion: targetSdkVersion,
      minSdkVersion: minSdkVersion,
      enabled: enabled,
      processName: m['processName']?.toString(),
      installLocation: installLocation,
    );
  }

  /// The unique package name identifier for the app (e.g., 'com.example.app').
  final String? packageName;

  /// The human-readable display name of the app.
  final String? appName;

  /// The version name as displayed to users (e.g., '1.0.0').
  final String? versionName;

  /// The internal version code used for version comparison.
  final int? versionCode;

  /// Linux/kernel-level UID assigned to the app on the device.
  ///
  /// This is not a globally unique or stable business identifier.
  final int? uid;

  /// Full path to the base APK file (Android ApplicationInfo.sourceDir).
  final String? apkPath;

  /// APK size in bytes (base APK + split APK files when present).
  ///
  /// Null when not available.
  final int? apkSizeBytes;

  /// Full path to the app's private data directory (Android ApplicationInfo.dataDir).
  final String? dataPath;

  /// Raw Android flag from ApplicationInfo.FLAG_EXTERNAL_STORAGE.
  final bool? isOnExternalStorage;

  /// The date and time when the app was first installed on the device.
  final DateTime? firstInstallTime;

  /// The date and time when the app was last updated.
  final DateTime? lastUpdateTime;

  /// Whether the app is a system app (preinstalled) or user-installed.
  final bool? isSystem;

  /// The app icon as raw bytes, if requested and available.
  final Uint8List? iconBytes;

  /// App category (Android ApplicationInfo.category, API 26+). Raw int from platform. Null when not set or API < 26.
  final int? category;

  /// Target SDK version (Android ApplicationInfo.targetSdkVersion).
  final int? targetSdkVersion;

  /// Min SDK version (Android ApplicationInfo.minSdkVersion).
  final int? minSdkVersion;

  /// Whether the app is enabled (Android ApplicationInfo.enabled).
  final bool? enabled;

  /// Process name (Android ApplicationInfo.processName).
  final String? processName;

  /// Install location (Android PackageInfo.installLocation).
  final int? installLocation;
}

/// Information about how an app was installed.
class AndroidAppInstallSourceInfo {
  /// Creates installation source information with optional platform metadata.
  const AndroidAppInstallSourceInfo({
    this.installingPackageName,
    this.initiatingPackageName,
    this.originatingPackageName,
    this.packageSource,
    this.updateOwnerPackageName,
  });

  /// Creates installation source information from a platform map.
  factory AndroidAppInstallSourceInfo.fromMap(Map<String, Object?> m) =>
      AndroidAppInstallSourceInfo(
        installingPackageName: m['installingPackageName']?.toString(),
        initiatingPackageName: m['initiatingPackageName']?.toString(),
        originatingPackageName: m['originatingPackageName']?.toString(),
        packageSource: m['packageSource'] != null
            ? int.tryParse(m['packageSource']!.toString())
            : null,
        updateOwnerPackageName: m['updateOwnerPackageName']?.toString(),
      );

  /// The installer of record, or null when unavailable.
  ///
  /// This value can be changed after installation. Null does not establish
  /// whether an app was sideloaded.
  final String? installingPackageName;

  /// The package that requested installation (Android API 30+).
  final String? initiatingPackageName;

  /// The package on whose behalf installation was requested (Android API 30+).
  ///
  /// Supplied by the installer and not verified by Android. Access is restricted
  /// by Android permissions, so this value is usually null for ordinary apps.
  final String? originatingPackageName;

  /// Raw Android PackageInstaller.PACKAGE_SOURCE_* value (API 33+).
  ///
  /// Null on older Android versions. Zero means the source is unspecified.
  final int? packageSource;

  /// The package owning updates (Android API 34+), or null when unavailable.
  final String? updateOwnerPackageName;
}

/// The legacy name for [AndroidAppFramework].
@Deprecated('Use AndroidAppFramework instead.')
typedef AppFramework = AndroidAppFramework;

/// The legacy name for [AndroidAppInfo].
@Deprecated('Use AndroidAppInfo instead.')
typedef AppInfo = AndroidAppInfo;

/// The legacy name for [AndroidAppInstallSourceInfo].
@Deprecated('Use AndroidAppInstallSourceInfo instead.')
typedef AppInstallSourceInfo = AndroidAppInstallSourceInfo;
