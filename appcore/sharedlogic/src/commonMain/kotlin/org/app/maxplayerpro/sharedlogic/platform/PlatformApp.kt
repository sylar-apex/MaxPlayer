package org.app.maxplayerpro.sharedlogic.platform

/** App-level platform actions used by the settings and Me screens. */
expect object PlatformApp {
    /** Lets the user save [text] as a file. Returns null if cancelled, else whether writing succeeded. */
    suspend fun saveText(suggestedName: String, text: String): Boolean?
    /** Lets the user pick a text file and returns its contents, or null if cancelled or unreadable. */
    suspend fun openText(mimeTypes: List<String>): String?
    /**
     * Lets the user choose a folder to save files in. Returns (id, label): a path on desktop, a granted
     * document-tree URI on Android; null if cancelled or not possible here.
     */
    suspend fun pickFolder(): Pair<String, String>?
    /** Opens the system screen for choosing this app's language. */
    fun openLanguageSettings()
    /** Closes the app's window and task. */
    fun quit()
}
