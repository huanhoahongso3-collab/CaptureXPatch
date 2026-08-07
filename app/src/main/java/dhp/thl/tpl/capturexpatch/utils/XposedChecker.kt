package dhp.thl.tpl.capturexpatch.utils

object XposedChecker {
    private var isEnabled = false

    fun flagAsEnabled() {
        isEnabled = true
    }

    fun isEnabled(): Boolean {
        return isEnabled
    }
}