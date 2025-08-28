import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'flutter_device_apps_android_method_channel.dart';

/// The interface that implementations of flutter_device_apps_android must implement.
///
/// Platform implementations should extend this class rather than implementing it as `FlutterDeviceAppsAndroidPlatform`.
/// Extending this class (using `extends`) ensures that the subclass will get the default
/// implementation, while platform implementations that `implements` this interface will be
/// broken by newly added [FlutterDeviceAppsAndroidPlatform] methods.
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

  /// Returns the platform version.
  ///
  /// Throws an [UnimplementedError] when the method is not implemented on the platform.
  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
