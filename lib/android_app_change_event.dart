/// Represents the type of change that occurred to an app.
enum AndroidAppChangeType {
  /// The app was newly installed on the device.
  installed,

  /// The app was removed/uninstalled from the device.
  removed,

  /// The app was updated to a new version.
  updated,
}

/// Represents an event when an app's state changes on the device.
///
/// This includes installations, removals, and updates.
class AndroidAppChangeEvent {
  /// Creates an [AndroidAppChangeEvent] with the specified properties.
  ///
  /// All parameters are optional and represent the details of the app change.
  const AndroidAppChangeEvent({this.packageName, this.type, this.isReplacing});

  /// Creates an [AndroidAppChangeEvent] from a map of key-value pairs.
  ///
  /// The map typically comes from platform-specific implementations.
  /// Handles type conversion and parsing for the change event data.
  factory AndroidAppChangeEvent.fromMap(Map<String, Object?> m) {
    final String? pkg = m['packageName']?.toString();
    final String? t = m['type']?.toString();
    final bool? replacing = m['isReplacing'] != null
        ? bool.tryParse(m['isReplacing']!.toString())
        : null;

    return AndroidAppChangeEvent(
      packageName: pkg,
      type: t != null ? _parseType(t) : null,
      isReplacing: replacing,
    );
  }

  /// The package name of the app that changed (e.g., 'com.example.app').
  final String? packageName;

  /// The type of change that occurred to the app.
  final AndroidAppChangeType? type;

  /// Whether this change is part of a replacement operation (e.g., app update).
  final bool? isReplacing;

  /// Converts this [AndroidAppChangeEvent] to a map representation.
  ///
  /// Useful for serialization to platform channels or other data formats.
  Map<String, Object?> toMap() => {
    'packageName': packageName,
    'type': type?.name,
    'isReplacing': isReplacing,
  };

  static AndroidAppChangeType? _parseType(String raw) => switch (raw) {
    'installed' => AndroidAppChangeType.installed,
    'removed' => AndroidAppChangeType.removed,
    'updated' => AndroidAppChangeType.updated,
    _ => null,
  };
}

/// The legacy name for [AndroidAppChangeType].
@Deprecated('Use AndroidAppChangeType instead.')
typedef AppChangeType = AndroidAppChangeType;

/// The legacy name for [AndroidAppChangeEvent].
@Deprecated('Use AndroidAppChangeEvent instead.')
typedef AppChangeEvent = AndroidAppChangeEvent;
