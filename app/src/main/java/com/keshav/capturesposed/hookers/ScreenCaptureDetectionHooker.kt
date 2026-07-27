package com.keshav.capturesposed.hookers

import android.annotation.SuppressLint
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.view.WindowManager.SCREEN_RECORDING_STATE_NOT_VISIBLE
import io.github.libxposed.api.XposedInterface.AfterHookCallback
import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.annotations.AfterInvocation
import io.github.libxposed.api.annotations.BeforeInvocation
import io.github.libxposed.api.annotations.XposedHooker
import java.util.concurrent.Executor
import java.util.function.Consumer

/*
    Hooks the client-side delivery of screenshot/screen-recording detection callbacks inside the
    target app's own process, instead of hooking system_server. This lets the module work when
    embedded directly into a single target app via LSPatch (no root, no system_server access),
    while behaving identically when loaded system-wide through LSPosed with root.
 */
object ScreenCaptureDetectionHooker {
    private var module: XposedModule? = null

    @SuppressLint("PrivateApi", "BlockedPrivateApi")
    fun hook(param: PackageLoadedParam, module: XposedModule) {
        this.module = module
        val classLoader = param.classLoader

        try {
            val screenCaptureObserverClass = classLoader.loadClass(
                "android.app.ScreenCaptureCallbackHandler\$ScreenCaptureObserver"
            )
            module.hook(
                screenCaptureObserverClass.getDeclaredMethod("onScreenCaptured"),
                OnScreenCapturedHooker::class.java
            )
        } catch (e: Throwable) {
            module.log("[CaptureXPatch] Could not hook screenshot detection callback: $e")
        }

        try {
            val activityClass = classLoader.loadClass("android.app.Activity")
            val screenCaptureCallbackClass = classLoader.loadClass("android.app.Activity\$ScreenCaptureCallback")
            module.hook(
                activityClass.getDeclaredMethod(
                    "registerScreenCaptureCallback",
                    Executor::class.java,
                    screenCaptureCallbackClass
                ),
                RegisterScreenCaptureCallbackHooker::class.java
            )
        } catch (e: Throwable) {
            module.log("[CaptureXPatch] Could not hook Activity.registerScreenCaptureCallback: $e")
        }

        try {
            val handlerClass = classLoader.loadClass("android.app.ScreenCaptureCallbackHandler")
            val screenCaptureCallbackClass = classLoader.loadClass("android.app.Activity\$ScreenCaptureCallback")
            module.hook(
                handlerClass.getDeclaredMethod(
                    "registerScreenCaptureCallback",
                    Executor::class.java,
                    screenCaptureCallbackClass
                ),
                RegisterScreenCaptureCallbackHooker::class.java
            )
        } catch (e: Throwable) {
            module.log("[CaptureXPatch] Could not hook ScreenCaptureCallbackHandler.registerScreenCaptureCallback: $e")
        }

        try {
            val contentResolverClass = classLoader.loadClass("android.content.ContentResolver")
            
            // Hook query(Uri, String[], Bundle, CancellationSignal)
            try {
                module.hook(
                    contentResolverClass.getDeclaredMethod(
                        "query",
                        Uri::class.java,
                        Array<String>::class.java,
                        Bundle::class.java,
                        CancellationSignal::class.java
                    ),
                    QueryBundleHooker::class.java
                )
            } catch (e: Throwable) {
                module.log("[CaptureXPatch] Could not hook ContentResolver.query(Bundle): $e")
            }

            // Hook query(Uri, String[], String, String[], String)
            try {
                module.hook(
                    contentResolverClass.getDeclaredMethod(
                        "query",
                        Uri::class.java,
                        Array<String>::class.java,
                        String::class.java,
                        Array<String>::class.java,
                        String::class.java
                    ),
                    QueryLegacyHooker::class.java
                )
            } catch (e: Throwable) {
                module.log("[CaptureXPatch] Could not hook ContentResolver.query(Legacy): $e")
            }

            // Hook query(Uri, String[], String, String[], String, CancellationSignal)
            try {
                module.hook(
                    contentResolverClass.getDeclaredMethod(
                        "query",
                        Uri::class.java,
                        Array<String>::class.java,
                        String::class.java,
                        Array<String>::class.java,
                        String::class.java,
                        CancellationSignal::class.java
                    ),
                    QueryLegacySignalHooker::class.java
                )
            } catch (e: Throwable) {
                module.log("[CaptureXPatch] Could not hook ContentResolver.query(LegacySignal): $e")
            }

        } catch (e: Throwable) {
            module.log("[CaptureXPatch] Could not hook ContentResolver: $e")
        }

        try {
            val contentProviderClientClass = classLoader.loadClass("android.content.ContentProviderClient")
            
            // Hook query(Uri, String[], Bundle, CancellationSignal)
            try {
                module.hook(
                    contentProviderClientClass.getDeclaredMethod(
                        "query",
                        Uri::class.java,
                        Array<String>::class.java,
                        Bundle::class.java,
                        CancellationSignal::class.java
                    ),
                    QueryBundleHooker::class.java
                )
            } catch (e: Throwable) {
                module.log("[CaptureXPatch] Could not hook ContentProviderClient.query(Bundle): $e")
            }

            // Hook query(Uri, String[], String, String[], String)
            try {
                module.hook(
                    contentProviderClientClass.getDeclaredMethod(
                        "query",
                        Uri::class.java,
                        Array<String>::class.java,
                        String::class.java,
                        Array<String>::class.java,
                        String::class.java
                    ),
                    QueryLegacyHooker::class.java
                )
            } catch (e: Throwable) {
                module.log("[CaptureXPatch] Could not hook ContentProviderClient.query(Legacy): $e")
            }

            // Hook query(Uri, String[], String, String[], String, CancellationSignal)
            try {
                module.hook(
                    contentProviderClientClass.getDeclaredMethod(
                        "query",
                        Uri::class.java,
                        Array<String>::class.java,
                        String::class.java,
                        Array<String>::class.java,
                        String::class.java,
                        CancellationSignal::class.java
                    ),
                    QueryLegacySignalHooker::class.java
                )
            } catch (e: Throwable) {
                module.log("[CaptureXPatch] Could not hook ContentProviderClient.query(LegacySignal): $e")
            }
        } catch (e: Throwable) {
            module.log("[CaptureXPatch] Could not hook ContentProviderClient: $e")
        }

        try {
            val fileObserverClass = classLoader.loadClass("android.os.FileObserver")
            module.hook(
                fileObserverClass.getDeclaredMethod("startWatching"),
                StartWatchingHooker::class.java
            )
        } catch (e: Throwable) {
            module.log("[CaptureXPatch] Could not hook FileObserver.startWatching: $e")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            try {
                val screenRecordingCallbacksClass = classLoader.loadClass(
                    "android.view.ScreenRecordingCallbacks"
                )
                module.hook(
                    screenRecordingCallbacksClass.getDeclaredMethod(
                        "addCallback", Executor::class.java, Consumer::class.java
                    ),
                    AddCallbackHooker::class.java
                )
                module.hook(
                    screenRecordingCallbacksClass.getDeclaredMethod(
                        "notifyCallbacks", Int::class.javaPrimitiveType
                    ),
                    NotifyCallbacksHooker::class.java
                )
            } catch (e: Throwable) {
                module.log("[CaptureXPatch] Could not hook screen recording detection callback: $e")
            }
        }
    }

