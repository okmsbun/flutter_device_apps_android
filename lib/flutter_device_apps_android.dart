import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'android_app_types.dart';

export 'android_app_types.dart';

/// Queries and manages installed Android apps through Flutter channels.
/// Android package visibility rules apply to app queries.
abstract final class FlutterDeviceAppsAndroid {
  /// Compatibility entry point for callers that previously registered Android.
  @Deprecated('Android app queries initialize automatically. Remove this call.')
  static void registerWith() {}

  static const MethodChannel _mch = MethodChannel('flutter_device_apps/methods');
  static const EventChannel _ech = EventChannel('flutter_device_apps/app_changes');
  static const String _unsupportedMessage = 'FlutterDeviceAppsAndroid only supports Android.';

  static bool get _isAndroid => !kIsWeb && defaultTargetPlatform == TargetPlatform.android;

  static Future<T?> _invokeMethod<T>(String method, [Object? arguments]) {
    if (!_isAndroid) return Future<T?>.error(UnsupportedError(_unsupportedMessage));
    return _mch.invokeMethod<T>(method, arguments);
  }

  static final StreamController<AndroidAppChangeEvent> _controller =
      StreamController<AndroidAppChangeEvent>.broadcast(onListen: _onListen, onCancel: _onCancel);

  static StreamSubscription<dynamic>? _eventSubscription;
  static Future<void>? _streamLifecycle;

  static Future<void> _onListen() => _queueStreamOperation(() async {
    if (!_controller.hasListener || _eventSubscription != null) return;
    await _invokeMethod<void>('startAppChangeStream');
    if (!_controller.hasListener) return;
    _eventSubscription = _ech.receiveBroadcastStream().listen(
      (event) {
        _controller.add(AndroidAppChangeEvent.fromMap(Map<String, Object?>.from(event as Map)));
      },
      onError: (error) => _controller.addError(error),
      onDone: _controller.close,
    );
  });

  static Future<void> _onCancel() => _queueStreamOperation(() async {
    await _eventSubscription?.cancel();
    _eventSubscription = null;
    await _invokeMethod<void>('stopAppChangeStream');
  });

  // Complete shutdown before starting a new subscription on the same channel.
  static Future<void> _queueStreamOperation(Future<void> Function() operation) {
    final Future<void> queued = (_streamLifecycle ?? Future<void>.value())
        .then((_) => operation())
        .catchError((Object error, StackTrace stackTrace) {
          if (!_controller.isClosed) _controller.addError(error, stackTrace);
        });
    _streamLifecycle = queued;
    return queued.whenComplete(() {
      if (identical(_streamLifecycle, queued)) _streamLifecycle = null;
    });
  }

  /// Watches app installs, updates, and removals while the stream has listeners.
  static Stream<AndroidAppChangeEvent> get appChanges => _isAndroid
      ? _controller.stream
      : Stream<AndroidAppChangeEvent>.error(UnsupportedError(_unsupportedMessage));

  /// Lists visible Android apps using optional case-sensitive package name filters.
  /// Empty filters are ignored. When both are provided, both must match.
  static Future<List<AndroidAppInfo>> listApps({
    bool includeSystem = false,
    bool onlyLaunchable = true,
    bool includeIcons = false,
    String? packageNamePrefix,
    String? packageNameContains,
  }) async {
    final List raw = await _invokeMethod('listApps', {
      'includeSystem': includeSystem,
      'onlyLaunchable': onlyLaunchable,
      'includeIcons': includeIcons,
      'packageNamePrefix': packageNamePrefix,
      'packageNameContains': packageNameContains,
    });
    return raw
        .cast<Map>()
        .map((m) => AndroidAppInfo.fromMap(Map<String, Object?>.from(m)))
        .toList();
  }

  /// Gets metadata for an exact package name, or null when it is unavailable.
  static Future<AndroidAppInfo?> getApp(String packageName, {bool includeIcon = false}) async {
    final Map? m = await _invokeMethod('getApp', {
      'packageName': packageName,
      'includeIcon': includeIcon,
    });
    return m == null ? null : AndroidAppInfo.fromMap(Map<String, Object?>.from(m));
  }

