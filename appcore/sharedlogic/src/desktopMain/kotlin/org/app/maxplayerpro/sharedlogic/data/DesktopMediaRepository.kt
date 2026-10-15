package org.app.maxplayerpro.sharedlogic.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.app.kmediaplayer.model.MediaFile
import org.app.kmediaplayer.model.SubtitleRef
import org.app.maxplayerpro.sharedlogic.model.StorageSummary
import org.app.kmediaplayer.desktop.VlcMediaProbe
import org.app.maxplayerpro.sharedlogic.scan.MediaFileWalker
import org.app.maxplayerpro.sharedlogic.scan.MetadataCache
import org.app.maxplayerpro.sharedlogic.scan.withInfo
import org.app.maxplayerpro.sharedlogic.scan.StorageRoot
import org.app.maxplayerpro.sharedlogic.scan.StorageRoots
import java.io.File
import kotlin.coroutines.coroutineContext

/**
 * Desktop media library: walks the home folder and every mounted drive, each drive in parallel,
 * showing each drive's videos as soon as that drive is done. [watchDrives] rescans when a drive is
 * plugged in or removed. Length, resolution and frame rate are then read from each file's header
 * (cached on disk), so sorting and the list fields work as on Android.
 */
class DesktopMediaRepository : MediaRepository {
    private val _videos = MutableStateFlow<List<MediaFile>>(emptyList())
    override val videos: StateFlow<List<MediaFile>> = _videos.asStateFlow()
    private val _audio = MutableStateFlow<List<MediaFile>>(emptyList())
    override val audio: StateFlow<List<MediaFile>> = _audio.asStateFlow()
    override val noMediaDirs: StateFlow<Set<String>> = MutableStateFlow(emptySet())
    private val _loading = MutableStateFlow(false)
    override val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val walker = MediaFileWalker(StorageRoots.skipFolderNames)
    private val scanLock = Mutex()
    private val metadata = MetadataCache()

    override suspend fun refresh() = scanLock.withLock {
        _loading.value = true
        try {
            val roots = StorageRoots.current()
            val byRoot = mutableMapOf<String, List<MediaFile>>()
            coroutineScope {
                roots.map { root ->
                    async(Dispatchers.IO) {
                        val files = walker.walk(File(root.path)) { !isActive }
                        synchronized(byRoot) { byRoot[root.path] = files; publish(byRoot.values.flatten()) }
                    }
                }.awaitAll()
            }
            publish(byRoot.values.flatten())
        } finally {
            _loading.value = false
        }
        enrich()
    }

    /** Fills in length, resolution and frame rate: cached values at once, then probes the rest in batches. */
    private suspend fun enrich() = withContext(Dispatchers.IO) {
        if (!VlcMediaProbe.isAvailable) return@withContext
        replaceAll { f -> metadata.get(f)?.let(f::withInfo) ?: f }
        val missing = (_videos.value + _audio.value).filter { it.durationMs == 0L }
        val probed = mutableMapOf<String, MediaFile>()
        for (f in missing) {
            if (!coroutineContext.isActive) break
            val info = VlcMediaProbe.probe(f.path) ?: continue
            metadata.put(f, info)
            probed[f.path] = f.withInfo(info)
            if (probed.size % 25 == 0) replaceAll { probed[it.path] ?: it }
        }
        replaceAll { probed[it.path] ?: it }
        metadata.save()
    }

    private fun replaceAll(change: (MediaFile) -> MediaFile) = refreshLists { list -> list.map(change) }

    /** Polls the mounted drives and rescans when one appears or disappears. Runs until cancelled. */
    suspend fun watchDrives(intervalMs: Long = 3_000) {
        var known: List<StorageRoot> = withContext(Dispatchers.IO) { StorageRoots.current() }
        while (coroutineContext.isActive) {
            delay(intervalMs)
            val now = withContext(Dispatchers.IO) { StorageRoots.current() }
            if (now != known) { known = now; refresh() }
        }
    }

    private fun publish(all: List<MediaFile>) {
        val unique = all.distinctBy { it.path }
        _videos.value = unique.filterNot { it.isAudio }
        _audio.value = unique.filter { it.isAudio }
    }

    override fun findByUri(uri: String) = (_videos.value + _audio.value).firstOrNull { it.uri == uri }

    override fun subtitleFilesFor(file: MediaFile): List<SubtitleRef> {
        val dir = File(file.path).parentFile ?: return emptyList()
        return dir.listFiles { f -> f.nameWithoutExtension.startsWith(file.title) && f.extension.lowercase() in SUBTITLE_EXTENSIONS }
            .orEmpty().map { SubtitleRef(it.toURI().toString(), it.name) }
    }

    override fun folderFileNames(folders: Collection<String>): Set<String> =
        folders.flatMap { File(it).list()?.toList().orEmpty() }.toSet()

    override suspend fun delete(files: List<MediaFile>, withSubtitles: Boolean): Boolean = withContext(Dispatchers.IO) {
        val ok = files.all { f ->
            if (withSubtitles) subtitleFilesFor(f).forEach { s -> runCatching { File(java.net.URI(s.uri)).delete() } }
            File(f.path).delete()
        }
        refreshLists { it.filterNot { m -> files.any { f -> f.path == m.path } } }
        ok
    }

    override suspend fun rename(file: MediaFile, newName: String): Boolean = withContext(Dispatchers.IO) {
        val target = File(File(file.path).parentFile, newName)
        val ok = !target.exists() && File(file.path).renameTo(target)
        if (ok) refreshLists { list -> list.map { if (it.path == file.path) it.copy(path = target.path, name = target.name, uri = target.toURI().toString()) else it } }
        ok
    }

    override suspend fun storageSummary(): StorageSummary? = null

    private fun refreshLists(change: (List<MediaFile>) -> List<MediaFile>) {
        _videos.value = change(_videos.value)
        _audio.value = change(_audio.value)
    }

    private companion object {
        val SUBTITLE_EXTENSIONS = setOf("srt", "vtt", "ass", "ssa", "sub")
    }
}
