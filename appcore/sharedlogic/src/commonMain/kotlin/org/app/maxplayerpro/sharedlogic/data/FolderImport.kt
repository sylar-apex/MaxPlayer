package org.app.maxplayerpro.sharedlogic.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** A folder the user added to the library (the web, where the app cannot scan storage itself). */
data class LibrarySource(val id: String, val name: String, val status: SourceStatus, val videoCount: Int = 0)

enum class SourceStatus {
    /** Files are listed and playable. */
    Ready,
    Scanning,
    /** Remembered, but the browser wants the user to allow access again (one click). */
    NeedsPermission,
    /** Only the name is remembered (browsers that cannot keep folder access); pick it again. */
    NeedsRepick,
    /** The folder was moved, renamed or deleted, or its drive is disconnected. */
    Missing,
}

/**
 * Lets the user add folders to the library on platforms where the app cannot scan storage itself
 * (the browser), and remembers them between visits where the browser allows it. Native apps scan
 * every drive on their own and use [None].
 */
interface FolderImport {
    val available: Boolean

    /** Folders added so far, including remembered ones waiting to be reconnected. */
    val sources: StateFlow<List<LibrarySource>>

    /** Opens the platform's folder picker; picked media lands in the [MediaRepository]. */
    fun pickFolder()

    /** Asks the browser again for every folder waiting for permission. Call from a click. */
    fun restoreAccess() {}

    /** Removes a folder from the library and stops remembering it. */
    fun forget(id: String) {}

    object None : FolderImport {
        override val available = false
        override val sources: StateFlow<List<LibrarySource>> = MutableStateFlow(emptyList())
        override fun pickFolder() {}
    }
}
