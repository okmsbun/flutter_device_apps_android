import 'dart:typed_data';

import 'package:flutter_device_apps_android/android_app_types.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('AndroidAppInfo', () {
    group('constructor', () {
      test('creates instance with all null values', () {
        const appInfo = AndroidAppInfo();

        expect(appInfo.packageName, isNull);
        expect(appInfo.appName, isNull);
        expect(appInfo.versionName, isNull);
        expect(appInfo.versionCode, isNull);
        expect(appInfo.uid, isNull);
        expect(appInfo.apkPath, isNull);
        expect(appInfo.apkSizeBytes, isNull);
        expect(appInfo.dataPath, isNull);
        expect(appInfo.isOnExternalStorage, isNull);
        expect(appInfo.firstInstallTime, isNull);
        expect(appInfo.lastUpdateTime, isNull);
        expect(appInfo.isSystem, isNull);
        expect(appInfo.iconBytes, isNull);
        expect(appInfo.category, isNull);
        expect(appInfo.targetSdkVersion, isNull);
        expect(appInfo.minSdkVersion, isNull);
        expect(appInfo.enabled, isNull);
        expect(appInfo.processName, isNull);
        expect(appInfo.installLocation, isNull);
      });

      test('creates instance with all values', () {
        final iconBytes = Uint8List.fromList([1, 2, 3]);
        final firstInstallTime = DateTime(2024, 1, 15);
        final lastUpdateTime = DateTime(2024, 6, 20);

        final appInfo = AndroidAppInfo(
          packageName: 'com.example.app',
          appName: 'Example App',
          versionName: '1.0.0',
          versionCode: 10,
          uid: 10123,
          apkPath: '/data/app/com.example.app/base.apk',
          apkSizeBytes: 12345678,
          dataPath: '/data/user/0/com.example.app',
          isOnExternalStorage: false,
          firstInstallTime: firstInstallTime,
          lastUpdateTime: lastUpdateTime,
          isSystem: false,
          iconBytes: iconBytes,
          category: 3,
          targetSdkVersion: 33,
          minSdkVersion: 21,
          enabled: true,
          processName: 'com.example.app',
          installLocation: 0,
        );

        expect(appInfo.packageName, 'com.example.app');
        expect(appInfo.appName, 'Example App');
        expect(appInfo.versionName, '1.0.0');
        expect(appInfo.versionCode, 10);
        expect(appInfo.uid, 10123);
        expect(appInfo.apkPath, '/data/app/com.example.app/base.apk');
        expect(appInfo.apkSizeBytes, 12345678);
        expect(appInfo.dataPath, '/data/user/0/com.example.app');
        expect(appInfo.isOnExternalStorage, false);
        expect(appInfo.firstInstallTime, firstInstallTime);
        expect(appInfo.lastUpdateTime, lastUpdateTime);
        expect(appInfo.isSystem, false);
        expect(appInfo.iconBytes, iconBytes);
        expect(appInfo.category, 3);
        expect(appInfo.targetSdkVersion, 33);
        expect(appInfo.minSdkVersion, 21);
        expect(appInfo.enabled, true);
        expect(appInfo.processName, 'com.example.app');
        expect(appInfo.installLocation, 0);
      });
    });

    group('fromMap', () {
      test('parses empty map', () {
        final appInfo = AndroidAppInfo.fromMap({});

        expect(appInfo.packageName, isNull);
        expect(appInfo.appName, isNull);
        expect(appInfo.versionName, isNull);
        expect(appInfo.versionCode, isNull);
        expect(appInfo.uid, isNull);
        expect(appInfo.apkPath, isNull);
        expect(appInfo.apkSizeBytes, isNull);
        expect(appInfo.dataPath, isNull);
        expect(appInfo.isOnExternalStorage, isNull);
        expect(appInfo.firstInstallTime, isNull);
        expect(appInfo.lastUpdateTime, isNull);
        expect(appInfo.isSystem, isNull);
        expect(appInfo.iconBytes, isNull);
        expect(appInfo.category, isNull);
        expect(appInfo.targetSdkVersion, isNull);
        expect(appInfo.minSdkVersion, isNull);
        expect(appInfo.enabled, isNull);
        expect(appInfo.processName, isNull);
        expect(appInfo.installLocation, isNull);
      });

      test('parses all string fields correctly', () {
        final map = <String, Object?>{
          'packageName': 'com.example.app',
          'appName': 'Example App',
          'versionName': '2.1.0',
          'apkPath': '/data/app/com.example.app/base.apk',
          'dataPath': '/data/user/0/com.example.app',
          'processName': 'com.example.process',
        };

        final appInfo = AndroidAppInfo.fromMap(map);

        expect(appInfo.packageName, 'com.example.app');
        expect(appInfo.appName, 'Example App');
        expect(appInfo.versionName, '2.1.0');
        expect(appInfo.apkPath, '/data/app/com.example.app/base.apk');
        expect(appInfo.dataPath, '/data/user/0/com.example.app');
        expect(appInfo.processName, 'com.example.process');
      });

      test('parses integer fields from int values', () {
        final map = <String, Object?>{
          'versionCode': 42,
          'uid': 10123,
          'apkSizeBytes': 4096,
          'category': 5,
          'targetSdkVersion': 34,
          'minSdkVersion': 23,
          'installLocation': 1,
        };

        final appInfo = AndroidAppInfo.fromMap(map);

        expect(appInfo.versionCode, 42);
        expect(appInfo.uid, 10123);
        expect(appInfo.apkSizeBytes, 4096);
        expect(appInfo.category, 5);
        expect(appInfo.targetSdkVersion, 34);
        expect(appInfo.minSdkVersion, 23);
        expect(appInfo.installLocation, 1);
      });

      test('parses integer fields from string values', () {
        final map = <String, Object?>{
          'versionCode': '42',
          'uid': '10123',
          'apkSizeBytes': '4096',
          'category': '5',
          'targetSdkVersion': '34',
          'minSdkVersion': '23',
          'installLocation': '1',
        };

        final appInfo = AndroidAppInfo.fromMap(map);

        expect(appInfo.versionCode, 42);
        expect(appInfo.uid, 10123);
        expect(appInfo.apkSizeBytes, 4096);
        expect(appInfo.category, 5);
        expect(appInfo.targetSdkVersion, 34);
        expect(appInfo.minSdkVersion, 23);
        expect(appInfo.installLocation, 1);
      });

      test('handles invalid integer strings gracefully', () {
        final map = <String, Object?>{
          'versionCode': 'invalid',
          'uid': 'invalid_uid',
          'apkSizeBytes': 'invalid_size',
          'category': 'not_a_number',
          'targetSdkVersion': '',
        };

        final appInfo = AndroidAppInfo.fromMap(map);

        expect(appInfo.versionCode, isNull);
        expect(appInfo.uid, isNull);
        expect(appInfo.apkSizeBytes, isNull);
        expect(appInfo.category, isNull);
        expect(appInfo.targetSdkVersion, isNull);
      });

      test('parses boolean fields from bool values', () {
        final map = <String, Object?>{
          'isSystem': true,
          'enabled': false,
          'isOnExternalStorage': true,
        };

        final appInfo = AndroidAppInfo.fromMap(map);

        expect(appInfo.isSystem, true);
        expect(appInfo.enabled, false);
        expect(appInfo.isOnExternalStorage, true);
      });

      test('parses boolean fields from string values', () {
        final map = <String, Object?>{
          'isSystem': 'true',
          'enabled': 'false',
          'isOnExternalStorage': 'true',
        };

        final appInfo = AndroidAppInfo.fromMap(map);

        expect(appInfo.isSystem, true);
        expect(appInfo.enabled, false);
        expect(appInfo.isOnExternalStorage, true);
      });

      test('handles invalid boolean strings gracefully', () {
        final map = <String, Object?>{
          'isSystem': 'yes',
          'enabled': 'no',
          'isOnExternalStorage': 'unknown',
        };

        final appInfo = AndroidAppInfo.fromMap(map);

        expect(appInfo.isSystem, isNull);
        expect(appInfo.enabled, isNull);
        expect(appInfo.isOnExternalStorage, isNull);
      });

      test('parses DateTime fields from milliseconds', () {
        final int firstInstallMs = DateTime(2024, 1, 15).millisecondsSinceEpoch;
        final int lastUpdateMs = DateTime(2024, 6, 20).millisecondsSinceEpoch;

        final map = <String, Object?>{
          'firstInstallTime': firstInstallMs,
          'lastUpdateTime': lastUpdateMs,
        };

        final appInfo = AndroidAppInfo.fromMap(map);

        expect(appInfo.firstInstallTime, DateTime(2024, 1, 15));
        expect(appInfo.lastUpdateTime, DateTime(2024, 6, 20));
      });

      test('parses DateTime fields from string milliseconds', () {
        final int firstInstallMs = DateTime(2024, 3, 10).millisecondsSinceEpoch;
        final int lastUpdateMs = DateTime(2024, 8, 5).millisecondsSinceEpoch;

        final map = <String, Object?>{
          'firstInstallTime': firstInstallMs.toString(),
          'lastUpdateTime': lastUpdateMs.toString(),
        };

        final appInfo = AndroidAppInfo.fromMap(map);

        expect(appInfo.firstInstallTime, DateTime(2024, 3, 10));
        expect(appInfo.lastUpdateTime, DateTime(2024, 8, 5));
      });

      test('handles invalid DateTime strings gracefully', () {
        final map = <String, Object?>{'firstInstallTime': 'not_a_timestamp', 'lastUpdateTime': ''};

        final appInfo = AndroidAppInfo.fromMap(map);

        expect(appInfo.firstInstallTime, isNull);
        expect(appInfo.lastUpdateTime, isNull);
      });

      test('parses iconBytes from List<int>', () {
        final iconData = <int>[255, 216, 255, 224, 0, 16];
        final map = <String, Object?>{'iconBytes': iconData};

        final appInfo = AndroidAppInfo.fromMap(map);

        expect(appInfo.iconBytes, isA<Uint8List>());
        expect(appInfo.iconBytes, [255, 216, 255, 224, 0, 16]);
      });

      test('handles non-List iconBytes gracefully', () {
        final map = <String, Object?>{'iconBytes': 'not_a_list'};

        final appInfo = AndroidAppInfo.fromMap(map);

        expect(appInfo.iconBytes, isNull);
      });

      test('handles null iconBytes gracefully', () {
        final map = <String, Object?>{'iconBytes': null};

        final appInfo = AndroidAppInfo.fromMap(map);

        expect(appInfo.iconBytes, isNull);
      });

      test('parses complete map with all fields', () {
        final int firstInstallMs = DateTime(2024).millisecondsSinceEpoch;
        final int lastUpdateMs = DateTime(2024, 12, 31).millisecondsSinceEpoch;
        final iconData = <int>[1, 2, 3, 4, 5];

        final map = <String, Object?>{
          'packageName': 'com.test.fullapp',
          'appName': 'Full Test App',
          'versionName': '3.2.1',
          'versionCode': 321,
          'uid': 10199,
          'apkPath': '/data/app/com.test.fullapp/base.apk',
          'apkSizeBytes': 555000,
          'dataPath': '/data/user/0/com.test.fullapp',
          'isOnExternalStorage': false,
          'firstInstallTime': firstInstallMs,
          'lastUpdateTime': lastUpdateMs,
          'isSystem': false,
          'iconBytes': iconData,
          'category': 7,
          'targetSdkVersion': 35,
          'minSdkVersion': 24,
          'enabled': true,
          'processName': 'com.test.fullapp.process',
          'installLocation': 2,
        };

        final appInfo = AndroidAppInfo.fromMap(map);

        expect(appInfo.packageName, 'com.test.fullapp');
        expect(appInfo.appName, 'Full Test App');
        expect(appInfo.versionName, '3.2.1');
        expect(appInfo.versionCode, 321);
        expect(appInfo.uid, 10199);
        expect(appInfo.apkPath, '/data/app/com.test.fullapp/base.apk');
        expect(appInfo.apkSizeBytes, 555000);
        expect(appInfo.dataPath, '/data/user/0/com.test.fullapp');
        expect(appInfo.isOnExternalStorage, false);
        expect(appInfo.firstInstallTime, DateTime(2024));
        expect(appInfo.lastUpdateTime, DateTime(2024, 12, 31));
        expect(appInfo.isSystem, false);
        expect(appInfo.iconBytes, isA<Uint8List>());
        expect(appInfo.iconBytes!.length, 5);
        expect(appInfo.category, 7);
        expect(appInfo.targetSdkVersion, 35);
        expect(appInfo.minSdkVersion, 24);
        expect(appInfo.enabled, true);
        expect(appInfo.processName, 'com.test.fullapp.process');
        expect(appInfo.installLocation, 2);
      });

      test('handles mixed valid and invalid values', () {
        final map = <String, Object?>{
          'packageName': 'com.example.mixed',
          'versionCode': 'invalid',
          'isSystem': 'true',
          'enabled': 'maybe',
          'category': 10,
        };

        final appInfo = AndroidAppInfo.fromMap(map);

        expect(appInfo.packageName, 'com.example.mixed');
        expect(appInfo.versionCode, isNull);
        expect(appInfo.isSystem, true);
        expect(appInfo.enabled, isNull);
        expect(appInfo.category, 10);
      });
    });
  });
}
