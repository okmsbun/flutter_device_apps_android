package com.okmsbun.flutter_device_apps_android

import java.io.IOException
import java.util.zip.ZipFile

/** Detects packaged framework indicators without loading or extracting APK code. */
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
    "libxamarin-app.so" to "dotnetAndroid"
  )
  private val frameworkOrder = frameworkByLibrary.values.distinct()
  private val nativeLibraryEntry = Regex("lib/[A-Za-z0-9][A-Za-z0-9_-]*/([^/]+)")

  /** Any unreadable APK fails the scan, including after a positive match. */
  @Throws(IOException::class)
  fun detect(apkPaths: List<String>): List<String> {
    if (apkPaths.isEmpty() || apkPaths.any { it.isBlank() }) {
      throw IOException("APK paths are unavailable")
    }
    val detected = mutableSetOf<String>()
    for (path in apkPaths.distinct()) {
      ZipFile(path).use { apk ->
        val entries = apk.entries()
        while (entries.hasMoreElements()) {
          val entry = entries.nextElement()
          if (entry.isDirectory) continue
          val library = nativeLibraryEntry.matchEntire(entry.name)?.groupValues?.get(1) ?: continue
          frameworkByLibrary[library]?.let { detected.add(it) }
        }
      }
    }
    return frameworkOrder.filter { it in detected }
  }
}
