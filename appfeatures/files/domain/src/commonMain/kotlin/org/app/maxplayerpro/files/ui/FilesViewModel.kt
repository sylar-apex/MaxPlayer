package org.app.maxplayerpro.files.ui

import androidx.lifecycle.viewModelScope
import com.psbapp.mvi.MVIBaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.app.kmediaplayer.model.MediaFile
import org.app.maxplayerpro.sharedlogic.data.FileBrowser
import org.app.maxplayerpro.sharedlogic.data.FileEntry
import org.app.maxplayerpro.sharedlogic.data.fileMime
import org.app.maxplayerpro.sharedlogic.usecase.MediaOperations
import org.app.maxplayerpro.sharedlogic.usecase.NetworkShareRequests
import org.app.maxplayerpro.sharedlogic.usecase.ShareTarget

/** The File manager: storage roots, then folders and files; any of them can be shared on the network. */
class FilesViewModel(private val browser: FileBrowser, private val operations: MediaOperations) :
    MVIBaseViewModel<FilesContract.Event, FilesContract.State, FilesContract.Effect>() {

    override fun createInitialState() = FilesContract.State()
    override fun handleEffect(effect: FilesContract.Effect) {}

    init { reload() }

    override fun handleEvent(event: FilesContract.Event) {
        when (event) {
            is FilesContract.Event.OpenFolder -> { updateState { copy(path = event.path) }; reload() }
            FilesContract.Event.Up -> {
                val path = currentState.path ?: return
                // Above a root goes back to the list of roots.
                val parent = if (currentState.roots.any { it.path == path }) null else path.substringBeforeLast('/', "").ifEmpty { null }
                updateState { copy(path = parent) }
                reload()
            }
            FilesContract.Event.Refresh -> reload()
            FilesContract.Event.RequestAccess -> browser.requestAccess()
            is FilesContract.Event.Play -> {
                val media = currentState.entries.filter { !it.isDirectory && (fileMime(it.name).startsWith("video/") || fileMime(it.name).startsWith("audio/")) }
                val files = media.map { it.toMediaFile() }
                val i = media.indexOfFirst { it.path == event.entry.path }.coerceAtLeast(0)
                if (fileMime(event.entry.name).startsWith("audio/")) operations.playMusic(files, i) else operations.play(files, i)
            }
            is FilesContract.Event.Share -> viewModelScope.launch {
                val targets = withContext(Dispatchers.Default) {
                    val loose = event.entries.filter { !it.isDirectory }.map { it.toMediaFile() }
                    // A folder is shared with everything under it; sub-paths go into the names (" › " keeps links intact).
                    val folders = event.entries.filter { it.isDirectory }.map { dir ->
                        ShareTarget(browser.walk(dir.path).map { f -> f.toMediaFile(name = f.path.removePrefix(dir.path + "/").replace("/", " › ")) }, dir.path)
                    }
                    folders + listOfNotNull(loose.takeIf { it.isNotEmpty() }?.let { ShareTarget(it) })
                }
                if (targets.all { it.files.isEmpty() }) operations.toast("Nothing to share in that folder") else NetworkShareRequests.request(targets)
            }
        }
    }

    private fun reload() {
        viewModelScope.launch {
            updateState { copy(loading = true, hasAccess = browser.hasAccess) }
            val roots = browser.roots()
            val path = currentState.path
            val entries = if (path == null) emptyList() else withContext(Dispatchers.Default) { browser.list(path) }
            updateState { copy(roots = roots, entries = entries, loading = false) }
        }
    }

    private fun FileEntry.toMediaFile(name: String = this.name): MediaFile {
        val mime = fileMime(this.name)
        return MediaFile(path.hashCode().toLong(), "file://$path", path, name, sizeBytes, 0, modifiedMs, mimeType = mime, isAudio = mime.startsWith("audio/"))
    }
}
