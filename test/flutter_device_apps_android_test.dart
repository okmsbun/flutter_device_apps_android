import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_device_apps_android/flutter_device_apps_android.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late List<MethodCall> methodCalls;

  setUp(() {
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

  test('legacy registration remains callable', () {
    // Exercise the public method retained for source compatibility.
    // ignore: deprecated_member_use_from_same_package
    expect(FlutterDeviceAppsAndroid.registerWith, returnsNormally);
  });

  test('the Android API reports unsupported operations off Android', () async {
    final TargetPlatform? originalPlatform = debugDefaultTargetPlatformOverride;
    debugDefaultTargetPlatformOverride = TargetPlatform.iOS;
    addTearDown(() => debugDefaultTargetPlatformOverride = originalPlatform);

    await expectLater(FlutterDeviceAppsAndroid.listApps(), throwsUnsupportedError);
    await expectLater(FlutterDeviceAppsAndroid.appChanges, emitsError(isA<UnsupportedError>()));
    expect(methodCalls, isEmpty);
  });

  group('listApps', () {
    test('calls method channel with default parameters', () async {
      await FlutterDeviceAppsAndroid.listApps();

      expect(methodCalls, hasLength(1));
      expect(methodCalls.first.method, 'listApps');
      expect(methodCalls.first.arguments, {
        'includeSystem': false,
        'onlyLaunchable': true,
        'includeIcons': false,
        'packageNamePrefix': null,
        'packageNameContains': null,
      });
    });

    test('calls method channel with custom parameters', () async {
      await FlutterDeviceAppsAndroid.listApps(
        includeSystem: true,
        onlyLaunchable: false,
        includeIcons: true,
        packageNamePrefix: 'com.',
        packageNameContains: 'example',
      );

      expect(methodCalls, hasLength(1));
      expect(methodCalls.first.arguments, {
        'includeSystem': true,
        'onlyLaunchable': false,
        'includeIcons': true,
        'packageNamePrefix': 'com.',
        'packageNameContains': 'example',
      });
    });

    test('preserves empty package name filters when calling native code', () async {
      await FlutterDeviceAppsAndroid.listApps(packageNamePrefix: '', packageNameContains: '');
      expect(methodCalls.first.arguments, {
        'includeSystem': false,
        'onlyLaunchable': true,
        'includeIcons': false,
        'packageNamePrefix': '',
        'packageNameContains': '',
      });
    });

    test('supports a prefix without a substring', () async {
      await FlutterDeviceAppsAndroid.listApps(packageNamePrefix: 'com.example.');
      expect(methodCalls.single.arguments, {
        'includeSystem': false,
        'onlyLaunchable': true,
        'includeIcons': false,
        'packageNamePrefix': 'com.example.',
        'packageNameContains': null,
      });
    });

    test('returns list of AndroidAppInfo', () async {
      final List<AndroidAppInfo> apps = await FlutterDeviceAppsAndroid.listApps();

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

      final List<AndroidAppInfo> apps = await FlutterDeviceAppsAndroid.listApps();
      expect(apps, isEmpty);
    });
  });

  group('getApp', () {
    test('calls method channel with package name', () async {
      await FlutterDeviceAppsAndroid.getApp('com.example.app1');

      expect(methodCalls, hasLength(1));
      expect(methodCalls.first.method, 'getApp');
      expect(methodCalls.first.arguments, {
        'packageName': 'com.example.app1',
        'includeIcon': false,
      });
    });

    test('calls method channel with includeIcon true', () async {
      await FlutterDeviceAppsAndroid.getApp('com.example.app1', includeIcon: true);

      expect(methodCalls.first.arguments, {'packageName': 'com.example.app1', 'includeIcon': true});
    });

    test('returns AndroidAppInfo when app exists', () async {
      final AndroidAppInfo? app = await FlutterDeviceAppsAndroid.getApp('com.example.app1');

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
      final AndroidAppInfo? app = await FlutterDeviceAppsAndroid.getApp('com.nonexistent.app');
      expect(app, isNull);
    });
  });

  group('getApps', () {
    test('sends one native request with the supplied names and default options', () async {
      final names = [
        'com.example.app2',
        'com.example.missing',
        'com.example.app1',
        'com.example.app2',
      ];
      final List<AndroidAppInfo> apps = await FlutterDeviceAppsAndroid.getApps(names);
      expect(methodCalls, hasLength(1));
      expect(methodCalls.single.method, 'getApps');
      expect(methodCalls.single.arguments, {'packageNames': names, 'includeIcons': false});
      expect(apps.map((app) => app.packageName), ['com.example.app2', 'com.example.app1']);
      expect(apps.first.uid, 10124);
    });

    test('forwards includeIcons and decodes the native response', () async {
      final bytes = Uint8List.fromList([137, 80, 78, 71]);
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
        const MethodChannel('flutter_device_apps/methods'),
        (MethodCall call) async {
          methodCalls.add(call);
          return [
            {..._createAppMap('com.example.app2', 'App 2'), 'iconBytes': bytes},
            _createAppMap('com.example.app1', 'App 1'),
          ];
        },
      );
      final List<AndroidAppInfo> apps = await FlutterDeviceAppsAndroid.getApps([
        'com.example.app2',
        'com.example.app1',
      ], includeIcons: true);
      expect(methodCalls.single.arguments, {
        'packageNames': ['com.example.app2', 'com.example.app1'],
        'includeIcons': true,
      });
      expect(apps.first.iconBytes, bytes);
      expect(apps.last.iconBytes, isNull);
      expect(apps.last.appName, 'App 1');
    });

    test('returns an empty native result for an empty input', () async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
        const MethodChannel('flutter_device_apps/methods'),
        (MethodCall call) async {
          methodCalls.add(call);
          return <Map>[];
        },
      );
      expect(await FlutterDeviceAppsAndroid.getApps([]), isEmpty);
      expect(methodCalls.single.arguments, {'packageNames': <String>[], 'includeIcons': false});
    });

    test('propagates native errors', () async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
        const MethodChannel('flutter_device_apps/methods'),
        (MethodCall call) async => throw PlatformException(code: 'ERR_GET_APPS'),
      );
      await expectLater(
        FlutterDeviceAppsAndroid.getApps(['com.example.app1']),
        throwsA(isA<PlatformException>().having((error) => error.code, 'code', 'ERR_GET_APPS')),
      );
    });
  });

  group('detectAppTechnologies', () {
    test(
      'groups all identifiers, supports multiple categories, and forwards the package',
      () async {
        final AndroidAppTechnologies technologies =
            await FlutterDeviceAppsAndroid.detectAppTechnologies('com.example.app');
        expect(_technologyNames(technologies.frameworks), [
          'flutter',
          'reactNative',
          'dotnetAndroid',
          'capacitor',
          'cordova',
          'qt',
          'nativeScript',
        ]);
        expect(technologies.programmingLanguages, [AndroidAppTechnology.dart]);
        expect(_technologyNames(technologies.uiToolkits), [
          'flutter',
          'reactNative',
          'jetpackCompose',
          'qtWidgets',
          'qtQuick',
        ]);
        expect(_technologyNames(technologies.gameEngines), [
          'unity',
          'unrealEngine',
          'godot',
          'cocosCreator',
        ]);
        expect(_technologyNames(technologies.runtimes), [
          'dotnetAndroid',
          'capacitor',
          'nativeScript',
          'hermes',
          'javaScriptCore',
          'mono',
          'il2cpp',
          'v8',
        ]);
        expect(_technologyNames(technologies.libraries), [
          'kotlinStandardLibrary',
          'androidxAppCompat',
          'qt',
          'sdl',
          'qtWidgets',
          'qtQuick',
          'kotlinCoroutines',
          'composeMaterial3',
          'androidMaterialComponents',
        ]);
        expect(
          () => technologies.frameworks.add(AndroidAppTechnology.flutter),
          throwsUnsupportedError,
        );
        expect(methodCalls, hasLength(1));
        expect(methodCalls.single.method, 'detectAppTechnologies');
        expect(methodCalls.single.arguments, {'packageName': 'com.example.app'});
      },
    );

    test(
      'does not infer languages from libraries, frameworks, game engines, or runtimes',
      () async {
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
          const MethodChannel('flutter_device_apps/methods'),
          (MethodCall call) async => ['flutter', 'unity', 'hermes', 'kotlinStandardLibrary'],
        );
        final AndroidAppTechnologies technologies =
            await FlutterDeviceAppsAndroid.detectAppTechnologies('com.example.app');
        expect(technologies.programmingLanguages, isEmpty);
        expect(technologies.libraries, [AndroidAppTechnology.kotlinStandardLibrary]);
      },
    );

    test('returns six empty lists when no markers match', () async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
        const MethodChannel('flutter_device_apps/methods'),
        (MethodCall call) async => <String>[],
      );
      final AndroidAppTechnologies technologies =
          await FlutterDeviceAppsAndroid.detectAppTechnologies('com.example.app');
      expect([
        technologies.frameworks,
        technologies.programmingLanguages,
        technologies.uiToolkits,
        technologies.gameEngines,
        technologies.runtimes,
        technologies.libraries,
      ], everyElement(isEmpty));
    });

    test('propagates argument, package, and scan failures', () async {
      for (final code in ['ARG', 'APP_NOT_FOUND', 'ERR_FRAMEWORK_SCAN']) {
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
          const MethodChannel('flutter_device_apps/methods'),
          (MethodCall call) async => throw PlatformException(code: code),
        );
        await expectLater(
          FlutterDeviceAppsAndroid.detectAppTechnologies('com.example.app'),
          throwsA(isA<PlatformException>().having((error) => error.code, 'code', code)),
        );
      }
    });
  });

  group('detectAppFrameworks', () {
    test('preserves the legacy channel and returns the legacy enum values', () async {
      expect(
        // Verify the entry point used by existing Android clients.
        // ignore: deprecated_member_use_from_same_package
        await FlutterDeviceAppsAndroid.detectAppFrameworks('com.example.app'),
        // Verify the original enum values as part of the compatibility contract.
        // ignore: deprecated_member_use_from_same_package
        AndroidAppFramework.values,
      );
      expect(methodCalls, hasLength(1));
      expect(methodCalls.single.method, 'detectAppFrameworks');
      expect(methodCalls.single.arguments, {'packageName': 'com.example.app'});
    });

    test('preserves a successful empty result', () async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
        const MethodChannel('flutter_device_apps/methods'),
        (MethodCall call) async => <String>[],
      );
      // The legacy API must still decode an empty native response.
      // ignore: deprecated_member_use_from_same_package
      expect(await FlutterDeviceAppsAndroid.detectAppFrameworks('com.example.app'), isEmpty);
    });

    test('does not turn argument, package, or scan failures into an empty result', () async {
      for (final code in ['ARG', 'APP_NOT_FOUND', 'ERR_FRAMEWORK_SCAN']) {
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
          const MethodChannel('flutter_device_apps/methods'),
          (MethodCall call) async => throw PlatformException(code: code),
        );
        await expectLater(
          // The legacy API must still surface native errors.
          // ignore: deprecated_member_use_from_same_package
          FlutterDeviceAppsAndroid.detectAppFrameworks('com.example.app'),
          throwsA(isA<PlatformException>().having((error) => error.code, 'code', code)),
        );
      }
    });
  });

  group('getAppIcon', () {
    test('returns typed icon bytes without requesting metadata', () async {
      final Uint8List? bytes = await FlutterDeviceAppsAndroid.getAppIcon('com.example.app1');
      expect(bytes, Uint8List.fromList([137, 80, 78, 71, 13, 10, 26, 10]));
      expect(methodCalls, hasLength(1));
      expect(methodCalls.first.method, 'getAppIcon');
      expect(methodCalls.first.arguments, {'packageName': 'com.example.app1'});
    });

    test('returns null for an unavailable package', () async {
      expect(await FlutterDeviceAppsAndroid.getAppIcon('com.nonexistent.app'), isNull);
    });

    test('propagates icon loading errors', () async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
        const MethodChannel('flutter_device_apps/methods'),
        (MethodCall call) async => throw PlatformException(code: 'ERR_ICON'),
      );
      await expectLater(
        FlutterDeviceAppsAndroid.getAppIcon('com.example.app1'),
        throwsA(isA<PlatformException>().having((error) => error.code, 'code', 'ERR_ICON')),
      );
    });
  });

  group('package queries', () {
    test('checks installation directly without fetching metadata', () async {
      expect(await FlutterDeviceAppsAndroid.isAppInstalled('com.example.app1'), isTrue);
      expect(await FlutterDeviceAppsAndroid.isAppInstalled('com.nonexistent.app'), isFalse);
      expect(methodCalls.map((call) => call.method), ['isAppInstalled', 'isAppInstalled']);
      expect(methodCalls.first.arguments, {'packageName': 'com.example.app1'});
    });

    test('distinguishes system, user and unavailable packages', () async {
      expect(await FlutterDeviceAppsAndroid.isSystemApp('com.android.settings'), isTrue);
      expect(await FlutterDeviceAppsAndroid.isSystemApp('com.example.app1'), isFalse);
      expect(await FlutterDeviceAppsAndroid.isSystemApp('com.nonexistent.app'), isNull);
      expect(methodCalls.every((call) => call.method == 'isSystemApp'), isTrue);
      expect(methodCalls.first.arguments, {'packageName': 'com.android.settings'});
    });

    test('distinguishes enabled, disabled and unavailable packages', () async {
      expect(await FlutterDeviceAppsAndroid.isAppEnabled('com.example.app1'), isTrue);
      expect(await FlutterDeviceAppsAndroid.isAppEnabled('com.example.disabled'), isFalse);
      expect(await FlutterDeviceAppsAndroid.isAppEnabled('com.nonexistent.app'), isNull);
      expect(methodCalls.every((call) => call.method == 'isAppEnabled'), isTrue);
      expect(methodCalls.first.arguments, {'packageName': 'com.example.app1'});
    });

    test('queries launchability without opening the app or requesting metadata', () async {
      expect(await FlutterDeviceAppsAndroid.isAppLaunchable('com.example.app1'), isTrue);
      expect(await FlutterDeviceAppsAndroid.isAppLaunchable('com.example.no_launcher'), isFalse);
      expect(await FlutterDeviceAppsAndroid.isAppLaunchable('com.nonexistent.app'), isFalse);
      expect(methodCalls.every((call) => call.method == 'isAppLaunchable'), isTrue);
      expect(methodCalls.first.arguments, {'packageName': 'com.example.app1'});
    });

    test('preserves query failures rather than reporting a missing package', () async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
        const MethodChannel('flutter_device_apps/methods'),
        (MethodCall call) async => throw PlatformException(code: 'ERR_QUERY'),
      );

      await expectLater(
        FlutterDeviceAppsAndroid.isAppInstalled('com.example.app1'),
        throwsA(isA<PlatformException>()),
      );
      await expectLater(
        FlutterDeviceAppsAndroid.isSystemApp('com.example.app1'),
        throwsA(isA<PlatformException>()),
      );
      await expectLater(
        FlutterDeviceAppsAndroid.isAppEnabled('com.example.app1'),
        throwsA(isA<PlatformException>().having((e) => e.code, 'code', 'ERR_QUERY')),
      );
      await expectLater(
        FlutterDeviceAppsAndroid.isAppLaunchable('com.example.app1'),
        throwsA(isA<PlatformException>().having((e) => e.code, 'code', 'ERR_QUERY')),
      );
    });
  });

  group('getRequestedPermissions', () {
    test('calls method channel with package name', () async {
      await FlutterDeviceAppsAndroid.getRequestedPermissions('com.example.app1');

      expect(methodCalls, hasLength(1));
      expect(methodCalls.first.method, 'getRequestedPermissions');
      expect(methodCalls.first.arguments, {'packageName': 'com.example.app1'});
    });

    test('returns list of permissions', () async {
      final List<String>? permissions = await FlutterDeviceAppsAndroid.getRequestedPermissions(
        'com.example.app1',
      );

      expect(permissions, isNotNull);
      expect(permissions, contains('android.permission.INTERNET'));
      expect(permissions, contains('android.permission.CAMERA'));
    });

    test('returns null for unknown package', () async {
      final List<String>? permissions = await FlutterDeviceAppsAndroid.getRequestedPermissions(
        'com.nonexistent.app',
      );
      expect(permissions, isNull);
    });
  });

  group('openApp', () {
    test('calls method channel with package name', () async {
      await FlutterDeviceAppsAndroid.openApp('com.example.app1');

      expect(methodCalls, hasLength(1));
      expect(methodCalls.first.method, 'openApp');
      expect(methodCalls.first.arguments, {'packageName': 'com.example.app1'});
    });

    test('returns true on success', () async {
      final bool result = await FlutterDeviceAppsAndroid.openApp('com.example.app1');
      expect(result, isTrue);
    });

    test('returns false on failure', () async {
      final bool result = await FlutterDeviceAppsAndroid.openApp('com.nonexistent.app');
      expect(result, isFalse);
    });
  });

  group('openAppSettings', () {
    test('calls method channel with package name', () async {
      await FlutterDeviceAppsAndroid.openAppSettings('com.example.app1');

      expect(methodCalls, hasLength(1));
      expect(methodCalls.first.method, 'openAppSettings');
      expect(methodCalls.first.arguments, {'packageName': 'com.example.app1'});
    });

    test('returns true on success', () async {
      final bool result = await FlutterDeviceAppsAndroid.openAppSettings('com.example.app1');
      expect(result, isTrue);
    });

    test('returns false on failure', () async {
      final bool result = await FlutterDeviceAppsAndroid.openAppSettings('com.nonexistent.app');
      expect(result, isFalse);
    });
  });

  group('uninstallApp', () {
    test('calls method channel with package name', () async {
      await FlutterDeviceAppsAndroid.uninstallApp('com.example.app1');

      expect(methodCalls, hasLength(1));
      expect(methodCalls.first.method, 'uninstallApp');
      expect(methodCalls.first.arguments, {'packageName': 'com.example.app1'});
    });

    test('returns true on success', () async {
      final bool result = await FlutterDeviceAppsAndroid.uninstallApp('com.example.app1');
      expect(result, isTrue);
    });

    test('returns false when method returns null', () async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
        const MethodChannel('flutter_device_apps/methods'),
        (MethodCall methodCall) async => null,
      );

      final bool result = await FlutterDeviceAppsAndroid.uninstallApp('com.example.app1');
      expect(result, isFalse);
    });
  });

  group('getInstallSourceInfo', () {
    test('queries source metadata through its own channel method', () async {
      final AndroidAppInstallSourceInfo? info = await FlutterDeviceAppsAndroid.getInstallSourceInfo(
        'com.example.app1',
      );
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
      expect(await FlutterDeviceAppsAndroid.getInstallSourceInfo('com.nonexistent.app'), isNull);
    });

    test('returns a model when the installer is unknown', () async {
      final AndroidAppInstallSourceInfo? info = await FlutterDeviceAppsAndroid.getInstallSourceInfo(
        'com.unknown.source',
      );
      expect(info, isNotNull);
      expect(info!.installingPackageName, isNull);
      expect(info.packageSource, isNull);
    });

    test('maps a legacy response with only an installer', () async {
      final AndroidAppInstallSourceInfo? info = await FlutterDeviceAppsAndroid.getInstallSourceInfo(
        'com.legacy.app',
      );
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
        FlutterDeviceAppsAndroid.getInstallSourceInfo('com.example.app1'),
        throwsA(isA<PlatformException>().having((e) => e.code, 'code', 'ERR_INSTALL_SOURCE')),
      );
    });
  });
}

