package org.app.maxplayerpro.sharedlogic.data

import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlin.js.Promise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.app.kmediaplayer.model.MediaFile
import org.app.kmediaplayer.model.MediaFormats
import org.app.kmediaplayer.model.SubtitleRef
import org.app.kmediaplayer.model.fileExtension
import org.app.kmediaplayer.web.WebBlobStore
import org.app.maxplayerpro.sharedlogic.model.StorageSummary
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.url.URL
import org.w3c.files.File
import org.w3c.files.get

/**
 * The browser library: browsers cannot read the disk on their own, so the user picks a folder and
 * every video and audio file inside it, in all subfolders, is added. Files play through blob URLs
 * for as long as the page is open.
 */
class WebMediaRepository : MediaRepository, FolderImport {
    private val scope = MainScope()
    private val _videos = MutableStateFlow<List<MediaFile>>(emptyList())
    override val videos: StateFlow<List<MediaFile>> = _videos.asStateFlow()
    private val _audio = MutableStateFlow<List<MediaFile>>(emptyList())
    override val audio: StateFlow<List<MediaFile>> = _audio.asStateFlow()
    override val noMediaDirs: StateFlow<Set<String>> = MutableStateFlow(emptySet())
    private val _loading = MutableStateFlow(false)
    override val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** Subtitle files picked alongside the media, by folder path. */
    private val subtitles = mutableMapOf<String, MutableList<SubtitleRef>>()

    override val available = true

    /**
     * Chrome and Edge get the File System Access picker, whose prompt says the site may *view* the
     * folder (accurate: nothing leaves the device). Other browsers fall back to a folder input, whose
     * browser-owned prompt talks about "uploading" even though files are only read locally.
     */
    override fun pickFolder() {
        if (!hasDirectoryPicker()) return pickWithInput()
        scope.launch {
            val picked = runCatching { pickDirectory().await<JsArray<JsAny>>() }.getOrNull() ?: return@launch // cancelled
            add((0 until picked.length).map { i -> entryFile(picked, i) to entryPath(picked, i) })
        }
    }

    private fun pickWithInput() {
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.multiple = true
        // Chromium, Firefox and Safari all support picking a whole folder this way.
        input.setAttribute("webkitdirectory", "")
        input.onchange = {
            val list = input.files
            if (list != null) add((0 until list.length).mapNotNull { list[it] }.map { it to relativePath(it).ifEmpty { it.name } })
            null
        }
        input.click()
    }

    /** Adds picked files; each comes with its path inside the picked folder, e.g. `Movies/S01/E01.mkv`. */
    private fun add(files: List<Pair<File, String>>) {
        _loading.value = true
        val media = mutableListOf<MediaFile>()
        for ((f, relative) in files) {
            val path = "/$relative"
            val ext = f.name.fileExtension()
            when {
                ext in SUBTITLE_EXTENSIONS -> {
                    val url = URL.createObjectURL(f)
                    subtitles.getOrPut(path.substringBeforeLast('/')) { mutableListOf() }.add(SubtitleRef(url, f.name))
                    // The player parses subtitles synchronously; read the (small) file now.
                    scope.launch { WebBlobStore.put(url, WebBlobStore.readAll(f)) }
                }
                ext in MediaFormats.videoExtensions || ext in MediaFormats.audioExtensions -> {
                    val audio = ext in MediaFormats.audioExtensions
                    media += MediaFile(
                        id = path.hashCode().toLong(), uri = URL.createObjectURL(f), path = path, name = f.name,
                        sizeBytes = f.size.toDouble().toLong(), durationMs = 0, dateModifiedMs = f.lastModified.toDouble().toLong(),
                        isAudio = audio, artist = if (audio) "Unknown" else "", album = if (audio) "Unknown" else "",
                    )
                }
            }
        }
        val (audio, video) = media.partition { it.isAudio }
        _videos.value = (_videos.value + video).distinctBy { it.path }
        _audio.value = (_audio.value + audio).distinctBy { it.path }
        _loading.value = false
    }

    override suspend fun refresh() {}
    override fun findByUri(uri: String) = (_videos.value + _audio.value).firstOrNull { it.uri == uri }
    override fun subtitleFilesFor(file: MediaFile): List<SubtitleRef> =
        subtitles[file.folderPath].orEmpty().filter { it.name.startsWith(file.title) }
    override fun folderFileNames(folders: Collection<String>): Set<String> =
        folders.flatMap { subtitles[it].orEmpty().map(SubtitleRef::name) }.toSet()
    override suspend fun delete(files: List<MediaFile>, withSubtitles: Boolean) = false
    override suspend fun rename(file: MediaFile, newName: String) = false
    override suspend fun storageSummary(): StorageSummary? = null

    private companion object {
        val SUBTITLE_EXTENSIONS = setOf("srt", "vtt", "ass", "ssa")
    }
}

/** The file's path inside the picked folder, e.g. `Movies/Series/S01/E01.mkv`. */
@JsFun("(f) => f.webkitRelativePath || ''")
private external fun relativePath(file: File): String

@JsFun("() => typeof window.showDirectoryPicker === 'function'")
private external fun hasDirectoryPicker(): Boolean

/** Walks the picked folder and every subfolder; resolves to `[{file, path}]`, rejects if cancelled. */
@JsFun("""() => (async () => {
    const root = await window.showDirectoryPicker({ mode: 'read' });
    const out = [];
    async function walk(dir, prefix) {
        for await (const [name, handle] of dir.entries()) {
            if (name.startsWith('.')) continue;
            if (handle.kind === 'file') out.push({ file: await handle.getFile(), path: prefix + name });
            else await walk(handle, prefix + name + '/');
        }
    }
    await walk(root, root.name + '/');
    return out;
})()""")
private external fun pickDirectory(): Promise<JsArray<JsAny>>

@JsFun("(a, i) => a[i].file")
private external fun entryFile(entries: JsArray<JsAny>, index: Int): File

@JsFun("(a, i) => a[i].path")
private external fun entryPath(entries: JsArray<JsAny>, index: Int): String
