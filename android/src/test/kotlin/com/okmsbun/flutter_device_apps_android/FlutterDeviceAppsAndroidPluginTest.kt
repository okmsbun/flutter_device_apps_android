package com.okmsbun.flutter_device_apps_android

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.InstallSourceInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.junit.Before
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Unit tests for FlutterDeviceAppsAndroidPlugin.
 * 
 * These tests verify that methods handle null/invalid arguments correctly
 * using a TestablePlugin that extends the main plugin for testing purposes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
internal class FlutterDeviceAppsAndroidPluginTest {

  @get:Rule
  val temporaryFolder = TemporaryFolder()

  private lateinit var plugin: TestableFlutterDeviceAppsAndroidPlugin

  @Before
  fun setUp() {
    val context = RuntimeEnvironment.getApplication()
    plugin = TestableFlutterDeviceAppsAndroidPlugin(context, context.packageManager)
  }

  private fun createMockResult(): MethodChannel.Result = mock(MethodChannel.Result::class.java)

  // ---- getAppIcon ----
  @Test
  fun getAppIcon_returnsDecodablePngWithoutFetchingMetadata() {
    val context = RuntimeEnvironment.getApplication()
    val bitmap = Bitmap.createBitmap(2, 3, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
    val packageManager = mock(PackageManager::class.java)
    Mockito.`when`(packageManager.getApplicationIcon("com.example.icon"))
      .thenReturn(BitmapDrawable(context.resources, bitmap))
    val iconPlugin = TestableFlutterDeviceAppsAndroidPlugin(context, packageManager)

    val result = queryIcon(iconPlugin, "com.example.icon")
    assertNull(result.errorCode)
    val bytes = result.value as ByteArray
    assertArrayEquals(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10), bytes.copyOf(8))
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    assertNotNull(decoded)
    assertEquals(2, decoded.width)
    assertEquals(3, decoded.height)
    assertEquals(Color.RED, decoded.getPixel(0, 0))
    verify(packageManager).getApplicationIcon("com.example.icon")
    Mockito.verifyNoMoreInteractions(packageManager)
  }

  @Test
  fun getAppIcon_rendersAdaptiveDrawables() {
    val context = RuntimeEnvironment.getApplication()
    val packageManager = mock(PackageManager::class.java)
    Mockito.`when`(packageManager.getApplicationIcon("com.example.adaptive"))
      .thenReturn(AdaptiveIconDrawable(ColorDrawable(Color.BLUE), ColorDrawable(Color.RED)))

    val result = queryIcon(TestableFlutterDeviceAppsAndroidPlugin(context, packageManager), "com.example.adaptive")
    assertNull(result.errorCode)
    val bytes = result.value as ByteArray
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    assertNotNull(decoded)
    assertTrue(decoded.width > 0)
    assertTrue(decoded.height > 0)
  }

  @Test
  fun getAppIcon_encodesPlatformDefaultIcon() {
    val context = RuntimeEnvironment.getApplication()
    val defaultIcon = context.resources.getDrawable(android.R.drawable.sym_def_app_icon, context.theme)
    val packageManager = mock(PackageManager::class.java)
    Mockito.`when`(packageManager.getApplicationIcon("com.example.noicon")).thenReturn(defaultIcon)
    val result = queryIcon(TestableFlutterDeviceAppsAndroidPlugin(context, packageManager), "com.example.noicon")
    assertNull(result.errorCode)
    val bytes = result.value as ByteArray
    assertNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
  }

  @Test
  fun getAppIcon_returnsNullForUnavailablePackage() {
    // Robolectric's icon shadow returns null rather than Android's documented exception.
    val context = RuntimeEnvironment.getApplication()
    val packageManager = mock(PackageManager::class.java)
    Mockito.`when`(packageManager.getApplicationIcon("com.example.missing"))
      .thenThrow(PackageManager.NameNotFoundException("com.example.missing"))
    val result = queryIcon(TestableFlutterDeviceAppsAndroidPlugin(context, packageManager), "com.example.missing")
    assertNull(result.errorCode)
    assertNull(result.value)
  }

  @Test
  fun getAppIcon_reportsUnexpectedFailures() {
    val context = RuntimeEnvironment.getApplication()
    val packageManager = mock(PackageManager::class.java)
    Mockito.`when`(packageManager.getApplicationIcon("com.example.denied"))
      .thenThrow(SecurityException("denied"))
    val result = queryIcon(TestableFlutterDeviceAppsAndroidPlugin(context, packageManager), "com.example.denied")
    assertEquals("ERR_ICON", result.errorCode)
  }

  @Test
  fun getAppIcon_rejectsMissingAndBlankPackageNames() {
    for (args in listOf(null, emptyMap<String, Any>(), mapOf("packageName" to ""), mapOf("packageName" to " "))) {
      val result = createMockResult()
      plugin.onMethodCall(MethodCall("getAppIcon", args), result)
      verify(result).error(Mockito.eq("ARG"), Mockito.eq("packageName required"), Mockito.isNull())
    }
  }

  private fun queryIcon(iconPlugin: FlutterDeviceAppsAndroidPlugin, packageName: String): AsyncResult =
    queryMethod(iconPlugin, "getAppIcon", mapOf("packageName" to packageName))

  private fun queryMethod(testPlugin: FlutterDeviceAppsAndroidPlugin, method: String, arguments: Map<String, Any?>): AsyncResult {
    val result = AsyncResult()
    testPlugin.onMethodCall(MethodCall(method, arguments), result)
    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
    while (result.done.count != 0L && System.nanoTime() < deadline) {
      Shadows.shadowOf(Looper.getMainLooper()).idle()
      result.done.await(10, TimeUnit.MILLISECONDS)
    }
    assertEquals("$method did not complete", 0L, result.done.count)
    return result
  }

