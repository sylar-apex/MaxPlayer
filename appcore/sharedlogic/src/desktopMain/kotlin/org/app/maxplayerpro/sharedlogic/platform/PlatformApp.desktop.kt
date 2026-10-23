package org.app.maxplayerpro.sharedlogic.platform

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import kotlin.system.exitProcess

actual object PlatformApp {
    actual suspend fun saveText(suggestedName: String, text: String): Boolean? {
        val dialog = FileDialog(null as Frame?, "Export", FileDialog.SAVE).apply { file = suggestedName; isVisible = true }
        val name = dialog.file ?: return null
        return runCatching { File(dialog.directory, name).writeText(text) }.isSuccess
    }

    actual suspend fun openText(mimeTypes: List<String>): String? {
        val dialog = FileDialog(null as Frame?, "Import", FileDialog.LOAD).apply { isVisible = true }
        val name = dialog.file ?: return null
        return runCatching { File(dialog.directory, name).readText() }.getOrNull()
    }

    actual suspend fun pickFolder(): Pair<String, String>? {
        val chooser = javax.swing.JFileChooser(System.getProperty("user.home")).apply {
            fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
            dialogTitle = "Save downloads to"
        }
        if (chooser.showOpenDialog(null) != javax.swing.JFileChooser.APPROVE_OPTION) return null
        val dir = chooser.selectedFile ?: return null
        return dir.absolutePath to dir.name.ifEmpty { dir.absolutePath }
    }

    actual fun openLanguageSettings() {}
    actual fun quit() { exitProcess(0) }
}
