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

  Stream<AppChangeEvent>? _appChanges;
  @override
  Stream<AppChangeEvent> get appChanges => _appChanges ??= _ech.receiveBroadcastStream().map((e) {
        return AppChangeEvent.fromMap(Map<String, Object?>.from(e as Map));
      });

  @override
  Future<void> startAppChangeStream() => _mch.invokeMethod('startAppChangeStream');

  @override
  Future<void> stopAppChangeStream() => _mch.invokeMethod('stopAppChangeStream');

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
}