  private class AsyncResult : MethodChannel.Result {
    val done = CountDownLatch(1)
    var value: Any? = null
    var errorCode: String? = null

    override fun success(result: Any?) {
      value = result
      done.countDown()
    }

    override fun error(code: String, message: String?, details: Any?) {
      errorCode = code
      done.countDown()
    }

    override fun notImplemented() {
      errorCode = "NOT_IMPLEMENTED"
      done.countDown()
    }
  }

  // ---- detectAppFrameworks ----
  @Test
  @Config(sdk = [28, 33])
  @Suppress("DEPRECATION")
  fun detectAppFrameworks_readsBaseAndSplitsWithoutLoadingAppMetadata() {
    val base = frameworkApk("base.apk", "lib/arm64-v8a/libunity.so")
    val split = frameworkApk("split.apk", "lib/arm64-v8a/libflutter.so", "lib/x86/libunity.so")
    val info = ApplicationInfo().apply {
      packageName = "com.example.app"
      sourceDir = base.path
      splitSourceDirs = arrayOf(split.path)
    }
    val packageManager = frameworkPackageManager(info)
    val testPlugin = TestableFlutterDeviceAppsAndroidPlugin(RuntimeEnvironment.getApplication(), packageManager)
    val result = queryMethod(testPlugin, "detectAppFrameworks", mapOf("packageName" to info.packageName))
    assertNull(result.errorCode)
    assertEquals(listOf("flutter", "unity"), result.value)
    if (android.os.Build.VERSION.SDK_INT >= 33) {
      verify(packageManager).getApplicationInfo(Mockito.eq(info.packageName), Mockito.any(PackageManager.ApplicationInfoFlags::class.java))
    } else {
      verify(packageManager).getApplicationInfo(info.packageName, 0)
    }
    Mockito.verifyNoMoreInteractions(packageManager)
  }

  @Test
  fun detectAppFrameworks_returnsEmptyForASuccessfulScanWithoutMatches() {
    val base = frameworkApk("base.apk", "classes.dex", "assets/libflutter.so")
    val info = ApplicationInfo().apply {
      packageName = "com.example.app"
      sourceDir = base.path
    }
    val testPlugin = TestableFlutterDeviceAppsAndroidPlugin(RuntimeEnvironment.getApplication(), frameworkPackageManager(info))
    val result = queryMethod(testPlugin, "detectAppFrameworks", mapOf("packageName" to info.packageName))
    assertNull(result.errorCode)
    assertEquals(emptyList<String>(), result.value)
  }

  @Test
  fun detectAppFrameworks_rejectsMissingBlankOrNonStringNames() {
    val packageManager = mock(PackageManager::class.java)
    val testPlugin = TestableFlutterDeviceAppsAndroidPlugin(RuntimeEnvironment.getApplication(), packageManager)
    for (args in listOf(null, emptyMap<String, Any?>(), mapOf("packageName" to ""), mapOf("packageName" to " "), mapOf("packageName" to 1))) {
      val result = createMockResult()
      testPlugin.onMethodCall(MethodCall("detectAppFrameworks", args), result)
      verify(result).error(Mockito.eq("ARG"), Mockito.eq("packageName required"), Mockito.isNull())
    }
    Mockito.verifyNoInteractions(packageManager)
  }

  @Test
  @Config(sdk = [28, 33])
  @Suppress("DEPRECATION")
  fun detectAppFrameworks_reportsMissingOrHiddenPackagesAsErrors() {
    val packageManager = mock(PackageManager::class.java)
    if (android.os.Build.VERSION.SDK_INT >= 33) {
      Mockito.`when`(packageManager.getApplicationInfo(Mockito.eq("com.missing.app"), Mockito.any(PackageManager.ApplicationInfoFlags::class.java)))
        .thenThrow(PackageManager.NameNotFoundException("missing"))
    } else {
      Mockito.`when`(packageManager.getApplicationInfo("com.missing.app", 0))
        .thenThrow(PackageManager.NameNotFoundException("missing"))
    }
    val testPlugin = TestableFlutterDeviceAppsAndroidPlugin(RuntimeEnvironment.getApplication(), packageManager)
    val result = queryMethod(testPlugin, "detectAppFrameworks", mapOf("packageName" to "com.missing.app"))
    assertEquals("APP_NOT_FOUND", result.errorCode)
    assertNull(result.value)
  }

  @Test
  fun detectAppFrameworks_doesNotReturnPartialMatchesWhenASplitCannotBeRead() {
    val base = frameworkApk("base.apk", "lib/arm64-v8a/libflutter.so")
    val missing = File(temporaryFolder.root, "missing.apk")
    val corrupt = temporaryFolder.newFile("corrupt.apk").apply { writeText("not an APK") }
    val info = ApplicationInfo().apply {
      packageName = "com.example.app"
      sourceDir = base.path
    }
    val testPlugin = TestableFlutterDeviceAppsAndroidPlugin(RuntimeEnvironment.getApplication(), frameworkPackageManager(info))
    for (failed in listOf(missing, corrupt)) {
      info.splitSourceDirs = arrayOf(failed.path)
      val result = queryMethod(testPlugin, "detectAppFrameworks", mapOf("packageName" to info.packageName))
      assertEquals("ERR_FRAMEWORK_SCAN", result.errorCode)
      assertNull(result.value)
    }
  }

  @Test
  fun detectAppFrameworks_reportsUnavailableBaseApkPathsAsErrors() {
    val info = ApplicationInfo().apply { packageName = "com.example.app" }
    val testPlugin = TestableFlutterDeviceAppsAndroidPlugin(RuntimeEnvironment.getApplication(), frameworkPackageManager(info))
    val result = queryMethod(testPlugin, "detectAppFrameworks", mapOf("packageName" to info.packageName))
    assertEquals("ERR_FRAMEWORK_SCAN", result.errorCode)
    assertNull(result.value)
  }

