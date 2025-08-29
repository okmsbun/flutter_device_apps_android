package com.okmsbun.flutter_device_apps_android

import android.content.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream

class FlutterDeviceAppsAndroidPlugin : FlutterPlugin, MethodChannel.MethodCallHandler, EventChannel.StreamHandler {

  private lateinit var methodChannel: MethodChannel
  private lateinit var eventChannel: EventChannel
  private lateinit var appContext: Context
  private lateinit var pm: PackageManager

  private val mainHandler = Handler(Looper.getMainLooper())
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private var receiverRegistered = false
  private var eventSink: EventChannel.EventSink? = null
  private val receiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      val i = intent ?: return
      val pkg = i.data?.schemeSpecificPart ?: return
      val replacing = i.getBooleanExtra(Intent.EXTRA_REPLACING, false)
      val action = i.action ?: return
      val type = when (action) {
        Intent.ACTION_PACKAGE_ADDED -> "installed"
        Intent.ACTION_PACKAGE_REMOVED -> "removed"
        Intent.ACTION_PACKAGE_CHANGED,
        Intent.ACTION_PACKAGE_REPLACED -> "updated"
        Intent.ACTION_PACKAGE_FULLY_REMOVED -> "removed"
        else -> return
      }
      val map = mapOf(
        "packageName" to pkg,
        "type" to type,
        "isReplacing" to replacing
      )
      mainHandler.post { eventSink?.success(map) }
    }
  }

  override fun onAttachedToEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    appContext = binding.applicationContext
    pm = appContext.packageManager
    methodChannel = MethodChannel(binding.binaryMessenger, "flutter_device_apps/methods")
    methodChannel.setMethodCallHandler(this)
    eventChannel = EventChannel(binding.binaryMessenger, "flutter_device_apps/app_changes")
    eventChannel.setStreamHandler(this)
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    methodChannel.setMethodCallHandler(null)
    eventChannel.setStreamHandler(null)
    unregisterReceiverIfNeeded()
    scope.cancel()
  }

  // ---- MethodChannel ----
  override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
    when (call.method) {
      "listApps" -> {
        val includeSystem = call.argument<Boolean>("includeSystem") ?: false
        val onlyLaunchable = call.argument<Boolean>("onlyLaunchable") ?: true
        val includeIcons = call.argument<Boolean>("includeIcons") ?: false
        scope.launch {
          try {
            val apps = listAppsInternal(includeSystem, onlyLaunchable, includeIcons)
            mainHandler.post { result.success(apps) }
          } catch (e: Exception) {
            mainHandler.post { result.error("ERR_LIST", e.message, null) }
          }
        }
      }
      "getApp" -> {
        val pkg = call.argument<String>("packageName")
        val includeIcon = call.argument<Boolean>("includeIcon") ?: false
        if (pkg == null) return result.error("ARG", "packageName required", null)
        scope.launch {
          try {
            val m = getAppMap(pkg, includeIcon)
            mainHandler.post { result.success(m) }
          } catch (e: Exception) {
            mainHandler.post { result.error("ERR_GET", e.message, null) }
          }
        }
      }
      "openApp" -> {
        val pkg = call.argument<String>("packageName")
        if (pkg == null) return result.error("ARG", "packageName required", null)
        try {
          val intent = pm.getLaunchIntentForPackage(pkg)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
          if (intent != null) {
            appContext.startActivity(intent)
            result.success(true)
          } else {
            result.success(false)
          }
        } catch (e: Exception) {
          result.error("ERR_OPEN", e.message, null)
        }
      }
      "startAppChangeStream" -> {
        registerReceiverIfNeeded()
        result.success(null)
      }
      "stopAppChangeStream" -> {
        unregisterReceiverIfNeeded()
        result.success(null)
      }
      "openAppSettings" -> {
        val pkg = call.argument<String>("packageName")
        if (pkg == null) {
          result.error("ARG", "packageName required", null)
          return
        }
        try {
          val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", pkg, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
          appContext.startActivity(intent)
          result.success(true)
        } catch (e: Exception) {
          try {
            val fallback = Intent(android.provider.Settings.ACTION_APPLICATION_SETTINGS).apply {
              addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(fallback)
            result.success(true)
          } catch (e2: Exception) {
            result.error("ERR_OPEN_SETTINGS", e2.message, null)
          }
        }
      }
      "uninstallApp" -> {
        val pkg = call.argument<String>("packageName")
        if (pkg == null) {
          result.error("ARG", "packageName required", null)
          return
        }
        try {
          val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
            data = android.net.Uri.parse("package:$pkg")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
          appContext.startActivity(intent)
          result.success(true)
        } catch (e: Exception) {
          try {
            val alt = Intent(Intent.ACTION_DELETE).apply {
              data = android.net.Uri.parse("package:$pkg")
              addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(alt)
            result.success(true)
          } catch (e2: Exception) {
            result.error("ERR_UNINSTALL", e2.message, null)
          }
        }
      }

      else -> result.notImplemented()
    }
  }

  // ---- EventChannel ----
  override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
    eventSink = events
    registerReceiverIfNeeded()
  }

  override fun onCancel(arguments: Any?) {
    eventSink = null
    unregisterReceiverIfNeeded()
  }

  private fun registerReceiverIfNeeded() {
    if (receiverRegistered) return
    val f = IntentFilter().apply {
      addAction(Intent.ACTION_PACKAGE_ADDED)
      addAction(Intent.ACTION_PACKAGE_REMOVED)
      addAction(Intent.ACTION_PACKAGE_CHANGED)
      addAction(Intent.ACTION_PACKAGE_REPLACED)
      addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
      addDataScheme("package")
    }
    appContext.registerReceiver(receiver, f)
    receiverRegistered = true
  }

  private fun unregisterReceiverIfNeeded() {
    if (!receiverRegistered) return
    try { appContext.unregisterReceiver(receiver) } catch (_: Exception) {}
    receiverRegistered = false
  }

  // ---- Helpers ----
  private fun listAppsInternal(includeSystem: Boolean, onlyLaunchable: Boolean, includeIcons: Boolean): List<Map<String, Any?>> {
    val packageNames: Set<String> = if (onlyLaunchable) {
      val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
      pm.queryIntentActivities(intent, 0).map { it.activityInfo.packageName }.toSet()
    } else {
      pm.getInstalledApplications(0).map { it.packageName }.toSet()
    }
    return packageNames.mapNotNull { pkg ->
      try {
        val m = getAppMap(pkg, includeIcons)
        if (m == null) null
        else {
          val isSystem = (m["isSystem"] as? Boolean) ?: false
          if (!includeSystem && isSystem) null else m
        }
      } catch (_: Exception) { null }
    }
  }

  private fun getAppMap(packageName: String, includeIcon: Boolean): Map<String, Any?>? {
    val pInfo: PackageInfo = try {
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
      } else {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(packageName, 0)
      }
    } catch (_: Exception) {
      return null
    }
  
    val aInfo: ApplicationInfo = pInfo.applicationInfo ?: return null
  
    val isSystem = (aInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    val label = try {
      pm.getApplicationLabel(aInfo).toString()
    } catch (_: Exception) {
      packageName
    }
  
    val iconBytes: ByteArray? = if (includeIcon) {
      try {
        val drawable = pm.getApplicationIcon(aInfo)
        drawableToBytes(drawable)
      } catch (_: Exception) {
        null
      }
    } else null
  
    val versionCode: Long = try {
      val m = pInfo.javaClass.getMethod("getLongVersionCode")
      (m.invoke(pInfo) as Long)
    } catch (_: Exception) {
      @Suppress("DEPRECATION")
      pInfo.versionCode.toLong()
    }
  
    return mapOf(
      "packageName" to packageName,
      "appName" to label,
      "versionName" to pInfo.versionName,
      "versionCode" to versionCode,
      "firstInstallTime" to pInfo.firstInstallTime,
      "lastUpdateTime"  to pInfo.lastUpdateTime,
      "isSystem" to isSystem,
      "iconBytes" to iconBytes
    )
  }
  

  private fun drawableToBytes(drawable: Drawable): ByteArray {
    val bmp = when (drawable) {
      is BitmapDrawable -> drawable.bitmap
      else -> {
        val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        drawable.setBounds(0, 0, c.width, c.height)
        drawable.draw(c)
        b
      }
    }
    val baos = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.PNG, 100, baos)
    return baos.toByteArray()
  }
}
