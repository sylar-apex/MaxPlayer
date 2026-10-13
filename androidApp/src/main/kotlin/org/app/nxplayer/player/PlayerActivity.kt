package org.app.nxplayer.player

import android.app.PictureInPictureParams
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Rational
import android.view.TextureView
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import org.app.nxplayer.core.Keys
import org.app.nxplayer.core.MediaFile
import org.app.nxplayer.core.OrientationMode
import org.app.nxplayer.data.AppGraph
import org.app.nxplayer.playback.PlayerHolder
import org.app.nxplayer.ui.NxTheme

class PlayerActivity : ComponentActivity() {
    lateinit var state: PlayerState
    var textureView: TextureView? = null
    var inPip by mutableStateOf(false)
    private var session: MediaSession? = null
    private var lastBackAt = 0L
    private var closing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppGraph.init(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= 28) window.attributes = window.attributes.apply { layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES }
        val player = PlayerHolder.get(this)
        state = PlayerState(this, player, lifecycleScope)
        if (!handleIntent(intent)) { finish(); return }
        session = runCatching { MediaSession.Builder(this, player).setId("video").build() }.getOrNull()
        player.addListener(isPlayingListener)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    state.panel != Panel.None -> state.panel = Panel.None
                    state.locked -> state.showControls()
                    AppGraph.settings[Keys.doubleTapBack] && System.currentTimeMillis() - lastBackAt > 2000 -> {
                        lastBackAt = System.currentTimeMillis()
                        Toast.makeText(this@PlayerActivity, "Press back again to exit", Toast.LENGTH_SHORT).show()
                    }
                    else -> close()
                }
            }
        })
        applyScreenSettings()
        setContent { NxTheme(org.app.nxplayer.ui.Mulish) { PlayerScreen(this) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** Returns false if there is nothing to play. */
    private fun handleIntent(intent: Intent): Boolean {
        val data = intent.data
        when {
            intent.getBooleanExtra(PlayerLauncher.EXTRA_PENDING, false) -> {
                intent.removeExtra(PlayerLauncher.EXTRA_PENDING)
                val url = PlayerLauncher.pendingUrl
                if (url != null) state.startUrl(url) else state.start(PlayerLauncher.pendingFiles, PlayerLauncher.pendingIndex)
            }
            data != null -> {
                val scheme = data.scheme ?: ""
                if (scheme.startsWith("http") && !AppGraph.settings[Keys.playMediaLinks]) {
                    Toast.makeText(this, "Playing media links is disabled in Settings → General", Toast.LENGTH_LONG).show()
                    return false
                }
                if (scheme == "content" || scheme == "file") {
                    val known = AppGraph.media.findByUri(data.toString()) ?: state.lookupContent(data.toString())
                    val name = data.lastPathSegment?.substringAfterLast('/') ?: "Video"
                    state.start(listOf(known ?: MediaFile(data.hashCode().toLong(), data.toString(), data.path ?: name, name, 0, 0, 0)), 0)
                } else state.startUrl(data.toString())
            }
            PlayerHolder.current?.mediaItemCount ?: 0 > 0 -> state.reattach()
            else -> return false
        }
        PlayerHolder.stopBackground(this)
        return true
    }

    fun applyScreenSettings() {
        val s = AppGraph.settings
        if (s[Keys.brightnessEnabled]) setBrightness(state.brightness)
        updateKeepScreenOn(state.isPlaying)
        applyOrientation()
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        when (s[Keys.fullScreen]) {
            "off" -> controller.show(WindowInsetsCompat.Type.systemBars())
            else -> controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun showSystemBars(show: Boolean) {
        if (AppGraph.settings[Keys.fullScreen] != "auto") return
        val c = WindowInsetsControllerCompat(window, window.decorView)
        if (show && AppGraph.settings[Keys.softButtons] != "hide") c.show(WindowInsetsCompat.Type.statusBars()) else c.hide(WindowInsetsCompat.Type.systemBars())
    }

    fun updateKeepScreenOn(playing: Boolean) {
        if (playing || AppGraph.settings[Keys.keepScreenOn]) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    fun setBrightness(level: Int) {
        window.attributes = window.attributes.apply { screenBrightness = (level / 30f).coerceIn(0.01f, 1f) }
    }

    fun applyOrientation() {
        val forced = state.landscape
        requestedOrientation = when {
            forced == true -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            forced == false -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            else -> when (OrientationMode.valueOf(AppGraph.settings[Keys.orientation])) {
                OrientationMode.Video -> if (state.videoH > state.videoW && state.videoW > 0) ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                OrientationMode.System -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                OrientationMode.Sensor -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                OrientationMode.Landscape -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                OrientationMode.ReverseLandscape -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                OrientationMode.Portrait -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    fun rotate() {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        state.landscape = !isLandscape
        applyOrientation()
    }

    fun enterPip(): Boolean {
        if (Build.VERSION.SDK_INT < 26 || !packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)) return false
        val w = state.videoW.coerceAtLeast(1); val h = state.videoH.coerceAtLeast(1)
        val ratio = (w.toFloat() / h).coerceIn(0.42f, 2.38f)
        state.panel = Panel.None
        return runCatching { enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational((ratio * 1000).toInt(), 1000)).build()) }.getOrDefault(false)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPip = isInPictureInPictureMode
        if (isInPictureInPictureMode) state.controlsVisible = false
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!closing && state.isPlaying && !state.backgroundPlay && AppGraph.settings[Keys.backgroundMode] == "pip") enterPip()
    }

    override fun onPause() {
        super.onPause()
        if (!isInPictureInPictureMode && !isFinishing && AppGraph.settings[Keys.pauseIfObscured] && !state.backgroundPlay) state.player.pause()
    }

    override fun onStop() {
        super.onStop()
        state.saveProgress()
        if (closing || isFinishing) return
        if (isInPictureInPictureMode) return
        val audioOnly = state.videoW == 0 && AppGraph.settings[Keys.backgroundAudio]
        if ((state.backgroundPlay || audioOnly) && state.player.playWhenReady) {
            state.player.clearVideoSurface()
            PlayerHolder.startBackground(this)
        } else state.player.pause()
    }

    override fun onStart() {
        super.onStart()
        PlayerHolder.stopBackground(this)
        textureView?.let { state.player.setVideoTextureView(it) }
    }

    fun close() {
        closing = true
        state.saveProgress()
        state.detach()
        state.player.stop()
        state.player.clearMediaItems()
        PlayerHolder.stopBackground(this)
        finish()
    }

    @Deprecated("Needed for MediaStore consent results")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val name = state.pendingRename
        if (requestCode == 8 && name != null) {
            state.pendingRename = null
            val f = state.current ?: return
            if (resultCode == RESULT_OK) lifecycleScope.launch {
                if (org.app.nxplayer.data.AppGraph.media.renameNow(f, name)) { state.title = name.substringBeforeLast('.'); state.toast = "Renamed" } else state.toast = "Rename failed"
            }
        }
    }

    override fun onDestroy() {
        session?.release()
        session = null
        if (::state.isInitialized) {
            state.player.removeListener(isPlayingListener)
            val keepPlaying = state.backgroundPlay && state.player.playWhenReady
            if (isFinishing && !closing && !keepPlaying) { state.detach(); state.player.stop() }
        }
        super.onDestroy()
    }

    fun screenshot() {
        val bmp: Bitmap = textureView?.bitmap ?: run { state.toast = "Screenshot unavailable"; return }
        val name = "NxPlayer_${System.currentTimeMillis()}.png"
        val ok = runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/NxPlayer")
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
            contentResolver.openOutputStream(uri)!!.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }.isSuccess
        if (ok) state.snack = "Screenshot saved to " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path + "/NxPlayer" else state.toast = "Screenshot failed"
    }

    fun shareCurrent() {
        val f = state.current ?: return
        val intent = Intent(Intent.ACTION_SEND).setType("video/*").putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse(f.uri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (f.uri.startsWith("http")) intent.setType("text/plain").putExtra(Intent.EXTRA_TEXT, f.uri)
        startActivity(Intent.createChooser(intent, "Share"))
    }

    val isPlayingListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) { updateKeepScreenOn(isPlaying) }
        override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) { if (state.landscape == null) applyOrientation() }
    }
}
