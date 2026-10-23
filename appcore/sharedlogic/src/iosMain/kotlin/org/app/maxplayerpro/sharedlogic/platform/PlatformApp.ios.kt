package org.app.maxplayerpro.sharedlogic.platform

actual object PlatformApp {
    actual suspend fun saveText(suggestedName: String, text: String): Boolean? = null
    actual suspend fun openText(mimeTypes: List<String>): String? = null
    actual suspend fun pickFolder(): Pair<String, String>? = null

    actual fun openLanguageSettings() {}
    actual fun quit() {}
}