Iterable<String> _technologyNames(List<AndroidAppTechnology> technologies) =>
    technologies.map((technology) => technology.name);

/// Mock handler for method calls
Object? _handleMethodCall(MethodCall call) {
  final args = call.arguments as Map<Object?, Object?>?;

  switch (call.method) {
    case 'listApps':
      return [
        _createAppMap('com.example.app1', 'App 1'),
        _createAppMap('com.example.app2', 'App 2'),
      ];

    case 'getApps':
      return [
        _createAppMap('com.example.app2', 'App 2'),
        _createAppMap('com.example.app1', 'App 1'),
      ];

    case 'detectAppTechnologies':
      return [
        'dart',
        'kotlinStandardLibrary',
        'flutter',
        'reactNative',
        'unity',
        'unrealEngine',
        'godot',
        'dotnetAndroid',
        'jetpackCompose',
        'androidxAppCompat',
        'capacitor',
        'cordova',
        'qt',
        'nativeScript',
        'sdl',
        'qtWidgets',
        'qtQuick',
        'hermes',
        'javaScriptCore',
        'mono',
        'il2cpp',
        'kotlinCoroutines',
        'v8',
        'cocosCreator',
        'composeMaterial3',
        'androidMaterialComponents',
        'flutter',
        'unknownFutureMarker',
      ];

    case 'detectAppFrameworks':
      return [
        'flutter',
        'reactNative',
        'unity',
        'unrealEngine',
        'godot',
        'dotnetAndroid',
        'jetpackCompose',
        'kotlin',
        'androidxAppCompat',
        'capacitor',
        'cordova',
        'qt',
        'nativeScript',
        'sdl',
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

    case 'isAppEnabled':
      final packageName = args!['packageName']! as String;
      if (packageName == 'com.nonexistent.app') return null;
      return packageName != 'com.example.disabled';

    case 'isAppLaunchable':
      return args!['packageName'] == 'com.example.app1';

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
