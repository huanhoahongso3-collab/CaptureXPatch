package com.keshav.capturesposed.hookers

import android.annotation.SuppressLint
import android.os.Build
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
            module.log("[CaptureSposed] Could not hook screenshot detection callback: $e")
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
                module.log("[CaptureSposed] Could not hook screen recording detection callback: $e")
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
                module?.log("[CaptureSposed] Blocked screenshot detection.")
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
                module?.log("[CaptureSposed] Blocked initial screen recording detection state.")
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
                module?.log("[CaptureSposed] Blocked screen recording detection state change.")
                callback.args[0] = SCREEN_RECORDING_STATE_NOT_VISIBLE
            }
        }
    }
}
