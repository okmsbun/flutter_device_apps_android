/// A technology identified by a packaged Android APK component.
enum AndroidAppTechnology {
  flutter({_Category.frameworks, _Category.uiToolkits}),
  reactNative({_Category.frameworks, _Category.uiToolkits}),
  unity({_Category.gameEngines}),
  unrealEngine({_Category.gameEngines}),
  godot({_Category.gameEngines}),
  dotnetAndroid({_Category.frameworks, _Category.runtimes}),
  jetpackCompose({_Category.uiToolkits}),

  /// The Kotlin standard library, which may be bundled by a dependency.
  kotlinStandardLibrary({_Category.libraries}),
  androidxAppCompat({_Category.libraries}),
  capacitor({_Category.frameworks, _Category.runtimes}),
  cordova({_Category.frameworks}),

  /// The Qt framework, detected through its Qt Core library.
  qt({_Category.frameworks, _Category.libraries}),
  nativeScript({_Category.frameworks, _Category.runtimes}),
  sdl({_Category.libraries}),
  qtWidgets({_Category.uiToolkits, _Category.libraries}),
  qtQuick({_Category.uiToolkits, _Category.libraries}),
  hermes({_Category.runtimes}),
  javaScriptCore({_Category.runtimes}),
  mono({_Category.runtimes}),
  il2cpp({_Category.runtimes}),
  kotlinCoroutines({_Category.libraries}),

  /// Dart code identified by a packaged Dart Kernel binary.
  dart({_Category.programmingLanguages});

  const AndroidAppTechnology(this._categories);

  final Set<_Category> _categories;
}

/// Categorized technologies identified in an installed Android app.
///
/// Lists are unmodifiable and contain each technology at most once. A
/// technology can appear in multiple categories. Packaged dependencies can
/// contribute matches.
class AndroidAppTechnologies {
  factory AndroidAppTechnologies(Iterable<AndroidAppTechnology> technologies) {
    final Set<AndroidAppTechnology> detected = technologies.toSet();
    List<AndroidAppTechnology> forCategory(_Category category) => List.unmodifiable(
      detected.where((technology) => technology._categories.contains(category)),
    );

    return AndroidAppTechnologies._(
      frameworks: forCategory(_Category.frameworks),
      programmingLanguages: forCategory(_Category.programmingLanguages),
      uiToolkits: forCategory(_Category.uiToolkits),
      gameEngines: forCategory(_Category.gameEngines),
      runtimes: forCategory(_Category.runtimes),
      libraries: forCategory(_Category.libraries),
    );
  }

  const AndroidAppTechnologies._({
    required this.frameworks,
    required this.programmingLanguages,
    required this.uiToolkits,
    required this.gameEngines,
    required this.runtimes,
    required this.libraries,
  });

  final List<AndroidAppTechnology> frameworks;

  /// Languages identified from packaged code, such as a Dart Kernel binary.
  ///
  /// Framework and runtime libraries alone do not identify source languages.
  final List<AndroidAppTechnology> programmingLanguages;

  final List<AndroidAppTechnology> uiToolkits;
  final List<AndroidAppTechnology> gameEngines;
  final List<AndroidAppTechnology> runtimes;
  final List<AndroidAppTechnology> libraries;
}

enum _Category { frameworks, programmingLanguages, uiToolkits, gameEngines, runtimes, libraries }