  @Test
  @Config(sdk = [28, 33])
  @Suppress("DEPRECATION")
  fun detectAppFrameworks_reportsAccessFailuresInsteadOfAnEmptyResult() {
    val packageManager = mock(PackageManager::class.java)
    if (android.os.Build.VERSION.SDK_INT >= 33) {
      Mockito.`when`(packageManager.getApplicationInfo(Mockito.eq("com.denied.app"), Mockito.any(PackageManager.ApplicationInfoFlags::class.java)))
        .thenThrow(SecurityException("denied"))
    } else {
      Mockito.`when`(packageManager.getApplicationInfo("com.denied.app", 0))
        .thenThrow(SecurityException("denied"))
    }
    val testPlugin = TestableFlutterDeviceAppsAndroidPlugin(RuntimeEnvironment.getApplication(), packageManager)
    val result = queryMethod(testPlugin, "detectAppFrameworks", mapOf("packageName" to "com.denied.app"))
    assertEquals("ERR_FRAMEWORK_SCAN", result.errorCode)
    assertNull(result.value)
  }

  @Suppress("DEPRECATION")
  private fun frameworkPackageManager(info: ApplicationInfo): PackageManager {
    val packageManager = mock(PackageManager::class.java)
    if (android.os.Build.VERSION.SDK_INT >= 33) {
      Mockito.`when`(packageManager.getApplicationInfo(Mockito.eq(info.packageName), Mockito.any(PackageManager.ApplicationInfoFlags::class.java)))
        .thenReturn(info)
    } else {
      Mockito.`when`(packageManager.getApplicationInfo(info.packageName, 0)).thenReturn(info)
    }
    return packageManager
  }

  private fun frameworkApk(name: String, vararg entries: String): File = temporaryFolder.newFile(name).also { file ->
    ZipOutputStream(file.outputStream()).use { output ->
      for (entry in entries) {
        output.putNextEntry(ZipEntry(entry))
        output.write(byteArrayOf(0))
        output.closeEntry()
      }
    }
  }

  // ---- getApps ----
  @Test
  @Config(sdk = [28, 33])
  @Suppress("DEPRECATION")
  fun getApps_queriesOnlyRequestedPackagesOnceInInputOrderWithIcons() {
    val user = createListPackage("com.example.user").apply {
      checkNotNull(applicationInfo).enabled = false
    }
    val system = createListPackage("com.android.system", ApplicationInfo.FLAG_SYSTEM)
    val unrequested = createListPackage("org.other.app")
    val packageManager = mockListPackageManager(listOf(user, system, unrequested))
    val missing = "com.example.missing"
    if (android.os.Build.VERSION.SDK_INT >= 33) {
      Mockito.`when`(packageManager.getPackageInfo(Mockito.eq(missing), Mockito.any(PackageManager.PackageInfoFlags::class.java)))
        .thenThrow(PackageManager.NameNotFoundException(missing))
    } else {
      Mockito.`when`(packageManager.getPackageInfo(missing, 0))
        .thenThrow(PackageManager.NameNotFoundException(missing))
    }
    val testPlugin = TestableFlutterDeviceAppsAndroidPlugin(RuntimeEnvironment.getApplication(), packageManager)
    val result = queryMethod(testPlugin, "getApps", mapOf(
      "packageNames" to listOf(system.packageName, missing, user.packageName, system.packageName, "", " "),
      "includeIcons" to true
    ))
    assertNull(result.errorCode)
    val apps = (result.value as List<*>).map { it as Map<*, *> }
    assertEquals(listOf(system.packageName, user.packageName), apps.map { it["packageName"] })
    assertEquals(true, apps[0]["isSystem"])
    assertEquals(false, apps[1]["enabled"])
    for (app in apps) {
      val bytes = app["iconBytes"] as ByteArray
      assertNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
    }
    for (pkg in listOf(system.packageName, missing, user.packageName)) {
      if (android.os.Build.VERSION.SDK_INT >= 33) {
        verify(packageManager).getPackageInfo(Mockito.eq(pkg), Mockito.any(PackageManager.PackageInfoFlags::class.java))
      } else {
        verify(packageManager).getPackageInfo(pkg, 0)
      }
    }
    for (info in listOf(system, user)) {
      verify(packageManager).getApplicationIcon(checkNotNull(info.applicationInfo))
    }
    if (android.os.Build.VERSION.SDK_INT >= 33) {
      verify(packageManager, Mockito.never()).getPackageInfo(Mockito.eq(unrequested.packageName), Mockito.any(PackageManager.PackageInfoFlags::class.java))
    } else {
      verify(packageManager, Mockito.never()).getPackageInfo(unrequested.packageName, 0)
    }
    verify(packageManager, Mockito.never()).getApplicationIcon(checkNotNull(unrequested.applicationInfo))
    verify(packageManager, Mockito.never()).getInstalledApplications(0)
    verify(packageManager, Mockito.never()).queryIntentActivities(Mockito.any(Intent::class.java), Mockito.eq(0))
    verify(packageManager, Mockito.never()).getLaunchIntentForPackage(Mockito.anyString())
  }

