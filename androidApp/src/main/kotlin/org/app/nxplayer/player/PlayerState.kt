package org.app.nxplayer.player

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.app.nxplayer.core.AbRepeat
import org.app.nxplayer.core.AspectRatio
import org.app.nxplayer.core.FileSelections
import org.app.nxplayer.core.Keys
import org.app.nxplayer.core.LoopMode
import org.app.nxplayer.core.MediaFile
import org.app.nxplayer.core.ResumeDecision
import org.app.nxplayer.core.ResumePolicy
import org.app.nxplayer.core.SmartPrevious
import org.app.nxplayer.core.Speed
import org.app.nxplayer.core.SubtitleDocument
import org.app.nxplayer.core.SubtitleParser
import org.app.nxplayer.core.VolumeState
import org.app.nxplayer.core.ZoomMode
import org.app.nxplayer.data.AppGraph
import org.app.nxplayer.playback.Decoder
import org.app.nxplayer.playback.PlayerHolder

/** Pending request handed from the library to [PlayerActivity] without serialising the queue into the intent. */
object PlayerLauncher {
    @Volatile var pendingFiles: List<MediaFile> = emptyList()
    @Volatile var pendingIndex: Int = 0
    @Volatile var pendingUrl: String? = null

    fun play(context: Context, files: List<MediaFile>, index: Int) {
        if (files.isEmpty()) return
        pendingFiles = files; pendingIndex = index.coerceIn(files.indices); pendingUrl = null
        context.startActivity(Intent(context, PlayerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra(EXTRA_PENDING, true))
    }

    fun playUrl(context: Context, url: String) {
        pendingFiles = emptyList(); pendingUrl = url
        context.startActivity(Intent(context, PlayerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra(EXTRA_PENDING, true))
    }

    const val EXTRA_PENDING = "pending"
}

enum class Panel { None, Overflow, Audio, Subtitle, Decoder, Speed, Equalizer, SleepTimer, AbRepeat, Aspect, Info, Bookmark, More, Display, Playlist, Customise }

data class TrackOption(val group: Tracks.Group, val index: Int, val label: String, val selected: Boolean, val id: String)

@OptIn(UnstableApi::class)
class PlayerState(private val context: Context, val player: ExoPlayer, private val scope: CoroutineScope) {
    private val settings = AppGraph.settings
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    var queue by mutableStateOf<List<MediaFile>>(emptyList())
    var index by mutableIntStateOf(0)
    var title by mutableStateOf("")
    var isPlaying by mutableStateOf(false)
    var buffering by mutableStateOf(false)
    var position by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)
    var videoW by mutableIntStateOf(0)
    var videoH by mutableIntStateOf(0)

    var controlsVisible by mutableStateOf(settings[Keys.showInterfaceAtStartup])
    var shortcutsExpanded by mutableStateOf(false)
    var locked by mutableStateOf(false)
    var panel by mutableStateOf(Panel.None)
    var resumePrompt by mutableStateOf<Long?>(null)
    var resumeNotice by mutableStateOf<Long?>(null)
    var error by mutableStateOf<String?>(null)
    var toast by mutableStateOf<String?>(null)
    var snack by mutableStateOf<String?>(null)
    var gestureLabel by mutableStateOf<String?>(null)
    var levelOsd by mutableStateOf<LevelOsd?>(null)
    var seekOsd by mutableStateOf<Pair<Long, Long>?>(null)
    var tapSeekOsd by mutableStateOf<Pair<Int, Int>?>(null)
    var pillOsd by mutableStateOf<Pair<Int, String>?>(null)
    var lockTapCount by mutableStateOf(0)
    var pendingRename: String? = null

    var zoom by mutableStateOf(ZoomMode.Fit)
    var aspect by mutableStateOf(AspectRatio.Default)
    var customAspect by mutableFloatStateOf(1.85f)
    var pinchScale by mutableFloatStateOf(1f)
    var panX by mutableFloatStateOf(0f)
    var panY by mutableFloatStateOf(0f)
    var nightMode by mutableStateOf(false)
    var muted by mutableStateOf(false)
    var loop by mutableStateOf(LoopMode.Off)
    var shuffle by mutableStateOf(false)
    var speed by mutableFloatStateOf(settings[Keys.defaultSpeed] / 100f)
    var tempSpeed by mutableStateOf<Float?>(null)
    var ab by mutableStateOf(AbRepeat())
    var sleepEndAt by mutableStateOf<Long?>(null)
    var sleepFinishLast by mutableStateOf(false)
    var decoder by mutableStateOf(Decoder.of(settings[Keys.defaultDecoder]))
    var backgroundPlay by mutableStateOf(settings[Keys.backgroundMode] == "background")
    var landscape by mutableStateOf<Boolean?>(null)

    var volume by mutableStateOf(VolumeState(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC), audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), settings[Keys.volumeBoost]))
    var brightness by mutableIntStateOf(settings[Keys.brightnessLevel])

    var audioTracks by mutableStateOf<List<TrackOption>>(emptyList())
    var textTracks by mutableStateOf<List<TrackOption>>(emptyList())
    var subtitlesDisabled by mutableStateOf(false)
    var embeddedCues by mutableStateOf<List<String>>(emptyList())
    var externalSubs by mutableStateOf<SubtitleDocument?>(null)
    var externalSubsName by mutableStateOf<String?>(null)
    var subtitleSyncMs by mutableLongStateOf((settings[Keys.subtitleDefaultSync] * 1000).toLong())
    var subtitleSpeed by mutableFloatStateOf(1f)
    var subtitleOffsetY by mutableFloatStateOf(0f)
    var subtitleZoom by mutableFloatStateOf(1f)

    val current: MediaFile? get() = queue.getOrNull(index)
    val effectiveSpeed: Float get() = tempSpeed ?: speed

    private var firstIndex = 0
    private var resumeCheckedFor: String? = null
    private var ticker: Job? = null
    private var hideJob: Job? = null
    private var restoredTracksFor: String? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing; scheduleHide() }
        override fun onPlaybackStateChanged(state: Int) {
            buffering = state == Player.STATE_BUFFERING
            duration = player.duration.coerceAtLeast(0)
            if (state == Player.STATE_READY) checkResume()
            if (state == Player.STATE_ENDED) onEnded()
        }
        override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
            saveProgress()
            index = player.currentMediaItemIndex
            onItemChanged()
        }
        override fun onVideoSizeChanged(size: VideoSize) {
            videoW = (size.width * size.pixelWidthHeightRatio).toInt(); videoH = size.height
        }
        override fun onTracksChanged(tracks: Tracks) { readTracks(tracks); restoreTracks() }
        override fun onCues(cueGroup: CueGroup) { embeddedCues = cueGroup.cues.mapNotNull { it.text?.toString() } }
        override fun onPlayerError(e: PlaybackException) {
            if (settings[Keys.suppressError] && player.hasNextMediaItem()) { player.seekToNextMediaItem(); player.prepare(); player.play() }
            else error = "Can't play this media.\n${e.errorCodeName}"
        }
    }

    fun start(files: List<MediaFile>, startIndex: Int) {
        queue = files
        PlayerHolder.queue.value = files
        PlayerHolder.isMusicSession.value = false
        PlayerHolder.decoder = decoder
        applyPreferredLanguages()
        player.setMediaItems(files.map { PlayerHolder.mediaItem(it) }, startIndex, 0)
        index = startIndex
        firstIndex = startIndex
        attach()
        onItemChanged()
        player.prepare()
        player.play()
    }

    /** Reads the real file record behind a MediaStore content URI, so Info/rename/delete see the actual file. */
    fun lookupContent(url: String): MediaFile? {
        val uri = Uri.parse(url)
        return runCatching {
            context.contentResolver.query(uri, arrayOf("_data", "_display_name", "_size", "duration", "date_modified", "width", "height", "mime_type"), null, null, null)?.use { c ->
                if (!c.moveToFirst()) null else {
                    val path = c.getString(0) ?: return@use null
                    MediaFile(
                        id = runCatching { android.content.ContentUris.parseId(uri) }.getOrDefault(url.hashCode().toLong()),
                        uri = url, path = path, name = c.getString(1) ?: path.substringAfterLast('/'),
                        sizeBytes = c.getLong(2), durationMs = c.getLong(3), dateModifiedMs = c.getLong(4) * 1000,
                        width = c.getInt(5), height = c.getInt(6), mimeType = c.getString(7) ?: "",
                    )
                }
            }
        }.getOrNull()
    }

    fun startUrl(url: String) {
        val uri = Uri.parse(url)
        val name = if (uri.scheme == "content") runCatching {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }.getOrNull() else null
        val display = (name ?: uri.lastPathSegment ?: url).substringBeforeLast('.').ifEmpty { url }
        val stored = if (uri.scheme == "content") lookupContent(url) else null
        queue = listOf(stored ?: MediaFile(url.hashCode().toLong(), url, url, display, 0, 0, 0))
        PlayerHolder.queue.value = emptyList()
        PlayerHolder.isMusicSession.value = false
        player.setMediaItem(PlayerHolder.streamItem(url))
        index = 0
        attach()
        title = queue[0].title
        player.prepare()
        player.play()
    }

    /** Re-attaches to a player already running in the background service. */
    fun reattach() {
        queue = PlayerHolder.queue.value
        index = player.currentMediaItemIndex
        attach()
        isPlaying = player.isPlaying
        duration = player.duration.coerceAtLeast(0)
        title = player.mediaMetadata.title?.toString() ?: ""
        readTracks(player.currentTracks)
    }

    private fun attach() {
        player.removeListener(listener)
        player.addListener(listener)
        player.setPlaybackSpeed(speed)
        player.repeatMode = Player.REPEAT_MODE_OFF
        ticker?.cancel()
        ticker = scope.launch {
            var n = 0
            while (isActive) {
                position = player.currentPosition
                ab.loopTarget(position)?.let { player.seekTo(it) }
                sleepEndAt?.let { end ->
                    if (System.currentTimeMillis() >= end) {
                        sleepEndAt = null
                        if (sleepFinishLast) player.pauseAtEndOfMediaItems = true else player.pause()
                    }
                }
                if (++n % 10 == 0) saveProgress()
                delay(200)
            }
        }
    }

    fun detach() {
        saveProgress()
        ticker?.cancel()
        player.removeListener(listener)
    }

    private fun onItemChanged() {
        val f = current ?: return
        title = f.title
        ab = AbRepeat()
        externalSubs = null; externalSubsName = null
        subtitleSyncMs = (settings[Keys.subtitleDefaultSync] * 1000).toLong()
        resumeCheckedFor = null
        restoredTracksFor = null
        if (settings[Keys.rememberSelections]) {
            AppGraph.userData.value.selections[f.uri]?.let { s ->
                s.zoom?.let { z -> ZoomMode.entries.firstOrNull { it.name == z }?.let { zoom = it } }
                s.aspect?.let { a -> AspectRatio.entries.firstOrNull { it.name == a }?.let { aspect = it } }
                s.speed?.let { setSpeed(it, remember = false) }
                s.subtitleSyncMs?.let { subtitleSyncMs = it }
                s.decoder?.let { decoder = Decoder.of(it) }
            }
        }
        autoLoadSubtitle(f)
        fadeIn(settings[Keys.fadeOnStart])
    }

    private fun checkResume() {
        val f = current ?: return
        if (resumeCheckedFor == f.uri) return
        resumeCheckedFor = f.uri
        val rec = AppGraph.userData.value.history[f.uri] ?: return
        when (ResumePolicy.decide(settings[Keys.resume], rec.positionMs, player.duration, index == firstIndex, settings[Keys.resumeOnlyFirst])) {
            ResumeDecision.Resume -> { player.seekTo(rec.positionMs); resumeNotice = rec.positionMs; showControls() }
            ResumeDecision.Ask -> { player.pause(); resumePrompt = rec.positionMs }
            ResumeDecision.StartOver -> {}
        }
    }

    fun startOverFromNotice() { resumeNotice = null; player.seekTo(0); scheduleHide() }

    fun answerResume(resume: Boolean) {
        resumePrompt?.let { if (resume) player.seekTo(it) }
        resumePrompt = null
        player.play()
    }

    private fun onEnded() {
        saveProgress()
        when {
            loop == LoopMode.One -> { player.seekTo(0); player.play() }
            loop == LoopMode.All && queue.size > 0 -> { player.seekTo(0, 0); player.play() }
            settings[Keys.backToList] -> finishRequested = true
        }
    }

    var finishRequested by mutableStateOf(false)

    fun saveProgress() {
        val f = current ?: return
        if (resumeCheckedFor != f.uri) return
        if (f.uri.startsWith("http") || f.uri.startsWith("rtsp")) return
        val d = player.duration
        if (d <= 0) return
        AppGraph.userData.update { it.recordPlayback(f.uri, f.folderPath, player.currentPosition, d, System.currentTimeMillis()) }
    }

    private fun rememberSelection(transform: (FileSelections) -> FileSelections) {
        val f = current ?: return
        if (!settings[Keys.rememberSelections]) return
        AppGraph.userData.update { d -> d.copy(selections = d.selections + (f.uri to transform(d.selections[f.uri] ?: FileSelections()))) }
    }

    // ---- transport ----

    fun togglePlay() { if (player.isPlaying) player.pause() else { if (player.playbackState == Player.STATE_ENDED) player.seekTo(0); player.play() }; scheduleHide() }

    fun seekBy(deltaMs: Long) { seekTo(player.currentPosition + deltaMs) }

    fun seekTo(ms: Long) {
        player.seekTo(ms.coerceIn(0, player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE))
        position = player.currentPosition
        fadeIn(settings[Keys.fadeOnSeek])
    }

    fun next() {
        if (settings[Keys.prevNextToRewFf]) { seekBy(settings[Keys.moveInterval] * 1000L); return }
        if (shuffle && queue.size > 1) player.seekTo(pickShuffled(), 0)
        else if (player.hasNextMediaItem()) player.seekToNextMediaItem()
        else toast = "Last item"
    }

    fun previous() {
        if (settings[Keys.prevNextToRewFf]) { seekBy(-settings[Keys.moveInterval] * 1000L); return }
        if (SmartPrevious.shouldRestart(settings[Keys.smartPrev], player.currentPosition) || !player.hasPreviousMediaItem()) player.seekTo(0)
        else player.seekToPreviousMediaItem()
    }

    fun jumpTo(i: Int) { if (i in queue.indices) { player.seekTo(i, 0); player.play() } }

    private fun pickShuffled(): Int {
        val others = queue.indices.filter { it != index }
        return others.random()
    }

    fun setSpeed(v: Float, remember: Boolean = true) {
        speed = Speed.clamp(v)
        if (tempSpeed == null) player.setPlaybackSpeed(speed)
        if (remember) rememberSelection { it.copy(speed = speed) }
    }

    fun setTemporarySpeed(v: Float?) { tempSpeed = v; player.setPlaybackSpeed(v ?: speed) }

    fun cycleZoom() {
        zoom = zoom.next(); pinchScale = 1f; panX = 0f; panY = 0f
        toast = zoom.label
        rememberSelection { it.copy(zoom = zoom.name) }
    }

    fun setAspectRatio(a: AspectRatio, applyToAll: Boolean) {
        aspect = a
        if (applyToAll && a != AspectRatio.Custom) settings[Keys.defaultAspect] = a.name
        rememberSelection { it.copy(aspect = a.name) }
    }

    fun aspectValue(): Float? = when (aspect) {
        AspectRatio.Custom -> customAspect
        AspectRatio.Default -> AspectRatio.valueOf(settings[Keys.defaultAspect]).ratio
        else -> aspect.ratio
    }

    fun toggleMute() { muted = !muted; player.volume = if (muted) 0f else 1f; toast = if (muted) "Mute" else "Unmute" }

    fun cycleLoop() { loop = loop.next(); toast = loop.label }

    fun toggleShuffle() { shuffle = !shuffle; toast = if (shuffle) "Shuffle on" else "Shuffle off" }

    fun setDecoderMode(d: Decoder) {
        if (d == decoder) return
        decoder = d
        PlayerHolder.decoder = d
        val pos = player.currentPosition
        val i = player.currentMediaItemIndex
        player.stop()
        player.seekTo(i, pos)
        player.prepare(); player.play()
        rememberSelection { it.copy(decoder = d.key) }
        toast = "${d.label} decoder"
    }

    fun startSleepTimer(minutes: Int, finishLast: Boolean) {
        sleepEndAt = System.currentTimeMillis() + minutes * 60_000L
        sleepFinishLast = finishLast
        player.pauseAtEndOfMediaItems = false
        toast = "Sleep timer: ${minutes / 60}h ${minutes % 60}m"
    }

    fun stopSleepTimer() { sleepEndAt = null; player.pauseAtEndOfMediaItems = false }

    // ---- volume & brightness ----

    fun adjustVolume(delta: Int) {
        volume = volume.copy(level = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).let { sys -> if (volume.level > volume.systemMax) volume.level else sys }).adjust(delta)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume.systemLevel, 0)
        PlayerHolder.effects()?.setBoostMillibels(volume.boostMillibels)
        if (muted && delta > 0) toggleMute()
    }

    // ---- tracks ----

    private fun readTracks(tracks: Tracks) {
        fun options(type: Int) = tracks.groups.filter { it.type == type }.flatMap { g ->
            (0 until g.length).filter { g.isTrackSupported(it) }.map { i ->
                val f = g.getTrackFormat(i)
                val label = listOfNotNull(f.label, f.language?.let { java.util.Locale(it).displayLanguage })
                    .distinct().joinToString(" - ").ifEmpty { "Track ${i + 1}" }
                TrackOption(g, i, label, g.isTrackSelected(i), f.id ?: "${g.mediaTrackGroup.id}:$i")
            }
        }
        audioTracks = options(C.TRACK_TYPE_AUDIO)
        textTracks = options(C.TRACK_TYPE_TEXT)
        subtitlesDisabled = player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
    }

    private fun restoreTracks() {
        val f = current ?: return
        if (restoredTracksFor == f.uri || audioTracks.isEmpty() && textTracks.isEmpty()) return
        restoredTracksFor = f.uri
        if (!settings[Keys.rememberSelections]) return
        val s = AppGraph.userData.value.selections[f.uri] ?: return
        s.audioTrack?.let { id -> audioTracks.firstOrNull { it.id == id }?.let { selectTrack(it, C.TRACK_TYPE_AUDIO, remember = false) } }
        s.subtitleTrack?.let { id -> if (id == "off") disableSubtitles(remember = false) else textTracks.firstOrNull { it.id == id }?.let { selectTrack(it, C.TRACK_TYPE_TEXT, remember = false) } }
    }

    fun selectTrack(t: TrackOption, type: Int, remember: Boolean = true) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(type, false)
            .setOverrideForType(TrackSelectionOverride(t.group.mediaTrackGroup, t.index))
            .build()
        if (type == C.TRACK_TYPE_TEXT) subtitlesDisabled = false
        if (remember) rememberSelection { if (type == C.TRACK_TYPE_AUDIO) it.copy(audioTrack = t.id) else it.copy(subtitleTrack = t.id) }
    }

    fun disableAudio() {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true).build()
    }

    fun disableSubtitles(remember: Boolean = true) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()
        subtitlesDisabled = true
        externalSubs = null; externalSubsName = null
        if (remember) rememberSelection { it.copy(subtitleTrack = "off") }
    }

    private fun applyPreferredLanguages() {
        val b = player.trackSelectionParameters.buildUpon()
        settings[Keys.audioLanguage].takeIf { it.isNotBlank() }?.let { b.setPreferredAudioLanguage(it) }
        settings[Keys.subtitleLanguage].takeIf { it.isNotBlank() }?.let { b.setPreferredTextLanguage(it) }
        player.trackSelectionParameters = b.build()
    }

    // ---- external subtitles ----

    fun loadSubtitle(uri: Uri, name: String) {
        val ext = name.substringAfterLast('.', "srt").lowercase()
        if (ext !in SubtitleParser.supportedExtensions) { toast = "Unsupported subtitle format: .$ext"; return }
        val text = runCatching {
            val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
            decodeSubtitle(bytes)
        }.getOrNull() ?: run { toast = "Can't read subtitle"; return }
        externalSubs = SubtitleParser.parse(text, ext, settings[Keys.italicTag])
        externalSubsName = name
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()
        subtitlesDisabled = false
        toast = "Subtitle: $name"
    }

    private fun decodeSubtitle(bytes: ByteArray): String {
        val forced = settings[Keys.subtitleCharset]
        if (forced.isNotEmpty()) return String(bytes, charset(forced))
        val utf8 = bytes.decodeToString()
        return if ('\uFFFD' in utf8) String(bytes, Charsets.ISO_8859_1) else utf8
    }

    private fun autoLoadSubtitle(f: MediaFile) {
        val candidates = runCatching { AppGraph.media.subtitleFilesFor(f) }.getOrDefault(emptyList())
            .filter { it.extension.lowercase() in SubtitleParser.supportedExtensions && it.canRead() }
        candidates.firstOrNull()?.let { loadSubtitle(Uri.fromFile(it), it.name) }
    }

    fun setSubtitleSync(ms: Long) { subtitleSyncMs = ms; rememberSelection { it.copy(subtitleSyncMs = ms) } }

    fun visibleSubtitleLines(): List<SubLine> {
        if (subtitlesDisabled && externalSubs == null) return emptyList()
        val ext = externalSubs
        return if (ext != null) ext.cuesAt(position, subtitleSyncMs, subtitleSpeed).map { SubLine(it.text, it.italic) }
        else embeddedCues.map { SubLine(it, false) }
    }

    // ---- interface visibility ----

    fun showControls() { controlsVisible = true; scheduleHide() }

    fun toggleControls() { controlsVisible = !controlsVisible; if (controlsVisible) scheduleHide() }

    fun scheduleHide() {
        hideJob?.cancel()
        if (!settings[Keys.interfaceAutoHide] || !player.isPlaying) return
        hideJob = scope.launch {
            delay((settings[Keys.interfaceAutoHideDelay] * 1000).toLong())
            if (panel == Panel.None) controlsVisible = false
        }
    }

    private var fadeJob: Job? = null

    private fun fadeIn(enabled: Boolean) {
        if (!enabled || muted) return
        fadeJob?.cancel()
        fadeJob = scope.launch {
            for (i in 0..10) { player.volume = i / 10f; delay(40) }
        }
    }
}

data class SubLine(val text: String, val italic: Boolean)

/** [boostFrom] is the bar fraction where volume boost starts, or null when there is no boost range. */
data class LevelOsd(val brightness: Boolean, val value: Int, val fraction: Float, val boostFrom: Float?)
