package org.app.maxplayerpro.player

import androidx.compose.runtime.Composable
import org.app.kmediaplayer.ui.android.KMediaPlayerActivity
import org.app.maxplayerpro.player.ui.MaxPlayerContent

/** The library's video player, in Max Player's theme. Also opens videos and streams from other apps. */
class PlayerActivity : KMediaPlayerActivity() {
    @Composable
    override fun Content() = MaxPlayerContent(this)
}