  @Test
  @Config(sdk = [28, 33])
  @Suppress("DEPRECATION")
  fun getApps_usesExactNamesWithoutTrimmingOrChangingCaseAndDefaultsToNoIcons() {
    val installed = createListPackage("com.example.user")
    val packageManager = mockListPackageManager(listOf(installed))
    val variants = listOf("Com.example.user", " com.example.user", "com.example.user ")
    for (pkg in variants) {
      if (android.os.Build.VERSION.SDK_INT >= 33) {
        Mockito.`when`(packageManager.getPackageInfo(Mockito.eq(pkg), Mockito.any(PackageManager.PackageInfoFlags::class.java)))
          .thenThrow(PackageManager.NameNotFoundException(pkg))
      } else {
        Mockito.`when`(packageManager.getPackageInfo(pkg, 0))
          .thenThrow(PackageManager.NameNotFoundException(pkg))
      }
    }
    val testPlugin = TestableFlutterDeviceAppsAndroidPlugin(RuntimeEnvironment.getApplication(), packageManager)
    val result = queryMethod(testPlugin, "getApps", mapOf("packageNames" to variants + installed.packageName))
    assertNull(result.errorCode)
    val apps = (result.value as List<*>).map { it as Map<*, *> }
    assertEquals(listOf(installed.packageName), apps.map { it["packageName"] })
    assertNull(apps.single()["iconBytes"])
    for (pkg in variants + installed.packageName) {
      if (android.os.Build.VERSION.SDK_INT >= 33) {
        verify(packageManager).getPackageInfo(Mockito.eq(pkg), Mockito.any(PackageManager.PackageInfoFlags::class.java))
      } else {
        verify(packageManager).getPackageInfo(pkg, 0)
      }
    }
    verify(packageManager, Mockito.never()).getApplicationIcon(Mockito.any(ApplicationInfo::class.java))
    verify(packageManager, Mockito.never()).getInstalledApplications(0)
    verify(packageManager, Mockito.never()).queryIntentActivities(Mockito.any(Intent::class.java), Mockito.eq(0))
  }

  @Test
  fun getApps_emptyOrBlankOnlyListsReturnEmptyWithoutQueryingPackages() {
    val packageManager = mock(PackageManager::class.java)
    val testPlugin = TestableFlutterDeviceAppsAndroidPlugin(RuntimeEnvironment.getApplication(), packageManager)
    for (names in listOf(emptyList(), listOf("", " ", "\n", "\t"))) {
      val result = queryMethod(testPlugin, "getApps", mapOf("packageNames" to names))
      assertNull(result.errorCode)
      assertEquals(emptyList<Any>(), result.value)
    }
    Mockito.verifyNoInteractions(packageManager)
  }

  @Test
  fun getApps_rejectsMissingOrNonStringPackageListsBeforeQueryingPackages() {
    val packageManager = mock(PackageManager::class.java)
    val testPlugin = TestableFlutterDeviceAppsAndroidPlugin(RuntimeEnvironment.getApplication(), packageManager)
    val arguments: List<Any?> = listOf(
      null,
      "invalid",
      emptyMap<String, Any?>(),
      mapOf("packageNames" to null),
      mapOf("packageNames" to "com.example.user"),
      mapOf("packageNames" to listOf(1)),
      mapOf("packageNames" to listOf("com.example.user", null))
    )
    for (args in arguments) {
      val result = createMockResult()
      testPlugin.onMethodCall(MethodCall("getApps", args), result)
      verify(result).error(Mockito.eq("ARG"), Mockito.eq("packageNames must be a list of strings"), Mockito.isNull())
    }
    Mockito.verifyNoInteractions(packageManager)
  }

  // ---- listApps package name filters ----
  @Test
  @Config(sdk = [28, 33])
  fun listApps_matchesPrefixOnlyAtTheStartAndIsCaseSensitive() {
    val packages = listOf(
      createListPackage("com.example.user"),
      createListPackage("com.example.other"),
      createListPackage("org.com.example.embedded"),
      createListPackage("Com.example.upper"),
      createListPackage("com.examples.lookalike")
    )
    val packageManager = mockListPackageManager(packages)
    val testPlugin = TestableFlutterDeviceAppsAndroidPlugin(RuntimeEnvironment.getApplication(), packageManager)

    for (onlyLaunchable in listOf(false, true)) {
      val apps = queryApps(testPlugin, mapOf(
        "onlyLaunchable" to onlyLaunchable,
        "packageNamePrefix" to "com.example."
      ))
      assertEquals(packages.take(2).map { it.packageName }, apps.map { it["packageName"] })
    }
  }

  @Test
  @Config(sdk = [28, 33])
  fun listApps_matchesSubstringAtStartMiddleAndEnd() {
    val packages = listOf(
      createListPackage("example.start"),
      createListPackage("com.example.middle"),
      createListPackage("com.vendor.example"),
      createListPackage("com.EXAMPLE.upper"),
      createListPackage("org.other.app")
    )
    val packageManager = mockListPackageManager(packages)
    val testPlugin = TestableFlutterDeviceAppsAndroidPlugin(RuntimeEnvironment.getApplication(), packageManager)

    for (onlyLaunchable in listOf(false, true)) {
      val apps = queryApps(testPlugin, mapOf(
        "onlyLaunchable" to onlyLaunchable,
        "packageNameContains" to "example"
      ))
      assertEquals(packages.take(3).map { it.packageName }, apps.map { it["packageName"] })
    }
  }