    private fun filterCursor(result: Any?, uri: Uri?): Any? {
        val cursor = result as? Cursor ?: return result
        try {
            var isScreenshot = false
            val count = cursor.count
            val isSingleItem = uri != null && uri.lastPathSegment?.toLongOrNull() != null
            if ((count == 1 || isSingleItem) && count > 0 && cursor.moveToFirst()) {
                val columnNames = cursor.columnNames
                for (colName in columnNames) {
                    val index = cursor.getColumnIndex(colName)
                    if (index >= 0) {
                        try {
                            val value = cursor.getString(index)
                            if (value != null && value.contains("screenshot", ignoreCase = true)) {
                                isScreenshot = true
                                module?.log("[CaptureXPatch] Found screenshot keyword in column $colName: $value")
                                break
                            }
                        } catch (e: Throwable) {
                            // Not a string column, ignore
                        }
                    }
                }
                cursor.moveToFirst() // Reset cursor position for the app
            }
            if (isScreenshot) {
                module?.log("[CaptureXPatch] Blocked screenshot query result for URI: $uri")
                return null
            }
        } catch (e: Throwable) {
            module?.log("[CaptureXPatch] Error filtering cursor: $e")
        }
        return cursor
    }

    @XposedHooker
    private class StartWatchingHooker : Hooker {
        companion object {
            @Suppress("unused")
            @JvmStatic
            @BeforeInvocation
            fun beforeInvocation(callback: BeforeHookCallback) {
                try {
                    val fileObserverClass = android.os.FileObserver::class.java
                    var isScreenshotDir = false
                    
                    // Check mFiles (modern Android)
                    try {
                        val mFilesField = fileObserverClass.getDeclaredField("mFiles")
                        mFilesField.isAccessible = true
                        val files = mFilesField.get(callback.thisObject) as? List<*>
                        if (files != null) {
                            for (f in files) {
                                val file = f as? java.io.File
                                if (file != null && file.absolutePath.contains("Screenshots", ignoreCase = true)) {
                                    isScreenshotDir = true
                                    break
                                }
                            }
                        }
                    } catch (e: Throwable) {
                        // Field not found
                    }

                    // Check mPath (legacy Android)
                    if (!isScreenshotDir) {
                        try {
                            val mPathField = fileObserverClass.getDeclaredField("mPath")
                            mPathField.isAccessible = true
                            val path = mPathField.get(callback.thisObject) as? String
                            if (path != null && path.contains("Screenshots", ignoreCase = true)) {
                                isScreenshotDir = true
                            }
                        } catch (e: Throwable) {
                            // Field not found
                        }
                    }

                    if (isScreenshotDir) {
                        module?.log("[CaptureXPatch] Blocked FileObserver.startWatching for Screenshots directory.")
                        callback.returnAndSkip(null)
                    }
                } catch (e: Throwable) {
                    module?.log("[CaptureXPatch] Error in StartWatchingHooker beforeInvocation: $e")
                }
            }
        }
    }

