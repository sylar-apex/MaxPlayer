package org.app.maxplayerpro.player.ui

import androidx.compose.runtime.Composable
import org.app.kmediaplayer.ui.video.PlayerWindow
import org.app.kmediaplayer.ui.video.VideoPlayerScreen
import org.app.maxplayerpro.appcore.theme.MaxTheme
import org.app.maxplayerpro.appcore.theme.mulishFontFamily

/** The library's video player, dressed in Max Player's theme and the Mulish typeface. */
@Composable
fun MaxPlayerContent(window: PlayerWindow) {
    MaxTheme(mulishFontFamily()) { VideoPlayerScreen(window) }
}