  /// Gets metadata for exact package names in one request.
  /// Returns each visible app once in input order and skips unavailable names.
  static Future<List<AndroidAppInfo>> getApps(
    List<String> packageNames, {
    bool includeIcons = false,
  }) async {
    final List raw = await _invokeMethod('getApps', {
      'packageNames': packageNames,
      'includeIcons': includeIcons,
    });
    return raw
        .cast<Map>()
        .map((m) => AndroidAppInfo.fromMap(Map<String, Object?>.from(m)))
        .toList();
  }

  /// Finds packaged technologies in the base and installed split APKs.
  ///
  /// A match can come from a dependency and appear in multiple categories.
  /// An unavailable package or unreadable APK throws.
  static Future<AndroidAppTechnologies> detectAppTechnologies(String packageName) async {
    final List<Object?>? raw = await _invokeMethod<List<Object?>>('detectAppTechnologies', {
      'packageName': packageName,
    });
    final Set<String> names = raw?.whereType<String>().toSet() ?? {};
    return AndroidAppTechnologies(
      AndroidAppTechnology.values.where((technology) => names.contains(technology.name)),
    );
  }

  /// Finds legacy framework components in the base and installed split APKs.
  /// A match can come from a transitive dependency. No match returns an empty
  /// list, and an unavailable package or unreadable APK throws.
  @Deprecated('Use detectAppTechnologies() for categorized results.')
  static Future<List<AndroidAppFramework>> detectAppFrameworks(String packageName) async {
    final List raw = await _invokeMethod('detectAppFrameworks', {'packageName': packageName});
    return raw.cast<String>().map(AndroidAppFramework.values.byName).toList();
  }

  /// Gets an app icon as PNG bytes, or null when it is unavailable.
  static Future<Uint8List?> getAppIcon(String packageName) =>
      _invokeMethod<Uint8List>('getAppIcon', {'packageName': packageName});

  /// Checks if an app is installed and visible, including disabled apps.
  static Future<bool> isAppInstalled(String packageName) async {
    final bool? installed = await _invokeMethod<bool>('isAppInstalled', {
      'packageName': packageName,
    });
    return installed ?? false;
  }

  /// Returns whether the app is a system app, or null when it is unavailable.
  static Future<bool?> isSystemApp(String packageName) =>
      _invokeMethod<bool>('isSystemApp', {'packageName': packageName});

  /// Returns whether the app is enabled, or null when it is unavailable.
  static Future<bool?> isAppEnabled(String packageName) =>
      _invokeMethod<bool>('isAppEnabled', {'packageName': packageName});

  /// Checks if Android provides a launch intent for the package.
  static Future<bool> isAppLaunchable(String packageName) async {
    final bool? launchable = await _invokeMethod<bool>('isAppLaunchable', {
      'packageName': packageName,
    });
    return launchable ?? false;
  }

  /// Gets an app's requested permission names, or null when unavailable.
  static Future<List<String>?> getRequestedPermissions(String packageName) async {
    final List<dynamic>? raw = await _invokeMethod<List<dynamic>>('getRequestedPermissions', {
      'packageName': packageName,
    });
    return raw?.map((e) => e.toString()).toList();
  }

  /// Launches an app and returns whether Android opened it.
  static Future<bool> openApp(String packageName) async {
    final bool ok = await _invokeMethod('openApp', {'packageName': packageName});
    return ok;
  }

  /// Opens an app's settings and returns whether Android opened them.
  static Future<bool> openAppSettings(String packageName) async {
    final bool ok = await _invokeMethod('openAppSettings', {'packageName': packageName});
    return ok;
  }

  /// Returns whether Android opened the app's uninstall confirmation screen.
  static Future<bool> uninstallApp(String packageName) async {
    final bool? ok = await _invokeMethod<bool>('uninstallApp', {'packageName': packageName});
    return ok ?? false;
  }

  /// Gets installer metadata. Android versions below API 30 provide the installer
  /// package name when available.
  static Future<AndroidAppInstallSourceInfo?> getInstallSourceInfo(String packageName) async {
    final Map? m = await _invokeMethod('getInstallSourceInfo', {'packageName': packageName});
    return m == null ? null : AndroidAppInstallSourceInfo.fromMap(Map<String, Object?>.from(m));
  }
}
