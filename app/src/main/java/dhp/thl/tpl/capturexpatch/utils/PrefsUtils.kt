package dhp.thl.tpl.capturexpatch.utils

import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

object PrefsUtils {
    fun loadPrefs() {
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) {
                XposedChecker.flagAsEnabled()
            }

            override fun onServiceDied(service: XposedService) {}
        })
    }
}
