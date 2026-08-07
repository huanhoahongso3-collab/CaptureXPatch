package dhp.thl.tpl.capturexpatch

import dhp.thl.tpl.capturexpatch.hookers.ScreenCaptureDetectionHooker
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

private lateinit var module: CaptureSposed

class CaptureSposed(base: XposedInterface, param: ModuleLoadedParam) : XposedModule(base, param) {
    init {
        module = this
    }

    /*
        Hooks run entirely client-side inside each scoped app's own process, so this module works
        whether it is loaded system-wide via LSPosed (with root) or embedded into a single target
        app via LSPatch (no root, no system_server access).
     */
    override fun onPackageLoaded(param: PackageLoadedParam) {
        super.onPackageLoaded(param)

        try {
            ScreenCaptureDetectionHooker.hook(param, module)
        } catch (e: Exception) {
            log("[CaptureXPatch] ERROR: $e")
        }
    }
}