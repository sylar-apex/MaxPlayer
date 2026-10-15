package org.app.maxplayerpro

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.app.kmediaplayer.KMediaPlayer
import org.app.kmediaplayer.model.MediaFile
import org.app.maxplayerpro.di.initApp
import java.io.File

/** Files given on the command line (or by "Open with") start playing straight away. */
fun main(args: Array<String>) = application {
    initApp()
    val files = args.map(::File).filter { it.isFile }.map { it.toMediaFile() }
    if (files.isNotEmpty()) KMediaPlayer.playVideo(files, 0)
    Window(
        onCloseRequest = ::exitApplication,
        title = "Max Player Pro",
    ) {
        App(embeddedPlayer = true)
    }
}

private fun File.toMediaFile() = MediaFile(path.hashCode().toLong(), toURI().toString(), path, name, length(), 0, lastModified())
