package com.okmsbun.flutter_device_apps_android

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.InstallSourceInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
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
import org.junit.Test
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

/**
 * Unit tests for FlutterDeviceAppsAndroidPlugin.
 * 
 * These tests verify that methods handle null/invalid arguments correctly
 * using a TestablePlugin that extends the main plugin for testing purposes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
internal class FlutterDeviceAppsAndroidPluginTest {

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

  private fun queryIcon(iconPlugin: FlutterDeviceAppsAndroidPlugin, packageName: String): IconResult {
    val result = IconResult()
    iconPlugin.onMethodCall(MethodCall("getAppIcon", mapOf("packageName" to packageName)), result)
    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
    while (result.done.count != 0L && System.nanoTime() < deadline) {
      Shadows.shadowOf(Looper.getMainLooper()).idle()
      result.done.await(10, TimeUnit.MILLISECONDS)
    }
    assertEquals("Icon query did not complete", 0L, result.done.count)
    return result
  }

  private class IconResult : MethodChannel.Result {
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
  }

  @Test
  @Config(sdk = [28, 33])
  fun packageQueries_handleUnavailablePackages() {
    verifyQuery("isAppInstalled", "com.example.missing", false)
    verifyQuery("isSystemApp", "com.example.missing", null)
  }

  @Test
  fun packageQueries_rejectMissingOrBlankPackageNames() {
    for (method in listOf("isAppInstalled", "isSystemApp")) {
      for (args in listOf(null, emptyMap<String, Any>(), mapOf("packageName" to ""), mapOf("packageName" to " "))) {
        val result = createMockResult()
        plugin.onMethodCall(MethodCall(method, args), result)
        verify(result).error(Mockito.eq("ARG"), Mockito.eq("packageName required"), Mockito.isNull())
      }
    }
  }

  @Test
  fun packageQueries_doNotTreatUnexpectedFailuresAsMissingPackages() {
    val packageManager = mock(PackageManager::class.java)
    Mockito.`when`(packageManager.getApplicationInfo("com.example.denied", 0))
      .thenThrow(SecurityException("denied"))
    val failingPlugin = TestableFlutterDeviceAppsAndroidPlugin(RuntimeEnvironment.getApplication(), packageManager)
    for (method in listOf("isAppInstalled", "isSystemApp")) {
      val result = createMockResult()
      failingPlugin.onMethodCall(MethodCall(method, mapOf("packageName" to "com.example.denied")), result)
      verify(result).error(Mockito.eq("ERR_QUERY"), Mockito.eq("denied"), Mockito.isNull())
    }
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

  // ---- getInstallerStore ----
  @Test
  @Config(sdk = [28, 30])
  @Suppress("DEPRECATION")
  fun getInstallerStore_preservesLegacyResultsUsingVersionAppropriateApi() {
    val packageManager = mock(PackageManager::class.java)
    if (android.os.Build.VERSION.SDK_INT >= 30) {
      val info = mock(InstallSourceInfo::class.java)
      Mockito.`when`(info.installingPackageName).thenReturn("com.android.vending", null)
      Mockito.`when`(packageManager.getInstallSourceInfo("com.example.app")).thenReturn(info)
    } else {
      Mockito.`when`(packageManager.getInstallerPackageName("com.example.app"))
        .thenReturn("com.android.vending", null)
    }
    val testPlugin = TestableFlutterDeviceAppsAndroidPlugin(RuntimeEnvironment.getApplication(), packageManager)
    for (installer in listOf("com.android.vending", null)) {
      val result = createMockResult()
      testPlugin.onMethodCall(MethodCall("getInstallerStore", mapOf("packageName" to "com.example.app")), result)
      verify(result).success(installer)
    }
    if (android.os.Build.VERSION.SDK_INT >= 30) {
      verify(packageManager, Mockito.times(2)).getInstallSourceInfo("com.example.app")
    } else {
      verify(packageManager, Mockito.times(2)).getInstallerPackageName("com.example.app")
    }
    Mockito.verifyNoMoreInteractions(packageManager)
  }

  @Test
  @Config(sdk = [28, 30])
  @Suppress("DEPRECATION")
  fun getInstallerStore_preservesErrorsForUnavailablePackages() {
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
    testPlugin.onMethodCall(MethodCall("getInstallerStore", mapOf("packageName" to "com.example.missing")), result)
    verify(result).error(Mockito.eq("ERR_INSTALLER"), Mockito.eq("missing"), Mockito.isNull())
  }

  @Test
  fun onMethodCall_getInstallerStore_handlesNullPackageGracefully() {
    val call = MethodCall("getInstallerStore", null)
    val mockResult = createMockResult()

    plugin.onMethodCall(call, mockResult)

    verify(mockResult).error(Mockito.eq("ARG"), Mockito.eq("packageName required"), Mockito.isNull())
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