    @XposedHooker
    private class RegisterScreenCaptureCallbackHooker : Hooker {
        companion object {
            @Suppress("unused")
            @JvmStatic
            @BeforeInvocation
            fun beforeInvocation(callback: BeforeHookCallback) {
                module?.log("[CaptureXPatch] Blocked registerScreenCaptureCallback.")
                callback.returnAndSkip(null)
            }
        }
    }

    @XposedHooker
    private class OnScreenCapturedHooker : Hooker {
        companion object {
            @Suppress("unused")
            @JvmStatic
            @BeforeInvocation
            fun beforeInvocation(callback: BeforeHookCallback) {
                module?.log("[CaptureXPatch] Blocked screenshot detection.")
                callback.returnAndSkip(null)
            }
        }
    }

    @XposedHooker
    private class AddCallbackHooker : Hooker {
        companion object {
            @Suppress("unused")
            @JvmStatic
            @AfterInvocation
            fun afterInvocation(callback: AfterHookCallback) {
                module?.log("[CaptureXPatch] Blocked initial screen recording detection state.")
                callback.result = SCREEN_RECORDING_STATE_NOT_VISIBLE
            }
        }
    }

    @XposedHooker
    private class NotifyCallbacksHooker : Hooker {
        companion object {
            @Suppress("unused")
            @JvmStatic
            @BeforeInvocation
            fun beforeInvocation(callback: BeforeHookCallback) {
                module?.log("[CaptureXPatch] Blocked screen recording detection state change.")
                callback.args[0] = SCREEN_RECORDING_STATE_NOT_VISIBLE
            }
        }
    }

    @XposedHooker
    private class QueryBundleHooker : Hooker {
        companion object {
            @Suppress("unused")
            @JvmStatic
            @AfterInvocation
            fun afterInvocation(callback: AfterHookCallback) {
                callback.result = ScreenCaptureDetectionHooker.filterCursor(callback.result, callback.args[0] as? Uri)
            }
        }
    }

    @XposedHooker
    private class QueryLegacyHooker : Hooker {
        companion object {
            @Suppress("unused")
            @JvmStatic
            @AfterInvocation
            fun afterInvocation(callback: AfterHookCallback) {
                callback.result = ScreenCaptureDetectionHooker.filterCursor(callback.result, callback.args[0] as? Uri)
            }
        }
    }

    @XposedHooker
    private class QueryLegacySignalHooker : Hooker {
        companion object {
            @Suppress("unused")
            @JvmStatic
            @AfterInvocation
            fun afterInvocation(callback: AfterHookCallback) {
                callback.result = ScreenCaptureDetectionHooker.filterCursor(callback.result, callback.args[0] as? Uri)
            }
        }
    }
}
