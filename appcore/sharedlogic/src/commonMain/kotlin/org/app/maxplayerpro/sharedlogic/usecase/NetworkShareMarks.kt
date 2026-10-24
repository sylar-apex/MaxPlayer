package org.app.maxplayerpro.sharedlogic.usecase

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.app.kmediaplayer.model.MediaFile

/**
 * What is shared on the network right now: folder paths (shared folders and their shared subfolders) and file URIs.
 * [roots] are the shared folders themselves; anything under one that is not shared can be added to it. Empty while
 * sharing is off.
 */
data class ShareMarks(val folders: Set<String> = emptySet(), val files: Set<String> = emptySet(), val roots: Set<String> = emptySet()) {
    /** The shared folder [path] is inside, if any. */
    fun rootOf(path: String): String? = roots.filter { path.startsWith("$it/") }.maxByOrNull { it.length }
}

/** Lets library lists mark shared folders and files, and make one private again from the mark. */
interface NetworkShareMarks {
    val marks: StateFlow<ShareMarks>
    /** Stops sharing a shared folder, or one subfolder of it. */
    fun makeFolderPrivate(path: String)
    /** Shares [files] inside the shared folder they are in, with its settings. */
    fun addToSharedFolder(files: List<MediaFile>)
    /** Stops sharing this one file, even when it is shared as part of a folder. */
    fun makeFilePrivate(uri: String)

    /** Platforms without sharing. */
    object None : NetworkShareMarks {
        override val marks: StateFlow<ShareMarks> = MutableStateFlow(ShareMarks()).asStateFlow()
        override fun makeFolderPrivate(path: String) {}
        override fun addToSharedFolder(files: List<MediaFile>) {}
        override fun makeFilePrivate(uri: String) {}
    }
}
