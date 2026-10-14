package org.app.maxplayerpro

import androidx.compose.ui.window.ComposeUIViewController
import org.app.maxplayerpro.di.initApp

fun MainViewController() = ComposeUIViewController {
    initApp()
    App(embeddedPlayer = true)
}
