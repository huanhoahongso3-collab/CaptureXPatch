package dhp.thl.tpl.capturexpatch.hookers

import android.content.ContentResolver
import android.content.ContentProviderClient
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.FileObserver
import android.view.WindowManager.SCREEN_RECORDING_STATE_NOT_VISIBLE
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.io.File
import java.util.concurrent.Executor
import java.util.function.Consumer

/*
    Legacy (de.robv.android.xposed) mirror of ScreenCaptureDetectionHooker. Some LSPatch/NPatch
    manager builds only expose modules to dynamic/live scope toggling through the legacy
    IXposedHookLoadPackage entry point (assets/xposed_init) rather than the modern
    io.github.libxposed.api entry point (META-INF/xposed/java_init.list) — see
    CaptureSposedLegacy. The detection/filtering logic itself is shared via
    ScreenCaptureFilterUtils so both entry points behave identically.
 */
object LegacyScreenCaptureDetectionHooker {

    private fun log(message: String) {
        XposedBridge.log("[CaptureXPatch-Legacy] $message")
    }

    fun hook(lpparam: LoadPackageParam) {
        val classLoader = lpparam.classLoader

        try {
            val screenCaptureObserverClass = classLoader.loadClass(
                "android.app.ScreenCaptureCallbackHandler\$ScreenCaptureObserver"
            )
            XposedHelpers.findAndHookMethod(
                screenCaptureObserverClass, "onScreenCaptured",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        log("Blocked screenshot detection.")
                        param.result = null
                    }
                }
            )
        } catch (e: Throwable) {
            log("Could not hook screenshot detection callback: $e")
        }

        try {
            val screenCaptureCallbackClass = classLoader.loadClass("android.app.Activity\$ScreenCaptureCallback")
            val registerHook = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    log("Blocked registerScreenCaptureCallback.")
                    param.result = null
                }
            }
            XposedHelpers.findAndHookMethod(
                "android.app.Activity", classLoader, "registerScreenCaptureCallback",
                Executor::class.java, screenCaptureCallbackClass, registerHook
            )
        } catch (e: Throwable) {
            log("Could not hook Activity.registerScreenCaptureCallback: $e")
        }

        try {
            val screenCaptureCallbackClass = classLoader.loadClass("android.app.Activity\$ScreenCaptureCallback")
            val registerHook = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    log("Blocked registerScreenCaptureCallback.")
                    param.result = null
                }
            }
            XposedHelpers.findAndHookMethod(
                "android.app.ScreenCaptureCallbackHandler", classLoader, "registerScreenCaptureCallback",
                Executor::class.java, screenCaptureCallbackClass, registerHook
            )
        } catch (e: Throwable) {
            log("Could not hook ScreenCaptureCallbackHandler.registerScreenCaptureCallback: $e")
        }

        val filterCursorHook = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val uri = param.args.getOrNull(0) as? Uri
                param.result = ScreenCaptureFilterUtils.filterCursor(param.result, uri, ::log)
            }
        }

        for (targetClass in arrayOf(ContentResolver::class.java, ContentProviderClient::class.java)) {
            try {
                XposedHelpers.findAndHookMethod(
                    targetClass, "query",
                    Uri::class.java, Array<String>::class.java, Bundle::class.java, CancellationSignal::class.java,
                    filterCursorHook
                )
            } catch (e: Throwable) {
                log("Could not hook ${targetClass.simpleName}.query(Bundle): $e")
            }

            try {
                XposedHelpers.findAndHookMethod(
                    targetClass, "query",
                    Uri::class.java, Array<String>::class.java, String::class.java, Array<String>::class.java, String::class.java,
                    filterCursorHook
                )
            } catch (e: Throwable) {
                log("Could not hook ${targetClass.simpleName}.query(Legacy): $e")
            }

            try {
                XposedHelpers.findAndHookMethod(
                    targetClass, "query",
                    Uri::class.java, Array<String>::class.java, String::class.java, Array<String>::class.java, String::class.java, CancellationSignal::class.java,
                    filterCursorHook
                )
            } catch (e: Throwable) {
                log("Could not hook ${targetClass.simpleName}.query(LegacySignal): $e")
            }
        }

        try {
            XposedHelpers.findAndHookMethod(
                FileObserver::class.java, "startWatching",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            var isScreenshotDir = false

                            try {
                                val mFilesField = FileObserver::class.java.getDeclaredField("mFiles")
                                mFilesField.isAccessible = true
                                val files = mFilesField.get(param.thisObject) as? List<*>
                                if (files != null) {
                                    for (f in files) {
                                        val file = f as? File
                                        if (file != null && file.absolutePath.contains("Screenshots", ignoreCase = true)) {
                                            isScreenshotDir = true
                                            break
                                        }
                                    }
                                }
                            } catch (e: Throwable) {
                                // Field not found
                            }

                            if (!isScreenshotDir) {
                                try {
                                    val mPathField = FileObserver::class.java.getDeclaredField("mPath")
                                    mPathField.isAccessible = true
                                    val path = mPathField.get(param.thisObject) as? String
                                    if (path != null && path.contains("Screenshots", ignoreCase = true)) {
                                        isScreenshotDir = true
                                    }
                                } catch (e: Throwable) {
                                    // Field not found
                                }
                            }

                            if (isScreenshotDir) {
                                log("Blocked FileObserver.startWatching for Screenshots directory.")
                                param.result = null
                            }
                        } catch (e: Throwable) {
                            log("Error in FileObserver.startWatching hook: $e")
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            log("Could not hook FileObserver.startWatching: $e")
        }

        try {
            XposedHelpers.findAndHookMethod(
                FileObserver::class.java, "onEvent", Int::class.javaPrimitiveType, String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val path = param.args.getOrNull(1) as? String
                            if (path != null && path.contains("screenshot", ignoreCase = true)) {
                                log("Blocked FileObserver.onEvent for: $path")
                                param.result = null
                            }
                        } catch (e: Throwable) {
                            log("Error in FileObserver.onEvent hook: $e")
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            log("Could not hook FileObserver.onEvent: $e")
        }

        try {
            val uriHook = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val uri = param.args.getOrNull(1) as? Uri
                        if (ScreenCaptureFilterUtils.isScreenshotUri(uri, ::log)) {
                            log("Blocked ContentObserver.dispatchChange for: $uri")
                            param.result = null
                        }
                    } catch (e: Throwable) {
                        log("Error in ContentObserver.dispatchChange(Uri) hook: $e")
                    }
                }
            }
            XposedHelpers.findAndHookMethod(
                ContentObserver::class.java, "dispatchChange",
                Boolean::class.javaPrimitiveType, Uri::class.java, uriHook
            )
            XposedHelpers.findAndHookMethod(
                ContentObserver::class.java, "dispatchChange",
                Boolean::class.javaPrimitiveType, Uri::class.java, Int::class.javaPrimitiveType, uriHook
            )
        } catch (e: Throwable) {
            log("Could not hook ContentObserver.dispatchChange(Uri): $e")
        }

        try {
            XposedHelpers.findAndHookMethod(
                ContentObserver::class.java, "dispatchChange",
                Boolean::class.javaPrimitiveType, Collection::class.java, Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            @Suppress("UNCHECKED_CAST")
                            val uris = param.args.getOrNull(1) as? Collection<Uri>
                            if (uris != null && uris.isNotEmpty() &&
                                uris.all { ScreenCaptureFilterUtils.isScreenshotUri(it, ::log) }
                            ) {
                                log("Blocked ContentObserver.dispatchChange for collection: $uris")
                                param.result = null
                            }
                        } catch (e: Throwable) {
                            log("Error in ContentObserver.dispatchChange(Collection) hook: $e")
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            log("Could not hook ContentObserver.dispatchChange(Collection,flags): $e")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            try {
                val screenRecordingCallbacksClass = classLoader.loadClass(
                    "android.view.ScreenRecordingCallbacks"
                )
                XposedHelpers.findAndHookMethod(
                    screenRecordingCallbacksClass, "addCallback",
                    Executor::class.java, Consumer::class.java,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            log("Blocked initial screen recording detection state.")
                            param.result = SCREEN_RECORDING_STATE_NOT_VISIBLE
                        }
                    }
                )
                XposedHelpers.findAndHookMethod(
                    screenRecordingCallbacksClass, "notifyCallbacks", Int::class.javaPrimitiveType,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            log("Blocked screen recording detection state change.")
                            param.args[0] = SCREEN_RECORDING_STATE_NOT_VISIBLE
                        }
                    }
                )
            } catch (e: Throwable) {
                log("Could not hook screen recording detection callback: $e")
            }
        }
    }
}
