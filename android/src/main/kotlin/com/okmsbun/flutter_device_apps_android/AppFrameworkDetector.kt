package com.okmsbun.flutter_device_apps_android

import java.io.IOException
import java.util.zip.ZipFile

/** Detects packaged technology indicators without loading or extracting APK code. */
internal object AppFrameworkDetector {
  private val frameworkByLibrary = linkedMapOf(
    "libflutter.so" to "flutter",
    "libreactnative.so" to "reactNative",
    "libreactnativejni.so" to "reactNative",
    "libunity.so" to "unity",
    "libUnreal.so" to "unrealEngine",
    "libUE4.so" to "unrealEngine",
    "libgodot_android.so" to "godot",
    "libmonodroid.so" to "dotnetAndroid",
    "libxamarin-app.so" to "dotnetAndroid",
    "libNativeScript.so" to "nativeScript",
    "libSDL2.so" to "sdl",
    "libSDL3.so" to "sdl"
  )
  private val frameworkByEntry = linkedMapOf(
    "META-INF/androidx.compose.ui_ui.version" to "jetpackCompose",
    "META-INF/androidx.compose.runtime_runtime.version" to "jetpackCompose",
    "META-INF/kotlin-stdlib.kotlin_module" to "kotlin",
    "kotlin/kotlin.kotlin_builtins" to "kotlin",
    "META-INF/androidx.appcompat_appcompat.version" to "androidxAppCompat",
    "assets/capacitor.config.json" to "capacitor",
    "assets/www/cordova.js" to "cordova"
  )
  private val frameworkOrder = (frameworkByLibrary.values + frameworkByEntry.values + "qt").distinct()
  private val technologyByLibrary = frameworkByLibrary + mapOf(
    "libQt5Widgets.so" to "qtWidgets",
    "libQt6Widgets.so" to "qtWidgets",
    "libQt5Quick.so" to "qtQuick",
    "libQt6Quick.so" to "qtQuick",
    "libhermes.so" to "hermes",
    "libhermesvm.so" to "hermes",
    "libjsc.so" to "javaScriptCore",
    "libmonosgen-2.0.so" to "mono",
    "libil2cpp.so" to "il2cpp",
    "libv8android.so" to "v8",
    "libcocos.so" to "cocosCreator"
  )
  private val technologyByEntry = frameworkByEntry.mapValues { (_, framework) ->
    if (framework == "kotlin") "kotlinStandardLibrary" else framework
  } + mapOf(
    "META-INF/kotlinx_coroutines_core.version" to "kotlinCoroutines",
    "META-INF/kotlinx_coroutines_android.version" to "kotlinCoroutines",
    "META-INF/androidx.compose.material3_material3.version" to "composeMaterial3",
    "META-INF/com.google.android.material_material.version" to "androidMaterialComponents"
  )
  private val nativeLibraryEntry = Regex("lib/([A-Za-z0-9][A-Za-z0-9_-]*)/([^/]+)")
  private val dartKernelMagic = intArrayOf(0x90, 0xAB, 0xCD, 0xEF)

  @Throws(IOException::class)
  fun detect(apkPaths: List<String>): List<String> {
    val detected = scan(apkPaths, includeAdditionalTechnologies = false)
    return frameworkOrder.filter { it in detected }
  }

  @Throws(IOException::class)
  fun detectTechnologies(apkPaths: List<String>): List<String> =
    scan(apkPaths, includeAdditionalTechnologies = true).toList()

  /** Any unreadable APK fails the scan, including after a positive match. */
  private fun scan(apkPaths: List<String>, includeAdditionalTechnologies: Boolean): Set<String> {
    if (apkPaths.isEmpty() || apkPaths.any { it.isBlank() }) {
      throw IOException("APK paths are unavailable")
    }
    val libraryMarkers = if (includeAdditionalTechnologies) technologyByLibrary else frameworkByLibrary
    val entryMarkers = if (includeAdditionalTechnologies) technologyByEntry else frameworkByEntry
    val detected = mutableSetOf<String>()
    for (path in apkPaths.distinct()) {
      ZipFile(path).use { apk ->
        if (includeAdditionalTechnologies && "dart" !in detected && hasDartKernel(apk)) {
          detected.add("dart")
        }
        val entries = apk.entries()
        while (entries.hasMoreElements()) {
          val entry = entries.nextElement()
          if (entry.isDirectory) continue
          if (includeAdditionalTechnologies && "dart" !in detected &&
            entry.name.endsWith("/libapp.so") && nativeLibraryEntry.matches(entry.name) &&
            DartAotDetector.matches(apk, entry)
          ) {
            detected.add("dart")
          }
          val technology = entryMarkers[entry.name]
            ?: matchNativeLibrary(entry.name, libraryMarkers)
            ?: continue
          detected.add(technology)
        }
      }
    }
    return detected
  }

  private fun matchNativeLibrary(entryName: String, markers: Map<String, String>): String? {
    val (abi, library) = nativeLibraryEntry.matchEntire(entryName)?.destructured ?: return null
    val abiSuffix = "_$abi.so"
    val normalized = if (library.startsWith("libQt") && library.endsWith(abiSuffix)) {
      library.removeSuffix(abiSuffix) + ".so"
    } else {
      library
    }
    return when (normalized) {
      "libQt5Core.so", "libQt6Core.so" -> "qt"
      else -> markers[normalized]
    }
  }

  private fun hasDartKernel(apk: ZipFile): Boolean {
    val entry = apk.getEntry("assets/flutter_assets/kernel_blob.bin") ?: return false
    if (entry.isDirectory || entry.size < 8) return false
    return apk.getInputStream(entry).use { input -> dartKernelMagic.all { input.read() == it } }
  }
}
