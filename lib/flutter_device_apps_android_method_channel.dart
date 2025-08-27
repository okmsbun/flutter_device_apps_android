import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'flutter_device_apps_android_platform_interface.dart';

/// An implementation of [FlutterDeviceAppsAndroidPlatform] that uses method channels.
class MethodChannelFlutterDeviceAppsAndroid extends FlutterDeviceAppsAndroidPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('flutter_device_apps_android');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
