import 'package:flutter_device_apps_android/android_app_types.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('AndroidAppChangeType', () {
    test('has all expected values', () {
      expect(AndroidAppChangeType.values, hasLength(3));
      expect(AndroidAppChangeType.values, contains(AndroidAppChangeType.installed));
      expect(AndroidAppChangeType.values, contains(AndroidAppChangeType.removed));
      expect(AndroidAppChangeType.values, contains(AndroidAppChangeType.updated));
    });

    test('enum names match expected strings', () {
      expect(AndroidAppChangeType.installed.name, 'installed');
      expect(AndroidAppChangeType.removed.name, 'removed');
      expect(AndroidAppChangeType.updated.name, 'updated');
    });
  });

  group('AndroidAppChangeEvent', () {
    group('constructor', () {
      test('creates instance with all null values', () {
        const event = AndroidAppChangeEvent();

        expect(event.packageName, isNull);
        expect(event.type, isNull);
        expect(event.isReplacing, isNull);
      });

      test('creates instance with all values', () {
        const event = AndroidAppChangeEvent(
          packageName: 'com.example.app',
          type: AndroidAppChangeType.installed,
          isReplacing: false,
        );

        expect(event.packageName, 'com.example.app');
        expect(event.type, AndroidAppChangeType.installed);
        expect(event.isReplacing, false);
      });

      test('creates instance with partial values', () {
        const event = AndroidAppChangeEvent(packageName: 'com.example.app');

        expect(event.packageName, 'com.example.app');
        expect(event.type, isNull);
        expect(event.isReplacing, isNull);
      });
    });

    group('fromMap', () {
      test('parses empty map', () {
        final event = AndroidAppChangeEvent.fromMap({});

        expect(event.packageName, isNull);
        expect(event.type, isNull);
        expect(event.isReplacing, isNull);
      });

      test('parses packageName correctly', () {
        final event = AndroidAppChangeEvent.fromMap({'packageName': 'com.test.app'});

        expect(event.packageName, 'com.test.app');
      });

      test('parses type "installed" correctly', () {
        final event = AndroidAppChangeEvent.fromMap({'type': 'installed'});

        expect(event.type, AndroidAppChangeType.installed);
      });

      test('parses type "removed" correctly', () {
        final event = AndroidAppChangeEvent.fromMap({'type': 'removed'});

        expect(event.type, AndroidAppChangeType.removed);
      });

      test('parses type "updated" correctly', () {
        final event = AndroidAppChangeEvent.fromMap({'type': 'updated'});

        expect(event.type, AndroidAppChangeType.updated);
      });

      test('returns null type for invalid type string', () {
        final event = AndroidAppChangeEvent.fromMap({'type': 'unknown'});

        expect(event.type, isNull);
      });

      test('returns null type for empty type string', () {
        final event = AndroidAppChangeEvent.fromMap({'type': ''});

        expect(event.type, isNull);
      });

      test('parses isReplacing from bool value', () {
        final eventTrue = AndroidAppChangeEvent.fromMap({'isReplacing': true});
        final eventFalse = AndroidAppChangeEvent.fromMap({'isReplacing': false});

        expect(eventTrue.isReplacing, true);
        expect(eventFalse.isReplacing, false);
      });

      test('parses isReplacing from string value', () {
        final eventTrue = AndroidAppChangeEvent.fromMap({'isReplacing': 'true'});
        final eventFalse = AndroidAppChangeEvent.fromMap({'isReplacing': 'false'});

        expect(eventTrue.isReplacing, true);
        expect(eventFalse.isReplacing, false);
      });

      test('returns null isReplacing for invalid string', () {
        final event = AndroidAppChangeEvent.fromMap({'isReplacing': 'yes'});

        expect(event.isReplacing, isNull);
      });

      test('parses complete map with all fields', () {
        final event = AndroidAppChangeEvent.fromMap({
          'packageName': 'com.example.fullapp',
          'type': 'updated',
          'isReplacing': true,
        });

        expect(event.packageName, 'com.example.fullapp');
        expect(event.type, AndroidAppChangeType.updated);
        expect(event.isReplacing, true);
      });
    });

    group('toMap', () {
      test('converts empty event to map', () {
        const event = AndroidAppChangeEvent();
        final Map<String, Object?> map = event.toMap();

        expect(map['packageName'], isNull);
        expect(map['type'], isNull);
        expect(map['isReplacing'], isNull);
      });

      test('converts full event to map', () {
        const event = AndroidAppChangeEvent(
          packageName: 'com.example.app',
          type: AndroidAppChangeType.installed,
          isReplacing: false,
        );
        final Map<String, Object?> map = event.toMap();

        expect(map['packageName'], 'com.example.app');
        expect(map['type'], 'installed');
        expect(map['isReplacing'], false);
      });

      test('converts removed type correctly', () {
        const event = AndroidAppChangeEvent(type: AndroidAppChangeType.removed);
        final Map<String, Object?> map = event.toMap();

        expect(map['type'], 'removed');
      });

      test('converts updated type correctly', () {
        const event = AndroidAppChangeEvent(type: AndroidAppChangeType.updated);
        final Map<String, Object?> map = event.toMap();

        expect(map['type'], 'updated');
      });

      test('converts partial event to map', () {
        const event = AndroidAppChangeEvent(packageName: 'com.partial.app', isReplacing: true);
        final Map<String, Object?> map = event.toMap();

        expect(map['packageName'], 'com.partial.app');
        expect(map['type'], isNull);
        expect(map['isReplacing'], true);
      });
    });

    group('round-trip (fromMap -> toMap)', () {
      test('installed event survives round-trip', () {
        final originalMap = <String, Object?>{
          'packageName': 'com.roundtrip.installed',
          'type': 'installed',
          'isReplacing': false,
        };

        final event = AndroidAppChangeEvent.fromMap(originalMap);
        final Map<String, Object?> resultMap = event.toMap();

        expect(resultMap['packageName'], originalMap['packageName']);
        expect(resultMap['type'], originalMap['type']);
        expect(resultMap['isReplacing'], originalMap['isReplacing']);
      });

      test('removed event survives round-trip', () {
        final originalMap = <String, Object?>{
          'packageName': 'com.roundtrip.removed',
          'type': 'removed',
          'isReplacing': false,
        };

        final event = AndroidAppChangeEvent.fromMap(originalMap);
        final Map<String, Object?> resultMap = event.toMap();

        expect(resultMap['packageName'], originalMap['packageName']);
        expect(resultMap['type'], originalMap['type']);
        expect(resultMap['isReplacing'], originalMap['isReplacing']);
      });

      test('updated event with replacing survives round-trip', () {
        final originalMap = <String, Object?>{
          'packageName': 'com.roundtrip.updated',
          'type': 'updated',
          'isReplacing': true,
        };

        final event = AndroidAppChangeEvent.fromMap(originalMap);
        final Map<String, Object?> resultMap = event.toMap();

        expect(resultMap['packageName'], originalMap['packageName']);
        expect(resultMap['type'], originalMap['type']);
        expect(resultMap['isReplacing'], originalMap['isReplacing']);
      });

      test('empty event survives round-trip', () {
        final originalMap = <String, Object?>{};

        final event = AndroidAppChangeEvent.fromMap(originalMap);
        final Map<String, Object?> resultMap = event.toMap();

        expect(resultMap['packageName'], isNull);
        expect(resultMap['type'], isNull);
        expect(resultMap['isReplacing'], isNull);
      });
    });
  });
}
