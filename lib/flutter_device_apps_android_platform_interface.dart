import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'flutter_device_apps_android_method_channel.dart';

abstract class FlutterDeviceAppsAndroidPlatform extends PlatformInterface {
  /// Constructs a FlutterDeviceAppsAndroidPlatform.
  FlutterDeviceAppsAndroidPlatform() : super(token: _token);

  static final Object _token = Object();

  static FlutterDeviceAppsAndroidPlatform _instance = MethodChannelFlutterDeviceAppsAndroid();

  /// The default instance of [FlutterDeviceAppsAndroidPlatform] to use.
  ///
  /// Defaults to [MethodChannelFlutterDeviceAppsAndroid].
  static FlutterDeviceAppsAndroidPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [FlutterDeviceAppsAndroidPlatform] when
  /// they register themselves.
  static set instance(FlutterDeviceAppsAndroidPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
