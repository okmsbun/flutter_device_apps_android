import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_device_apps_platform_interface/flutter_device_apps_app_change_event.dart';
import 'package:flutter_device_apps_platform_interface/flutter_device_apps_platform_interface.dart';

/// Android implementation of [FlutterDeviceAppsPlatform].
class FlutterDeviceAppsAndroid extends FlutterDeviceAppsPlatform {
  /// Constructs a [FlutterDeviceAppsAndroid].
  FlutterDeviceAppsAndroid() : super();

  /// Registers this class as the platform implementation for Android.
  static void registerWith() {
    FlutterDeviceAppsPlatform.instance = FlutterDeviceAppsAndroid();
  }

  static const MethodChannel _mch = MethodChannel('flutter_device_apps/methods');
  static const EventChannel _ech = EventChannel('flutter_device_apps/app_changes');

  late final StreamController<AppChangeEvent> _controller =
      StreamController<AppChangeEvent>.broadcast(
    onListen: _onListen,
    onCancel: _onCancel,
  );

  Future<void> _onListen() async {
    await _mch.invokeMethod('startAppChangeStream');
    _ech.receiveBroadcastStream().listen(
      (event) {
        _controller.add(AppChangeEvent.fromMap(Map<String, Object?>.from(event as Map)));
      },
      onError: (error) => _controller.addError(error),
      onDone: () => _controller.close(),
    );
  }

  Future<void> _onCancel() async {
    await _mch.invokeMethod('stopAppChangeStream');
  }

  @override
  Stream<AppChangeEvent> get appChanges => _controller.stream;

  @override
  Future<List<AppInfo>> listApps({
    bool includeSystem = false,
    bool onlyLaunchable = true,
    bool includeIcons = false,
  }) async {
    final List raw = await _mch.invokeMethod('listApps', {
      'includeSystem': includeSystem,
      'onlyLaunchable': onlyLaunchable,
      'includeIcons': includeIcons,
    });
    return raw.cast<Map>().map((m) => AppInfo.fromMap(Map<String, Object?>.from(m))).toList();
  }

  @override
  Future<AppInfo?> getApp(String packageName, {bool includeIcon = false}) async {
    final Map? m = await _mch.invokeMethod('getApp', {
      'packageName': packageName,
      'includeIcon': includeIcon,
    });
    return m == null ? null : AppInfo.fromMap(Map<String, Object?>.from(m));
  }

  @override
  Future<List<String>?> getRequestedPermissions(String packageName) async {
    final List<dynamic>? raw = await _mch.invokeMethod<List<dynamic>>(
      'getRequestedPermissions',
      {
        'packageName': packageName,
      },
    );
    return raw?.map((e) => e.toString()).toList();
  }

  @override
  Future<bool> openApp(String packageName) async {
    final bool ok = await _mch.invokeMethod('openApp', {'packageName': packageName});
    return ok;
  }

  @override
  Future<bool> openAppSettings(String packageName) async {
    final bool ok = await _mch.invokeMethod('openAppSettings', {
      'packageName': packageName,
    });
    return ok;
  }

  @override
  Future<bool> uninstallApp(String packageName) async {
    final bool? ok = await _mch.invokeMethod<bool>('uninstallApp', {
      'packageName': packageName,
    });
    return ok ?? false;
  }

  @override
  Future<String?> getInstallerStore(String packageName) async {
    final String? store = await _mch.invokeMethod('getInstallerStore', {
      'packageName': packageName,
    });
    return store;
  }
}
