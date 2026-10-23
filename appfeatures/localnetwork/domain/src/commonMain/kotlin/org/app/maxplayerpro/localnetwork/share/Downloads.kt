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
)

/** Saves shared files from other devices. */
interface Downloader {
    val supported: Boolean
    val tasks: StateFlow<List<DownloadTask>>
    /** Label of the default folder, for the save sheet. */
    val defaultLabel: String
    fun start(url: String, name: String, sizeBytes: Long, mimeType: String, location: SaveLocation): String
    fun cancel(id: String)
    fun clearFinished()
}

/** Platforms that can't save files from here (browser, iOS for now). */
object NoDownloader : Downloader {
    override val supported = false
    override val tasks: StateFlow<List<DownloadTask>> = MutableStateFlow(emptyList<DownloadTask>()).asStateFlow()
    override val defaultLabel = ""
    override fun start(url: String, name: String, sizeBytes: Long, mimeType: String, location: SaveLocation) = ""
    override fun cancel(id: String) {}
    override fun clearFinished() {}
}
