package org.app.maxplayerpro.sharedlogic.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.app.kmediaplayer.model.MediaFile
import org.app.kmediaplayer.model.MediaFormats
import org.app.kmediaplayer.model.SubtitleRef
import org.app.kmediaplayer.settings.ObservableSettings
import org.app.kmediaplayer.settings.PlayerKeys
import org.app.maxplayerpro.sharedlogic.model.StorageSummary
import org.app.maxplayerpro.sharedlogic.platform.requestConsent
import org.app.maxplayerpro.sharedlogic.scan.MediaFileWalker
import org.app.maxplayerpro.sharedlogic.scan.StorageVolumes
import org.app.maxplayerpro.sharedlogic.settings.AppKeys
import java.io.File

/** Media library read from MediaStore. */
class AndroidMediaRepository(
    private val context: Context,
    private val settings: ObservableSettings,
    private val userData: UserDataRepository,
) : MediaRepository {
    private val _videos = MutableStateFlow<List<MediaFile>>(emptyList())
    override val videos: StateFlow<List<MediaFile>> = _videos.asStateFlow()
    private val _audio = MutableStateFlow<List<MediaFile>>(emptyList())
    override val audio: StateFlow<List<MediaFile>> = _audio.asStateFlow()
    private val _noMediaDirs = MutableStateFlow<Set<String>>(emptySet())
    override val noMediaDirs: StateFlow<Set<String>> = _noMediaDirs.asStateFlow()
    private val walker = MediaFileWalker(skipFolderNames = setOf("Android", "LOST.DIR"))
    private val _loading = MutableStateFlow(false)
    override val loading: StateFlow<Boolean> = _loading.asStateFlow()

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        _loading.value = true
        try {
            val userExtra = settings[AppKeys.fileExtensions]
                .split(',').map { it.trim().removePrefix(".").lowercase() }.filter { it.isNotEmpty() }.toSet()
            // MediaStore leaves WMV, VOB, RMVB... out of its video table; with All files access we find them by name.
            val extra = if (hasAllFilesAccess()) userExtra + MediaFormats.legacyVideoExtensions else userExtra
            val roots = settings[AppKeys.scanFolders]
                .split('\n').map { it.trim().trimEnd('/') }.filter { it.isNotEmpty() }
            fun inRoots(f: MediaFile) = roots.isEmpty() || roots.any { f.path.startsWith("$it/") }
            val v = queryVideos().filter(::inRoots) + queryExtraFiles(extra).filter(::inRoots)
            _videos.value = v.distinctBy { it.path }
            _audio.value = queryAudio().filter(::inRoots)
            // Like MX Player: with All files access, also walk every volume (internal, SD card, USB pen
            // drive) for files the media scanner has not indexed. MediaStore results show first.
            if (hasAllFilesAccess()) {
                val (walkedAudio, walkedVideo) = StorageVolumes.directories(context)
                    .flatMap { walker.walk(it) }.filter(::inRoots).partition { it.isAudio }
                _videos.value = (_videos.value + walkedVideo).distinctBy { it.path }
                _audio.value = (_audio.value + walkedAudio).distinctBy { it.path }
            }
            _noMediaDirs.value = findNoMediaDirs((_videos.value + _audio.value).map { it.folderPath }.toSet())
        } finally {
            _loading.value = false
        }
    }

    private fun queryVideos(): List<MediaFile> {
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val proj = arrayOf(
            MediaStore.Video.Media._ID, MediaStore.Video.Media.DATA, MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE, MediaStore.Video.Media.DURATION, MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.WIDTH, MediaStore.Video.Media.HEIGHT, MediaStore.Video.Media.MIME_TYPE,
        )
        val out = mutableListOf<MediaFile>()
        context.contentResolver.query(uri, proj, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val path = c.getString(1) ?: continue
                out += MediaFile(
                    id = id, uri = ContentUris.withAppendedId(uri, id).toString(), path = path,
                    name = c.getString(2) ?: path.substringAfterLast('/'), sizeBytes = c.getLong(3),
                    durationMs = c.getLong(4), dateModifiedMs = c.getLong(5) * 1000,
                    width = c.getInt(6), height = c.getInt(7), mimeType = c.getString(8) ?: "",
                )
            }
        }
        return out
    }

    private fun queryAudio(): List<MediaFile> {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val proj = arrayOf(
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
        )
        val out = mutableListOf<MediaFile>()
        context.contentResolver.query(uri, proj, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val path = c.getString(1) ?: continue
                out += MediaFile(
                    id = id, uri = ContentUris.withAppendedId(uri, id).toString(), path = path,
                    name = c.getString(2) ?: path.substringAfterLast('/'), sizeBytes = c.getLong(3),
                    durationMs = c.getLong(4), dateModifiedMs = c.getLong(5) * 1000,
                    mimeType = c.getString(6) ?: "", isAudio = true,
                    artist = c.getString(7)?.takeIf { it != "<unknown>" } ?: "Unknown",
                    album = c.getString(8) ?: "Unknown",
                )
            }
        }
        return out
    }

    /** Files with user-added extensions that MediaStore does not classify as video. */
    private fun queryExtraFiles(extensions: Set<String>): List<MediaFile> {
        if (extensions.isEmpty()) return emptyList()
        val uri = MediaStore.Files.getContentUri("external")
        val selection = extensions.joinToString(" OR ") { "${MediaStore.Files.FileColumns.DATA} LIKE ?" }
        val args = extensions.map { "%.$it" }.toTypedArray()
        val proj = arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.SIZE, MediaStore.Files.FileColumns.DATE_MODIFIED)
        val out = mutableListOf<MediaFile>()
        context.contentResolver.query(uri, proj, selection, args, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val path = c.getString(1) ?: continue
                out += MediaFile(id, ContentUris.withAppendedId(uri, id).toString(), path, path.substringAfterLast('/'), c.getLong(2), 0, c.getLong(3) * 1000)
            }
        }
        return out
    }

    private fun hasAllFilesAccess(): Boolean = Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager()

    private fun findNoMediaDirs(folders: Set<String>): Set<String> {
        val candidates = mutableSetOf<String>()
        folders.forEach { f ->
            var p = f
            while (p.count { it == '/' } > 1) { candidates += p; p = p.substringBeforeLast('/') }
        }
        return candidates.filter { File(it, ".nomedia").exists() }.toSet()
    }

    override fun findByUri(uri: String): MediaFile? = _videos.value.firstOrNull { it.uri == uri } ?: _audio.value.firstOrNull { it.uri == uri }

    private fun subtitleFiles(file: MediaFile): List<File> {
        val dirs = listOfNotNull(File(file.folderPath), settings[PlayerKeys.subtitleFolder].takeIf { it.isNotBlank() }?.let(::File))
        return dirs.flatMap { d ->
            d.listFiles { f -> f.nameWithoutExtension.startsWith(file.title) && f.extension.lowercase() in setOf("srt", "vtt", "ass", "ssa", "sub") }?.toList().orEmpty()
        }
    }

    override fun subtitleFilesFor(file: MediaFile): List<SubtitleRef> =
        subtitleFiles(file).filter { it.canRead() }.map { SubtitleRef(Uri.fromFile(it).toString(), it.name) }

    override fun folderFileNames(folders: Collection<String>): Set<String> =
        folders.flatMap { d -> File(d).list()?.toList().orEmpty() }.toSet()

    /** On Android 11+ deleting other apps' media needs user consent. */
    private fun deleteRequest(files: List<MediaFile>): IntentSender? =
        if (Build.VERSION.SDK_INT >= 30) MediaStore.createDeleteRequest(context.contentResolver, files.map { Uri.parse(it.uri) }).intentSender else null

    private fun writeRequest(files: List<MediaFile>): IntentSender? =
        if (Build.VERSION.SDK_INT >= 30) MediaStore.createWriteRequest(context.contentResolver, files.map { Uri.parse(it.uri) }).intentSender else null

    override suspend fun delete(files: List<MediaFile>, withSubtitles: Boolean): Boolean {
        val sender = deleteRequest(files)
        if (sender != null && !requestConsent(sender)) return false
        withContext(Dispatchers.IO) {
            files.forEach { f ->
                if (sender == null) runCatching { context.contentResolver.delete(Uri.parse(f.uri), null, null) }
                if (withSubtitles) subtitleFiles(f).forEach { it.delete() }
                userData.update { it.forgetFile(f.uri) }
            }
        }
        refresh()
        return true
    }

    override suspend fun rename(file: MediaFile, newName: String): Boolean {
        val sender = writeRequest(listOf(file))
        if (sender != null && !requestConsent(sender)) return false
        val ok = withContext(Dispatchers.IO) {
            runCatching {
                val values = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, newName) }
                context.contentResolver.update(Uri.parse(file.uri), values, null, null) > 0
            }.getOrDefault(false)
        }
        if (ok) refresh()
        return ok
    }

    override suspend fun storageSummary(): StorageSummary = withContext(Dispatchers.IO) {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        val total = stat.totalBytes
        val used = total - stat.availableBytes
        StorageSummary(used, total, _videos.value.sumOf { it.sizeBytes }, _audio.value.sumOf { it.sizeBytes }, imageBytes())
    }

    private fun imageBytes(): Long {
        var sum = 0L
        context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.Media.SIZE), null, null, null)?.use { c ->
            while (c.moveToNext()) sum += c.getLong(0)
        }
        return sum
    }
}
