package org.app.maxplayerpro.sharedlogic.data

import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.app.kmediaplayer.model.MediaFile
import org.app.kmediaplayer.model.MediaFormats
import org.app.kmediaplayer.model.SubtitleRef
import org.app.kmediaplayer.model.fileExtension
import org.app.kmediaplayer.web.WebBlobStore
import org.app.kmediaplayer.web.WebFiles
import org.app.maxplayerpro.sharedlogic.model.StorageSummary
import org.w3c.dom.HTMLInputElement
import org.w3c.files.File
import org.w3c.files.get

/**
 * The browser library. Browsers cannot read the disk on their own, so the user picks folders; every
 * video, audio and subtitle file inside them (all subfolders) is listed and played locally, never
 * uploaded.
 *
 * Folders are remembered between visits where the browser allows it:
 * - Chromium (Chrome, Edge, Opera): the folder handle is stored; on return the folder loads at once
 *   if access is still granted, otherwise one "Restore access" click brings it back.
 * - Other browsers: only folder names are stored; the user picks the folder again. Watch progress
 *   still matches because files keep the same stable ID (see [WebFiles]).
 */
class WebMediaRepository : MediaRepository, FolderImport {
    private val scope = MainScope()
    private val lock = Mutex()

    private val _videos = MutableStateFlow<List<MediaFile>>(emptyList())
    override val videos: StateFlow<List<MediaFile>> = _videos.asStateFlow()
    private val _audio = MutableStateFlow<List<MediaFile>>(emptyList())
    override val audio: StateFlow<List<MediaFile>> = _audio.asStateFlow()
    override val noMediaDirs: StateFlow<Set<String>> = MutableStateFlow(emptySet())
    private val _loading = MutableStateFlow(false)
    override val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _sources = MutableStateFlow<List<LibrarySource>>(emptyList())
    override val sources: StateFlow<List<LibrarySource>> = _sources.asStateFlow()

    /** Every browser can at least pick files; folders and remembering depend on the browser. */
    override val available = true

    /** One added folder. [handle] is null where the browser cannot keep folder access. */
    private class Source(val id: String, val name: String, val handle: JsAny?) {
        var status = SourceStatus.Scanning
        var media: List<MediaFile> = emptyList()
        var subtitles: Map<String, List<SubtitleRef>> = emptyMap()
    }

    private val folders = mutableListOf<Source>()

    init { scope.launch { restoreRemembered() } }

    // region remembering

    private suspend fun restoreRemembered() = lock.withLock {
        if (WebFolderAccess.canRememberFolders) {
            val saved = loadRememberedFolders().await<JsArray<JsAny>>()
            for (i in 0 until saved.length) folders += Source(rememberedId(saved, i), rememberedName(saved, i), rememberedHandle(saved, i))
            publish()
            folders.forEach { checkAndScan(it) }
        } else {
            rememberedNames().forEach { name -> folders += Source(name, name, null).apply { status = SourceStatus.NeedsRepick } }
            publish()
        }
    }

    private fun rememberedNames(): List<String> = loadFolderNames().split('\n').filter { it.isNotBlank() }

    private fun saveNames() = saveFolderNames(folders.filter { it.handle == null }.map { it.name }.distinct().joinToString("\n"))

    // endregion

    // region scanning

    /** Scans [source] when access is already granted; otherwise marks it for a one-click restore. */
    private suspend fun checkAndScan(source: Source) {
        val handle = source.handle ?: return
        when (queryPermission(handle).await<JsString>().toString()) {
            "granted" -> scan(source)
            else -> { source.status = SourceStatus.NeedsPermission; publish() }
        }
    }

    private suspend fun scan(source: Source) {
        val handle = source.handle ?: return
        source.status = SourceStatus.Scanning
        publish()
        val result = walkHandle(handle).await<JsAny>()
        when (val error = walkError(result)) {
            "" -> { ingest(source, (0 until walkCount(result)).map { walkFile(result, it) to walkPath(result, it) }); source.status = SourceStatus.Ready }
            "NotAllowedError", "SecurityError" -> source.status = SourceStatus.NeedsPermission
            else -> { source.status = SourceStatus.Missing; source.media = emptyList(); source.subtitles = emptyMap(); println("Folder ${source.name}: $error") }
        }
        publish()
    }

    /** Lists [files] (with their paths inside the folder) under stable IDs. */
    private fun ingest(source: Source, files: List<Pair<File, String>>) {
        val media = mutableListOf<MediaFile>()
        val subtitles = mutableMapOf<String, MutableList<SubtitleRef>>()
        for ((file, relative) in files) {
            val ext = file.name.fileExtension()
            val path = "/$relative"
            val id = WebFiles.stableId(relative)
            when {
                ext in SUBTITLE_EXTENSIONS -> {
                    WebFiles.register(id, file)
                    subtitles.getOrPut(path.substringBeforeLast('/')) { mutableListOf() } += SubtitleRef(id, file.name)
                    // The player parses subtitles synchronously; read the (small) file now.
                    scope.launch { runCatching { WebBlobStore.put(id, WebBlobStore.readAll(file)) } }
                }
                ext in MediaFormats.videoExtensions || ext in MediaFormats.audioExtensions -> {
                    WebFiles.register(id, file)
                    val audio = ext in MediaFormats.audioExtensions
                    media += MediaFile(
                        id = id.hashCode().toLong(), uri = id, path = path, name = file.name,
                        sizeBytes = file.size.toDouble().toLong(), durationMs = 0, dateModifiedMs = file.lastModified.toDouble().toLong(),
                        isAudio = audio, artist = if (audio) "Unknown" else "", album = if (audio) "Unknown" else "",
                    )
                }
            }
        }
        source.media = media
        source.subtitles = subtitles
    }

