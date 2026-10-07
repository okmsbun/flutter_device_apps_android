import 'package:flutter/services.dart';
import 'package:flutter_device_apps_android/flutter_device_apps_android.dart';
import 'package:flutter_device_apps_platform_interface/flutter_device_apps_app_change_event.dart';
import 'package:flutter_device_apps_platform_interface/flutter_device_apps_platform_interface.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late FlutterDeviceAppsAndroid plugin;
  late List<MethodCall> methodCalls;

  setUp(() {
    plugin = FlutterDeviceAppsAndroid();
    methodCalls = [];

    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
      const MethodChannel('flutter_device_apps/methods'),
      (MethodCall methodCall) async {
        methodCalls.add(methodCall);
        return _handleMethodCall(methodCall);
      },
    );
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
      const MethodChannel('flutter_device_apps/methods'),
      null,
    );
  });

  group('registerWith', () {
    test('registers instance as platform implementation', () {
      FlutterDeviceAppsAndroid.registerWith();
      expect(FlutterDeviceAppsPlatform.instance, isA<FlutterDeviceAppsAndroid>());
    });
  });

  group('listApps', () {
    test('calls method channel with default parameters', () async {
      await plugin.listApps();

      expect(methodCalls, hasLength(1));
      expect(methodCalls.first.method, 'listApps');
      expect(methodCalls.first.arguments, {
        'includeSystem': false,
        'onlyLaunchable': true,
        'includeIcons': false,
      });
    });

    test('calls method channel with custom parameters', () async {
      await plugin.listApps(includeSystem: true, onlyLaunchable: false, includeIcons: true);

      expect(methodCalls, hasLength(1));
      expect(methodCalls.first.arguments, {
        'includeSystem': true,
        'onlyLaunchable': false,
        'includeIcons': true,
      });
    });

    test('returns list of AppInfo', () async {
      final List<AppInfo> apps = await plugin.listApps();

      expect(apps, hasLength(2));
      expect(apps[0].packageName, 'com.example.app1');
      expect(apps[0].uid, 10123);
      expect(apps[0].apkPath, '/data/app/com.example.app1/base.apk');
      expect(apps[0].apkSizeBytes, 12345678);
      expect(apps[0].dataPath, '/data/user/0/com.example.app1');
      expect(apps[0].isOnExternalStorage, false);
      expect(apps[1].packageName, 'com.example.app2');
      expect(apps[1].uid, 10124);
      expect(apps[1].apkPath, '/data/app/com.example.app2/base.apk');
      expect(apps[1].apkSizeBytes, 22334455);
      expect(apps[1].dataPath, '/data/user/0/com.example.app2');
      expect(apps[1].isOnExternalStorage, true);
    });

    test('returns empty list when no apps', () async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
        const MethodChannel('flutter_device_apps/methods'),
        (MethodCall methodCall) async => <Map>[],
      );

      final List<AppInfo> apps = await plugin.listApps();
      expect(apps, isEmpty);
    });
  });

  group('getApp', () {
    test('calls method channel with package name', () async {
      await plugin.getApp('com.example.app1');

      expect(methodCalls, hasLength(1));
      expect(methodCalls.first.method, 'getApp');
      expect(methodCalls.first.arguments, {
        'packageName': 'com.example.app1',
        'includeIcon': false,
      });
    });

    test('calls method channel with includeIcon true', () async {
      await plugin.getApp('com.example.app1', includeIcon: true);

      expect(methodCalls.first.arguments, {'packageName': 'com.example.app1', 'includeIcon': true});
    });

    test('returns AppInfo when app exists', () async {
      final AppInfo? app = await plugin.getApp('com.example.app1');

      expect(app, isNotNull);
      expect(app!.packageName, 'com.example.app1');
      expect(app.appName, 'App 1');
      expect(app.uid, 10123);
      expect(app.apkPath, '/data/app/com.example.app1/base.apk');
      expect(app.apkSizeBytes, 12345678);
      expect(app.dataPath, '/data/user/0/com.example.app1');
      expect(app.isOnExternalStorage, false);
    });

    test('returns null when app does not exist', () async {
      final AppInfo? app = await plugin.getApp('com.nonexistent.app');
      expect(app, isNull);
    });
  });

  group('getAppIcon', () {
    test('returns typed icon bytes without requesting metadata', () async {
      final Uint8List? bytes = await plugin.getAppIcon('com.example.app1');
      expect(bytes, Uint8List.fromList([137, 80, 78, 71, 13, 10, 26, 10]));
      expect(methodCalls, hasLength(1));
      expect(methodCalls.first.method, 'getAppIcon');
      expect(methodCalls.first.arguments, {'packageName': 'com.example.app1'});
    });

    test('returns null for an unavailable package', () async {
      expect(await plugin.getAppIcon('com.nonexistent.app'), isNull);
    });

    test('propagates icon loading errors', () async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
        const MethodChannel('flutter_device_apps/methods'),
        (MethodCall call) async => throw PlatformException(code: 'ERR_ICON'),
      );
      await expectLater(
        plugin.getAppIcon('com.example.app1'),
        throwsA(isA<PlatformException>().having((error) => error.code, 'code', 'ERR_ICON')),
      );
    });
  });

  group('package queries', () {
    test('checks installation directly without fetching metadata', () async {
      expect(await plugin.isAppInstalled('com.example.app1'), isTrue);
      expect(await plugin.isAppInstalled('com.nonexistent.app'), isFalse);
      expect(methodCalls.map((call) => call.method), ['isAppInstalled', 'isAppInstalled']);
      expect(methodCalls.first.arguments, {'packageName': 'com.example.app1'});
    });

    test('distinguishes system, user and unavailable packages', () async {
      expect(await plugin.isSystemApp('com.android.settings'), isTrue);
      expect(await plugin.isSystemApp('com.example.app1'), isFalse);
      expect(await plugin.isSystemApp('com.nonexistent.app'), isNull);
      expect(methodCalls.every((call) => call.method == 'isSystemApp'), isTrue);
      expect(methodCalls.first.arguments, {'packageName': 'com.android.settings'});
    });

    test('preserves query failures rather than reporting a missing package', () async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
        const MethodChannel('flutter_device_apps/methods'),
        (MethodCall call) async => throw PlatformException(code: 'ERR_QUERY'),
      );

      await expectLater(
        plugin.isAppInstalled('com.example.app1'),
        throwsA(isA<PlatformException>()),
      );
      await expectLater(plugin.isSystemApp('com.example.app1'), throwsA(isA<PlatformException>()));
    });
  });

  group('getRequestedPermissions', () {
    test('calls method channel with package name', () async {
      await plugin.getRequestedPermissions('com.example.app1');

      expect(methodCalls, hasLength(1));
      expect(methodCalls.first.method, 'getRequestedPermissions');
      expect(methodCalls.first.arguments, {'packageName': 'com.example.app1'});
    });

    test('returns list of permissions', () async {
      final List<String>? permissions = await plugin.getRequestedPermissions('com.example.app1');

      expect(permissions, isNotNull);
      expect(permissions, contains('android.permission.INTERNET'));
      expect(permissions, contains('android.permission.CAMERA'));
    });

    test('returns null for unknown package', () async {
      final List<String>? permissions = await plugin.getRequestedPermissions('com.nonexistent.app');
      expect(permissions, isNull);
    });
  });

  group('openApp', () {
    test('calls method channel with package name', () async {
      await plugin.openApp('com.example.app1');

      expect(methodCalls, hasLength(1));
      expect(methodCalls.first.method, 'openApp');
      expect(methodCalls.first.arguments, {'packageName': 'com.example.app1'});
    });

    test('returns true on success', () async {
      final bool result = await plugin.openApp('com.example.app1');
      expect(result, isTrue);
    });

    test('returns false on failure', () async {
      final bool result = await plugin.openApp('com.nonexistent.app');
      expect(result, isFalse);
    });
  });

  group('openAppSettings', () {
    test('calls method channel with package name', () async {
      await plugin.openAppSettings('com.example.app1');

      expect(methodCalls, hasLength(1));
      expect(methodCalls.first.method, 'openAppSettings');
      expect(methodCalls.first.arguments, {'packageName': 'com.example.app1'});
    });

    test('returns true on success', () async {
      final bool result = await plugin.openAppSettings('com.example.app1');
      expect(result, isTrue);
    });

    test('returns false on failure', () async {
      final bool result = await plugin.openAppSettings('com.nonexistent.app');
      expect(result, isFalse);
    });
  });

  group('uninstallApp', () {
    test('calls method channel with package name', () async {
      await plugin.uninstallApp('com.example.app1');

      expect(methodCalls, hasLength(1));
      expect(methodCalls.first.method, 'uninstallApp');
      expect(methodCalls.first.arguments, {'packageName': 'com.example.app1'});
    });

    test('returns true on success', () async {
      final bool result = await plugin.uninstallApp('com.example.app1');
      expect(result, isTrue);
    });

    test('returns false when method returns null', () async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
        const MethodChannel('flutter_device_apps/methods'),
        (MethodCall methodCall) async => null,
      );

      final bool result = await plugin.uninstallApp('com.example.app1');
      expect(result, isFalse);
    });
  });

  group('getInstallSourceInfo', () {
    test('queries source metadata through its own channel method', () async {
      final AppInstallSourceInfo? info = await plugin.getInstallSourceInfo('com.example.app1');
      expect(methodCalls, hasLength(1));
      expect(methodCalls.first.method, 'getInstallSourceInfo');
      expect(methodCalls.first.arguments, {'packageName': 'com.example.app1'});
      expect(info!.installingPackageName, 'com.android.vending');
      expect(info.initiatingPackageName, 'com.example.installer');
      expect(info.originatingPackageName, 'com.example.browser');
      expect(info.packageSource, 2);
      expect(info.updateOwnerPackageName, 'com.example.owner');
    });

    test('returns null for an unavailable package', () async {
      expect(await plugin.getInstallSourceInfo('com.nonexistent.app'), isNull);
    });

    test('returns a model when the installer is unknown', () async {
      final AppInstallSourceInfo? info = await plugin.getInstallSourceInfo('com.unknown.source');
      expect(info, isNotNull);
      expect(info!.installingPackageName, isNull);
      expect(info.packageSource, isNull);
    });

    test('maps a legacy response with only an installer', () async {
      final AppInstallSourceInfo? info = await plugin.getInstallSourceInfo('com.legacy.app');
      expect(info!.installingPackageName, 'com.android.vending');
      expect(info.initiatingPackageName, isNull);
      expect(info.originatingPackageName, isNull);
      expect(info.packageSource, isNull);
      expect(info.updateOwnerPackageName, isNull);
    });

    test('propagates platform errors', () async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
        const MethodChannel('flutter_device_apps/methods'),
        (MethodCall call) async => throw PlatformException(code: 'ERR_INSTALL_SOURCE'),
      );
      await expectLater(
        plugin.getInstallSourceInfo('com.example.app1'),
        throwsA(isA<PlatformException>().having((e) => e.code, 'code', 'ERR_INSTALL_SOURCE')),
      );
    });
  });

  group('getInstallerStore', () {
    test('calls method channel with package name', () async {
      // Exercise the deprecated API to verify backwards compatibility.
      // ignore: deprecated_member_use_from_same_package
      await plugin.getInstallerStore('com.example.app1');

      expect(methodCalls, hasLength(1));
      expect(methodCalls.first.method, 'getInstallerStore');
      expect(methodCalls.first.arguments, {'packageName': 'com.example.app1'});
    });

    test('returns store package name', () async {
      // Exercise the deprecated API to verify backwards compatibility.
      // ignore: deprecated_member_use_from_same_package
      final String? store = await plugin.getInstallerStore('com.example.app1');
      expect(store, 'com.android.vending');
    });

    test('returns null for sideloaded app', () async {
      // Exercise the deprecated API to verify backwards compatibility.
      // ignore: deprecated_member_use_from_same_package
      final String? store = await plugin.getInstallerStore('com.sideloaded.app');
      expect(store, isNull);
    });
  });

  group('appChanges stream', () {
    test('returns a stream', () {
      expect(plugin.appChanges, isA<Stream>());
    });

    test('stream is broadcast', () {
      final Stream<AppChangeEvent> stream = plugin.appChanges
        // Broadcast streams allow multiple listeners
        ..listen((_) {});
      expect(() => stream.listen((_) {}), returnsNormally);
    });

    test('calls startAppChangeStream when listening starts', () async {
      plugin.appChanges.listen((_) {});

      // Give time for async onListen to execute
      await Future<void>.delayed(const Duration(milliseconds: 50));

      expect(methodCalls.any((c) => c.method == 'startAppChangeStream'), isTrue);
    });
  });
}

