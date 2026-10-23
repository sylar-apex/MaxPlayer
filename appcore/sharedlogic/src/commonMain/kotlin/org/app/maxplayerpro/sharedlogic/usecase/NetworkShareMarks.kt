package org.app.maxplayerpro.sharedlogic.usecase

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What is shared on the network right now: folder paths and file URIs. Empty while sharing is off. */
data class ShareMarks(val folders: Set<String> = emptySet(), val files: Set<String> = emptySet())

/** Lets library lists mark shared folders and files, and make one private again from the mark. */
interface NetworkShareMarks {
    val marks: StateFlow<ShareMarks>
    fun makeFolderPrivate(path: String)
    /** Stops sharing this one file, even when it is shared as part of a folder. */
    fun makeFilePrivate(uri: String)

    /** Platforms without sharing. */
    object None : NetworkShareMarks {
        override val marks: StateFlow<ShareMarks> = MutableStateFlow(ShareMarks()).asStateFlow()
        override fun makeFolderPrivate(path: String) {}
        override fun makeFilePrivate(uri: String) {}
    }
}
