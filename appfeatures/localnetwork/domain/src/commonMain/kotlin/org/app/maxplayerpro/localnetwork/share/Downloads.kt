package org.app.maxplayerpro.localnetwork.share

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where a download is saved: the device's Downloads folder, or a folder the user picked. */
sealed interface SaveLocation {
    data object Default : SaveLocation
    /** [id] is a path (desktop) or a granted document-tree URI (Android); [label] is what the user sees. */
    data class Folder(val id: String, val label: String) : SaveLocation
}

enum class DownloadState { Running, Done, Failed, Cancelled }

data class DownloadTask(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val doneBytes: Long = 0,
    val state: DownloadState = DownloadState.Running,
    /** Where it ended up, or why it failed. */
    val detail: String? = null,
    /** A folder download: how many files it has and how many are saved; 0 for a single file. */
    val files: Int = 0,
    val doneFiles: Int = 0,
)

/** One file of a folder download; [dir] is where it goes inside the saved folder (`a/b`), "" for its top. */
data class FolderFile(val url: String, val name: String, val dir: String, val sizeBytes: Long, val mimeType: String)

/** Saves shared files from other devices. */
interface Downloader {
    val supported: Boolean
    val tasks: StateFlow<List<DownloadTask>>
    /** Label of the default folder, for the save sheet. */
    val defaultLabel: String
    fun start(url: String, name: String, sizeBytes: Long, mimeType: String, location: SaveLocation): String
    /** Saves [files] into a new folder [name] at [location], keeping their subfolders; one task for all of them. */
    fun startFolder(name: String, files: List<FolderFile>, location: SaveLocation): String
    fun cancel(id: String)
    fun clearFinished()
}

/** Platforms that can't save files from here (browser, iOS for now). */
object NoDownloader : Downloader {
    override val supported = false
    override val tasks: StateFlow<List<DownloadTask>> = MutableStateFlow(emptyList<DownloadTask>()).asStateFlow()
    override val defaultLabel = ""
    override fun start(url: String, name: String, sizeBytes: Long, mimeType: String, location: SaveLocation) = ""
    override fun startFolder(name: String, files: List<FolderFile>, location: SaveLocation) = ""
    override fun cancel(id: String) {}
    override fun clearFinished() {}
}
