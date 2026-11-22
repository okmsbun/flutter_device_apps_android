package com.okmsbun.flutter_device_apps_android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal class AppFrameworkDetectorTest {
  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun combinesBaseAndSplitsAcrossAbisInCanonicalOrderWithoutDuplicates() {
    val base = apk("base.apk",
      "assets/www/cordova.js",
      "META-INF/kotlin-stdlib.kotlin_module",
      "META-INF/androidx.appcompat_appcompat.version",
      "META-INF/androidx.compose.ui_ui.version",
      "lib/arm64-v8a/libNativeScript.so",
      "lib/arm64-v8a/libSDL2.so",
      "lib/arm64-v8a/libmonodroid.so",
      "lib/arm64-v8a/libgodot_android.so",
      "lib/arm64-v8a/libUnreal.so",
      "lib/arm64-v8a/libunity.so"
    )
    val split = apk("split.apk",
      "assets/capacitor.config.json",
      "lib/x86_64/libNativeScript.so",
      "lib/x86_64/libSDL3.so",
      "lib/arm64-v8a/libQt6Core_arm64-v8a.so",
      "kotlin/kotlin.kotlin_builtins",
      "META-INF/androidx.compose.runtime_runtime.version",
      "lib/x86_64/libunity.so",
      "lib/arm64-v8a/libreactnative.so",
      "lib/x86/libreactnativejni.so",
      "lib/armeabi-v7a/libflutter.so",
      "lib/arm64-v8a/libflutter.so"
    )
    assertEquals(
      listOf(
        "flutter", "reactNative", "unity", "unrealEngine", "godot", "dotnetAndroid", "nativeScript", "sdl",
        "jetpackCompose", "kotlin", "androidxAppCompat", "capacitor", "cordova", "qt"
      ),
      AppFrameworkDetector.detect(listOf(base.path, split.path, base.path))
    )
    assertEquals(
      setOf(
        "flutter", "reactNative", "unity", "unrealEngine", "godot", "dotnetAndroid", "nativeScript", "sdl",
        "jetpackCompose", "kotlinStandardLibrary", "androidxAppCompat", "capacitor", "cordova", "qt"
      ),
      AppFrameworkDetector.detectTechnologies(listOf(base.path, split.path, base.path)).toSet()
    )
  }

  @Test
  fun recognizesBothSdlVersionsIndependently() {
    for (version in 2..3) {
      val app = apk("sdl$version.apk", "lib/arm64-v8a/libSDL$version.so")
      assertEquals(listOf("sdl"), AppFrameworkDetector.detect(listOf(app.path)))
    }
  }

  @Test
  fun recognizesRuntimeMetadataAndLegacyQtWithoutOtherMarkers() {
    val app = apk("runtime.apk",
      "META-INF/androidx.compose.runtime_runtime.version",
      "kotlin/kotlin.kotlin_builtins",
      "lib/armeabi-v7a/libQt5Core.so"
    )
    assertEquals(listOf("jetpackCompose", "kotlin", "qt"), AppFrameworkDetector.detect(listOf(app.path)))
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
      "assets/libNativeScript.so",
      "lib/arm64-v8a/nested/libSDL3.so",
      "lib/arm64-v8a/libsdl2.so",
      "assets/public/cordova.js",
      "assets/www/cordova.js/",
      "assets/public/capacitor.config.json",
      "META-INF/custom.kotlin_module",
      "META-INF/androidx.activity_activity-compose.version",
      "META-INF/androidx.compose.ui_ui.version.bak",
      "META-INF/androidx.appcompat_appcompat-resources.version",
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
      "lib/arm64-v8a/libQt6Core_x86.so",
      "lib/arm64-v8a/libQt6Core_extra.so",
      "lib/arm64-v8a/libc++_shared.so"
    )
    assertEquals(emptyList<String>(), AppFrameworkDetector.detect(listOf(app.path)))
  }

  @Test
  fun returnsEmptyOnlyAfterReadingValidApksWithNoSupportedIndicators() {
    val base = apk("base.apk", "AndroidManifest.xml", "classes.dex")
    val split = apk("empty-split.apk")
    assertEquals(emptyList<String>(), AppFrameworkDetector.detect(listOf(base.path, split.path)))
    assertEquals(emptyList<String>(), AppFrameworkDetector.detectTechnologies(listOf(base.path, split.path)))
  }

  @Test
  fun recognizesAdditionalLibrariesAcrossSplitsWithoutChangingLegacyResults() {
    val base = apk("base.apk",
      "lib/arm64-v8a/libhermes.so",
      "lib/arm64-v8a/libQt6Widgets_arm64-v8a.so",
      "lib/arm64-v8a/libmonosgen-2.0.so",
      "lib/arm64-v8a/libil2cpp.so",
      "lib/arm64-v8a/libcocos.so",
      "META-INF/kotlinx_coroutines_core.version",
      "META-INF/com.google.android.material_material.version"
    )
    val split = apk("split.apk",
      "lib/x86_64/libhermesvm.so",
      "lib/x86_64/libjsc.so",
      "lib/x86_64/libv8android.so",
      "lib/x86_64/libQt5Quick.so",
      "lib/x86_64/libQt6Core_arm64-v8a.so",
      "META-INF/kotlinx_coroutines_android.version",
      "META-INF/androidx.compose.material3_material3.version"
    )
    val paths = listOf(base.path, split.path)
    assertEquals(
      setOf("hermes", "javaScriptCore", "mono", "il2cpp", "qtWidgets", "qtQuick", "kotlinCoroutines",
        "v8", "cocosCreator", "composeMaterial3", "androidMaterialComponents"),
      AppFrameworkDetector.detectTechnologies(paths).toSet()
    )
    assertEquals(listOf("hermes", "javaScriptCore", "v8", "qtQuick", "kotlinCoroutines", "composeMaterial3"),
      AppFrameworkDetector.detectTechnologies(listOf(split.path)))
    assertEquals(emptyList<String>(), AppFrameworkDetector.detect(paths))
  }

  @Test
  fun ignoresLookalikeTechnologyEntries() {
    val app = apk("lookalikes.apk",
      "assets/lib/arm64-v8a/libhermes.so",
      "lib/arm64-v8a/libhermestooling.so",
      "lib/arm64-v8a/libjsctooling.so",
      "lib/arm64-v8a/libQt6Quick_x86_64.so",
      "lib/arm64-v8a/libQt6Widgets.so.bak",
      "META-INF/kotlinx_coroutines_core.version.bak",
      "lib/arm64-v8a/libv8executor.so",
      "assets/lib/arm64-v8a/libcocos.so",
      "META-INF/androidx.compose.material3_material3.version.bak",
      "assets/META-INF/com.google.android.material_material.version",
      "lib/arm64-v8a/libmono.so"
    )
    assertEquals(emptyList<String>(), AppFrameworkDetector.detectTechnologies(listOf(app.path)))
  }

  @Test
  fun detectsDartFromKernelHeaderInThePackagedAsset() {
    val kernel = "assets/flutter_assets/kernel_blob.bin"
    val header = byteArrayOf(0x90.toByte(), 0xAB.toByte(), 0xCD.toByte(), 0xEF.toByte(), 0, 0, 0, 1)
    val base = apk("base.apk", "lib/arm64-v8a/libflutter.so", "lib/arm64-v8a/libapp.so")
    val split = apk("kernel.apk", mapOf(kernel to header))
    val truncated = apk("truncated.apk", mapOf(kernel to header.copyOf(4)))
    val wrongMagic = apk("wrong-magic.apk", mapOf(kernel to ByteArray(8)))
    val elsewhere = apk("other-path.apk", mapOf("assets/kernel_blob.bin" to header))
    assertEquals(listOf("flutter", "dart"), AppFrameworkDetector.detectTechnologies(listOf(base.path, split.path)))
    assertEquals(listOf("flutter"), AppFrameworkDetector.detect(listOf(base.path, split.path)))
    assertEquals(
      listOf("flutter"),
      AppFrameworkDetector.detectTechnologies(listOf(base.path, truncated.path, wrongMagic.path, elsewhere.path))
    )
  }

  @Test
  fun detectsCurrentAndLegacyDartAotExportsInElf32AndElf64() {
    val base = apk("base.apk", "lib/arm64-v8a/libflutter.so")
    val symbolPairs = listOf(
      listOf("_kDartSnapshotData", "_kDartSnapshotText"),
      listOf("_kDartIsolateSnapshotData", "_kDartIsolateSnapshotInstructions")
    )
    for (abi in listOf("armeabi-v7a", "arm64-v8a")) {
      for ((index, symbols) in symbolPairs.withIndex()) {
        val elf = aotElf(abi == "arm64-v8a", symbols)
        val split = apk("split-$abi-$index.apk", mapOf("lib/$abi/libapp.so" to elf))
        val paths = listOf(base.path, split.path)
        assertEquals(listOf("flutter", "dart"), AppFrameworkDetector.detectTechnologies(paths))
        assertEquals(listOf("flutter"), AppFrameworkDetector.detect(paths))
      }
    }
  }

  @Test
  fun ignoresInvalidOrIncompleteDartAotIndicators() {
    val names = listOf("_kDartSnapshotData", "_kDartSnapshotText")
    val elf = aotElf(true, names)
    val invalidLibraries = listOf(
      "lib/arm64-v8a/libapp.so" to names.joinToString("\u0000").toByteArray(),
      "lib/arm64-v8a/libapp.so" to aotElf(true, names.take(1)),
      "lib/arm64-v8a/libapp.so" to aotElf(true, listOf(names.first(), "_kDartIsolateSnapshotInstructions")),
      "lib/arm64-v8a/libapp.so" to aotElf(true, names, defined = false),
      "lib/arm64-v8a/libapp.so" to elf.copyOf(elf.size - 1),
      "assets/lib/arm64-v8a/libapp.so" to elf,
      "lib/arm64-v8a/libdifferent.so" to elf
    )
    for ((index, candidate) in invalidLibraries.withIndex()) {
      val app = apk("invalid-aot-$index.apk", mapOf(candidate))
      assertEquals(emptyList<String>(), AppFrameworkDetector.detectTechnologies(listOf(app.path)))
    }
  }

  @Test
  fun failsForUnreadableSplitsEvenAfterFindingEverySupportedFramework() {
    val base = apk("base.apk",
      "lib/arm64-v8a/libflutter.so",
      "lib/arm64-v8a/libreactnative.so",
      "lib/arm64-v8a/libunity.so",
      "lib/arm64-v8a/libUnreal.so",
      "lib/arm64-v8a/libgodot_android.so",
      "lib/arm64-v8a/libmonodroid.so",
      "lib/arm64-v8a/libNativeScript.so",
      "lib/arm64-v8a/libSDL3.so",
      "META-INF/androidx.compose.ui_ui.version",
      "META-INF/kotlin-stdlib.kotlin_module",
      "META-INF/androidx.appcompat_appcompat.version",
      "assets/capacitor.config.json",
      "assets/www/cordova.js",
      "lib/arm64-v8a/libQt6Core_arm64-v8a.so"
    )
    val missing = File(temporaryFolder.root, "missing.apk")
    val corrupt = temporaryFolder.newFile("corrupt.apk").apply { writeText("not a ZIP archive") }
    for (failed in listOf(missing, corrupt)) {
      assertThrows(IOException::class.java) { AppFrameworkDetector.detect(listOf(base.path, failed.path)) }
      assertThrows(IOException::class.java) { AppFrameworkDetector.detect(listOf(failed.path)) }
      assertThrows(IOException::class.java) { AppFrameworkDetector.detectTechnologies(listOf(base.path, failed.path)) }
    }
  }

  @Test
  fun rejectsUnavailableApkPathsInsteadOfReportingNoFrameworks() {
    for (paths in listOf(emptyList(), listOf(""), listOf(" \t"))) {
      assertThrows(IOException::class.java) { AppFrameworkDetector.detect(paths) }
    }
  }

  private fun aotElf(is64Bit: Boolean, names: List<String>, defined: Boolean = true): ByteArray {
    val sectionSize = if (is64Bit) 64 else 40
    val symbolSize = if (is64Bit) 24 else 16
    val encodedNames = names.map { "$it\u0000".toByteArray(Charsets.US_ASCII) }
    val stringsOffset = 64
    val stringsSize = 1 + encodedNames.sumOf { it.size }
    val symbolsOffset = stringsOffset + stringsSize
    val symbolsSize = (1 + names.size) * symbolSize
    val sectionsOffset = symbolsOffset + symbolsSize
    val elf = ByteBuffer.allocate(sectionsOffset + 4 * sectionSize).order(ByteOrder.LITTLE_ENDIAN)

    fun putWord(offset: Int, value: Int) {
      if (is64Bit) elf.putLong(offset, value.toLong()) else elf.putInt(offset, value)
    }

    elf.putInt(0, 0x464c457f)
    elf.put(4, if (is64Bit) 2 else 1)
    elf.put(5, 1)
    elf.put(6, 1)
    elf.putShort(16, 3)
    putWord(if (is64Bit) 40 else 32, sectionsOffset)
    elf.putShort(if (is64Bit) 58 else 46, sectionSize.toShort())
    elf.putShort(if (is64Bit) 60 else 48, 4)

    val stringHeader = sectionsOffset + sectionSize
    elf.putInt(stringHeader + 4, 3)
    putWord(stringHeader + (if (is64Bit) 24 else 16), stringsOffset)
    putWord(stringHeader + (if (is64Bit) 32 else 20), stringsSize)

    val symbolHeader = sectionsOffset + 2 * sectionSize
    elf.putInt(symbolHeader + 4, 11)
    putWord(symbolHeader + (if (is64Bit) 24 else 16), symbolsOffset)
    putWord(symbolHeader + (if (is64Bit) 32 else 20), symbolsSize)
    elf.putInt(symbolHeader + (if (is64Bit) 40 else 24), 1)
    putWord(symbolHeader + (if (is64Bit) 56 else 36), symbolSize)
    elf.putInt(sectionsOffset + 3 * sectionSize + 4, 1)

    elf.position(stringsOffset + 1)
    for ((index, name) in encodedNames.withIndex()) {
      val symbol = symbolsOffset + (index + 1) * symbolSize
      val infoOffset = symbol + (if (is64Bit) 4 else 12)
      elf.putInt(symbol, elf.position() - stringsOffset)
      elf.put(infoOffset, 0x11)
      elf.putShort(infoOffset + 2, if (defined) 3 else 0)
      elf.put(name)
    }
    return elf.array()
  }

  private fun apk(name: String, vararg entries: String): File =
    apk(name, entries.associateWith { byteArrayOf(0) })

  private fun apk(name: String, entries: Map<String, ByteArray>): File = temporaryFolder.newFile(name).also { file ->
    ZipOutputStream(file.outputStream()).use { output ->
      for ((entry, bytes) in entries) {
        output.putNextEntry(ZipEntry(entry))
        if (!entry.endsWith("/")) output.write(bytes)
        output.closeEntry()
      }
    }
  }
}
