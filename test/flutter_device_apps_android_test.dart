import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_device_apps_android/flutter_device_apps_android.dart';
import 'package:flutter_device_apps_android/flutter_device_apps_android_platform_interface.dart';
import 'package:flutter_device_apps_android/flutter_device_apps_android_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockFlutterDeviceAppsAndroidPlatform
    with MockPlatformInterfaceMixin
    implements FlutterDeviceAppsAndroidPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final FlutterDeviceAppsAndroidPlatform initialPlatform = FlutterDeviceAppsAndroidPlatform.instance;

  test('$MethodChannelFlutterDeviceAppsAndroid is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelFlutterDeviceAppsAndroid>());
  });

  test('getPlatformVersion', () async {
    FlutterDeviceAppsAndroid flutterDeviceAppsAndroidPlugin = FlutterDeviceAppsAndroid();
    MockFlutterDeviceAppsAndroidPlatform fakePlatform = MockFlutterDeviceAppsAndroidPlatform();
    FlutterDeviceAppsAndroidPlatform.instance = fakePlatform;

    expect(await flutterDeviceAppsAndroidPlugin.getPlatformVersion(), '42');
  });
}