  @Test
  @Config(sdk = [28, 33])
  @Suppress("DEPRECATION")
  fun listApps_combinesBothFiltersBeforeLoadingMetadataAndIconsWithExistingOptions() {
    val packages = listOf(
      createListPackage("com.example.user"),
      createListPackage("com.example.system", ApplicationInfo.FLAG_SYSTEM),
      createListPackage("com.example.background"),
      createListPackage("org.example.app"),
      createListPackage("com.other.app"),
      createListPackage("com.Example.upper")
    )
    val launcherPackages = packages.filter { it.packageName != "com.example.background" }
    val packageManager = mockListPackageManager(packages, launcherPackages)
    val testPlugin = TestableFlutterDeviceAppsAndroidPlugin(RuntimeEnvironment.getApplication(), packageManager)

    for (onlyLaunchable in listOf(false, true)) {
      for (includeSystem in listOf(false, true)) {
        val apps = queryApps(testPlugin, mapOf(
          "includeSystem" to includeSystem,
          "onlyLaunchable" to onlyLaunchable,
          "includeIcons" to true,
          "packageNamePrefix" to "com.",
          "packageNameContains" to "example"
        ))
        val expected = mutableListOf("com.example.user")
        if (includeSystem) expected.add("com.example.system")
        if (!onlyLaunchable) expected.add("com.example.background")
        assertEquals(expected, apps.map { it["packageName"] })
        for (app in apps) {
          val bytes = app["iconBytes"] as ByteArray
          assertNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
        }
      }
    }

    for (ignored in packages.takeLast(3)) {
      if (android.os.Build.VERSION.SDK_INT >= 33) {
        verify(packageManager, Mockito.never()).getPackageInfo(Mockito.eq(ignored.packageName), Mockito.any(PackageManager.PackageInfoFlags::class.java))
      } else {
        verify(packageManager, Mockito.never()).getPackageInfo(ignored.packageName, 0)
      }
      val appInfo = checkNotNull(ignored.applicationInfo)
      verify(packageManager, Mockito.never()).getApplicationLabel(appInfo)
      verify(packageManager, Mockito.never()).getApplicationIcon(appInfo)
    }
    verify(packageManager, Mockito.times(2)).getInstalledApplications(0)
    verify(packageManager, Mockito.times(2)).queryIntentActivities(Mockito.any(Intent::class.java), Mockito.eq(0))
  }

  @Test
  @Config(sdk = [28, 33])
  fun listApps_omittedNullAndEmptyFiltersPreserveListing() {
    val packages = listOf(createListPackage("com.example.user"), createListPackage("org.other.app"))
    val packageManager = mockListPackageManager(packages)
    val testPlugin = TestableFlutterDeviceAppsAndroidPlugin(RuntimeEnvironment.getApplication(), packageManager)
    val prefixes = listOf(emptyMap(), mapOf("packageNamePrefix" to null), mapOf("packageNamePrefix" to ""))
    val substrings = listOf(emptyMap(), mapOf("packageNameContains" to null), mapOf("packageNameContains" to ""))

    for (onlyLaunchable in listOf(false, true)) {
      for (prefixArguments in prefixes) {
        for (substringArguments in substrings) {
          val apps = queryApps(testPlugin, mapOf("onlyLaunchable" to onlyLaunchable) + prefixArguments + substringArguments)
          assertEquals(packages.map { it.packageName }, apps.map { it["packageName"] })
          assertTrue(apps.all { it["iconBytes"] == null })
        }
      }
    }
    verify(packageManager, Mockito.never()).getApplicationIcon(Mockito.any(ApplicationInfo::class.java))
  }

  @Test
  @Config(sdk = [28, 33])
  fun listApps_unmatchedFiltersReturnEmptyWithoutLoadingMetadata() {
    val packages = listOf(createListPackage("com.example.user"))
    val packageManager = mockListPackageManager(packages)
    val testPlugin = TestableFlutterDeviceAppsAndroidPlugin(RuntimeEnvironment.getApplication(), packageManager)

    val filters = listOf(
      mapOf("packageNamePrefix" to "net.missing."),
      mapOf("packageNamePrefix" to " com.example."),
      mapOf("packageNameContains" to "net.missing."),
      mapOf("packageNameContains" to " com.example.")
    )
    for (onlyLaunchable in listOf(false, true)) {
      for (filterArguments in filters) {
        val apps = queryApps(testPlugin, mapOf(
          "onlyLaunchable" to onlyLaunchable,
          "includeIcons" to true
        ) + filterArguments)
        assertTrue(apps.isEmpty())
      }
    }
    verify(packageManager, Mockito.times(4)).getInstalledApplications(0)
    verify(packageManager, Mockito.times(4)).queryIntentActivities(Mockito.any(Intent::class.java), Mockito.eq(0))
    Mockito.verifyNoMoreInteractions(packageManager)
  }

  private fun createListPackage(pkg: String, flags: Int = 0): PackageInfo = PackageInfo().apply {
    packageName = pkg
    applicationInfo = ApplicationInfo().apply {
      packageName = pkg
      this.flags = flags
      enabled = true
    }
  }

  @Suppress("DEPRECATION")
  private fun mockListPackageManager(packages: List<PackageInfo>, launcherPackages: List<PackageInfo> = packages): PackageManager {
    val packageManager = mock(PackageManager::class.java)
    Mockito.`when`(packageManager.getInstalledApplications(0)).thenReturn(packages.map { checkNotNull(it.applicationInfo) })
    val entries = launcherPackages.map { info ->
      ResolveInfo().apply { activityInfo = ActivityInfo().apply { packageName = info.packageName } }
    }
    Mockito.`when`(packageManager.queryIntentActivities(Mockito.any(Intent::class.java), Mockito.eq(0))).thenReturn(entries)
    for (info in packages) {
      if (android.os.Build.VERSION.SDK_INT >= 33) {
        Mockito.`when`(packageManager.getPackageInfo(Mockito.eq(info.packageName), Mockito.any(PackageManager.PackageInfoFlags::class.java))).thenReturn(info)
      } else {
        Mockito.`when`(packageManager.getPackageInfo(info.packageName, 0)).thenReturn(info)
      }
      val appInfo = checkNotNull(info.applicationInfo)
      Mockito.`when`(packageManager.getApplicationLabel(appInfo)).thenReturn(info.packageName)
      Mockito.`when`(packageManager.getApplicationIcon(appInfo)).thenReturn(ColorDrawable(Color.RED))
    }
    return packageManager
  }

