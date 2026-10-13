package org.app.nxplayer.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import org.app.nxplayer.MainActivity
import org.app.nxplayer.core.Keys
import org.app.nxplayer.data.AppGraph
import org.app.nxplayer.player.PlayerActivity

/**
 * Background play: exposes the shared player through a media session so Media3 posts the
 * playback notification, lock-screen controls and responds to headset/Bluetooth media buttons.
 */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
        val player = PlayerHolder.get(this)
        val target = if (PlayerHolder.isMusicSession.value) MainActivity::class.java else PlayerActivity::class.java
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, target).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, player)
            .setSessionActivity(open)
            .setCallback(object : MediaSession.Callback {
                override fun onMediaButtonEvent(session: MediaSession, controllerInfo: MediaSession.ControllerInfo, intent: Intent): Boolean =
                    !AppGraph.settings[Keys.mediaButtons]
            })
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0 || p.playbackState == Player.STATE_ENDED) stopSelf()
    }

    override fun onDestroy() {
        session?.run { release() }
        session = null
        super.onDestroy()
    }
}
