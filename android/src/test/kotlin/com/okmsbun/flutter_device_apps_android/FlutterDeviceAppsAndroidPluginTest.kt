package com.okmsbun.flutter_device_apps_android

import android.content.Context
import android.content.pm.PackageManager
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

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

  // ---- getInstallerStore ----
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