  private fun queryApps(testPlugin: FlutterDeviceAppsAndroidPlugin, arguments: Map<String, Any?>): List<Map<*, *>> {
    val result = queryMethod(testPlugin, "listApps", arguments)
    assertNull(result.errorCode)
    return (result.value as List<*>).map { it as Map<*, *> }
  }

  // Exercise both the legacy and typed ApplicationInfoFlags overloads.
  @Test
  @Config(sdk = [28, 33])
  fun packageQueries_distinguishSystemAndUserAppsWithoutLauncherEntries() {
    installPackage("com.example.user", 0, true)
    installPackage("com.example.system", ApplicationInfo.FLAG_SYSTEM, true)

    verifyQuery("isAppInstalled", "com.example.user", true)
    verifyQuery("isAppInstalled", "com.example.system", true)
    verifyQuery("isSystemApp", "com.example.user", false)
    verifyQuery("isSystemApp", "com.example.system", true)
    verifyQuery("isAppEnabled", "com.example.user", true)
    verifyQuery("isAppEnabled", "com.example.system", true)
  }

  @Test
  @Config(sdk = [28, 33])
  fun packageQueries_includeDisabledApps() {
    installPackage("com.example.disabled", 0, false)
    installPackage("com.example.disabled.system", ApplicationInfo.FLAG_SYSTEM, false)

    verifyQuery("isAppInstalled", "com.example.disabled", true)
    verifyQuery("isSystemApp", "com.example.disabled", false)
    verifyQuery("isAppInstalled", "com.example.disabled.system", true)
    verifyQuery("isSystemApp", "com.example.disabled.system", true)
    verifyQuery("isAppEnabled", "com.example.disabled", false)
    verifyQuery("isAppEnabled", "com.example.disabled.system", false)
  }

  @Test
  @Config(sdk = [28, 33])
  fun packageQueries_handleUnavailablePackages() {
    verifyQuery("isAppInstalled", "com.example.missing", false)
    verifyQuery("isSystemApp", "com.example.missing", null)
    verifyQuery("isAppEnabled", "com.example.missing", null)
  }

  @Test
  fun packageQueries_rejectMissingOrBlankPackageNames() {
    for (method in listOf("isAppInstalled", "isSystemApp", "isAppEnabled", "isAppLaunchable")) {
      for (args in listOf(null, emptyMap<String, Any>(), mapOf("packageName" to ""), mapOf("packageName" to " "))) {
        val result = createMockResult()
        plugin.onMethodCall(MethodCall(method, args), result)
        verify(result).error(Mockito.eq("ARG"), Mockito.eq("packageName required"), Mockito.isNull())
      }
    }
  }

  @Test
  @Config(sdk = [28, 33])
  @Suppress("DEPRECATION")
  fun packageQueries_doNotTreatUnexpectedFailuresAsMissingPackages() {
    val packageManager = mock(PackageManager::class.java)
    if (android.os.Build.VERSION.SDK_INT >= 33) {
      Mockito.`when`(packageManager.getApplicationInfo(Mockito.eq("com.example.denied"), Mockito.any(PackageManager.ApplicationInfoFlags::class.java)))
        .thenThrow(SecurityException("denied"))
    } else {
      Mockito.`when`(packageManager.getApplicationInfo("com.example.denied", 0))
        .thenThrow(SecurityException("denied"))
    }
    val failingPlugin = TestableFlutterDeviceAppsAndroidPlugin(RuntimeEnvironment.getApplication(), packageManager)
    for (method in listOf("isAppInstalled", "isSystemApp", "isAppEnabled")) {
      val result = createMockResult()
      failingPlugin.onMethodCall(MethodCall(method, mapOf("packageName" to "com.example.denied")), result)
      verify(result).error(Mockito.eq("ERR_QUERY"), Mockito.eq("denied"), Mockito.isNull())
    }
  }

  @Test
  @Config(sdk = [28, 33])
  fun isAppLaunchable_checksLaunchIntentWithoutLaunchingOrLoadingMetadata() {
    val context = mock(Context::class.java)
    val packageManager = mock(PackageManager::class.java)
    Mockito.`when`(packageManager.getLaunchIntentForPackage("com.example.app"))
      .thenReturn(Intent(Intent.ACTION_MAIN).setPackage("com.example.app"))
    val testPlugin = TestableFlutterDeviceAppsAndroidPlugin(context, packageManager)
    val result = createMockResult()

    testPlugin.onMethodCall(MethodCall("isAppLaunchable", mapOf("packageName" to "com.example.app")), result)

    verify(result).success(true)
    verify(packageManager).getLaunchIntentForPackage("com.example.app")
    Mockito.verifyNoMoreInteractions(packageManager)
    Mockito.verifyNoInteractions(context)
  }

  @Test
  @Config(sdk = [28, 33])
  fun isAppLaunchable_returnsFalseForPackagesWithoutAnEntryActivity() {
    installPackage("com.example.no_launcher", 0, true)
    installPackage("com.example.disabled", 0, false)

    verifyQuery("isAppLaunchable", "com.example.no_launcher", false)
    verifyQuery("isAppLaunchable", "com.example.disabled", false)
    verifyQuery("isAppLaunchable", "com.example.missing", false)
  }

  @Test
  fun isAppLaunchable_propagatesUnexpectedFailures() {
    val packageManager = mock(PackageManager::class.java)
    Mockito.`when`(packageManager.getLaunchIntentForPackage("com.example.denied"))
      .thenThrow(SecurityException("denied"))
    val testPlugin = TestableFlutterDeviceAppsAndroidPlugin(RuntimeEnvironment.getApplication(), packageManager)
    val result = createMockResult()

    testPlugin.onMethodCall(MethodCall("isAppLaunchable", mapOf("packageName" to "com.example.denied")), result)

    verify(result).error(Mockito.eq("ERR_QUERY"), Mockito.eq("denied"), Mockito.isNull())
  }

