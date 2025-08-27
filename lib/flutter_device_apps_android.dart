
import 'flutter_device_apps_android_platform_interface.dart';

class FlutterDeviceAppsAndroid {
  Future<String?> getPlatformVersion() {
    return FlutterDeviceAppsAndroidPlatform.instance.getPlatformVersion();
  }
}
