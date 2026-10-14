package org.app.maxplayerpro

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import org.app.maxplayerpro.di.initApp

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initApp()
    ComposeViewport {
        App(embeddedPlayer = true)
    }
}
