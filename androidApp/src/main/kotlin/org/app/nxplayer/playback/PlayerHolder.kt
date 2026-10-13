package org.app.nxplayer.playback

import android.content.Context
import android.content.Intent
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.flow.MutableStateFlow
import org.app.nxplayer.core.Keys
import org.app.nxplayer.core.MediaFile
import org.app.nxplayer.data.AppGraph

enum class Decoder(val label: String, val key: String) { HW("HW", "hw"), HWPlus("HW+", "hwplus"), SW("SW", "sw");
    companion object { fun of(key: String?) = entries.firstOrNull { it.key == key } ?: HW }
}

/** One process-wide player shared by the video screen, the music screens and the background service. */
@OptIn(UnstableApi::class)
object PlayerHolder {
    @Volatile var decoder: Decoder = Decoder.HW

    /** The files currently queued, in player order. Empty for network streams. */
    val queue = MutableStateFlow<List<MediaFile>>(emptyList())
    /** True when the current queue was started from the music library. */
    val isMusicSession = MutableStateFlow(false)

    private var player: ExoPlayer? = null
    private var effects: AudioEffects? = null

    fun get(context: Context): ExoPlayer = player ?: create(context.applicationContext).also { player = it }

    val current: ExoPlayer? get() = player

    private fun create(context: Context): ExoPlayer {
        val selector = MediaCodecSelector { mimeType, secure, tunneling ->
            val all = MediaCodecUtil.getDecoderInfos(mimeType, secure, tunneling)
            when (decoder) {
                Decoder.SW -> all.sortedBy { !it.softwareOnly }
                else -> all
            }
        }
        val renderers = DefaultRenderersFactory(context)
            .setMediaCodecSelector(selector)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        val http = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent(AppGraph.settings[Keys.userAgent].ifBlank { "NxPlayer" })
        val sources = DefaultMediaSourceFactory(DefaultDataSource.Factory(context, http))
        val settings = AppGraph.settings
        return ExoPlayer.Builder(context, renderers)
            .setMediaSourceFactory(sources)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
                settings[Keys.playAlone],
            )
            .setHandleAudioBecomingNoisy(settings[Keys.pauseOnHeadsetDisconnect])
            .setSeekBackIncrementMs(settings[Keys.moveInterval] * 1000L)
            .setSeekForwardIncrementMs(settings[Keys.moveInterval] * 1000L)
            .build().apply {
                setSeekParameters(if (settings[Keys.fastSeek]) SeekParameters.CLOSEST_SYNC else SeekParameters.EXACT)
                addListener(object : androidx.media3.common.Player.Listener {
                    override fun onAudioSessionIdChanged(audioSessionId: Int) { attachEffects(audioSessionId) }
                })
                attachEffects(audioSessionId)
            }
    }

    /** Re-applies settings that the player only reads at build time. */
    fun applySettings() {
        val p = player ?: return
        val s = AppGraph.settings
        p.setAudioAttributes(p.audioAttributes, s[Keys.playAlone])
        p.setHandleAudioBecomingNoisy(s[Keys.pauseOnHeadsetDisconnect])
        p.setSeekParameters(if (s[Keys.fastSeek]) SeekParameters.CLOSEST_SYNC else SeekParameters.EXACT)
    }

    fun effects(): AudioEffects? = effects

    private fun attachEffects(sessionId: Int) {
        effects?.release()
        effects = if (sessionId == C.AUDIO_SESSION_ID_UNSET) null else runCatching { AudioEffects(sessionId) }.getOrNull()
    }

    fun mediaItem(file: MediaFile, subtitles: List<MediaItem.SubtitleConfiguration> = emptyList()): MediaItem =
        MediaItem.Builder()
            .setUri(Uri.parse(file.uri))
            .setMediaId(file.uri)
            .setSubtitleConfigurations(subtitles)
            .setMediaMetadata(
                MediaMetadata.Builder().setTitle(file.title).setArtist(file.artist.ifEmpty { null }).setAlbumTitle(file.album.ifEmpty { null }).build(),
            )
            .build()

    fun streamItem(url: String): MediaItem =
        MediaItem.Builder().setUri(url).setMediaId(url)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(Uri.parse(url).lastPathSegment ?: url).build()).build()

    /** Starts the foreground media service so playback survives leaving the app. */
    fun startBackground(context: Context) {
        context.startService(Intent(context, PlaybackService::class.java))
    }

    fun stopBackground(context: Context) {
        context.stopService(Intent(context, PlaybackService::class.java))
    }

    fun release() {
        effects?.release(); effects = null
        player?.release(); player = null
        queue.value = emptyList()
    }
}

/** Equalizer, bass boost, reverb and loudness (volume boost) bound to one audio session. */
class AudioEffects(sessionId: Int) {
    val equalizer = Equalizer(0, sessionId)
    val bassBoost: BassBoost? = runCatching { BassBoost(0, sessionId) }.getOrNull()
    val reverb: PresetReverb? = runCatching { PresetReverb(0, sessionId) }.getOrNull()
    val loudness: LoudnessEnhancer? = runCatching { LoudnessEnhancer(sessionId) }.getOrNull()

    init { applySaved() }

    fun applySaved() {
        val s = AppGraph.settings
        val on = s[Keys.equalizerEnabled]
        runCatching {
            equalizer.enabled = on
            val preset = s[Keys.equalizerPreset]
            val bands = s[Keys.equalizerBands].split(',').mapNotNull { it.toShortOrNull() }
            if (preset >= 0 && preset < equalizer.numberOfPresets) equalizer.usePreset(preset.toShort())
            else bands.forEachIndexed { i, level -> if (i < equalizer.numberOfBands) equalizer.setBandLevel(i.toShort(), level) }
            bassBoost?.apply { enabled = on && s[Keys.bassBoost] > 0; if (strengthSupported) setStrength(s[Keys.bassBoost].toShort()) }
            reverb?.apply { setPreset(s[Keys.reverb].toShort()); enabled = on && s[Keys.reverb] > 0 }
        }
    }

    fun setBoostMillibels(mb: Int) {
        loudness?.runCatching { setTargetGain(mb); enabled = mb > 0 }
    }

    fun release() {
        runCatching { equalizer.release() }
        runCatching { bassBoost?.release() }
        runCatching { reverb?.release() }
        runCatching { loudness?.release() }
    }

    companion object {
        val reverbNames = listOf("None", "Small room", "Medium room", "Large room", "Medium hall", "Large hall", "Plate")
    }
}
