import 'package:flutter_device_apps_android/android_app_types.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('parses installation source metadata from a platform map', () {
    final AndroidAppInstallSourceInfo info = AndroidAppInstallSourceInfo.fromMap({
      'installingPackageName': 'com.android.vending',
      'initiatingPackageName': 'com.example.installer',
      'originatingPackageName': 'com.example.browser',
      'packageSource': 2,
      'updateOwnerPackageName': 'com.example.owner',
    });

    expect(info.installingPackageName, 'com.android.vending');
    expect(info.initiatingPackageName, 'com.example.installer');
    expect(info.originatingPackageName, 'com.example.browser');
    expect(info.packageSource, 2);
    expect(info.updateOwnerPackageName, 'com.example.owner');
  });

  test('preserves unavailable fields in legacy and empty responses', () {
    final AndroidAppInstallSourceInfo legacy = AndroidAppInstallSourceInfo.fromMap({
      'installingPackageName': 'com.android.vending',
    });
    final AndroidAppInstallSourceInfo unknown = AndroidAppInstallSourceInfo.fromMap({});

    expect(legacy.installingPackageName, 'com.android.vending');
    expect(legacy.initiatingPackageName, isNull);
    expect(legacy.originatingPackageName, isNull);
    expect(legacy.packageSource, isNull);
    expect(legacy.updateOwnerPackageName, isNull);
    expect(unknown.installingPackageName, isNull);
    expect(unknown.packageSource, isNull);
  });

  test('distinguishes unspecified source from unavailable or invalid source', () {
    expect(AndroidAppInstallSourceInfo.fromMap({'packageSource': 0}).packageSource, 0);
    expect(AndroidAppInstallSourceInfo.fromMap({'packageSource': '2'}).packageSource, 2);
    expect(AndroidAppInstallSourceInfo.fromMap({'packageSource': 'invalid'}).packageSource, isNull);
    expect(AndroidAppInstallSourceInfo.fromMap({'packageSource': null}).packageSource, isNull);
  });
}
