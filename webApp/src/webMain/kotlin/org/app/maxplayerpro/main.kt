package org.app.maxplayerpro

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.window
import org.app.kmediaplayer.KMediaPlayer
import org.app.maxplayerpro.di.initApp
import org.w3c.dom.url.URLSearchParams

/** `?play=<url>` opens that video straight away, so links can point at a stream. */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initApp()
    URLSearchParams(window.location.search.toJsString()).get("play")?.let(KMediaPlayer::playUrl)
    ComposeViewport {
        App(embeddedPlayer = true)
    }
}