/// Mock handler for method calls
Object? _handleMethodCall(MethodCall call) {
  final args = call.arguments as Map<Object?, Object?>?;

  switch (call.method) {
    case 'listApps':
      return [
        _createAppMap('com.example.app1', 'App 1'),
        _createAppMap('com.example.app2', 'App 2'),
      ];

    case 'getApp':
      final packageName = args!['packageName']! as String;
      if (packageName == 'com.nonexistent.app') return null;
      return _createAppMap(packageName, 'App 1');

    case 'getAppIcon':
      if (args!['packageName'] == 'com.nonexistent.app') return null;
      return Uint8List.fromList([137, 80, 78, 71, 13, 10, 26, 10]);

    case 'isAppInstalled':
      return args!['packageName'] != 'com.nonexistent.app';

    case 'isSystemApp':
      final packageName = args!['packageName']! as String;
      if (packageName == 'com.nonexistent.app') return null;
      return packageName == 'com.android.settings';

    case 'getRequestedPermissions':
      final packageName = args!['packageName']! as String;
      if (packageName == 'com.nonexistent.app') return null;
      return ['android.permission.INTERNET', 'android.permission.CAMERA'];

    case 'openApp':
    case 'openAppSettings':
      final packageName = args!['packageName']! as String;
      return packageName != 'com.nonexistent.app';

    case 'uninstallApp':
      final packageName = args!['packageName']! as String;
      return packageName != 'com.nonexistent.app';

    case 'getInstallSourceInfo':
      final packageName = args!['packageName']! as String;
      if (packageName == 'com.nonexistent.app') return null;
      if (packageName == 'com.unknown.source') return <String, Object?>{};
      if (packageName == 'com.legacy.app') return {'installingPackageName': 'com.android.vending'};
      return {
        'installingPackageName': 'com.android.vending',
        'initiatingPackageName': 'com.example.installer',
        'originatingPackageName': 'com.example.browser',
        'packageSource': 2,
        'updateOwnerPackageName': 'com.example.owner',
      };

    case 'getInstallerStore':
      final packageName = args!['packageName']! as String;
      if (packageName == 'com.sideloaded.app') return null;
      return 'com.android.vending';

    case 'startAppChangeStream':
    case 'stopAppChangeStream':
      return null;

    default:
      return null;
  }
}

Map<String, Object?> _createAppMap(String packageName, String appName) {
  final int uid = packageName == 'com.example.app1' ? 10123 : 10124;
  final bool isOnExternalStorage = packageName == 'com.example.app2';
  final int apkSizeBytes = packageName == 'com.example.app1' ? 12345678 : 22334455;

  return {
    'packageName': packageName,
    'appName': appName,
    'versionName': '1.0.0',
    'versionCode': 1,
    'uid': uid,
    'apkPath': '/data/app/$packageName/base.apk',
    'apkSizeBytes': apkSizeBytes,
    'dataPath': '/data/user/0/$packageName',
    'isOnExternalStorage': isOnExternalStorage,
    'isSystem': false,
    'firstInstallTime': 1700000000000,
    'lastUpdateTime': 1700000000000,
    'category': 0,
    'targetSdkVersion': 34,
    'minSdkVersion': 21,
    'enabled': true,
    'processName': packageName,
    'installLocation': 0,
  };
}