    /** Pushes the merged library, folder states and loading flag; frees URLs of files no longer listed. */
    private fun publish() {
        val ready = folders.filter { it.status == SourceStatus.Ready }
        val all = ready.flatMap { it.media }.distinctBy { it.uri }
        _videos.value = all.filterNot { it.isAudio }
        _audio.value = all.filter { it.isAudio }
        _loading.value = folders.any { it.status == SourceStatus.Scanning }
        _sources.value = folders.map { LibrarySource(it.id, it.name, it.status, it.media.count { m -> !m.isAudio }) }
        WebFiles.retainOnly(all.map { it.uri }.toSet() + ready.flatMap { s -> s.subtitles.values.flatten().map { it.uri } })
    }

    // endregion

    // region user actions

    override fun pickFolder() {
        if (WebFolderAccess.canRememberFolders) scope.launch { pickRememberedFolder() } else pickWithInput()
    }

    /** Chromium: pick a folder handle, remember it, scan it. Picking a folder already added rescans it. */
    private suspend fun pickRememberedFolder() {
        val handle = pickHandle().await<JsAny?>() ?: return // cancelled
        lock.withLock {
            val existing = folders.firstOrNull { f -> f.handle != null && isSameEntry(f.handle, handle).await<JsBoolean>().toBoolean() }
            val source = existing ?: Source(newFolderId(), handleName(handle), handle).also { folders += it }
            if (existing == null) rememberFolder(source.id, source.name, handle).await<JsBoolean>()
            scan(source)
        }
    }

    /**
     * Other browsers: a folder input (or, where even that is missing, a multi-file input). Access
     * lasts for this visit; the folder name is remembered so the user is asked to pick it again.
     */
    private fun pickWithInput() {
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.multiple = true
        if (supportsDirectoryInput()) input.setAttribute("webkitdirectory", "")
        input.onchange = {
            val list = input.files
            if (list != null && list.length > 0) {
                val files = (0 until list.length).mapNotNull { list[it] }.map { f -> f to relativePath(f).ifEmpty { f.name } }
                scope.launch { lock.withLock { addPickedFiles(files) } }
            }
            null
        }
        input.click()
    }

    private fun addPickedFiles(files: List<Pair<File, String>>) {
        // Files from a folder input share their top folder; plain file picks are grouped as "Files".
        val name = files.first().second.substringBefore('/', "Files").ifEmpty { "Files" }
        val relativeFiles = if (files.first().second.contains('/')) files else files.map { (f, p) -> f to "$name/$p" }
        val source = folders.firstOrNull { it.handle == null && it.name == name } ?: Source(name, name, null).also { folders += it }
        ingest(source, relativeFiles)
        source.status = SourceStatus.Ready
        saveNames()
        publish()
    }

    /**
     * Asks again for every folder waiting for permission. Browsers only show the prompt right after a
     * click, so this must be called from one; folders the user declines stay listed for next time.
     */
    override fun restoreAccess() {
        scope.launch {
            lock.withLock {
                for (source in folders.filter { it.status == SourceStatus.NeedsPermission }) {
                    val handle = source.handle ?: continue
                    if (requestPermission(handle).await<JsString>().toString() == "granted") scan(source)
                }
                if (folders.any { it.status == SourceStatus.NeedsRepick }) pickWithInput()
            }
        }
    }

    override fun forget(id: String) {
        scope.launch {
            lock.withLock {
                val source = folders.firstOrNull { it.id == id } ?: return@withLock
                folders -= source
                if (source.handle != null) forgetFolder(source.id).await<JsBoolean>() else saveNames()
                publish()
            }
        }
    }

    /** Rescans every reachable folder: picks up added and removed files, notices moved folders. */
    override suspend fun refresh() = lock.withLock {
        folders.filter { it.handle != null }.forEach { checkAndScan(it) }
        publish()
    }

    // endregion

    override fun findByUri(uri: String) = (_videos.value + _audio.value).firstOrNull { it.uri == uri }

    override fun subtitleFilesFor(file: MediaFile): List<SubtitleRef> =
        folders.flatMap { it.subtitles[file.folderPath].orEmpty() }.filter { it.name.startsWith(file.title) }

    override fun folderFileNames(folders: Collection<String>): Set<String> =
        this.folders.flatMap { s -> folders.flatMap { s.subtitles[it].orEmpty().map(SubtitleRef::name) } }.toSet()

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