  private fun installPackage(packageName: String, flags: Int, enabled: Boolean) {
    val info = PackageInfo().apply {
      this.packageName = packageName
      applicationInfo = ApplicationInfo().apply {
        this.packageName = packageName
        this.flags = flags
        this.enabled = enabled
      }
    }
    Shadows.shadowOf(RuntimeEnvironment.getApplication().packageManager).installPackage(info)
  }

  private fun verifyQuery(method: String, packageName: String, expected: Boolean?) {
    val result = createMockResult()
    plugin.onMethodCall(MethodCall(method, mapOf("packageName" to packageName)), result)
    verify(result).success(expected)
  }

  // ---- getRequestedPermissions ----
  @Test
  fun onMethodCall_getRequestedPermissions_handlesNullPackageGracefully() {
    val call = MethodCall("getRequestedPermissions", null)
    val mockResult = createMockResult()

    plugin.onMethodCall(call, mockResult)

    verify(mockResult).error(Mockito.eq("ARG"), Mockito.eq("packageName required"), Mockito.isNull())
  }

  @Test
  fun onMethodCall_getRequestedPermissions_handlesEmptyArgsGracefully() {
    val call = MethodCall("getRequestedPermissions", emptyMap<String, Any>())
    val mockResult = createMockResult()

    plugin.onMethodCall(call, mockResult)

    verify(mockResult).error(Mockito.eq("ARG"), Mockito.eq("packageName required"), Mockito.isNull())
  }

  // ---- getApp ----
  @Test
  fun onMethodCall_getApp_handlesNullPackageGracefully() {
    val call = MethodCall("getApp", null)
    val mockResult = createMockResult()

    plugin.onMethodCall(call, mockResult)

    verify(mockResult).error(Mockito.eq("ARG"), Mockito.eq("packageName required"), Mockito.isNull())
  }

  @Test
  fun onMethodCall_getApp_handlesEmptyArgsGracefully() {
    val call = MethodCall("getApp", mapOf("includeIcon" to false))
    val mockResult = createMockResult()

    plugin.onMethodCall(call, mockResult)

    verify(mockResult).error(Mockito.eq("ARG"), Mockito.eq("packageName required"), Mockito.isNull())
  }

  // ---- openApp ----
  @Test
  fun onMethodCall_openApp_handlesNullPackageGracefully() {
    val call = MethodCall("openApp", null)
    val mockResult = createMockResult()

    plugin.onMethodCall(call, mockResult)

    verify(mockResult).error(Mockito.eq("ARG"), Mockito.eq("packageName required"), Mockito.isNull())
  }

  // ---- openAppSettings ----
  @Test
  fun onMethodCall_openAppSettings_handlesNullPackageGracefully() {
    val call = MethodCall("openAppSettings", null)
    val mockResult = createMockResult()

    plugin.onMethodCall(call, mockResult)

    verify(mockResult).error(Mockito.eq("ARG"), Mockito.eq("packageName required"), Mockito.isNull())
  }

  // ---- uninstallApp ----
  @Test
  fun onMethodCall_uninstallApp_handlesNullPackageGracefully() {
    val call = MethodCall("uninstallApp", null)
    val mockResult = createMockResult()

    plugin.onMethodCall(call, mockResult)

    verify(mockResult).error(Mockito.eq("ARG"), Mockito.eq("packageName required"), Mockito.isNull())
  }

  // ---- getInstallSourceInfo ----
  @Test
  @Config(sdk = [30])
  fun getInstallSourceInfo_readsNamesOnAndroid30() {
    verifyModernInstallSource(null, null)
  }

  @Test
  @Config(sdk = [33])
  fun getInstallSourceInfo_readsPackageSourceOnAndroid33() {
    verifyModernInstallSource(2, null)
  }

  @Test
  @Config(sdk = [34])
  fun getInstallSourceInfo_readsUpdateOwnerOnAndroid34() {
    verifyModernInstallSource(2, "com.example.owner")
  }

  private fun verifyModernInstallSource(packageSource: Int?, updateOwner: String?) {
    val packageManager = mock(PackageManager::class.java)
    val info = mock(InstallSourceInfo::class.java)
    Mockito.`when`(packageManager.getInstallSourceInfo("com.example.app")).thenReturn(info)
    Mockito.`when`(info.installingPackageName).thenReturn("com.android.vending")
    Mockito.`when`(info.initiatingPackageName).thenReturn("com.example.installer")
    Mockito.`when`(info.originatingPackageName).thenReturn("com.example.browser")
    if (packageSource != null) Mockito.`when`(info.packageSource).thenReturn(packageSource)
    if (updateOwner != null) Mockito.`when`(info.updateOwnerPackageName).thenReturn(updateOwner)
    val testPlugin = TestableFlutterDeviceAppsAndroidPlugin(RuntimeEnvironment.getApplication(), packageManager)
    val result = createMockResult()

    testPlugin.onMethodCall(MethodCall("getInstallSourceInfo", mapOf("packageName" to "com.example.app")), result)

    verify(result).success(mapOf(
      "installingPackageName" to "com.android.vending",
      "initiatingPackageName" to "com.example.installer",
      "originatingPackageName" to "com.example.browser",
      "packageSource" to packageSource,
      "updateOwnerPackageName" to updateOwner
    ))
    verify(packageManager).getInstallSourceInfo("com.example.app")
    Mockito.verifyNoMoreInteractions(packageManager)
    verify(info).getInstallingPackageName()
    verify(info).getInitiatingPackageName()
    verify(info).getOriginatingPackageName()
    if (packageSource != null) verify(info).getPackageSource()
    if (updateOwner != null) verify(info).getUpdateOwnerPackageName()
    Mockito.verifyNoMoreInteractions(info)
  }

