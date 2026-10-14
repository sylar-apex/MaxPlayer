package org.app.maxplayerpro

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.app.maxplayerpro.di.initApp

fun main() = application {
    initApp()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Max Player Pro",
    ) {
        App(embeddedPlayer = true)
    }
}
