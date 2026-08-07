package dhp.thl.tpl.capturexpatch

import dhp.thl.tpl.capturexpatch.hookers.LegacyScreenCaptureDetectionHooker
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

/*
    Legacy (de.robv.android.xposed) entry point, listed in assets/xposed_init. Kept alongside the
    modern io.github.libxposed.api entry point (CaptureSposed, listed in
    META-INF/xposed/java_init.list) because some LSPatch/NPatch manager builds only expose
    dynamic/live module scope toggling through the legacy loading path — see the commit history
    around ScreenCaptureDetectionHooker for the manager-side NullPointerException that motivated
    this dual entry point.
 */
class CaptureSposedLegacy : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        try {
            LegacyScreenCaptureDetectionHooker.hook(lpparam)
        } catch (e: Throwable) {
            XposedBridge.log("[CaptureXPatch-Legacy] ERROR: $e")
        }
    }
}
