import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_device_apps_android/flutter_device_apps_android.dart';
import 'package:flutter_device_apps_platform_interface/flutter_device_apps_app_change_event.dart';
import 'package:flutter_test/flutter_test.dart';

Future<void> _flushEvents() async {
  await Future<void>.delayed(Duration.zero);
  await Future<void>.delayed(Duration.zero);
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const methods = MethodChannel('flutter_device_apps/methods');
  const events = MethodChannel('flutter_device_apps/app_changes');
  final TestDefaultBinaryMessenger messenger =
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
  late FlutterDeviceAppsAndroid plugin;
  late List<String> calls;
  late List<StreamSubscription<AppChangeEvent>> subscriptions;
  Completer<void>? startGate;
  Completer<void>? stopGate;
  late bool failNextStart;
  late bool receiverRunning;
  late int activeChannels;
  late int maxActiveChannels;

  StreamSubscription<AppChangeEvent> subscribe(
    List<AppChangeEvent> received, {
    List<Object>? errors,
  }) {
    final StreamSubscription<AppChangeEvent> subscription = plugin.appChanges.listen(
      received.add,
      onError: errors?.add,
    );
    subscriptions.add(subscription);
    return subscription;
  }

  Future<void> sendEvent() async {
    await messenger.handlePlatformMessage(
      events.name,
      events.codec.encodeSuccessEnvelope({
        'packageName': 'com.example.app',
        'type': 'updated',
        'isReplacing': true,
      }),
      null,
    );
    await _flushEvents();
  }

  setUp(() {
    plugin = FlutterDeviceAppsAndroid();
    calls = [];
    subscriptions = [];
    startGate = null;
    stopGate = null;
    failNextStart = false;
    receiverRunning = false;
    activeChannels = 0;
    maxActiveChannels = 0;

    messenger
      ..setMockMethodCallHandler(methods, (MethodCall call) async {
        calls.add(call.method);
        if (call.method == 'startAppChangeStream') {
          if (failNextStart) {
            failNextStart = false;
            throw PlatformException(code: 'ERR_STREAM');
          }
          await startGate?.future;
          receiverRunning = true;
        } else if (call.method == 'stopAppChangeStream') {
          await stopGate?.future;
          receiverRunning = false;
        }
        return null;
      })
      ..setMockMethodCallHandler(events, (MethodCall call) async {
        calls.add('event.${call.method}');
        if (call.method == 'listen') {
          activeChannels++;
          if (activeChannels > maxActiveChannels) maxActiveChannels = activeChannels;
          receiverRunning = true;
        } else if (call.method == 'cancel') {
          activeChannels--;
          receiverRunning = false;
        }
        return null;
      });
  });

  tearDown(() async {
    for (final gate in [startGate, stopGate]) {
      if (gate != null && !gate.isCompleted) gate.complete();
    }
    for (final subscription in subscriptions) {
      await subscription.cancel();
    }
    await _flushEvents();
    await messenger.platformMessagesFinished;
    messenger
      ..setMockMethodCallHandler(methods, null)
      ..setMockMethodCallHandler(events, null);
  });

  test('broadcast listeners share one channel until the last listener cancels', () async {
    expect(plugin.appChanges.isBroadcast, isTrue);
    expect(calls, isEmpty);
    final firstEvents = <AppChangeEvent>[];
    final secondEvents = <AppChangeEvent>[];
    final StreamSubscription<AppChangeEvent> first = subscribe(firstEvents);
    final StreamSubscription<AppChangeEvent> second = subscribe(secondEvents);
    await _flushEvents();
    expect(calls, ['startAppChangeStream', 'event.listen']);

    await sendEvent();
    expect(firstEvents, hasLength(1));
    expect(secondEvents, hasLength(1));
    expect(firstEvents.single.packageName, 'com.example.app');
    expect(firstEvents.single.type, AppChangeType.updated);
    expect(firstEvents.single.isReplacing, isTrue);

    await first.cancel();
    await _flushEvents();
    expect(calls, ['startAppChangeStream', 'event.listen']);
    await sendEvent();
    expect(firstEvents, hasLength(1));
    expect(secondEvents, hasLength(2));

    await second.cancel();
    await _flushEvents();
    expect(calls, ['startAppChangeStream', 'event.listen', 'event.cancel', 'stopAppChangeStream']);
    expect(activeChannels, 0);
    expect(receiverRunning, isFalse);
  });

  test('repeated listen and cancel cycles release the channel each time', () async {
    final received = <AppChangeEvent>[];
    for (int cycle = 0; cycle < 3; cycle++) {
      final StreamSubscription<AppChangeEvent> subscription = subscribe(received);
      await _flushEvents();
      expect(activeChannels, 1);
      await sendEvent();
      expect(received, hasLength(cycle + 1));
      await subscription.cancel();
      await _flushEvents();
      expect(activeChannels, 0);
      expect(receiverRunning, isFalse);
    }
    expect(calls.where((call) => call == 'event.listen'), hasLength(3));
    expect(calls.where((call) => call == 'event.cancel'), hasLength(3));
    expect(maxActiveChannels, 1);
  });

  test('cancel during startup does not leave a receiver or channel running', () async {
    startGate = Completer<void>();
    final StreamSubscription<AppChangeEvent> subscription = subscribe([]);
    await _flushEvents();
    expect(calls, ['startAppChangeStream']);
    await subscription.cancel();
    await _flushEvents();
    expect(calls, ['startAppChangeStream']);

    startGate!.complete();
    await _flushEvents();
    expect(calls, ['startAppChangeStream', 'stopAppChangeStream']);
    expect(activeChannels, 0);
    expect(receiverRunning, isFalse);
  });

  test('immediate resubscribe cancels the previous channel before starting a new one', () async {
    final oldEvents = <AppChangeEvent>[];
    final StreamSubscription<AppChangeEvent> first = subscribe(oldEvents);
    await _flushEvents();
    await first.cancel();
    final newEvents = <AppChangeEvent>[];
    final StreamSubscription<AppChangeEvent> second = subscribe(newEvents);
    await _flushEvents();
    expect(calls, [
      'startAppChangeStream',
      'event.listen',
      'event.cancel',
      'stopAppChangeStream',
      'startAppChangeStream',
      'event.listen',
    ]);
    expect(activeChannels, 1);
    expect(maxActiveChannels, 1);
    await sendEvent();
    expect(oldEvents, isEmpty);
    expect(newEvents, hasLength(1));
    await second.cancel();
    await _flushEvents();
    expect(activeChannels, 0);
    expect(receiverRunning, isFalse);
  });

  test('resubscribe waits for the previous native stop to finish', () async {
    final StreamSubscription<AppChangeEvent> first = subscribe([]);
    await _flushEvents();
    stopGate = Completer<void>();
    await first.cancel();
    await _flushEvents();
    final StreamSubscription<AppChangeEvent> second = subscribe([]);
    await _flushEvents();
    expect(calls, ['startAppChangeStream', 'event.listen', 'event.cancel', 'stopAppChangeStream']);
    expect(activeChannels, 0);

    stopGate!.complete();
    await _flushEvents();
    expect(calls.last, 'event.listen');
    expect(activeChannels, 1);
    expect(receiverRunning, isTrue);
    await second.cancel();
    await _flushEvents();
    expect(activeChannels, 0);
    expect(receiverRunning, isFalse);
  });

  test('startup errors reach listeners and do not prevent a later subscription', () async {
    failNextStart = true;
    final errors = <Object>[];
    final StreamSubscription<AppChangeEvent> first = subscribe([], errors: errors);
    await _flushEvents();
    expect(errors, [isA<PlatformException>().having((e) => e.code, 'code', 'ERR_STREAM')]);
    expect(activeChannels, 0);
    await first.cancel();
    await _flushEvents();

    final received = <AppChangeEvent>[];
    final StreamSubscription<AppChangeEvent> second = subscribe(received);
    await _flushEvents();
    await sendEvent();
    expect(received, hasLength(1));
    await second.cancel();
    await _flushEvents();
    expect(activeChannels, 0);
  });

  test('platform stream errors are forwarded without stopping event delivery', () async {
    final received = <AppChangeEvent>[];
    final errors = <Object>[];
    final StreamSubscription<AppChangeEvent> subscription = subscribe(received, errors: errors);
    await _flushEvents();
    await messenger.handlePlatformMessage(
      events.name,
      events.codec.encodeErrorEnvelope(code: 'ERR_EVENT'),
      null,
    );
    await _flushEvents();
    expect(errors, [isA<PlatformException>().having((e) => e.code, 'code', 'ERR_EVENT')]);
    await sendEvent();
    expect(received, hasLength(1));
    await subscription.cancel();
    await _flushEvents();
    expect(activeChannels, 0);
  });
}
