package com.okmsbun.flutter_device_apps_android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal class AppFrameworkDetectorTest {
  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun combinesBaseAndSplitsAcrossAbisInCanonicalOrderWithoutDuplicates() {
    val base = apk("base.apk",
      "lib/arm64-v8a/libmonodroid.so",
      "lib/arm64-v8a/libgodot_android.so",
      "lib/arm64-v8a/libUnreal.so",
      "lib/arm64-v8a/libunity.so"
    )
    val split = apk("split.apk",
      "lib/x86_64/libunity.so",
      "lib/arm64-v8a/libreactnative.so",
      "lib/x86/libreactnativejni.so",
      "lib/armeabi-v7a/libflutter.so",
      "lib/arm64-v8a/libflutter.so"
    )
    assertEquals(
      listOf("flutter", "reactNative", "unity", "unrealEngine", "godot", "dotnetAndroid"),
      AppFrameworkDetector.detect(listOf(base.path, split.path, base.path))
    )
  }

  @Test
  fun recognizesLegacyLibraryNamesWithoutInferringMauiOrXamarin() {
    val app = apk("legacy.apk",
      "lib/x86/libreactnativejni.so",
      "lib/armeabi-v7a/libUE4.so",
      "lib/arm64-v8a/libxamarin-app.so"
    )
    assertEquals(
      listOf("reactNative", "unrealEngine", "dotnetAndroid"),
      AppFrameworkDetector.detect(listOf(app.path))
    )
  }

  @Test
  fun ignoresAssetsNestedPathsDirectoriesCaseMismatchesAndGenericLibraries() {
    val app = apk("lookalikes.apk",
      "assets/lib/arm64-v8a/libflutter.so",
      "assets/libunity.so",
      "lib/arm64-v8a/nested/libUnreal.so",
      "lib/libgodot_android.so",
      "lib//libUE4.so",
      "lib/../libflutter.so",
      "lib/arm64-v8a/libflutter.so/",
      "Lib/arm64-v8a/libflutter.so",
      "lib/arm64-v8a/libFlutter.so",
      "lib/arm64-v8a/libflutter.so.sym",
      "lib/arm64-v8a/libhermes.so",
      "lib/arm64-v8a/libmono.so",
      "lib/arm64-v8a/libmonosgen-2.0.so",
      "lib/arm64-v8a/libil2cpp.so",
      "lib/arm64-v8a/libc++_shared.so"
    )
    assertEquals(emptyList<String>(), AppFrameworkDetector.detect(listOf(app.path)))
  }

  @Test
  fun returnsEmptyOnlyAfterReadingValidApksWithNoSupportedIndicators() {
    val base = apk("base.apk", "AndroidManifest.xml", "classes.dex")
    val split = apk("empty-split.apk")
    assertEquals(emptyList<String>(), AppFrameworkDetector.detect(listOf(base.path, split.path)))
  }

  @Test
  fun failsForUnreadableSplitsEvenAfterFindingEverySupportedFramework() {
    val base = apk("base.apk",
      "lib/arm64-v8a/libflutter.so",
      "lib/arm64-v8a/libreactnative.so",
      "lib/arm64-v8a/libunity.so",
      "lib/arm64-v8a/libUnreal.so",
      "lib/arm64-v8a/libgodot_android.so",
      "lib/arm64-v8a/libmonodroid.so"
    )
    val missing = File(temporaryFolder.root, "missing.apk")
    val corrupt = temporaryFolder.newFile("corrupt.apk").apply { writeText("not a ZIP archive") }
    for (failed in listOf(missing, corrupt)) {
      assertThrows(IOException::class.java) { AppFrameworkDetector.detect(listOf(base.path, failed.path)) }
      assertThrows(IOException::class.java) { AppFrameworkDetector.detect(listOf(failed.path)) }
    }
  }

  @Test
  fun rejectsUnavailableApkPathsInsteadOfReportingNoFrameworks() {
    for (paths in listOf(emptyList(), listOf(""), listOf(" \t"))) {
      assertThrows(IOException::class.java) { AppFrameworkDetector.detect(paths) }
    }
  }

  private fun apk(name: String, vararg entries: String): File = temporaryFolder.newFile(name).also { file ->
    ZipOutputStream(file.outputStream()).use { output ->
      for (entry in entries) {
        output.putNextEntry(ZipEntry(entry))
        if (!entry.endsWith("/")) output.write(byteArrayOf(0))
        output.closeEntry()
      }
    }
  }
}
