package org.app.nxplayer.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.app.nxplayer.core.MediaFile
import java.io.File

data class StorageSummary(val usedBytes: Long, val totalBytes: Long, val videoBytes: Long, val audioBytes: Long, val imageBytes: Long)

class MediaRepository(private val context: Context) {
    private val _videos = MutableStateFlow<List<MediaFile>>(emptyList())
    val videos: StateFlow<List<MediaFile>> = _videos.asStateFlow()
    private val _audio = MutableStateFlow<List<MediaFile>>(emptyList())
    val audio: StateFlow<List<MediaFile>> = _audio.asStateFlow()
    private val _noMediaDirs = MutableStateFlow<Set<String>>(emptySet())
    val noMediaDirs: StateFlow<Set<String>> = _noMediaDirs.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val thumbs = object : LruCache<Long, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: Long, value: Bitmap) = value.byteCount
    }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        _loading.value = true
        try {
            val extra = AppGraph.settings[org.app.nxplayer.core.Keys.fileExtensions]
                .split(',').map { it.trim().removePrefix(".").lowercase() }.filter { it.isNotEmpty() }.toSet()
            val roots = AppGraph.settings[org.app.nxplayer.core.Keys.scanFolders]
                .split('\n').map { it.trim().trimEnd('/') }.filter { it.isNotEmpty() }
            fun inRoots(f: MediaFile) = roots.isEmpty() || roots.any { f.path.startsWith("$it/") }
            val v = queryVideos().filter(::inRoots) + queryExtraFiles(extra).filter(::inRoots)
            _videos.value = v.distinctBy { it.path }
            _audio.value = queryAudio().filter(::inRoots)
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

    private fun findNoMediaDirs(folders: Set<String>): Set<String> {
        val candidates = mutableSetOf<String>()
        folders.forEach { f ->
            var p = f
            while (p.count { it == '/' } > 1) { candidates += p; p = p.substringBeforeLast('/') }
        }
        return candidates.filter { File(it, ".nomedia").exists() }.toSet()
    }

    suspend fun thumbnail(file: MediaFile, px: Int = 320): Bitmap? = withContext(Dispatchers.IO) {
        thumbs.get(file.id)?.let { return@withContext it }
        val bmp = runCatching {
            if (Build.VERSION.SDK_INT >= 29) context.contentResolver.loadThumbnail(Uri.parse(file.uri), Size(px, px * 9 / 16), null)
            else @Suppress("DEPRECATION") MediaStore.Video.Thumbnails.getThumbnail(context.contentResolver, file.id, MediaStore.Video.Thumbnails.MINI_KIND, null)
        }.getOrNull()
        if (bmp != null && AppGraph.settings[org.app.nxplayer.core.Keys.cacheThumbnail]) thumbs.put(file.id, bmp)
        bmp
    }

    fun clearThumbnailCache() = thumbs.evictAll()

    /** On Android 11+ deleting other apps' media needs user consent; returns the consent request in that case. */
    fun deleteRequest(files: List<MediaFile>): IntentSender? =
        if (Build.VERSION.SDK_INT >= 30) MediaStore.createDeleteRequest(context.contentResolver, files.map { Uri.parse(it.uri) }).intentSender else null

    fun writeRequest(files: List<MediaFile>): IntentSender? =
        if (Build.VERSION.SDK_INT >= 30) MediaStore.createWriteRequest(context.contentResolver, files.map { Uri.parse(it.uri) }).intentSender else null

    /** Deletes directly (pre-Android 11, or after consent was granted). */
    suspend fun deleteNow(files: List<MediaFile>, withSubtitles: Boolean) = withContext(Dispatchers.IO) {
        files.forEach { f ->
            runCatching { context.contentResolver.delete(Uri.parse(f.uri), null, null) }
            if (withSubtitles) subtitleFilesFor(f).forEach { it.delete() }
            AppGraph.userData.update { it.forgetFile(f.uri) }
        }
        refresh()
    }

    suspend fun renameNow(file: MediaFile, newName: String): Boolean = withContext(Dispatchers.IO) {
        val ok = runCatching {
            val values = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, newName) }
            context.contentResolver.update(Uri.parse(file.uri), values, null, null) > 0
        }.getOrDefault(false)
        if (ok) refresh()
        ok
    }

    fun subtitleFilesFor(file: MediaFile): List<File> {
        val dirs = listOfNotNull(File(file.folderPath), AppGraph.settings[org.app.nxplayer.core.Keys.subtitleFolder].takeIf { it.isNotBlank() }?.let(::File))
        return dirs.flatMap { d ->
            d.listFiles { f -> f.nameWithoutExtension.startsWith(file.title) && f.extension.lowercase() in setOf("srt", "vtt", "ass", "ssa", "sub") }?.toList().orEmpty()
        }
    }

    fun storageSummary(): StorageSummary {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        val total = stat.totalBytes
        val used = total - stat.availableBytes
        return StorageSummary(used, total, _videos.value.sumOf { it.sizeBytes }, _audio.value.sumOf { it.sizeBytes }, imageBytes())
    }

    private fun imageBytes(): Long {
        var sum = 0L
        context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.Media.SIZE), null, null, null)?.use { c ->
            while (c.moveToNext()) sum += c.getLong(0)
        }
        return sum
    }

    fun findByUri(uri: String): MediaFile? = _videos.value.firstOrNull { it.uri == uri } ?: _audio.value.firstOrNull { it.uri == uri }
}
