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
      expect(
        FlutterDeviceAppsPlatform.instance,
        isA<FlutterDeviceAppsAndroid>(),
      );
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
      await plugin.listApps(
        includeSystem: true,
        onlyLaunchable: false,
        includeIcons: true,
      );

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
      expect(apps[1].packageName, 'com.example.app2');
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

      expect(methodCalls.first.arguments, {
        'packageName': 'com.example.app1',
        'includeIcon': true,
      });
    });

    test('returns AppInfo when app exists', () async {
      final AppInfo? app = await plugin.getApp('com.example.app1');

      expect(app, isNotNull);
      expect(app!.packageName, 'com.example.app1');
      expect(app.appName, 'App 1');
    });

    test('returns null when app does not exist', () async {
      final AppInfo? app = await plugin.getApp('com.nonexistent.app');
      expect(app, isNull);
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

  group('getInstallerStore', () {
    test('calls method channel with package name', () async {
      await plugin.getInstallerStore('com.example.app1');

      expect(methodCalls, hasLength(1));
      expect(methodCalls.first.method, 'getInstallerStore');
      expect(methodCalls.first.arguments, {'packageName': 'com.example.app1'});
    });

    test('returns store package name', () async {
      final String? store = await plugin.getInstallerStore('com.example.app1');
      expect(store, 'com.android.vending');
    });

    test('returns null for sideloaded app', () async {
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

      expect(
        methodCalls.any((c) => c.method == 'startAppChangeStream'),
        isTrue,
      );
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
  return {
    'packageName': packageName,
    'appName': appName,
    'versionName': '1.0.0',
    'versionCode': 1,
    'systemApp': false,
    'firstInstallTime': 1700000000000,
    'lastUpdateTime': 1700000000000,
    'apkFilePath': '/data/app/$packageName/base.apk',
    'dataDir': '/data/data/$packageName',
    'category': 0,
    'targetSdkVersion': 34,
    'minSdkVersion': 21,
    'enabled': true,
    'processName': packageName,
    'installLocation': 0,
  };
}