  @Test
  @Suppress("DEPRECATION")
  fun getInstallSourceInfo_usesLegacyInstallerAndPreservesUnknownSource() {
    val packageManager = mock(PackageManager::class.java)
    Mockito.`when`(packageManager.getInstallerPackageName("com.example.app"))
      .thenReturn("com.android.vending")
    val testPlugin = TestableFlutterDeviceAppsAndroidPlugin(RuntimeEnvironment.getApplication(), packageManager)
    for ((pkg, installer) in listOf("com.example.app" to "com.android.vending", "com.example.unknown" to null)) {
      val result = createMockResult()
      testPlugin.onMethodCall(MethodCall("getInstallSourceInfo", mapOf("packageName" to pkg)), result)
      verify(result).success(mapOf("installingPackageName" to installer))
      verify(packageManager).getInstallerPackageName(pkg)
    }
    Mockito.verifyNoMoreInteractions(packageManager)
  }

  @Test
  @Config(sdk = [30, 34])
  fun getInstallSourceInfo_preservesAvailablePackagesWithUnknownSource() {
    val packageManager = mock(PackageManager::class.java)
    Mockito.`when`(packageManager.getInstallSourceInfo("com.example.app"))
      .thenReturn(mock(InstallSourceInfo::class.java))
    val testPlugin = TestableFlutterDeviceAppsAndroidPlugin(RuntimeEnvironment.getApplication(), packageManager)
    val result = createMockResult()
    testPlugin.onMethodCall(MethodCall("getInstallSourceInfo", mapOf("packageName" to "com.example.app")), result)
    verify(result).success(mapOf(
      "installingPackageName" to null,
      "initiatingPackageName" to null,
      "originatingPackageName" to null,
      "packageSource" to if (android.os.Build.VERSION.SDK_INT >= 33) 0 else null,
      "updateOwnerPackageName" to null
    ))
  }

  @Test
  @Config(sdk = [28, 30])
  @Suppress("DEPRECATION")
  fun getInstallSourceInfo_returnsNullForUnavailablePackages() {
    val packageManager = mock(PackageManager::class.java)
    if (android.os.Build.VERSION.SDK_INT >= 30) {
      Mockito.`when`(packageManager.getInstallSourceInfo("com.example.missing"))
        .thenThrow(PackageManager.NameNotFoundException("missing"))
    } else {
      Mockito.`when`(packageManager.getInstallerPackageName("com.example.missing"))
        .thenThrow(IllegalArgumentException("missing"))
    }
    val testPlugin = TestableFlutterDeviceAppsAndroidPlugin(RuntimeEnvironment.getApplication(), packageManager)
    val result = createMockResult()
    testPlugin.onMethodCall(MethodCall("getInstallSourceInfo", mapOf("packageName" to "com.example.missing")), result)
    verify(result).success(null)
  }

  @Test
  fun getInstallSourceInfo_rejectsMissingOrBlankPackageNames() {
    for (args in listOf(null, emptyMap<String, Any>(), mapOf("packageName" to ""), mapOf("packageName" to " "))) {
      val result = createMockResult()
      plugin.onMethodCall(MethodCall("getInstallSourceInfo", args), result)
      verify(result).error(Mockito.eq("ARG"), Mockito.eq("packageName required"), Mockito.isNull())
    }
  }

  @Test
  @Config(sdk = [28, 30])
  @Suppress("DEPRECATION")
  fun getInstallSourceInfo_propagatesUnexpectedFailures() {
    val packageManager = mock(PackageManager::class.java)
    if (android.os.Build.VERSION.SDK_INT >= 30) {
      Mockito.`when`(packageManager.getInstallSourceInfo("com.example.denied"))
        .thenThrow(SecurityException("denied"))
    } else {
      Mockito.`when`(packageManager.getInstallerPackageName("com.example.denied"))
        .thenThrow(SecurityException("denied"))
    }
    val testPlugin = TestableFlutterDeviceAppsAndroidPlugin(RuntimeEnvironment.getApplication(), packageManager)
    val result = createMockResult()
    testPlugin.onMethodCall(MethodCall("getInstallSourceInfo", mapOf("packageName" to "com.example.denied")), result)
    verify(result).error(Mockito.eq("ERR_INSTALL_SOURCE"), Mockito.eq("denied"), Mockito.isNull())
  }

  // ---- Unknown method ----
  @Test
  fun onMethodCall_unknownMethod_returnsNotImplemented() {
    val call = MethodCall("unknownMethod", null)
    val mockResult = createMockResult()

    plugin.onMethodCall(call, mockResult)

    verify(mockResult).notImplemented()
  }

  // ---- startAppChangeStream / stopAppChangeStream ----
  @Test
  fun onMethodCall_startAppChangeStream_returnsSuccess() {
    val call = MethodCall("startAppChangeStream", null)
    val mockResult = createMockResult()

    plugin.onMethodCall(call, mockResult)

    verify(mockResult).success(null)
  }

  @Test
  fun onMethodCall_stopAppChangeStream_returnsSuccess() {
    val call = MethodCall("stopAppChangeStream", null)
    val mockResult = createMockResult()

    plugin.onMethodCall(call, mockResult)

    verify(mockResult).success(null)
  }
}

/**
 * A testable version of FlutterDeviceAppsAndroidPlugin that allows
 * direct initialization without FlutterPluginBinding.
 */
internal class TestableFlutterDeviceAppsAndroidPlugin(
  context: Context,
  packageManager: PackageManager
) : FlutterDeviceAppsAndroidPlugin() {

  init {
    appContext = context
    pm = packageManager
  }
}
